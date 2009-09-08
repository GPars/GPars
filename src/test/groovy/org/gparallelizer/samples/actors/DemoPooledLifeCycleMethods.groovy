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

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActor
import org.gparallelizer.actors.pooledActors.PooledActors

/**
 * Two actors are created to show possible ways to handle all lifecycle events of event-driven actors.
 * @author Vaclav Pech
 */

private class ExceptionFlag {
    static final boolean THROW_EXCEPTIION = true  //change the flag to test either exception or timeout
}

final PooledActor actor1 = PooledActors.actor {
    println("Running actor1")
    if (ExceptionFlag.THROW_EXCEPTIION) throw new RuntimeException('test')
    else {
        react(10.milliseconds) {}  //will timeout
    }
}
actor1.metaClass {
    afterStart = {->
        println "actor1 has started"
    }

    afterStop = {List undeliveredMessages ->
        println "actor1 has stopped"
    }

    onInterrupt = {InterruptedException e ->
        println "actor1 has been interrupted"
    }

    onTimeout = {->
        println "actor1 has timed out"
    }

    onException = {Exception e ->
        println "actor1 threw an exception"
    }
}
actor1.start()

Thread.sleep 1000

class PooledLifeCycleSampleActor extends AbstractPooledActor {

    protected void act() {
        println("Running actor2")
        if (ExceptionFlag.THROW_EXCEPTIION) throw new RuntimeException('test')
        else {
            react(10.milliseconds) {}  //will timeout
        }
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

    private void onTimeout() {
        println "actor2 has timed out"
    }

    private void onException(Exception e) {
        println "actor2 threw an exception"
    }
}

new PooledLifeCycleSampleActor().start().join()
