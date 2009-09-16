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

package org.gparallelizer.remote.serial;

import org.gparallelizer.remote.RemoteHost;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Base class for objects which needs to be exposed to remote nodes via serialization.
 *
 * @author Alex Tkachman
 */
public abstract class WithSerialId implements Serializable {
    public transient volatile SerialHandle serialHandle;

    public final SerialHandle getOrCreateSerialHandle() {
        if (serialHandle == null)
            synchronized (this) {
                if (serialHandle == null) {
                    serialHandle = new SerialHandle(this);
                }

            }
        return serialHandle;
    }

    public Class getRemoteClass() {
        throw new UnsupportedOperationException();
    }

    protected final Object writeReplace () throws ObjectStreamException {
        getOrCreateSerialHandle();
        if (this instanceof RemoteSerialized) {
            return new LocalHandle(serialHandle.getSerialId());
        }

        final RemoteHost host = RemoteHost.getThreadContext();
        serialHandle.subscribe(host);
        return new RemoteHandle(serialHandle.getSerialId(), host.getHostId(), getRemoteClass());
    }
}
