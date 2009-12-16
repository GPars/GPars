//  GPars (formerly GParallelizer)
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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.actor.AbstractPooledActor
import static groovyx.gpars.dataflow.DataFlow.start

/**
 * Demonstrates the way to handle lifecycle events on a dataflow thread.
 * Dataflow threads are essentially pooled actors and so they have identical lifecycle methods.
 */
def throwException = true

start {
    enhance(delegate)
    println("Running thread")
    if (throwException) throw new RuntimeException('test')
    else {
        react(10.milliseconds) {}  //will timeout
    }
}

private void enhance(AbstractPooledActor thread) {
    thread.metaClass {
        afterStop = {List undeliveredMessages ->
            println "thread has stopped"
        }

        onInterrupt = {InterruptedException e ->
            println "thread has been interrupted"
        }

        onTimeout = {->
            println "thread has timed out"
        }

        onException = {Exception e ->
            println "thread threw an exception"
        }
    }
}

Thread.sleep 5000
System.exit 0 
