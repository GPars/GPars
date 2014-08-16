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

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor

class ChatServerActor extends DefaultActor {
    Map<String, Actor> connectedClients = new HashMap<>()

    @Override
    protected void act() {
        loop {
            react { ChatMessage message ->
                switch (message.action) {
                    case "say":
                        def otherClients = connectedClients.findResults { name, client -> name != message.sender ? client : null }
                        otherClients.each { it << message }
                        break
                    case "register":
                        connectedClients.each { name, client -> client << new ChatMessage(action: "say", sender: "server", message: "${message.sender} has joined") }
                        connectedClients.put(message.sender, message.message)
                        break
                    case "unregister":
                        connectedClients.remove(message.sender)
                        connectedClients.each { name, client -> client << new ChatMessage(action: "say", sender: "server", message: "${message.sender} has left")}
                        break
                    case "show":
                        connectedClients[message.sender] << new ChatMessage(action: "show", sender: "server", message: connectedClients.keySet().toListString())
                        break
                }
            }
        }
    }
}
