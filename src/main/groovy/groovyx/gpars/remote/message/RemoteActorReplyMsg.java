package groovyx.gpars.remote.message;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

public class RemoteActorReplyMsg extends SerialMsg {
    private final String name;
    private final Actor actor;

    public RemoteActorReplyMsg(String name, Actor actor) {
        this.name = name;
        this.actor = actor;
    }

    @Override
    public void execute(RemoteConnection conn) {
        conn.getLocalHost().registerRemote(name, actor);
    }
}
