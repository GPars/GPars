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
class UpperCase {
    @ActiveMethod
    def toUpperCase(String text) {
        sleep 3000
        return text.toUpperCase()
    }
}

@ActiveObject
class Revert {
    private UpperCase upperCase = new UpperCase()

    @ActiveMethod
    def revert(String text) {
        Promise<String> promise = upperCase.toUpperCase(text)
        promise >> {
            sleep 3000
            it.reverse()
        }
    }
}

final revert = new Revert()
//noinspection SpellCheckingInspection
final promiseForRevert = revert.revert("!ecneirepxe ynnuf dna lufesu a si sesimorP gnisopmoC")
println "The computation is running at full speed. We will now wait for the result:"
println promiseForRevert.get()
