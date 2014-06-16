package groovyx.gpars.remote.netty;

import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.message.RemoteActorRequestMsg;
import io.netty.channel.Channel;

public class NettyClientRemoteConnection extends NettyRemoteConnection {
    private String actorName;

    public NettyClientRemoteConnection(LocalHost provider, Channel channel, String actorName) {
        super(provider, channel);
        this.actorName = actorName;
    }

    @Override
    public void onConnect() {
        write(new RemoteActorRequestMsg(actorName));
    }
}
