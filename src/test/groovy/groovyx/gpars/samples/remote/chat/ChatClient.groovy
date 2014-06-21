package groovyx.gpars.samples.remote.chat

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.actor.remote.RemoteActors

class ClientActor extends DefaultActor {
    def HOST = "localhost"
    def PORT = 9000

    def name

    def consoleActor = Actors.reactor {
        println it
    }

    def server

    ClientActor(String name) {
        this.name = name
    }

    @Override
    protected void act() {
        server = RemoteActors.get(HOST, PORT, "chat-server").get()
        // register

        loop {
            react {
                server << it
            }
        }
    }
}

def reader = new BufferedReader(new InputStreamReader(System.in))

println """Welcome to chat!

Type @exit to quit
"""
println "Enter client name:"
def name = reader.readLine()
def client = new ClientActor(name)
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