// GPars (formerly GParallelizer)
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

def butler = new Butler(enters: entersList, exits: exitsList)

def philosophers = (0..<PHILOSOPHERS).collect {i ->
    return new Philosopher(leftFork: lefts[i].out(),
            rightFork: rights[i].out(),
            enter: enters[i].out(),
            exit: exits[i].out(), id: i)
}

def forks = (0..<PHILOSOPHERS).collect {i ->
    return new Fork(left: lefts[i].in(),
            right: rights[(i + 1) % PHILOSOPHERS].in())
}


def processList = philosophers + forks + butler

new PAR(processList).run()
                               

