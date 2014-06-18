package groovyx.gpars.remote.netty;


import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.RemoteConnection;
import io.netty.channel.Channel;

public class NettyClientChannelInitializer extends NettyServerChannelInitializer {
    private String actorName;

    public NettyClientChannelInitializer(LocalHost localHost, String actorName) {
        super(localHost);
        this.actorName = actorName;
    }

    @Override
    protected RemoteConnection getRemoteConnection(Channel channel) {
        return new NettyClientRemoteConnection(localHost, channel, actorName);
    }
}
