package org.gparallelizer.remote.netty;

import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.buffer.ChannelBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;
import org.gparallelizer.remote.RemoteConnection;
import org.gparallelizer.remote.RemoteHost;

@ChannelPipelineCoverage("one")
public class RemoteObjectDecoder extends ObjectDecoder {
    private RemoteConnection connection;

    /**
     * Creates a new encoder.
     * @param connection connection handling serialization details
     */
    public RemoteObjectDecoder(RemoteConnection connection) {
        super();
        this.connection = connection;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        RemoteHost remoteHost = connection.getHost();

        if (remoteHost != null)
           remoteHost.enter();
        try {
            return super.decode(ctx, channel, buffer);
        }
        finally {
            if (remoteHost != null)
               remoteHost.leave();
        }
    }
}