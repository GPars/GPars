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

package org.gparallelizer.remote.messages;

import java.io.Serializable;
import java.util.UUID;

/**
 * Message set as call to send for remote actor
 * 
 * @author Alex Tkachman
 */
public class MessageToActor<T extends Serializable> extends BaseMsg {
    private final UUID  to;
    private final UUID  from;
    private final T     payload;

    public MessageToActor(UUID hostId, UUID to, UUID from, T payload) {
        super(hostId);
        this.to = to;
        this.from = from;
        this.payload = payload;
    }

    public UUID getTo() {
        return to;
    }

    public UUID getFrom() {
        return from;
    }

    public Serializable getPayload() {
        return payload;
    }
}
