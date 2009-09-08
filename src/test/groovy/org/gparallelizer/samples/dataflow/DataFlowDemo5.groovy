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

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start

/**
 * Shows threads manipulating mutually dependant 4 variables.
 */

DataFlowVariable<Integer> x = new DataFlowVariable<Integer>()
DataFlowVariable<Integer> y = new DataFlowVariable<Integer>()
DataFlowVariable<Integer> z = new DataFlowVariable<Integer>()
DataFlowVariable<Integer> v = new DataFlowVariable<Integer>()

start {
    println 'Thread main'

    x << 1

    println("'x' set to: " + x.val)
    println("Waiting for 'y' to be set...")

    if (x.val > y.val) {
        z << x
        println("'z' set to 'x': " + z.val)
    } else {
        z << y
        println("'z' set to 'y': " + z.val)
    }
 }

start {
    println("Thread 'setY', sleeping...")
    Thread.sleep(5000)
    y << 2
    println("'y' set to: " + y.val)
}

start {
    println("Thread 'setV'")
    v << y
    println("'v' set to 'y': " + v.val)
}

System.in.read()
System.exit 0
