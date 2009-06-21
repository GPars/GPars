import org.gparallelizer.dataflow.DataFlow
import org.gparallelizer.dataflow.DataFlowVariable

final DataFlowVariable variable = new DataFlowVariable<Integer>()

DataFlow.thread {
    println variable.retrieve()
    println variable.retrieve()
    println variable.retrieve()
}

Thread.sleep 3000

variable.bind 10

println "Result: ${variable.retrieve()}"

System.in.read()

variable.bind 20
