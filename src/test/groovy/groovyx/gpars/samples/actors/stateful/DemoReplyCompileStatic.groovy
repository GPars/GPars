// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
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

package groovyx.gpars.samples.actors.stateful

import groovy.transform.CompileStatic
import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.impl.MessageStream
import groovyx.gpars.dataflow.DataflowVariable

import static groovyx.gpars.actor.Actors.actor

/**
 * Shows that DefaultActor can be used with @CompileStatic
 *
 * A demo showing sender identification retrieval.
 * Two actors submit their messages to a third actor, which retrieves the identity of one of the message originators.
 * @author Vaclav Pech
 */

@CompileStatic
void perform() {
    def fasterTypist = new DataflowVariable()

    Actor judge = actor {
        List<MessageStream> senders = []
        react { message1 ->
            senders << sender
            reply "Thank you"
            react { message2 ->
                senders << sender
                reply "Thank you"
                final MessageStream winner = senders[1]  //We're not really fair, are we?
                println "The winner is: $winner"
                fasterTypist.bind(winner)
            }
        }
    }

    Actor typist1 = actor {
        judge.send 'This is how much I can type in 10 seconds'
        react {
            println "Typist1 got a reply: " + it
        }
    }

    Actor typist2 = actor {
        judge.send 'This is how much I can type in 10 seconds. I\'ve practised a lot over night.'
        react {
            println "Typist2 got a reply: " + it
        }
    }

    assert fasterTypist.val == typist2 || fasterTypist.val == typist1
}

perform()