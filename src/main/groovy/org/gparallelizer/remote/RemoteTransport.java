package org.gparallelizer.remote;

import org.gparallelizer.actors.ActorMessage;
import org.gparallelizer.actors.Actor;

import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

public abstract class RemoteTransport {
//
//    protected RemoteNode node;
//
//    public RemoteTransport(RemoteNode node) {
//        this.node = node;
//    }
//
//    protected void onDisconnect() {
////        node.onDisconnect (smn);
//    }
//
//    protected abstract void deliver(byte[] bytes) throws IOException;
//
//    protected void send(Actor receiver, ActorMessage<Serializable> message) {
//        final RemoteMessage toSend = new RemoteMessage(((RemoteActor) receiver).getId(), id2Actor.getId(message.getSender()), message.getPayLoad());
//        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
//        try {
//            final ObjectOutputStream oout = new ObjectOutputStream(bout);
//            oout.writeObject(toSend);
//            oout.close();
//            deliver(bout.toByteArray());
//        } catch (IOException e) {
//            onDisconnect ();
//        }
//    }
}
