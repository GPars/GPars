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

package c9


import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*
import phw.util.Ask

def sources = Ask.Int("Number of event sources between 1 and 9 ? ", 1, 9)

minTimes = [10, 20, 30, 40, 50, 10, 20, 30, 40]
maxTimes = [100, 150, 200, 50, 60, 30, 60, 100, 80]

One2OneChannel[] es2ep = Channel.createOne2One(sources)

ChannelInputList eventsList = new ChannelInputList(es2ep)

def sourcesList = (0..<sources).collect {i ->
    new EventSource(source: i + 1,
            outChannel: es2ep[i].out(),
            minTime: minTimes[i],
            maxTime: maxTimes[i])
}

def eventProcess = new EventProcessing(eventStreams: eventsList,
        minTime: 10,
        maxTime: 400)

new PAR(sourcesList + eventProcess).run()