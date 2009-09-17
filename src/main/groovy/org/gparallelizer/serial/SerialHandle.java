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

package org.gparallelizer.serial;

import org.codehaus.groovy.util.ManagedReference;
import org.codehaus.groovy.util.ReferenceManager;
import org.gparallelizer.remote.RemoteHost;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Weak reference to object, which was serialized to remote hosts.
 * Also keep ids of all hosts, where the object was serialized.
 *
 * @author Alex Tkachman
 */
public class SerialHandle extends ManagedReference<WithSerialId> {
    /**
     * serial id of the object
     */
    protected final UUID serialId;

    /**
     * serial id of provider the object belongs to
     */
    protected final UUID providerId;

    /**
     * local host
     */
    private final SerialContext context;

    /**
     * remote hosts subscribed to this objects
     */
    private volatile Object subscribers;

    /**
     * Construct handle for object and assign id to it
     *
     * @param value
     */
    public SerialHandle(WithSerialId value) {
        this(value, null);
    }

    /**
     * Construct handle for object with given id to it
     *
     * @param value
     */
    public SerialHandle(WithSerialId value, UUID id) {
        super(ReferenceManager.getDefaultWeakBundle(), value);

        context = SerialContext.get();
        if (id == null) {
            serialId = UUID.randomUUID();
            providerId = context.getLocalHostId();
        } else {
            serialId = id;
            providerId = context.getHostId();
        }
        context.add(this);
    }

    /**
     * Serial id of the object
     *
     * @return
     */
    public UUID getSerialId() {
        return serialId;
    }

    @Override
    public void finalizeReference() {
        context.finalizeHandle(this);
        super.finalizeReference();
    }

    /**
     * @return id of provider the referenced object belongs to
     */
    public UUID getProviderId() {
        return providerId;
    }

    /**
     * Getter for subscribers
     *
     * @return
     */
    public Object getSubscribers() {
        return subscribers;
    }

    /**
     * Subscribes host as interested in the object
     *
     * @param host
     */
    public void subscribe(SerialContext host) {
        synchronized (this) {
            if (subscribers == null) {
                subscribers = host;
            } else {
                if (subscribers instanceof SerialContext) {
                    if (subscribers != host) {
                        final ArrayList<SerialContext> list = new ArrayList<SerialContext>(2);
                        list.add((RemoteHost) subscribers);
                        list.add(host);
                        subscribers = list;
                    }
                } else {
                    @SuppressWarnings({"unchecked"})
                    final List<SerialContext> list = (List<SerialContext>) subscribers;
                    for (SerialContext remoteHost : list) {
                        if (remoteHost == host)
                            return;
                    }
                    list.add(host);
                }
            }
        }
    }
}
