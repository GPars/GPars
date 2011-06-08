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

package groovyx.gpars.samples.dataflow.expression

import groovyx.gpars.dataflow.DataflowVariable
import static groovyx.gpars.dataflow.Dataflow.task

/**
 * Illustrates the capabilities of DataflowVariables to represent the values they hold.
 * When retrieving a property from a DataflowVariable, the property is retrieved from the value bound to the DataflowVariable,
 * potentially waiting till a value gets assigned.
 */

def book = new DataflowVariable()
def searchPhrase = new DataflowVariable()
task {
    book << [title:'Groovy in Action 2nd edition   ', author:'Dierk Koenig', publisher:'Manning']
}

task {
    searchPhrase << '2nd'
}

book.title.trim().contains(searchPhrase).whenBound {println it}  //Asynchronous waiting

println book.title.trim().contains(searchPhrase).val  //Synchronous waiting