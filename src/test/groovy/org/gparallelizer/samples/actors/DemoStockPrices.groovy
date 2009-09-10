package org.gparallelizer.samples.actors

import static org.gparallelizer.actors.pooledActors.PooledActors.*
import org.gparallelizer.dataflow.DataFlowStream

def getYearEndClosing(String symbol, int year ) {
  def url = "http://ichart.finance.yahoo.com/table.csv?s=$symbol&amp;a=11&amp;b=01&amp;c=$year&amp;d=11&amp;e=31&amp;f=$year&amp;g=m;ignore=.csv"
  def data = url.toURL().text
  def price = data.split("\n")[1].split(",")[4].toDouble()
  Thread.sleep(1000); // slow down internet
  [symbol, price]
}

def symbols = ['AAPL', 'GOOG', 'IBM', 'JAVA', 'MSFT']

def start = System.nanoTime()

final def quotes = new DataFlowStream()
symbols.each {stock -> actor { quotes << getYearEndClosing(stock, 2008) }.start() }
def top = ["", 0.0]
1.upto(symbols.size()) {
    def quote = quotes.val
    if (quote[1] > top[1]) top = quote
}

def end = System.nanoTime()

println "Top stock is ${top[0]} with price ${top[1]}"
println "Time taken ${(end - start) / 10000000000.0}"