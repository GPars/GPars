package org.gparallelizer.samples.actors

import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.PooledActor
import groovy.swing.SwingBuilder
import javax.swing.JFrame
import org.gparallelizer.actors.pooledActors.PooledActors

final def frame = new SwingBuilder().frame(title: 'Demo', defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
    vbox() {
        button('Click', actionPerformed: {
            bar()
            PooledActors.actor {
                react {
                    doLater {
                        println 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'
                    }
                }
            }.start().send 'Message'

        })
    }
}

frame.visible = true
frame.pack()

public void foo() {
    println 'Running'
    final def group = new PooledActorGroup(20)

    final def nestedActor = group.actor {
        println 'Started nested actor'
        react {
            println 'Finished nested actor'
            reply 20
        }
    }
    nestedActor.start()

    final PooledActor actor = group.actor {
        println 'Started an actor'
        nestedActor.sendAndWait(10)
        println 'Done'
    }
    actor.start()
    println 'Running controller'
}

public void bar() {
    final def group = new PooledActorGroup(20)

    final PooledActor actor = group.actor {
        println 'Started an actor ' + delegate + ":" + owner
        final def nestedActor = group.actor {
            println 'Started nested actor ' + delegate + ":" + owner
            react {
                println 'Finished nested actor ' + delegate + ":" + owner.owner + ":" + getResolveStrategy()
                reply 20
            }
        }.start()
        nestedActor.sendAndWait(10)
        println 'Done'
    }
    actor.start()
    println 'Running controller'

}