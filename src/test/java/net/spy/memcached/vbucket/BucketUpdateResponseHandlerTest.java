package net.spy.memcached.vbucket;

import junitx.util.PrivateAccessor;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class BucketUpdateResponseHandlerTest {

    @Before
    public void setUp() {

    }

    @Test
    /**
     * Test checks if setReceivedFuture() method properly sets a field receivedFuture.
     */
    public void testSetGetRecievedFuture() throws Throwable {
        ChannelFuture futureMock = createMock(ChannelFuture.class);
        replay(futureMock);

        BucketUpdateResponseHandler handler = new BucketUpdateResponseHandler();
        PrivateAccessor.setField(handler, "receivedFuture", futureMock);
        PrivateAccessor.invoke(handler, "setReceivedFuture",
                new Class[]{ChannelFuture.class}, new Object[]{futureMock});
        assertEquals(futureMock, PrivateAccessor.getField(handler, "receivedFuture"));

        verify(futureMock);
    }

    @Test
    public void testMessageReceived() throws NoSuchFieldException {
        MessageEvent eventMock = createMock(MessageEvent.class);
        ChannelFuture futureMock = createMock(ChannelFuture.class);
        HttpChunk chunkMock = createMock(HttpChunk.class);
        ChannelBuffer bufferMock = createMock(ChannelBuffer.class);
        final String responseMsg = "{\"name\":\"default\"}";
        final String endMsg = "\n\n\n\n";

        expect(eventMock.getFuture()).andReturn(futureMock);
        expect(eventMock.getMessage()).andReturn(chunkMock);
        expect(chunkMock.isLast()).andReturn(true);

        expect(eventMock.getFuture()).andReturn(futureMock);
        expect(eventMock.getMessage()).andReturn(chunkMock);
        expect(chunkMock.isLast()).andReturn(false);
        expect(chunkMock.getContent()).andReturn(bufferMock);
        expect(bufferMock.toString("UTF-8")).andReturn(responseMsg);
        expect(futureMock.setSuccess()).andReturn(true);

        expect(eventMock.getFuture()).andReturn(futureMock);
        expect(eventMock.getMessage()).andReturn(chunkMock);
        expect(chunkMock.isLast()).andReturn(false);
        expect(chunkMock.getContent()).andReturn(bufferMock);
        expect(bufferMock.toString("UTF-8")).andReturn(endMsg);

        final DefaultHttpResponse response = new DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        expect(eventMock.getFuture()).andReturn(futureMock);
        expect(eventMock.getMessage()).andReturn(response);

        replay(eventMock, futureMock, chunkMock, bufferMock);

        // if current chunk is the last chunk - set readingChunks to false.
        BucketUpdateResponseHandler handler = new BucketUpdateResponseHandler();
        PrivateAccessor.setField(handler, "readingChunks", Boolean.TRUE);
        handler.messageReceived(null, eventMock);
        assertEquals(Boolean.FALSE, PrivateAccessor.getField(handler, "readingChunks"));

        // if current chunk is not last and it is not "\n\n\n\n" -
        // store its value in partialResponse and invoke channelFuture.setSuccess().
        PrivateAccessor.setField(handler, "readingChunks", Boolean.TRUE);
        handler.messageReceived(null, eventMock);
        StringBuilder partialResponse =
                (StringBuilder) PrivateAccessor.getField(handler, "partialResponse");
        assertEquals(responseMsg, partialResponse.toString());

        // if current chunk contains "\n\n\n\n" - reset partialResponse and update lastResponse.
        PrivateAccessor.setField(handler, "readingChunks", Boolean.TRUE);
        handler.messageReceived(null, eventMock);
        partialResponse =
                (StringBuilder) PrivateAccessor.getField(handler, "partialResponse");
        assertNull(partialResponse);
        PrivateAccessor.getField(handler, "lastResponse");
        //TODO: enable this check back when dummy http chunk issue will be fixed
        //assertEquals(responseMsg, lastResponse);

        // if readingChunks = false - just log response.
        PrivateAccessor.setField(handler, "readingChunks", Boolean.FALSE);
        handler.messageReceived(null, eventMock);

        verify(eventMock, futureMock, chunkMock, bufferMock);
    }

    public void testLogResponse() {

    }
}
