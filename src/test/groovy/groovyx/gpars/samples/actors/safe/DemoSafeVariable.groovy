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
 * Create a new Safe wrapping a list of strings
 */
def jugMembers = new Safe<List<String>>(['Me'])  //add Me

jugMembers.send {it.add 'James'}  //add James

final Thread t1 = Thread.start {
    jugMembers.send {it.add 'Joe'}  //add Joe
}

final Thread t2 = Thread.start {
    jugMembers << {it.add 'Dave'}  //add Dave
    jugMembers << {it.add 'Alice'}  //add Alice
}

[t1, t2]*.join()
println jugMembers.val
jugMembers.valAsync {println "Current members: $it"}

jugMembers.await()
jugMembers.stop().join()
