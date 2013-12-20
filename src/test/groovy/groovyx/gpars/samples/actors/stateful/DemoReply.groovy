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

import groovyx.gpars.actor.Actor

import static groovyx.gpars.actor.Actors.actor

/**
 * A demo showing sender identification retrieval.
 * Two actors submit their messages to a third actor, which retrieves the identity of one of the message originators.
 * @author Vaclav Pech
 */

def fasterTypist = null

def judge = actor {
    def messages = []
    react { message1 ->
        messages << [message1, sender]
        react { message2 ->
            messages << [message2, sender]
            final Object winningMessage = messages.max { it[0].size() }
            Actor winner = winningMessage[1]
            println "The winner is: $winner"
            fasterTypist = winner
        }
    }
}

def typist1 = actor {
    judge.send 'This is how much I can type in 10 seconds'
}

def typist2 = actor {
    judge.send 'This is how much I can type in 10 seconds. I\'ve practised a lot over night.'
}

judge.join()
assert fasterTypist == typist2
