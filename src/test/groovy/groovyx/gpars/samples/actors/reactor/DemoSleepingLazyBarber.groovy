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

package groovyx.gpars.samples.actors.reactor

import groovyx.gpars.group.DefaultPGroup
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Demo for solving a variation of the sleeping barber problem
 * where the barber may not cut more than 1 head of hair in any
 * 15 minute period. The is one way to throttle back transactions
 * so they only occur once every 15 minutes.
 *
 * The barber is a scheduled executor service that cuts hair every
 * 15 minutes. The waiting room is an ArrayBlockQueue. The barber
 * shop is a GPars actor that moves customers into the waiting room.
 *
 * @author Hamlet D'Arcy
 * Date: Dec 17, 2009
 */

class Patron {
    String name
}

// waiting room can't hold more than 12!
def waitingRoom = new ArrayBlockingQueue(12)

def barber = Executors.newScheduledThreadPool(1)
barber.scheduleAtFixedRate({
    println 'Barber: Next customer please!'
    def customer = waitingRoom.poll(10, TimeUnit.SECONDS)
    if (customer)
        println "${customer.name} gets a haircut at ${new Date().format('H:m:s')}"
    else {
        barber.shutdown()
        println "The barber goes home now."
    }
//}, 0, 15, TimeUnit.MINUTES)
}, 0, 15, TimeUnit.SECONDS)  //we're using SECONDS to save your time while watching the demo

def barberShop = new DefaultPGroup().reactor {message ->
    //noinspection GroovySwitchStatementWithNoDefault
    switch (message) {
        case EnterShop:
            println "${message.customer.name} waits for a haircut..."
            waitingRoom.add(message.customer)
            break
    }
}

class EnterShop {
    Patron customer
}

barberShop << new EnterShop(customer: new Patron(name: 'Jerry'))
barberShop << new EnterShop(customer: new Patron(name: 'Phil'))
barberShop << new EnterShop(customer: new Patron(name: 'Bob'))
barberShop << new EnterShop(customer: new Patron(name: 'Ron'))
