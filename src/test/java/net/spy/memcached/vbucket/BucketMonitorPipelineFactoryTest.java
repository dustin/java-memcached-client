package net.spy.memcached.vbucket;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.junit.Before;
import org.junit.Test;

public class BucketMonitorPipelineFactoryTest {

    @Before
    public void setUp() {

    }

    @Test
    public void testGetPipeline() throws Exception {
        BucketMonitorPipelineFactory factory = new BucketMonitorPipelineFactory();
        ChannelPipeline pipeline = factory.getPipeline();

        ChannelHandler decoder = pipeline.get("decoder");
        ChannelHandler encoder = pipeline.get("encoder");
        ChannelHandler handler = pipeline.get("handler");
        assertTrue(decoder instanceof HttpResponseDecoder);
        assertTrue(encoder instanceof HttpRequestEncoder);
        assertTrue(handler instanceof BucketUpdateResponseHandler);
        assertEquals(handler, pipeline.getLast());
        assertEquals(decoder, pipeline.getFirst());
    }
}
