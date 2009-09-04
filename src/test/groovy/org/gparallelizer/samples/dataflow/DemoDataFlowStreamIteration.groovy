import java.util.concurrent.CyclicBarrier
import org.gparallelizer.dataflow.DataFlow
import org.gparallelizer.dataflow.DataFlowStream

/**
 * This demo shows the ways to work with DataFlowStream.
 * It demonstrates the iterative methods, which use the current snapshot of the stream,
 * as well as the 'val' property to gradually take elements away from the stream.
 */
final CyclicBarrier barrier = new CyclicBarrier(2)

final DataFlowStream stream = new DataFlowStream()
DataFlow.start {
    (0..10).each {stream << it}
    barrier.await()
    react {
        stream << 11
    }
}

barrier.await()
println 'Current snapshot:'
stream.each {print "$it " }
println ''

stream << 11
stream << 12

println 'Another snapshot:'
stream.each {print "$it " }
println ''

println 'Reading from the stream'
(1..stream.length()).each {print "${stream.val} "}
println ''
println "The stream is now empty. Length =  ${stream.length()}"

System.exit 0