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

package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.pooledActors.AbstractPooledActor

/**
 * An actor representing a reactor. When it receives a message, the supplied block of code is run with the message
 * as a parameter and the result of the code is send in reply.
 *
 * <pre>
 * final def doubler = reactor {message ->
 *     2 * message
 * }
 *
 * def result = doubler.sendAndWait(10)
 *
 * </pre>
 *
 * @author Vaclav Pech, Alex Tkachman
 * Date: Jun 26, 2009
 */
public class ReactiveActor extends RunnableBackedPooledActor {

    ReactiveActor (Closure body) {
      setAction {
        def cloned = body.clone ()
        cloned.delegate = this
        cloned.resolveStrategy = Closure.DELEGATE_FIRST
        loop {
            react {
                it.replyIfExists cloned(it)
            }
        }
      }
    }
}
