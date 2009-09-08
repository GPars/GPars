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

package org.gparallelizer.samples.actors

import org.gparallelizer.actors.Actors
import org.gparallelizer.actors.DefaultThreadActor

/**
 * Two actors are created to show possible ways to handle all lifecycle events of thread-bound actors.
 * @author Vaclav Pech
 */

final DefaultThreadActor actor1 = Actors.actor {
    println("Running actor1")
    if (true) throw new RuntimeException('test')
}
actor1.metaClass {
    afterStart = {->
        println "actor1 has started"
    }

    afterStop = {List undeliveredMessages ->
        println "actor1 has stopped"
    }

    onInterrupt = {InterruptedException e ->
        println "actor2 has been interrupted"
    }

    onException = {Exception e ->
        println "actor1 threw an exception"
        stop()
    }
}
actor1.start()

Thread.sleep 1000

class LifeCycleSampleActor extends DefaultThreadActor {

    protected void act() {
        println("Running actor2")
        if (true) throw new RuntimeException('test')
    }

    private void afterStart() {
        println "actor2 has started"
    }

    private void afterStop(List undeliveredMessages) {
        println "actor2 has stopped"
    }

    private void onInterrupt(InterruptedException e) {
        println "actor2 has been interrupted"
    }

    private void onException(Exception e) {
        println "actor2 threw an exception"
        stop()
    }
}

new LifeCycleSampleActor().start().join()

