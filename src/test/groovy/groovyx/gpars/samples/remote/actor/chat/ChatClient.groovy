package groovyx.gpars.samples.remote.actor.chat

import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9000

def reader = new BufferedReader(new InputStreamReader(System.in))

println """Welcome to chat!

Type @exit to quit
Type @show to see list of clients connected to server
Type message to send to other connected clients
"""

println "Enter client name:"
def name = reader.readLine()

def remoteActors = RemoteActors.create()
def serverPromise = remoteActors.get HOST, PORT, "chat-server"
def client = new ChatClientActor(serverPromise, name)
client.start()

while (true) {
    println "Say:"
    def line = reader.readLine()
    if (line == '@exit') {
        client.stop()
        break
    }

    client << line
}