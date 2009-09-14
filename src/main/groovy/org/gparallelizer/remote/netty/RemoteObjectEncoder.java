package org.gparallelizer.remote.netty;

import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;
import org.gparallelizer.remote.RemoteConnection;
import org.gparallelizer.remote.RemoteHost;

@ChannelPipelineCoverage("one")
public class RemoteObjectEncoder extends ObjectEncoder {
    private RemoteConnection connection;

    /**
     * Creates a new encoder.
     *
     * @param connection
     */
    public RemoteObjectEncoder(RemoteConnection connection) {
        super();
        this.connection = connection;
    }

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        RemoteHost remoteHost = connection.getHost();

        if (remoteHost != null)
           remoteHost.enter();
        try {
            return super.encode(ctx, channel, msg);
        }
        finally {
            if (remoteHost != null)
               remoteHost.leave();
        }
    }
}
