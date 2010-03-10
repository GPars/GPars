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

package c3

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

One2OneChannel N2I = Channel.createOne2One()
One2OneChannel I2D = Channel.createOne2One()
One2OneChannel D2P = Channel.createOne2One()

def testList = [new GNumbers(outChannel: N2I.out()),
        new GIntegrate(inChannel: N2I.in(),
                outChannel: I2D.out()),
        new DifferentiateNeg(inChannel: I2D.in(),
                outChannel: D2P.out()),
        new GPrint(inChannel: D2P.in(), heading: "Differentiated Numbers")
]

new PAR(testList).run()