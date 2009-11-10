//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package groovyx.gpars.samples.actors.safe

import groovyx.gpars.actor.Safe

/**
 * Conference stores number of registartions and allows parties to register and unregister.
 * It inherits from the Safe class and adds the register() and unregister() private methods,
 * which callers may use it the commands they submit to the Conference.
 */
class Conference extends Safe {
    
    def Conference() { super(0L) }

    private def register(long num) { data += num }

    private def unregister(long num) { data -= num }
}

final Safe conference = new Conference()  //new Conference created

/**
 * Three external parties will try to register/unregister concurrently
 */

final Thread t1 = Thread.start {
    conference << {register(10L)}               //send a command to register 10 attendees
}

final Thread t2 = Thread.start {
    conference << {register(5L)}                //send a command to register 5 attendees
}

final Thread t3 = Thread.start {
    conference << {unregister(3L)}              //send a command to unregister 3 attendees
}

[t1, t2, t3]*.join()

assert 12L == conference.val
conference.stop().join()

