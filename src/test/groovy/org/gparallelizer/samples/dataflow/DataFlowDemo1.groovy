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

import org.gparallelizer.dataflow.DataFlowVariable as WAIT
import static org.gparallelizer.dataflow.DataFlow.*

/**
 * Basic sample showing three green threads cooperating on three variables.
 */
WAIT<Integer> x = new WAIT()
WAIT<Integer> y = new WAIT()
WAIT<Integer> z = new WAIT()

start { z << x.val + y.val }

start { x << 40 }
start { y << 2 }

println "z=${z.val}"
assert 42 == z.val
