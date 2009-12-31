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

package groovyx.gpars.samples.actors

import groovyx.gpars.actor.Actor
import static groovyx.gpars.actor.Actors.actor

/**
 * A demo showing sender identification retrieval.
 * Two actors submit their messages to a third actor, which retrieves the identity of one of the message originators.
 * @author Vaclav Pech
 */

volatile def fasterTypist = null

def judge = actor {
    react {message1 ->
        react {message2 ->
            final Object winningMessage = [message1, message2].max {it.size()}
            Actor winner = winningMessage.sender
            println "The winner is: $winner"
            fasterTypist = winner
        }
    }
}

def typist1 = actor {
    judge.send 'This is how much I can type in 10 secon'
}

def typist2 = actor {
    judge.send 'This is how much I can type in 10 seconds. I\'ve practised a lot over night.'
}

judge.join()
assert fasterTypist == typist2
