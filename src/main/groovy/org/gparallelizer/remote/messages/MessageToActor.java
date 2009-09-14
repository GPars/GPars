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

import org.gparallelizer.MessageStream;
import org.gparallelizer.actors.ActorMessage;

import java.util.UUID;

/**
 * Message set as call to send for remote actor
 * 
 * @author Alex Tkachman
 */
public class MessageToActor<T> extends BaseMsg {
    private final MessageStream   to;
    private final ActorMessage<T> message;

    public MessageToActor(UUID hostId, MessageStream to, ActorMessage<T> message) {
        super(hostId);
        this.to = to;
        this.message = message;
    }

    public MessageStream getTo() {
        return to;
    }

    public ActorMessage<T> getMessage() {
        return message;
    }
}
