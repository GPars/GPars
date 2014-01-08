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

package groovyx.gpars.samples.dataflow

import static groovyx.gpars.dataflow.Dataflow.lazyTask
import static groovyx.gpars.dataflow.Dataflow.whenAllBound

/**
 * Demonstrates use of lazyTasks to lazily and asynchronously load mutually dependent components into memory.
 * The components (modules) will be loaded in the order of their dependencies and concurrently, if possible.
 * Each module will only be loaded once, irrespective of the number of modules that depend on it.
 * Thanks to laziness only the modules that are transitively needed will be loaded.
 * Our example uses a simple "diamond" dependency scheme: <br/>
 * D depends on B and C <br/>
 * C depends on A <br/>
 * B depends on A <br/>
 * When loading D, A will get loaded first. B and C will be loaded concurrently once A has been loaded. D will start loading
 * once both B and C have been loaded.
 */

def moduleA = lazyTask {
    println "Loading moduleA into memory"
    sleep 3000
    println "Loaded moduleA into memory"
    return "moduleA"
}

def moduleB = lazyTask {
    moduleA.then {
        println "->Loading moduleB into memory, since moduleA is ready"
        sleep 3000
        println "  Loaded moduleB into memory"
        return "moduleB"
    }
}

def moduleC = lazyTask {
    moduleA.then {
        println "->Loading moduleC into memory, since moduleA is ready"
        sleep 3000
        println "  Loaded moduleC into memory"
        return "moduleC"
    }
}

def moduleD = lazyTask {
    whenAllBound(moduleB, moduleC) { b, c ->
        println "-->Loading moduleD into memory, since moduleB and moduleC are ready"
        sleep 3000
        println "   Loaded moduleD into memory"
        return "moduleD"
    }
}

println "Nothing loaded so far"
println "==================================================================="
println "Load module: " + moduleD.get()
println "==================================================================="
println "All requested modules loaded"