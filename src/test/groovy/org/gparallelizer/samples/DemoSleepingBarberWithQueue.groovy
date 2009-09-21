package org.gparallelizer.samples

import java.util.concurrent.ArrayBlockingQueue as Queue
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

def shaveTime   = 100
def random      = new Random()
def seatCount   = 3
def fairAccess  = true
def seats       = new Queue(seatCount, fairAccess)

class Customer extends AbstractPooledActor {
    int id
    Queue seats
    int shaveTime

    void act() {
        def result = seats.offer(this)
        if (!result) {
            println "customer $id leaves since no seat is available"
            stop() // <- do we need this?
            return
        }
        println "customer $id is taking a seat"
        react {
            println "customer $id get's a shave"
            sleep shaveTime
            reply "customer $id shaved"
            stop() // <- do we need this?
        }
    }
}

def barber = Thread.startDaemon {
	while(true) {
		if (seats.empty) println "sleeping"
		println seats.take().sendAndWait(null)
	}
}

15.times {
    new Customer(id:it, seats:seats, shaveTime: shaveTime).start()
    sleep random.nextInt(shaveTime)
}