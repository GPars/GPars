// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.samples.actors.stateful

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DefaultActor

/**
 * Shows actor solution to The Dining Philosophers problem
 */

Actors.defaultActorPGroup.resize 5

final class Philosopher extends DefaultActor {
    private Random random = new Random()

    String name
    def forks = []

    void act() {
        assert 2 == forks.size()
        loop {
            think()
            forks*.send new Take()
            def messages = []
            react {a ->
                messages << [a, sender]
                react {b ->
                    messages << [b, sender]
                    if ([a, b].any {Rejected.isCase it}) {
                        println "$name: \tOops, can't get my forks! Giving up."
                        final def accepted = messages.find {Accepted.isCase it[0]}
                        if (accepted != null) accepted[1].send new Finished()
                    } else {
                        eat()
                        messages.collect {it[1]}*.send(new Finished())
                    }
                }
            }
        }
    }

    void think() {
        println "$name: \tI'm thinking"
        Thread.sleep random.nextInt(5000)
        println "$name: \tI'm done thinking"
    }

    void eat() {
        println "$name: \tI'm EATING"
        Thread.sleep random.nextInt(2000)
        println "$name: \tI'm done EATING"
    }
}

final class Fork extends DefaultActor {

    String name
    boolean available = true

    void act() {
        loop {
            react {message ->
                switch (message) {
                    case Take:
                        if (available) {
                            available = false
                            reply new Accepted()
                        } else reply new Rejected()
                        break
                    case Finished:
                        assert !available
                        available = true
                        break
                    default: throw new IllegalStateException("Cannot process the message: $message")
                }
            }
        }
    }
}

final class Take {}
final class Accepted {}
final class Rejected {}
final class Finished {}

def forks = [
        new Fork(name: 'Fork 1'),
        new Fork(name: 'Fork 2'),
        new Fork(name: 'Fork 3'),
        new Fork(name: 'Fork 4'),
        new Fork(name: 'Fork 5')
]

def philosophers = [
        new Philosopher(name: 'Joe', forks: [forks[0], forks[1]]),
        new Philosopher(name: 'Dave', forks: [forks[1], forks[2]]),
        new Philosopher(name: 'Alice', forks: [forks[2], forks[3]]),
        new Philosopher(name: 'James', forks: [forks[3], forks[4]]),
        new Philosopher(name: 'Phil', forks: [forks[4], forks[0]]),
]

forks*.start()
philosophers*.start()

Thread.sleep 10000

forks*.stop()
forks*.join()
philosophers*.stop()
philosophers*.join()
