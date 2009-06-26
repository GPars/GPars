import org.gparallelizer.actors.pooledActors.PooledActorGroup

/**
 * Demonstrates use of reactor - a specialized actor responding to incomming messages with result of running its body
 * on the message.
 */

final def group = new PooledActorGroup()

final def processor = group.reactor {
    2 * it
}.start()

group.actor {
    println 'Result1:' + processor.sendAndWait(10)
}.start()

group.actor {
    println 'Result2:' + processor.sendAndWait(20)
}.start()

group.actor {
    println 'Result3:' + processor.sendAndWait(30)
}.start()

for(i in (1..100)) {
    println "$i: ${processor.sendAndWait(i)}"
}
processor.stop()
processor.join()
