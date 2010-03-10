package c12.fork

import org.jcsp.lang.*
import org.jcsp.groovy.*

def PHILOSOPHERS = 5

One2OneChannel[] lefts = Channel.createOne2One(PHILOSOPHERS)
One2OneChannel[] rights = Channel.createOne2One(PHILOSOPHERS)
One2OneChannel[] enters = Channel.createOne2One(PHILOSOPHERS)
One2OneChannel[] exits = Channel.createOne2One(PHILOSOPHERS)

def entersList = new ChannelInputList(enters)
def exitsList = new ChannelInputList(exits)

def butler = new Butler ( enters: entersList, exits: exitsList )

def philosophers = ( 0 ..< PHILOSOPHERS).collect { i ->  
                    return new Philosopher ( leftFork: lefts[i].out(), 
                                              rightFork: rights[i].out(), 
                                              enter: enters[i].out(), 
                                              exit: exits[i].out(), id:i ) }

def forks = ( 0 ..< PHILOSOPHERS).collect { i ->  
               return new Fork ( left: lefts[i].in(), 
                                  right: rights[(i+1)%PHILOSOPHERS].in() ) }


def processList = philosophers + forks + butler

new PAR ( processList ).run()                               
                               

