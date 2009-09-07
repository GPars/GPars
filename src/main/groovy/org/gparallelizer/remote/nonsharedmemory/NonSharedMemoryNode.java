package org.gparallelizer.remote.nonsharedmemory;

import org.gparallelizer.remote.RemoteNode;
import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.remote.RemoteActor;
import org.gparallelizer.remote.RemoteMessage;
import org.gparallelizer.remote.sharedmemory.SharedMemoryActor;
import org.gparallelizer.remote.sharedmemory.SharedMemoryNode;
import org.gparallelizer.actors.ActorMessage;
import org.gparallelizer.actors.Actor;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class NonSharedMemoryNode extends RemoteNode {
    private final LocalNode localNode;

    private final LinkedBlockingQueue<byte []> incoming = new LinkedBlockingQueue<byte[]> ();

    public NonSharedMemoryNode(final LocalNode node) {
        super();
        this.localNode = node;

        final Actor main = localNode.getMainActor();
        if (main != null)
            localActorsId.put(MAIN_ACTOR_ID, main);

        node.getScheduler().execute(new Runnable(){
            public void run() {
                while (true) {
                    try {
                        final byte[] bytes = incoming.take();
                        final RemoteMessage remoteMessage = decodeMessage(bytes);
                        node.getScheduler().execute(new Runnable(){
                            public void run() {
                                getLocalActor(remoteMessage.getTo()).send(remoteMessage.getPayload());
                            }
                        });
                    } catch (InterruptedException e) {
                        //
                    }
                }
            }
        });
    }

    public UUID getId() {
        return localNode.getId();
    }

    public LocalNode getLocalNode() {
        return localNode;
    }

    public void onDisconnect(RemoteNode smn) {
        localNode.onDisconnect(smn);
    }

    protected RemoteActor createRemoteActor(UUID uid) {
        if (uid == RemoteNode.MAIN_ACTOR_ID)
            return new RemoteActor(this, uid);

        throw new UnsupportedOperationException();
    }

    protected void deliver(byte[] bytes) throws IOException {
        try {
            incoming.put(bytes);
        } catch (InterruptedException e) {
            throw new IOException();
        }

    }
}