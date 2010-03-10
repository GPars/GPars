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

package c17.sniff

import org.jcsp.lang.*
import org.jcsp.groovy.*

def One2OneChannel Copy2Sniff = Channel.createOne2One()
def One2OneChannel Out2Comp = Channel.createOne2One()

def network = [new SnifferComparator(fromCopy: Copy2Sniff.in(),
        fromScaler: Out2Comp.in(),
        interval: 15000),
        new ScalingSystem(toSniffer: Copy2Sniff.out(),
                toComparator: Out2Comp.out())
]

new PAR(network).run()
