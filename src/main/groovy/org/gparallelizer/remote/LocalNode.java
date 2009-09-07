package org.gparallelizer.remote;

import org.gparallelizer.scheduler.Scheduler;
import org.gparallelizer.remote.sharedmemory.SharedMemoryTransport;
import org.gparallelizer.actors.Actor;
import org.gparallelizer.actors.Actors;
import org.gparallelizer.actors.pooledActors.PooledActors;
import org.gparallelizer.actors.pooledActors.AbstractPooledActorGroup;

import java.util.*;

import groovy.lang.Closure;

/**
 * Representation of local node
 */
public class LocalNode extends RemoteNode {
    private final List<RemoteNodeDiscoveryListener> listeners = Collections.synchronizedList(new LinkedList<RemoteNodeDiscoveryListener> ());

    private static Set<LocalNode> registry = Collections.synchronizedSet(new HashSet<LocalNode> ());

    private final Scheduler scheduler;

    private final Actor mainActor;

    private final AbstractPooledActorGroup actorGroup;

    public LocalNode(Scheduler scheduler) {
        this(scheduler, null);
    }

    public LocalNode(final Scheduler scheduler, Runnable runnable) {
        super(new SharedMemoryTransport());
        ((SharedMemoryTransport)getTransport()).setNode(this);
        this.scheduler = scheduler;

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

        connect();
    }

    public void connect() {
        registry.add(this);

        for (final LocalNode n : registry) {
            if (n == this)
                continue;

            scheduler.execute(new Runnable(){
                public void run() {
                    onConnect(n);
                    n.onConnect(LocalNode.this);
                }
            });
        }
    }

    public void disconnect() {
        registry.remove(this);

        for (final LocalNode n : registry) {
            if (n == this)
                continue;

            scheduler.execute(new Runnable(){
                public void run() {
                    onDisonnect(n);
                    n.onDisonnect(LocalNode.this);
                }
            });
        }
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

    public void onDisonnect (final RemoteNode node) {
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
}
