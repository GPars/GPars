package org.gparallelizer.samples.actors

import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.DefaultThreadActor
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

/**
 * Demonstrates work balancing among adaptable set of workers.
 * The load balancer receives tasks and queues them in a temporary task queue.
 * When a worker finishes his assignment, it asks the load balancer for a new task.
 * If the load balancer doesn't have any tasks available in the task queue, the worker is stopped.
 * If the number of tasks in the task queue exceeds certain limit, a new worker is created
 * to increase size of the worker pool.
 */

final class LoadBalancer extends DefaultThreadActor {
    int workers = 0
    List taskQueue = []
    private static final QUEUE_SIZE_TRIGGER = 10

    void act() {
        def message = receive()
        switch (message) {
            case NeedMoreWork:
                if (taskQueue.size()==0) {
                    println 'No more tasks in the task queue. Terminating the worker.'
                    message.reply DemoWorker.EXIT
                    workers -= 1
                } else message.reply taskQueue.remove(0)
                break
            case WorkToDo:
                taskQueue << message
                if ((workers==0) || (taskQueue.size()>=QUEUE_SIZE_TRIGGER)) {
                    println 'Need more workers. Starting one.'
                    workers += 1
                    new DemoWorker(this).start()
                }
        }
        println "Active workers=${workers}\tTasks in queue=${taskQueue.size()}"
    }
}

final class DemoWorker extends AbstractPooledActor {
    final static Object EXIT = new Object()
    private static final Random random = new Random()

    Actor balancer

    def DemoWorker(balancer) {
        this.balancer = balancer
    }

    void act() {
        loop {
            this.balancer << new NeedMoreWork()
            react {
                switch (it) {
                    case WorkToDo:
                        processMessage(it)
                        break
                    case EXIT:stop()
                }
            }
        }

    }

    private void processMessage(message) {
        synchronized(random) {
            Thread.sleep random.nextInt(5000)
        }
    }
}
final class WorkToDo{}
final class NeedMoreWork{}

final Actor balancer = new LoadBalancer().start()

//produce tasks
for(i in 1..20) {
    Thread.sleep 100
    balancer << new WorkToDo()
}

//produce tasks in a parallel thread
Thread.start {
    for(i in 1..10) {
        Thread.sleep 1000
        balancer << new WorkToDo()
    }
}

System.in.read()
balancer << new WorkToDo()
balancer << new WorkToDo()
System.in.read()