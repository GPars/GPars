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

import groovyx.gpars.dataflow.Dataflows
import java.util.concurrent.Executors

final LIMIT = 3 * 100 * 1000

final df = new Dataflows(LIMIT + 1, 0.9f, 16)

final many = 1..LIMIT

def scheduler = Executors.newFixedThreadPool(20)

scheduler.execute { df.result = many.collect { df[it] }.sum() }

// each in a newly started executor:
many.each {num ->
    scheduler.execute {
        df[num] = 1
    }
}

// Wait for the result to be available
// This is in the main thread, which is DF unaware!
assert many.size() == df.result
scheduler.shutdown()
println "done"
