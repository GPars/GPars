import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.*
import org.gparallelizer.dataflow.DataFlowStream
import org.gparallelizer.actors.pooledActors.PooledActors

//Example 4

PooledActors.defaultPooledActorGroup.threadPool.resize 4

void ints(int n, int max, DataFlowStream<Integer> stream) {
    if (n != max) {
        println "Generating int: $n"
        stream << n
        ints(n+1, max, stream)
    }
}

void sum(int s, DataFlowStream<Integer> inStream, DataFlowStream<Integer> outStream) {
    println "Calculating $s"
    outStream << s
    sum(~inStream + s, inStream, outStream)
}

void printSum(DataFlowStream stream) {
    println "Result ${~stream}"
    printSum stream
}

final def producer = new DataFlowStream<Integer>()
final def consumer = new DataFlowStream<Integer>()

thread {
    ints(0, 1000, producer)
}

thread {
    sum(0, producer, consumer)
}

thread {
    printSum (consumer)
}

System.in.read()