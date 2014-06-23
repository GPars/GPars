package groovyx.gpars.samples.remote.chat

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.actor.remote.RemoteActors

def HOST = "localhost"
def PORT = 9000

def reader = new BufferedReader(new InputStreamReader(System.in))

println """Welcome to chat!

Type @exit to quit
"""

println "Enter client name:"
def name = reader.readLine()

def client = new ChatClientActor(HOST, PORT, name)
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