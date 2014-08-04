package groovyx.gpars.samples.remote.chat

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

NettyTransportProvider.stopClients()