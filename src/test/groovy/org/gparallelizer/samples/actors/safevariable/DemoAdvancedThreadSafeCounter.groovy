//  GParallelizer
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

package org.gparallelizer.samples.actors.safevariable

import org.gparallelizer.actors.pooledActors.SafeVariable

class Conference extends SafeVariable {
    def Conference() { super(0L) }
    private def register(long num) { data += num }
    private def unregister(long num) { data -= num }
}

final SafeVariable conference = new Conference()

final Thread t1 = Thread.start {
    conference << {register(10)}
}

final Thread t2 = Thread.start {
    conference << {register(5)}
}

final Thread t3 = Thread.start {
    conference << {unregister(3)}
}

[t1, t2, t3]*.join()

assert 12 == conference.val

