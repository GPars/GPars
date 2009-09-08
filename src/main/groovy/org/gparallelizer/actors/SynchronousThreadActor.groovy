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

package org.gparallelizer.actors;

import java.util.concurrent.SynchronousQueue;


/**
 * Provides an Actor implementation with fair SynchronousQueue storing the messages.
 * The send() method blocks until a call to receive() takes the message for processing.
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */

public class SynchronousThreadActor extends AbstractThreadActor {
    def SynchronousThreadActor() {
        super(new SynchronousQueue<ActorMessage>(false));
    }
}
