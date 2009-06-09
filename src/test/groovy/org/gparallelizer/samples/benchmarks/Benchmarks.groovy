package org.gparallelizer.samples.benchmarks

import org.gparallelizer.actors.pooledActors.PooledActors


PooledActors.defaultPooledActorGroup.resize 3

final int iterations = 10000
final int warmupIterations = 3000

new PooledActorBenchmark().perform(warmupIterations)
println 'PooledActors with send and reply: ' + new PooledActorBenchmark().perform(iterations)
//
new PooledActorBenchmarkWithoutReply().perform(warmupIterations)
println 'PooledActors with signal: ' + new PooledActorBenchmarkWithoutReply().perform(iterations)

new ActorBenchmark().perform(warmupIterations)
println 'Actors with send and reply: ' + new ActorBenchmark().perform(iterations)

new ActorBenchmarkWithoutReply().perform(warmupIterations)
println 'Actors with signal: ' + new ActorBenchmarkWithoutReply().perform(iterations)

new PooledActorCreationBenchmark().perform(warmupIterations)
println 'PooledActors creation: ' + new PooledActorCreationBenchmark().perform(iterations)

new ActorCreationBenchmark().perform(warmupIterations)
println 'Actors creation: ' + new ActorCreationBenchmark().perform(iterations)
