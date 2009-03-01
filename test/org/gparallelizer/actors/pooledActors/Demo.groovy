import static org.gparallelizer.actors.pooledActors.PooledActors.*

final def decryptor = actor {
    loop {
        react {String message->
            reply message.reverse()
        }
    }
}.start()

actor {
    decryptor.send 'suonorhcnysa si yvoorG'
    react {
        println 'Decrypted message: ' + it
    }
}.start()

def adder = actor {
     loop {
         react {a ->
             react {b ->
                 println a+b
                 replyIfExists a+b  //sends reply, if the sender of b is a PooledActor
             }
         }
         //this line will never be reached
     }
     //this line will never be reached
}.start()

adder.send 10

actor {
    adder.send 20
    react {
        println "Result: $it"
    }
}.start()

getPool().resize 2

def actors = []
def messages = []
1.upto(1000) {count ->
    actors << actor {
        loop {
            react {message ->
                messages << "Message received: $count:$message"
            }
        }

    }.start()
}

Thread.sleep(10000)
actors.each {it.send 'a'}
Thread.sleep(10000)

println messages