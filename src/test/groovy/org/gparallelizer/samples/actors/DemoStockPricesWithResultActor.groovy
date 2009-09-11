package org.gparallelizer.samples.actors

import static org.gparallelizer.actors.pooledActors.PooledActors.actor

def getYearEndClosing(String symbol, int year) {
    try {
        def url = "http://ichart.finance.yahoo.com/table.csv?s=$symbol&amp;a=11&amp;b=01&amp;c=$year&amp;d=11&amp;e=31&amp;f=$year&amp;g=m;ignore=.csv"
        def data = url.toURL().text
        def price = data.split("\n")[1].split(",")[4].toDouble()
        Thread.sleep(1000)      // slow down internet
        return [symbol:symbol, price:price]
    } catch (Exception e) {
        return [symbol:symbol, exception:e]
    }
}

def symbols = ['AAPL', 'GOOG', 'IBM', 'JAVA', 'GROOVY', 'SCALA', 'MSFT']

def observer = actor {
    def start = System.nanoTime()
    symbols.each {stock -> actor { owner << getYearEndClosing(stock, 2008) }.start() }
    def top = [symbol:"", price:0.0]
    def quoteNum = 0
    loop {
        quoteNum += 1
        if (quoteNum <= symbols.size()) {
            react {quote ->
                if (quote.exception) println "Attempt to get a quote for ${quote.symbol} has failed with ${quote.exception}"
                else {
                    println "Received a quote for ${quote.symbol} priced ${quote.price}"
                    if (quote.price > top.price) top = quote
                }
            }
        } else {
            def end = System.nanoTime()

            println "Top stock is ${top.symbol} with price ${top.price}"
            println "Time taken ${(end - start) / 10000000000.0}"
            stop()
        }
    }
}.start()

observer.join()
