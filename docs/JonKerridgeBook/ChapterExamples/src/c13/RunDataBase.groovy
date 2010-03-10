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

package c13

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*
import phw.util.*

def nReaders = Ask.Int("Number of Readers ? ", 1, 5)
def nWriters = Ask.Int("Number of Writers ? ", 1, 5)

def connections = nReaders + nWriters

One2OneChannel[] toDatabase = Channel.createOne2One(connections)
One2OneChannel[] fromDatabase = Channel.createOne2One(connections)
One2OneChannel[] consoleData = Channel.createOne2One(connections)

def toDB = new ChannelInputList(toDatabase)
def fromDB = new ChannelOutputList(fromDatabase)

def readers = (0..<nReaders).collect {r ->
    return new Read(id: r,
            r2db: toDatabase[r].out(),
            db2r: fromDatabase[r].in(),
            toConsole: consoleData[r].out())
}

def writers = (0..<nWriters).collect {w ->
    int wNo = w + nReaders
    return new Write(id: w,
            w2db: toDatabase[wNo].out(),
            db2w: fromDatabase[wNo].in(),
            toConsole: consoleData[wNo].out())
}

def database = new DataBase(inChannels: toDB,
        outChannels: fromDB,
        readers: nReaders,
        writers: nWriters)

def consoles = (0..<connections).collect {c ->
    def frameString = c < nReaders ?
        "Reader " + c :
        "Writer " + (c - nReaders)
    return new GConsole(toConsole: consoleData[c].in(),
            frameLabel: frameString)
}
def procList = readers + writers + database + consoles

new PAR(procList).run()