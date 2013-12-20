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

import static groovyx.gpars.actor.Actors.actor

def getYearEndClosing(String symbol, int year) {
    try {
        def url = "http://ichart.finance.yahoo.com/table.csv?s=$symbol&amp;a=11&amp;b=01&amp;c=$year&amp;d=11&amp;e=31&amp;f=$year&amp;g=m;ignore=.csv"
        def data = url.toURL().text
        def price = data.split("\n")[1].split(",")[4].toDouble()
        Thread.sleep(1000); // slow down internet
        [symbol: symbol, price: price]
    } catch (all) {
        [symbol: symbol, price: 0]
    }
}

//noinspection SpellCheckingInspection
def symbols = ['AAPL', 'GOOG', 'IBM', 'MSFT']

def observer = actor {
    def start = System.nanoTime()
    symbols.each { stock ->
        actor {
            react { receivedStock -> reply getYearEndClosing(receivedStock, 2008) }
        } << stock
    }

    def top = [symbol: "", price: 0.0]
    def quoteNum = 0
    loop {
        quoteNum += 1
        if (quoteNum <= symbols.size()) {
            react { quote ->
                println "Received a quote for ${quote.symbol} priced ${quote.price}"
                if (quote.price > top.price) top = quote
            }
        } else {
            def end = System.nanoTime()

            println "Top stock is ${top.symbol} with price ${top.price}"
            println "Time taken ${(end - start) / 10000000000.0}"
            stop()
        }
    }
}

observer.join()
