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

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.group.DefaultPGroup

/**
 * Shows solution to the popular Sleeping Barber concurrency problem - http://en.wikipedia.org/wiki/Sleeping_barber_problem
 */

final def group = new DefaultPGroup()
final def random = new Random()

final def barber = group.reactor {message ->
    switch (message) {
        case Enter:
            message.customer.send new Start()
            println "Barber: Processing customer ${message.customer.name}"
            doTheHaircut(random)
            message.customer.send new Done()
            return new Next()
        case Wait:
            println "Barber: No customers. Going to have a sleep"
            break
    }
}

private def doTheHaircut(Random random) {
    Thread.sleep(random.nextInt(10) * 1000)
}

Actor waitingRoom

waitingRoom = group.actor {
    final int capacity = 5
    final List<Customer> waitingCustomers = []
    boolean barberAsleep = true

    loop {
        react {message ->
            switch (message) {
                case Enter:
                    if (waitingCustomers.size() == capacity) {
                        reply new Full()
                    } else {
                        waitingCustomers << message.customer
                        if (barberAsleep) {
                            assert waitingCustomers.size() == 1
                            barberAsleep = false
                            waitingRoom.send new Next()
                        }
                        else reply new Wait()
                    }
                    break
                case Next:
                    if (waitingCustomers.size() > 0) {
                        def customer = waitingCustomers.remove(0)
                        barber.send new Enter(customer: customer)
                    } else {
                        barber.send new Wait()
                        barberAsleep = true
                    }
            }
        }
    }

}

class Customer extends DefaultActor {
    String name
    Actor localBarbers

    void act() {
        localBarbers << new Enter(customer: this)
        loop {
            react {message ->
                switch (message) {
                    case Full:
                        println "Customer: $name: The waiting room is full. I am leaving."
                        stop()
                        break
                    case Wait:
                        println "Customer: $name: I will wait."
                        break
                    case Start:
                        println "Customer: $name: I am now being served."
                        break
                    case Done:
                        println "Customer: $name: I have been served."
                        stop()
                        break

                }
            }
        }
    }
}

class Enter {
    Customer customer
}
class Full {}
class Wait {}
class Next {}
class Start {}
class Done {}

def customers = []
customers << new Customer(name: 'Joe', localBarbers: waitingRoom).start()
customers << new Customer(name: 'Dave', localBarbers: waitingRoom).start()
customers << new Customer(name: 'Alice', localBarbers: waitingRoom).start()

sleep 15000
customers << new Customer(name: 'James', localBarbers: waitingRoom).start()
sleep 5000
customers*.join()
barber.stop()
waitingRoom.stop()

