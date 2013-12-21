// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroup

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

final PGroup group = new NonDaemonPGroup(1)
final DataflowQueue stocksStream = new DataflowQueue()
final DataflowQueue pricedStocks = new DataflowQueue()

//noinspection SpellCheckingInspection
['AAPL', 'GOOG', 'IBM', 'JAVA', 'MSFT'].each {
    stocksStream << it
}

1.upto(3) {
    group.operator(inputs: [stocksStream], outputs: [pricedStocks]) { stock ->
        def price = getYearEndClosing(stock, 2008)
        bindOutput(0, [stock: stock, price: price])
    }
}

def top = [stock: 'None', price: 0.0]

group.operator(inputs: [pricedStocks], outputs: []) { pricedStock ->
    println "Received stock ${pricedStock.stock} priced to ${pricedStock.price}"
    if (top.price < pricedStock.price) {
        top = pricedStock
        println "Top stock so far is $top.stock with price ${top.price}"
    }
}

Thread.sleep 5000

//noinspection SpellCheckingInspection
['AAPL', 'IBM'].each {
    stocksStream << it
}

Thread.sleep 5000
group.shutdown()
