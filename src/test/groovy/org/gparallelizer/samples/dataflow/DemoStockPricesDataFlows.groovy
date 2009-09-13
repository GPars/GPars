package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlows

def getYearEndClosing(String stock, int year) {
    def url = "http://ichart.finance.yahoo.com/table.csv?s=$stock&amp;a=11&amp;b=01&amp;c=$year&amp;d=11&amp;e=31&amp;f=$year&amp;g=m;ignore=.csv"
    try {
        def data = url.toURL().text
        return data.split("\n")[1].split(",")[4].toDouble()
    } catch (all) {
        println "Could not get $stock, assuming value $price. $all.message"
        return 0
    }
}

def stocks = ['AAPL', 'GOOG', 'IBM', 'JAVA', 'MSFT']

def price = new DataFlows()
stocks.each {stock ->
    Thread.start {
        price[stock] = getYearEndClosing(stock, 2008)
    }
}
def topStock = stocks.max { price[it] }
println "Top stock is $topStock with price ${price[topStock]}"