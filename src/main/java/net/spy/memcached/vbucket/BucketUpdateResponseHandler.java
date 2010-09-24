
package net.spy.memcached.vbucket;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@ChannelPipelineCoverage("one")
public class BucketUpdateResponseHandler extends SimpleChannelUpstreamHandler {

    private volatile boolean readingChunks;
    private String lastResponse;
    private ChannelFuture receivedFuture;
    private CountDownLatch latch;
    private StringBuilder partialResponse;
    private static final Logger LOGGER = Logger.getLogger(BucketUpdateResponseHandler.class.getName());

    @Override
    public void messageReceived(final ChannelHandlerContext context, final MessageEvent event) {
        ChannelFuture channelFuture = event.getFuture();
        setReceivedFuture(channelFuture);
        if (this.partialResponse == null) {
            this.partialResponse = new StringBuilder();
        }
        if (readingChunks) {
            HttpChunk chunk = (HttpChunk) event.getMessage();
            if (chunk.isLast()) {
                readingChunks = false;
            } else {
                String curChunk = chunk.getContent().toString("UTF-8");
                /*
                 * Server sends four new lines in a chunk as a sentinal between
                 * responses.
                 */
                if (curChunk.matches("\n\n\n\n")) {
                    setLastResponse(partialResponse.toString());
                    partialResponse = null;
                    getLatch().countDown();
                    if (monitor != null) {
                        monitor.invalidate();
                    }
                } else {
                    finerLog(curChunk);
                    finerLog("Chunk length is: " + curChunk.length());
                    partialResponse.append(curChunk);
                    channelFuture.setSuccess();
                }

            }
        } else {
            HttpResponse response = (HttpResponse) event.getMessage();
            logResponse(response);
        }
    }

    private void logResponse(HttpResponse response) {
        finerLog("STATUS: " + response.getStatus());
        finerLog("VERSION: " + response.getProtocolVersion());

        if (!response.getHeaderNames().isEmpty()) {
            for (String name : response.getHeaderNames()) {
                for (String value : response.getHeaders(name)) {
                    finerLog("HEADER: " + name + " = " + value);
                }
            }
            finerLog(System.getProperty("line.separator"));
        }

        if (response.getStatus().getCode() == 200 && response.isChunked()) {
            readingChunks = true;
            finerLog("CHUNKED CONTENT {");
        } else {
            ChannelBuffer content = response.getContent();
            if (content.readable()) {
                finerLog("CONTENT {");
                finerLog(content.toString("UTF-8"));
                finerLog("} END OF CONTENT");
            }
        }
    }

    /**
     * @return the lastResponse
     */
    protected String getLastResponse() {
        ChannelFuture channelFuture = getReceivedFuture();
        if (channelFuture.awaitUninterruptibly(30, TimeUnit.SECONDS)) {
            return lastResponse;
        } else { // TODO: make this work with multiple servers
            throw new ConnectionException("Cannot contact any server in the pool");
        }
    }

    /**
     * @param lastResponse the lastResponse to set
     */
    private void setLastResponse(String lastResponse) {
        this.lastResponse = lastResponse;
    }

    /**
     * @return the receivedFuture
     */
    private ChannelFuture getReceivedFuture() {
        try {
            getLatch().await();
        } catch (InterruptedException ex) {
            finerLog("Getting received future has been interrupted.");
        }
        return receivedFuture;
    }

    /**
     * @param receivedFuture the receivedFuture to set
     */
    private void setReceivedFuture(ChannelFuture receivedFuture) {
        this.receivedFuture = receivedFuture;
    }

    /**
     * @return the latch
     */
    private CountDownLatch getLatch() {
        if (this.latch == null) {
            latch = new CountDownLatch(1);
        }
        return latch;
    }

    private void finerLog(String message) {
        LOGGER.log(Level.FINER, message);
    }

    @Override
    public void handleUpstream(ChannelHandlerContext context, ChannelEvent event) throws Exception {

        if (event instanceof ChannelStateEvent) {
            LOGGER.log(Level.FINEST, "Channel state changed: " + event + "\n\n");
        }
        super.handleUpstream(context, event);
    }

    BucketMonitor monitor;

    protected void setBucketMonitor(BucketMonitor monitor) {
        this.monitor = monitor;
    }

    /* @todo we need to investigate why the exception occurs, and if there
     *       is a better solution to the problem than just shutting down the
     *       connection. For now just invalidate the BucketMonitor, and the
     *       Client manager will recreate the connection.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOGGER.log(Level.INFO, "Exception occurred: ");
        if (monitor != null) {
            monitor.invalidate();
        }
    }
}
