// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.samples.dataflow.operators

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.DefaultPGroup

/**
 * Dataflow operators used to processed stream stock data
 *
 * @author Vaclav Pech
 */

//Three channels deliver daily prices of a particular stock traded at three locations - Paris, Vienna and Chicago.
//The prices are in EUR for Paris and Vienna and in USD for Chicago.
//A fourth channels delivers daily exchange rates for EUR to USD conversion (price of one USD in EUR).
//The operator-based calculation uses several intermediate channels and three operators to:
//  1. convert USD prices to EUR
//  2. calculate daily average price of the stock across all three markets
//  3. calculate a five-day moving average of the prices calculated in 2.
//The output of 2. and 3. each comes from a separate channel (avgPrices and fiveDayAverages).
//The two channels are consumed by tasks that print the values on the screen.

//Dummy price and exchange rate data
final prices1 = [10, 11, 12, 10, 9, 8, 7, 9, 6, 7,
                 10, 9, 12, 12, 14, 14, 12, 13, 12, 14,
                 10, 11, 10, 12, 14, 15, 12, 13, 14, 12]
final prices2 = [10, 11, 12, 10, 9, 8, 7, 9, 6, 7,
                 10, 9, 12, 12, 14, 14, 12, 13, 12, 14,
                 10, 11, 10, 12, 14, 15, 12, 13, 14, 12]
final prices3 = [12, 12, 12, 12, 12, 9, 8, 9, 7, 9,
                 11, 11, 13, 11, 14, 15, 13, 14, 12, 14,
                 11, 12, 11, 11, 13, 15, 13, 14, 15, 13]
final rates = [0.81, 0.82, 0.82, 0.81, 0.81, 0.81, 0.82, 0.81, 0.81, 0.82,
               0.79, 0.81, 0.85, 0.88, 0.81, 0.81, 0.83, 0.83, 0.84, 0.81,
               0.81, 0.80, 0.83, 0.81, 0.85, 0.89, 0.93, 0.87, 0.82, 0.81]

//a thread pool to use
final group = new DefaultPGroup()

group.with {
    //input channels
    final parisEURPrices = new DataflowQueue()
    final viennaEURPrices = new DataflowQueue()
    final chicagoUSDPrices = new DataflowQueue()
    final usd2eurRates = new DataflowQueue()

    //output channels
    final avgPrices = new DataflowQueue()
    final fiveDayAverages = new DataflowQueue()

    //utility intermediate channels
    final chicagoEURPrices = new DataflowQueue()
    final dailyPrices = new DataflowQueue()

    //convert Chicago prices to EUR
    operator(inputs: [chicagoUSDPrices, usd2eurRates], outputs: [chicagoEURPrices]) { p, r ->
        bindOutput(p * r)
    }

    //calculate daily averages across all stock exchanges
    operator(inputs: [parisEURPrices, viennaEURPrices, chicagoEURPrices], outputs: [avgPrices, dailyPrices]) { p, v, c ->
        bindAllOutputs((p + v + c) / 3)
    }

    //calculate five-day averages, starting with an empty history
    operator(inputs: [dailyPrices], outputs: [fiveDayAverages], stateObject: [history: []]) { p ->
        stateObject.history << p
        if (stateObject.history.size > 5) stateObject.history.remove(0)
        bindOutput(stateObject.history.sum() / stateObject.history.size())
    }

    //Generate the input data streams from the dummy data
    task {
        prices1.each { parisEURPrices << it }
    }
    task {
        prices2.each { viennaEURPrices << it }
    }
    task {
        prices3.each { chicagoUSDPrices << it }
    }
    task {
        rates.each { usd2eurRates << it }
    }

    //Retrieve the results
    def results = task {
        def result = []
        30.times {
            result << (int) avgPrices.val
        }
        [dailyAveragesKey: result]
    }.then { results ->
        def result = []
        30.times {
            result << (int) fiveDayAverages.val
        }
        results["fiveDayAveragesKey"] = result
        results
    }.get()

    //Print the results
    println "Daily averages:    \t${results['dailyAveragesKey'].join(',\t')}"
    println "Five day averages: \t${results['fiveDayAveragesKey'].join(',\t')}"

    assert results['dailyAveragesKey'] == [9, 10, 11, 9, 9, 7, 6, 8, 5, 7, 9, 8, 11, 11, 13, 13, 11, 12, 11, 13, 9, 10, 9, 10, 13, 14, 12, 12, 13, 11]
    assert results['fiveDayAveragesKey'] == [9, 10, 10, 10, 10, 9, 9, 8, 7, 7, 7, 7, 8, 9, 10, 11, 12, 12, 12, 12, 11, 11, 10, 10, 10, 11, 12, 12, 13, 12]
}

group.shutdown()