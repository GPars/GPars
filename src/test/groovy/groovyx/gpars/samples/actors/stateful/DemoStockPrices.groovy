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

package groovyx.gpars.samples.actors.stateful

import groovyx.gpars.dataflow.DataflowQueue

import static groovyx.gpars.actor.Actors.actor

def getYearEndClosing(String symbol, int year) {
    def url = "http://ichart.finance.yahoo.com/table.csv?s=$symbol&amp;a=11&amp;b=01&amp;c=$year&amp;d=11&amp;e=31&amp;f=$year&amp;g=m;ignore=.csv"
    try {
        final data = url.toURL().text
        final price = data.split("\n")[1].split(",")[4].toDouble()
        Thread.sleep(1000); // slow down internet
        return [symbol, price]
    } catch (all) {
        return [symbol, 0]
    }
}

//noinspection SpellCheckingInspection
def symbols = ['AAPL', 'GOOG', 'IBM', 'MSFT']

def start = System.nanoTime()

final def quotes = new DataflowQueue()
symbols.each { stock -> actor { quotes << getYearEndClosing(stock, 2008) } }
def top = ["", 0.0]
1.upto(symbols.size()) {
    def quote = quotes.val
    if (quote[1] > top[1]) top = quote
}

def end = System.nanoTime()

println "Top stock is ${top[0]} with price ${top[1]}"
println "Time taken ${(end - start) / 10000000000.0}"