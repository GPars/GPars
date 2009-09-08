//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.remote;

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
    private RemoteTransportProvider transportProvider;

    public LocalNode() {
        this(null, null);
    }

    public LocalNode(Runnable runnable) {
        this(null, runnable);
    }

    public LocalNode(RemoteTransportProvider provider) {
        this(provider, null);
    }

    public LocalNode(RemoteTransportProvider provider, Runnable runnable) {
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

        transportProvider = provider;
        if (runnable != null) {
            connect(provider);
        }
    }

    public void connect() {
        if (transportProvider != null)
            connect(transportProvider);
        else
            LocalNodeRegistry.connect(this);
    }

    public void connect(final RemoteTransportProvider provider) {
        scheduler.execute(new Runnable(){
            public void run() {
                provider.connect(LocalNode.this);
            }
        });
    }

    public void disconnect() {
        if (transportProvider == null)
            LocalNodeRegistry.disconnect(this);
        else
            scheduler.execute(new Runnable(){
                public void run() {
                    transportProvider.disconnect(LocalNode.this);
                }
            });
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
