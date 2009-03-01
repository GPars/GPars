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
