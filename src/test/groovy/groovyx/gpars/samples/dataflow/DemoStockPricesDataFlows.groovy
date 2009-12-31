package groovyx.gpars.samples.dataflow

import groovyx.gpars.Asynchronizer
import groovyx.gpars.dataflow.DataFlows

/* Demonstrating how to process the results of various threads
(here fetching stock prices in parallel) while allowing maximum
concurrency with the help of DataFlows that store the single
results.
All synchronization logic is hidden in the access to DataFlows.
@author Dierk Koenig
 */

/** Fetch the stock price for the end of that year from the yahoo REST service.
 * @return price as double or 0 if call failed.     */
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

def stocks = ['AAPL', 'GOOG', 'IBM', 'JAVA', 'MSFT']
def price = new DataFlows() // key: stock name, value: price

Asynchronizer.doParallel {
    // spawn a thread per stock that stores the result in its DataFlow
    stocks.each({stock ->
        price[stock] = getYearEndClosing(stock, 2008)
    }.async())

// Even though max() goes through the DataFlows in given order
// the fetching threads can run in full parallel
    def topStock = stocks.max { price[it] }
    println "Top stock is $topStock with price ${price[topStock]}"
}

