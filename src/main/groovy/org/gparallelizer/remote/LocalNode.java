package org.gparallelizer.remote;

import org.gparallelizer.scheduler.Scheduler;
import org.gparallelizer.actors.Actor;
import org.gparallelizer.actors.pooledActors.AbstractPooledActorGroup;

import java.util.*;

import groovy.lang.Closure;

/**
 * Representation of local node
 */
public class LocalNode {
    private final List<RemoteNodeDiscoveryListener> listeners = Collections.synchronizedList(new LinkedList<RemoteNodeDiscoveryListener> ());

    private final Scheduler scheduler;

    private final Actor mainActor;

    private final AbstractPooledActorGroup actorGroup;

    private final UUID id = UUID.randomUUID();

    public LocalNode() {
        this(null);
    }

    public LocalNode(Runnable runnable) {
        this.scheduler = new Scheduler();

        actorGroup = new AbstractPooledActorGroup() {{threadPool = scheduler;}};

        if (runnable != null) {
            if (runnable instanceof Closure) {
                ((Closure)runnable).setDelegate(this);
                ((Closure)runnable).setResolveStrategy(Closure.DELEGATE_FIRST);
            }
            mainActor = actorGroup.actor(runnable);
            mainActor.start();
        }
        else {
            mainActor = null;
        }

        if (runnable != null)
            connect();
    }

    public void connect() {
        LocalNodeRegistry.connect(this);
    }

    public void disconnect() {
        LocalNodeRegistry.disconnect(this);
    }

    public void addDiscoveryListener (RemoteNodeDiscoveryListener l) {
        listeners.add(l);
    }

    public void addDiscoveryListener (final Closure l) {
        listeners.add(new RemoteNodeDiscoveryListener() {
            @Override
            public void onConnect(RemoteNode node) {
                l.call(new Object[]{node, "connected"});
            }

            @Override
            public void onDisconnect(RemoteNode node) {
                l.call(new Object[]{node, "disconnected"});
            }
        });
    }

    public void removeDiscoveryListener (RemoteNodeDiscoveryListener l) {
        listeners.remove(l);
    }

    public void onConnect (final RemoteNode node) {
        for(final RemoteNodeDiscoveryListener l : listeners)
            scheduler.execute(new Runnable(){
                public void run() {
                    l.onConnect(node);
                }
            });
    }

    public void onDisconnect(final RemoteNode node) {
        for(final RemoteNodeDiscoveryListener l : listeners)
            scheduler.execute(new Runnable(){
                public void run() {
                    l.onDisconnect(node);
                }
            });
    }

    public Actor getMainActor() {
        return mainActor;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return getId().toString();
    }
}
