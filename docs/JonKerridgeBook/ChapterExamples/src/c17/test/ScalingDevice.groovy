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

package c17.test

import org.jcsp.lang.*
import org.jcsp.groovy.*
import c5.*


class ScalingDevice implements CSProcess {

    def ChannelInput inChannel
    def ChannelOutput outChannel

    void run() {
        def oldScale = Channel.createOne2One()
        def newScale = Channel.createOne2One()
        def pause = Channel.createOne2One()

        def scaler = new Scale(inChannel: inChannel,
                outChannel: outChannel,
                factor: oldScale.out(),
                suspend: pause.in(),
                injector: newScale.in(),
                multiplier: 2,
                scaling: 2)

        def control = new Controller(testInterval: 7000,
                computeInterval: 700,
                addition: 1,
                factor: oldScale.in(),
                suspend: pause.out(),
                injector: newScale.out())

        def testList = [scaler, control]

        new PAR(testList).run()
    }

}