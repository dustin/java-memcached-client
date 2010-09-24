/*
 * portions Copyright 2009 Red Hat, Inc. as provided under an Apache 2.0 license
 * from the netty project.
 *
 */

package net.spy.memcached.vbucket;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import static org.jboss.netty.channel.Channels.pipeline;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;


public class BucketMonitorPipelineFactory implements ChannelPipelineFactory {

    public ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("decoder", new HttpResponseDecoder());
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("handler", new BucketUpdateResponseHandler());
        return pipeline;
    }
}
