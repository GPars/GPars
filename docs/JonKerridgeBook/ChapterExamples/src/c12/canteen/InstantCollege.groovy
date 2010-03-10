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

package c12.canteen

import org.jcsp.lang.*
import org.jcsp.groovy.*

Any2OneChannel service = Channel.createAny2One()
One2AnyChannel deliver = Channel.createOne2Any()
One2OneChannel supply = Channel.createOne2One()
def philosopherList = (0..4).collect {
    i ->
    return new Philosopher(philosopherId: i,
            service: service.out(),
            deliver: deliver.in())
}
def processList = [new InstantServery(service: service.in(),
        deliver: deliver.out(),
        supply: supply.in()),
        new Kitchen(supply: supply.out())
]
processList = processList + philosopherList
new PAR(processList).run()