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

package c8

import org.jcsp.groovy.*
import org.jcsp.lang.*
import phw.util.Ask

import c7.Client

def clients = Ask.Int("Number of clients per server; 1 to 9 ? ", 1, 9)
def servers = 2

One2OneChannel[] C0ToM0 = Channel.createOne2One(clients)
One2OneChannel[] M0ToC0 = Channel.createOne2One(clients)
One2OneChannel[] C1ToM1 = Channel.createOne2One(clients)
One2OneChannel[] M1ToC1 = Channel.createOne2One(clients)

One2OneChannel[] M1ToS = Channel.createOne2One(servers)
One2OneChannel[] M0ToS = Channel.createOne2One(servers)

One2OneChannel[] S0ToM = Channel.createOne2One(servers)
One2OneChannel[] S1ToM = Channel.createOne2One(servers)

def clientsToM0 = new ChannelInputList(C0ToM0)
def clientsToM1 = new ChannelInputList(C1ToM1)

def M0ToClients = new ChannelOutputList(M0ToC0)
def M1ToClients = new ChannelOutputList(M1ToC1)

def Mux0ToServers = new ChannelOutputList(M0ToS)
def Mux1ToServers = new ChannelOutputList(M1ToS)

def Server0ToMuxes = new ChannelOutputList(S0ToM)
def Server1ToMuxes = new ChannelOutputList(S1ToM)

def Server0FromMuxes = new ChannelInputList()
Server0FromMuxes.append(M0ToS[0].in())
Server0FromMuxes.append(M1ToS[0].in())


def Server1FromMuxes = new ChannelInputList()
Server1FromMuxes.append(M0ToS[1].in())
Server1FromMuxes.append(M1ToS[1].in())

def Mux0FromServers = new ChannelInputList()
Mux0FromServers.append(S0ToM[0].in())
Mux0FromServers.append(S1ToM[0].in())


def Mux1FromServers = new ChannelInputList()
Mux1FromServers.append(S0ToM[1].in())
Mux1FromServers.append(S1ToM[1].in())

def server0Map = [1: 10, 2: 20, 3: 30, 4: 40, 5: 50, 6: 60, 7: 70, 8: 80, 9: 90, 10: 100]

def server1Map = [11: 110, 12: 120, 13: 130, 14: 140, 15: 150, 16: 160, 17: 170, 18: 180, 19: 190, 20: 200]

def serverKeyLists = [[1, 2, 3, 4, 5, 6, 7, 8, 9, 10], [11, 12, 13, 14, 15, 16, 17, 18, 19, 20]]

def client0List = [1, 12, 13, 14, 5, 16, 7, 18, 9, 10]

def client1List = [11, 2, 3, 4, 15, 6, 17, 8, 19, 20]


def network = []

def server0ClientList = (0..<clients).collect {i ->
    return new Client(requestChannel: C0ToM0[i].out(),
            receiveChannel: M0ToC0[i].in(),
            clientNumber: i,
            selectList: client0List)
}

def server1ClientList = (0..<clients).collect {i ->
    return new Client(requestChannel: C1ToM1[i].out(),
            receiveChannel: M1ToC1[i].in(),
            clientNumber: i + 10,
            selectList: client1List)
}

network << new CSMux(inClientChannels: clientsToM0,
        outClientChannels: M0ToClients,
        fromServers: Mux0FromServers,
        toServers: Mux0ToServers,
        serverAllocation: serverKeyLists)

network << new CSMux(inClientChannels: clientsToM1,
        outClientChannels: M1ToClients,
        fromServers: Mux1FromServers,
        toServers: Mux1ToServers,
        serverAllocation: serverKeyLists)

network << new Server(fromMux: Server0FromMuxes,
        toMux: Server0ToMuxes,
        dataMap: server0Map)

network << new Server(fromMux: Server1FromMuxes,
        toMux: Server1ToMuxes,
        dataMap: server1Map)

new PAR(network + server0ClientList + server1ClientList).run()             