package groovyx.gpars.samples.dataflow

import groovyx.gpars.actor.ActorGroup
import groovyx.gpars.actor.NonDaemonActorGroup
import groovyx.gpars.dataflow.DataFlowStream
import static groovyx.gpars.dataflow.DataFlow.operator

def getYearEndClosing(String stock, int year) {
    def url = "http://ichart.finance.yahoo.com/table.csv?s=$stock&amp;a=11&amp;b=01&amp;c=$year&amp;d=11&amp;e=31&amp;f=$year&amp;g=m;ignore=.csv"
    try {
        def data = url.toURL().text
        return data.split("\n")[1].split(",")[4].toDouble()
    } catch (all) {
        println "Could not get $stock, assuming value 0. $all.message"
        return 0
    }
}

final ActorGroup group = new NonDaemonActorGroup(1)
final DataFlowStream stocksStream = new DataFlowStream()
final DataFlowStream pricedStocks = new DataFlowStream()

['AAPL', 'GOOG', 'IBM', 'JAVA', 'MSFT'].each {
    stocksStream << it
}

1.upto(3) {
    operator(inputs: [stocksStream], outputs: [pricedStocks], group) {stock ->
        def price = getYearEndClosing(stock, 2008)
        bindOutput(0, [stock: stock, price: price])
    }
}

def top = [stock: 'None', price: 0.0]

operator(inputs: [pricedStocks], outputs: [], group) {pricedStock ->
    println "Received stock ${pricedStock.stock} priced to ${pricedStock.price}"
    if (top.price < pricedStock.price) {
        top = pricedStock
        println "Top stock so far is $top.stock with price ${top.price}"
    }
}

Thread.sleep 5000

['AAPL', 'IBM'].each {
    stocksStream << it
}

Thread.sleep 5000
group.shutdown()
