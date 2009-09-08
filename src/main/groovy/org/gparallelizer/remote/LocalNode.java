package org.gparallelizer.remote;

import org.gparallelizer.scheduler.Scheduler;
import org.gparallelizer.actors.Actor;
import org.gparallelizer.actors.pooledActors.AbstractPooledActorGroup;
import org.gparallelizer.actors.pooledActors.DefaultPool;

import java.util.*;
import java.util.concurrent.*;

import groovy.lang.Closure;

/**
 * Representation of local node
 */
public class LocalNode {
    private final List<RemoteNodeDiscoveryListener> listeners = Collections.synchronizedList(new LinkedList<RemoteNodeDiscoveryListener> ());

    private final ThreadPoolExecutor scheduler;

    private final Actor mainActor;

    private final AbstractPooledActorGroup actorGroup;

    private final UUID id = UUID.randomUUID();
    protected ThreadFactory threadFactory;

    public LocalNode() {
        this(null);
    }

    public LocalNode(Runnable runnable) {
        this.scheduler = new ThreadPoolExecutor(1, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(100),
                new ThreadFactory(){
                    ThreadFactory threadFactory = Executors.defaultThreadFactory();

                    public Thread newThread(Runnable r) {
                        final Thread thread = threadFactory.newThread(r);
                        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                            public void uncaughtException(final Thread t, final Throwable e) {
                                System.err.println("Uncaught exception occured in actor pool " + t.getName());
                                e.printStackTrace(System.err);
                            }
                        });
                        return thread;
                    }
                });

        actorGroup = new AbstractPooledActorGroup() {
            {
                threadPool = new DefaultPool(true){
                    @Override
                    protected ThreadPoolExecutor createPool(boolean daemon, int poolSize) {
                        return scheduler;
                    }
                };
            }
        };

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

    public Executor getScheduler() {
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
