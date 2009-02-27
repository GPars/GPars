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

