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

package c17.counted

import org.jcsp.groovy.*
import org.jcsp.lang.*

class CountedEvaluator implements CSProcess {

    def ChannelInput inChannel

    void run() {
        while (true) {
            def v = inChannel.read()
            //def ok = ( (v.value % 2) == 0 )
            def ok = ((v.value / (v.counter - 1)) == 2)
            println "Evaluation: ${ok} from " + v.toString()
        }
    }
}