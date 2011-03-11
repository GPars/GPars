#! /usr/bin/env groovy

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

//  This is a model of the "The Sleeping Barber" problem,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  The barber sleeping is modelled by the barber actor blocking awaiting a message.  The barbers' chairs
//  are modelled by the message queue between the barbersShop actor and the barber actor.  As the queue is
//  an arbitrary length list, the barbersShop object has to control how many customers are allowed into the
//  queue.

//  Author: Russel Winder <russel.winder@concertant.com>
//  Date: 2009-09-29T07:05+01:00

package groovyx.gpars.samples.actors.reactor

import groovyx.gpars.group.DefaultPGroup

//  Have to call this something other than Customer because of the Customer class in DemoSleepingBarber.

class Customer_RW {
    final Integer id

    public Customer_RW(final int i) { id = i }
}

class BarberCustomer {}

class PendingCustomer extends BarberCustomer {
    final Customer_RW customer

    public PendingCustomer(final Customer_RW c) { customer = c }
}

class SuccessfulCustomer extends BarberCustomer {
    final Customer_RW customer

    public SuccessfulCustomer(final Customer_RW c) { customer = c }
}

def group = new DefaultPGroup()
def barbersShop
def barber = group.reactor {message ->
    if (message instanceof PendingCustomer) {
        println('Barber : Starting with Customer ' + message.customer.id)
        Thread.sleep((Math.random() * 600 + 100) as int)
        println('Barber : Finished with Customer ' + message.customer.id)
        new SuccessfulCustomer(message.customer)
    }
    else {
        throw new RuntimeException('barber got a message of unexpected type ' + message.class)
    }
}
barbersShop = group.actor {
    def seatsTaken = 0
    def isOpen = true
    def customersRejected = 0
    def customersProcessed = 0
    loop {
        react {message ->
            switch (message) {
                case Customer_RW:
                    if (seatsTaken < 4) {
                        println('Shop : Customer ' + message.id + ' takes a seat.')
                        barber.send(new PendingCustomer(message))
                        ++seatsTaken
                    }
                    else {
                        println('Shop : Customer ' + message.id + ' turned away.')
                        ++customersRejected
                    }
                    break
                case SuccessfulCustomer:
                    --seatsTaken
                    ++customersProcessed
                    println('Shop : Customer ' + message.customer.id + ' leaving trimmed.')
                    if (!isOpen && (seatsTaken == 0)) {
                        println('Processed ' + customersProcessed + ' customers and rejected ' + customersRejected + ' today.')
                        stop()
                    }
                    break
                case '': isOpen = false; break
                default: throw new RuntimeException('barbersShop got a message of unexpected type ' + message.class)
            }
        }
    }
}
(0..<20).each {number ->
    Thread.sleep((Math.random() * 200 + 100) as int)
    barbersShop.send(new Customer_RW(number))
}
barbersShop.send('')
barbersShop.join()
