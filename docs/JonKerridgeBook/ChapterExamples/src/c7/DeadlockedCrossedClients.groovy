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

package c7

import org.jcsp.lang.*
import org.jcsp.groovy.*

def One2OneChannel S02S1request = Channel.createOne2One()
def One2OneChannel S12S0send = Channel.createOne2One()

def One2OneChannel S12S0request = Channel.createOne2One()
def One2OneChannel S02S1send = Channel.createOne2One()

def One2OneChannel C02S0request = Channel.createOne2One()
def One2OneChannel S02C0send = Channel.createOne2One()

def One2OneChannel C12S1request = Channel.createOne2One()
def One2OneChannel S12C1send = Channel.createOne2One()

def server0Map = [1: 10, 2: 20, 3: 30, 4: 40, 5: 50, 6: 60, 7: 70, 8: 80, 9: 90, 10: 100]

def server1Map = [11: 110, 12: 120, 13: 130, 14: 140, 15: 150, 16: 160, 17: 170, 18: 180, 19: 190, 20: 200]


def client0List = [1, 2, 3, 14, 15, 6, 7, 18, 9, 10]

def client1List = [11, 12, 13, 4, 5, 16, 17, 8, 19, 20]


def client0 = new Client(requestChannel: C02S0request.out(),
        receiveChannel: S02C0send.in(),
        selectList: client0List,
        clientNumber: 0)

def client1 = new Client(requestChannel: C12S1request.out(),
        receiveChannel: S12C1send.in(),
        selectList: client1List,
        clientNumber: 1)

def server0 = new Server(clientRequest: C02S0request.in(),
        clientSend: S02C0send.out(),
        thisServerRequest: S02S1request.out(),
        thisServerReceive: S12S0send.in(),
        otherServerRequest: S12S0request.in(),
        otherServerSend: S02S1send.out(),
        dataMap: server0Map)

def server1 = new Server(clientRequest: C12S1request.in(),
        clientSend: S12C1send.out(),
        thisServerRequest: S12S0request.out(),
        thisServerReceive: S02S1send.in(),
        otherServerRequest: S02S1request.in(),
        otherServerSend: S12S0send.out(),
        dataMap: server1Map)

def network = [client0, client1, server0, server1]


new PAR(network).run()

