// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.samples.dataflow.thenChaining

import groovyx.gpars.dataflow.Promise

import static groovyx.gpars.GParsPool.withPool
import static groovyx.gpars.dataflow.Dataflow.whenAllBound

/**
 * Demonstrates the way multiple Promises can be waited for without blocking the thread
 */

withPool {
    //Asynchronous services
    final flightBookingService = {
        sleep 1000  //Simulate work
        "FlightBookingId:12345"
    }.asyncFun()

    final hotelBookingService = {
        sleep 1000  //Simulate work
        "HotelBookingId:12345"
    }.asyncFun()

    final taxiBookingService = {
        sleep 1000  //Simulate work
        "TaxiBookingId:12345"
    }.asyncFun()

    //Calling asynchronous services and receiving back promises for the reservations
    Promise flightReservation = flightBookingService('PRG <-> BRU')
    Promise hotelReservation = hotelBookingService('BRU:Feb 24 2009 - Feb 29 2009')
    Promise taxiReservation = taxiBookingService('BRU:Feb 24 2009 10:31')

    //when all reservations have been made we need to build an agenda for our trip
    Promise agenda = whenAllBound(flightReservation, hotelReservation, taxiReservation) { flight, hotel, taxi ->
        "Agenda: $flight | $hotel | $taxi"
    }

    //since this is a demo, we will only print the agenda and block till it is ready
    println agenda.val
}
