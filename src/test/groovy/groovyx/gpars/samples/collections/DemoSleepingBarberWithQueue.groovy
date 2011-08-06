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

package groovyx.gpars.samples.collections

import groovyx.gpars.actor.DefaultActor
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/*
Demo for solving the classic sleeping barber problem with the help
of core Java features (blocking queue) in combination with gpars' actors that
care for putting queue operations and business operations into
proper uninterrupted sequence.
The println of sleeping, taking seats, leaving, and starting/stopping the shave
are placeholders for business operations.
@author Dierk Koenig, Vaclav Pech
*/

/** The constants that define the world context of the barber shop.     */
class Shop {
    static final shaveTime = 100
    static final seatCount = 3
    static final fairAccess = true
    static final seats = new ArrayBlockingQueue(seatCount, fairAccess)
}

/** The active element that makes sure that all actions happen in proper
 * uninterrupted sequence, e.g. taking a seat before getting a shave.
 */
class Customer extends DefaultActor {
    int id

    void act() {
        def result = Shop.seats.offer(this)
        if (!result) {
            println "customer $id leaves since no seat is available"
            return
        }
        println "customer $id is taking a seat"
        react {
            println "customer $id get's a shave"
            sleep Shop.shaveTime
            reply "customer $id shaved"
        }
    }
}

/**
 * The barber serves its customers and is therefore a classic daemon thread.
 */
AtomicBoolean shopOpen = new AtomicBoolean(true)
def barber = Thread.startDaemon {
    final NO_MESSAGE = null
    while (shopOpen.get()) {
        // here is a gotcha if a customer enters the shop after the empty check but before the println!
        if (Shop.seats.empty) {
            println "sleeping"
            sleep 100
        }
        println Shop.seats.take().sendAndWait(NO_MESSAGE)
    }
}

// start 15 customer actors with some delay to get the simulation running.
def customers = (1..15).collect { new Customer(id: it) }
def random = new Random()
for (customer in customers) {
    customer.start()
    sleep random.nextInt(Shop.shaveTime)
}
// make sure the simulation ends after all customers visited the shop
customers*.join()
shopOpen.set(false)