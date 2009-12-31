//  GPars (formerly GParallelizer)
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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.DataFlowVariable as WAIT

import static groovyx.gpars.dataflow.DataFlow.task

/**
 * Basic sample showing three green threads cooperating on three variables.
 */
WAIT<Integer> x = new WAIT()
WAIT<Integer> y = new WAIT()
WAIT<Integer> z = new WAIT()

task { z << x.val + y.val }

task { x << 40 }
task { y << 2 }

println "z=${z.val}"
assert 42 == z.val
