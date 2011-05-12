// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

import groovyx.gpars.dataflow.DataflowVariable
import static groovyx.gpars.dataflow.Dataflow.task

/**
 * In the example using the right shift operator in place of the whenBound method, you can see one task consuming
 * user input from the console, while the second thread uses the supplied values concurrently to calculate the tax rate,
 * which is then in turn displayed by the first console task.
 * Using the continuation style we never consume a thread while we wait for the user input and have nothing to compute at the moment.
 *
 * @author Vaclav Pech
 * Date: June 2, 2010
 */

def age = new DataflowVariable()
def kids = new DataflowVariable()
def taxRate = new DataflowVariable()

task {
    println "What is your age?"
    age << 30  //Simulating user input here
    println "How many kids you have?"
    kids << 4  //Simulating user input here
    println "Your tax rate is ${taxRate.val}. How do you feel about it?"
}

age >> {
    def rate = calculateAgeBasedRate(it)
    kids >> {
        taxRate << rate - calculateKidsReduction(it)
    }
}

def calculateKidsReduction(int kids) {
    kids * 5
}

def calculateAgeBasedRate(int age) {
    60 - age
}