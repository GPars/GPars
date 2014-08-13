// GPars - Groovy Parallel Systems
//
// Copyright © 2014  The original author or authors
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

package groovyx.gpars.samples.remote.actor.chat

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DefaultActor

class ChatClientActor extends DefaultActor {
    String name
    def serverPromise

    def consoleActor = Actors.actor {
        loop {
            react { ChatMessage message ->
                println "${message.sender}: ${message.message}"
            }
        }
    }

    ChatClientActor(def serverPromise, String name) {
        this.serverPromise = serverPromise
        this.name = name
    }

    public afterStop(List<Object> messages) {
        def server = serverPromise.get()
        server << new ChatMessage(action: "unregister", sender: name)
    }

    @Override
    protected void act() {
        def server = serverPromise.get()

        // register
        server << new ChatMessage(action: "register", sender: name, message: consoleActor)

        loop {
            react { line ->
                if (line == "@show") {
                    server << new ChatMessage(action: "show", sender: name)
                }
                else {
                    server << new ChatMessage(action: "say", sender: name, message: line)
                }
            }
        }
    }
}
