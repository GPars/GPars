//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 


package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlows
import static org.gparallelizer.dataflow.DataFlow.*
import java.util.concurrent.Executors

static final LIMIT = 3*100*1000

final df = new DataFlows(LIMIT+1, 0.9f, DataFlows.MAX_SEGMENTS)

final many = 1..LIMIT

def scheduler = Executors.newFixedThreadPool (20)

scheduler.execute { df.result = many.collect{
    def v = df[it]
    df.remove it
    v
}.sum() }

// each in a newly started actor:
many.each { num ->
    scheduler.execute {
      df[num] = 1
      println num
    }
}

// Wait for the result to be available
// This is in the main thread, which is DF unaware!
assert many.size() == df.result
println "done"
