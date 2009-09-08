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

final df = new DataFlows()

start { df.result = df.x + df.y }

start { df.x = 10 }

start { df.y = 5 }

assert 15 == df.result

start { df [0] = df[2] + df[1] }

start { df [1] = 10 }

start { df [2] = 5 }

assert 15 == df[0]
