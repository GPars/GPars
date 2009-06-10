package org.gparallelizer.samples.benchmarks

import org.gparallelizer.actors.pooledActors.PooledActors


PooledActors.defaultPooledActorGroup.resize 3

final int iterations = 10000
final int warmupIterations = 3000

new PooledActorBenchmark().perform(warmupIterations)
println 'PooledActors with send and reply: ' + new PooledActorBenchmark().perform(iterations)

new ActorBenchmark().perform(warmupIterations)
println 'Actors with send and reply: ' + new ActorBenchmark().perform(iterations)

new PooledActorBenchmarkWithoutReply().perform(warmupIterations)
println 'PooledActors with fastSend: ' + new PooledActorBenchmarkWithoutReply().perform(iterations)

new ActorBenchmarkWithoutReply().perform(warmupIterations)
println 'Actors with fastSend: ' + new ActorBenchmarkWithoutReply().perform(iterations)

new PooledActorCreationBenchmark().perform(warmupIterations)
println 'PooledActors creation: ' + new PooledActorCreationBenchmark().perform(iterations)

new ActorCreationBenchmark().perform(warmupIterations)
println 'Actors creation: ' + new ActorCreationBenchmark().perform(iterations)

new PooledActorNetworkingBenchmark().perform(warmupIterations)
println 'PooledActors networking: ' + new PooledActorNetworkingBenchmark().perform(iterations)

new ActorNetworkingBenchmark().perform(warmupIterations)
println 'Actors networking: ' + new ActorNetworkingBenchmark().perform(iterations)

new SequentialWordSortBenchmark().perform(warmupIterations)
println 'Sequential Word Sort: ' + new SequentialWordSortBenchmark().perform(0)

new PooledWordSortBenchmark().perform(warmupIterations)
println 'Actors Word Sort: ' + new PooledWordSortBenchmark().perform(0)
