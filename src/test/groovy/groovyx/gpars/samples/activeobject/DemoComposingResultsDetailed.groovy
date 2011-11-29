// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.activeobject

import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.dataflow.Promise

/**
 * The demo shows that DataflowVariables returned from active methods are composed.
 * The revert() method is implemented in a way that prevents blocking the Revert class from blocking
 * when waiting for the result of the UpperCase.toUpperCase() method.
 */

@ActiveObject
class UpperCaseWithLogging {
    @ActiveMethod
    def toUpperCase(String text) {
        println "Starting toUpperCase() for $text"
        sleep 3000
        println "Finished toUpperCase() for $text"
        return text.toUpperCase()
    }
}

@ActiveObject
class RevertWithLogging {
    private UpperCaseWithLogging upperCase = new UpperCaseWithLogging()

    @ActiveMethod
    def revert(String text) {
        println "Starting revert() for $text"
        Promise<String> promise = upperCase.toUpperCase(text)
        def myResultPromise = promise >> {
            println "Started the actual revert() for $it"
            sleep 3000
            println "Finished the actual revert() for $it"
            it.reverse()
        }
        println "Finished revert() for $text"
        return myResultPromise
    }
}

final revert = new RevertWithLogging()
final promiseForRevert1 = revert.revert("Message1")
final promiseForRevert2 = revert.revert("Message2")
println "The computations are running at full speed. We will now wait for the results:"
println promiseForRevert1.get()
println promiseForRevert2.get()
