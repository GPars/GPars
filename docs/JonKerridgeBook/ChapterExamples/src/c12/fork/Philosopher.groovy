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

class Philosopher implements CSProcess {

    def ChannelOutput leftFork
    def ChannelOutput rightFork
    def ChannelOutput enter
    def ChannelOutput exit
    def int id

    def timer = new CSTimer()

    def void action(id, type, delay) {
        println "${type} : ${id} "
        timer.sleep(delay)
    }

    def void run() {
        while (true) {
            action(id, "            thinking", 1000)
            enter.write(1)
            println "${id}: entered"
            leftFork.write(1)
            println "${id}: got left fork"
            rightFork.write(1)
            println "${id}: got right fork"
            action(id, "            eating", 2000)
            leftFork.write(1)
            println "${id}: put down left"
            rightFork.write(1)
            println "${id}: put down right"
            exit.write(1)
            println "${id}: exited"
        }
    }
}

      
