import org.gparallelizer.dataflow.DataFlowVariable

final DataFlowVariable a = new DataFlowVariable<String>()
final DataFlowVariable b = new DataFlowVariable<String>()

Thread.start {
    println "Received: $a.val"
    Thread.sleep 2000
    b << 'Thank you'
}

Thread.start {
    Thread.sleep 2000
    a << 'An important message from the second thread'
    println "Reply: $b.val"
}
