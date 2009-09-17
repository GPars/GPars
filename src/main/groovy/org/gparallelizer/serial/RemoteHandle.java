//  GParallelizer
//
//  Copyright � 2008-9  The original author or authors
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

import org.gparallelizer.remote.RemoteHost;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.WriteAbortedException;
import java.lang.reflect.Constructor;
import java.util.UUID;

/**
 * @author Alex Tkachman
 */
public class RemoteHandle implements Serializable {
    private final UUID serialId;
    private final UUID hostId;

    private final Class klazz;

    public RemoteHandle(UUID id, UUID hostId, Class klazz) {
        this.serialId = id;
        this.hostId = hostId;
        this.klazz = klazz;
    }

    protected Object readResolve() throws ObjectStreamException {
        SerialContext context = SerialContext.get();
        SerialHandle serialHandle = context.get(serialId);

        WithSerialId obj;
        if (serialHandle == null || (obj = serialHandle.get()) == null) {
            try {
                final Constructor constructor = klazz.getConstructor(RemoteHost.class);

                obj = (WithSerialId) constructor.newInstance(context);
                obj.serialHandle = new SerialHandle(obj, serialId);
            } catch (Exception t) {
                throw new WriteAbortedException(t.getMessage(), t);
            }
        }
        return obj;
    }
}
