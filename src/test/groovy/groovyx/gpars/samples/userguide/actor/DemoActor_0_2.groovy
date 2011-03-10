// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.userguide.actor

import groovy.time.TimeCategory
import groovyx.gpars.actor.Actors
import java.util.concurrent.TimeUnit

/**
 * @author Jan Novotný
 */

def passiveActor = Actors.actor {
    loop {
        react { msg -> println "Received: $msg"; }
    }
}
passiveActor.send 'Message 1'
passiveActor << 'Message 2'    //using the << operator
passiveActor 'Message 3'       //using the implicit call() method

def replyingActor = Actors.actor {
    loop {
        react { msg ->
            println "Received: $msg";
            reply "I've got $msg"
        }
    }
}
def reply1 = replyingActor.sendAndWait('Message 4')
def reply2 = replyingActor.sendAndWait('Message 5', 10, TimeUnit.SECONDS)
use(TimeCategory) {
    def reply3 = replyingActor.sendAndWait('Message 6', 10.seconds)
}
//throw new RuntimeException('test')
