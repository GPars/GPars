import org.gparallelizer.remote.LocalNode
import org.gparallelizer.remote.netty.NettyTransportProvider

println """Welcome to chat!
Every line you will type will be printed on other nodes on the net.
"""

// Here we start new actor communicating over IP
def main = new LocalNode (new NettyTransportProvider(), {

  def connected = [] as Set

  addDiscoveryListener { node, operation ->
    System.err.println "${node.id} $operation"

    if (operation == "connected") {
       connected.add node
    }
    else {
       connected.remove node
    }
  }

  loop {
    react { msg ->
       if (msg instanceof String) {
         connected.each { n ->
           n.mainActor << [msg]
         }
       }
       else {
         println(msg[0])
       }
    }
  }
})

def reader = new LineNumberReader(new InputStreamReader(System.in))
while (true) {
  def line = reader.readLine()
  main.mainActor << line
}