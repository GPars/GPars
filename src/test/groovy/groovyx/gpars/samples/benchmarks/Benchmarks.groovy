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

package groovyx.gpars.samples.benchmarks

import groovyx.gpars.actor.Actors

Actors.defaultPooledActorGroup.resize 10

final int iterations = 10000
final int warmupIterations = 3000

new PooledActorBenchmark().perform(warmupIterations)
println 'Actors with send and reply: ' + new PooledActorBenchmark().perform(iterations)

new PooledActorCreationBenchmark().perform(warmupIterations)
println 'Actors creation: ' + new PooledActorCreationBenchmark().perform(iterations)

new PooledActorNetworkingBenchmark().perform(warmupIterations)
println 'Actors networking: ' + new PooledActorNetworkingBenchmark().perform(iterations)

new SequentialWordSortBenchmark().perform(warmupIterations)
println 'Sequential Word Sort: ' + new SequentialWordSortBenchmark().perform(0)

new PooledWordSortBenchmark().perform(warmupIterations)
println 'Actors Word Sort: ' + new PooledWordSortBenchmark().perform(0)
