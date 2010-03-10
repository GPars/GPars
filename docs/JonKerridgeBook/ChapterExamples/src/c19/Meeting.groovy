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

package c19;

import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.mobile.*
import org.jcsp.groovy.*

class Meeting implements CSProcess {
    def List requestChannels // a list of channels used to make client requests
    def ChannelOutput nReuse
    def int newClients

    def ChannelOutput fReuse
    def int findClients

    void run() {
        println "Meeting: Started"
        def meetingMap = [:]
        def alt = new ALT(requestChannels)
        while (true) {
            def index = alt.select()
            println "Meeting: alt channel index is ${index}"
            switch (index) {
                case 0..<newClients:
                    def newMeeting = requestChannels[index].read()
                    // create the reply channel
                    def replyData = new MeetingData()
                    def reply = Mobile.createOne2Net(newMeeting.returnChannel)
                    if (meetingMap.containsKey(newMeeting.meetingName)) {
                        // meeting already exists
                        replyData = meetingMap.get(newMeeting.meetingName)
                        replyData.attendees = replyData.getAttendees() + 1
                        println " Meeting: meeting already exists"
                    }
                    else {
                        // meeting needs to be created
                        replyData = newMeeting
                        replyData.attendees = 1
                        println " Meeting: meeting created"
                    }
                    meetingMap.put(replyData.meetingName, replyData)
                    println " Meeting: current meetings are-"
                    meetingMap.each {println "Meeting: ${it.key}" }
                    reply.write(replyData)
                    // now enable the NewMeetingClient to be reused
                    nReuse.write(replyData.clientId)
                    break
                case newClients..<(findClients + newClients):
                    def findMeeting = requestChannels[index].read()
                    // create the reply channel
                    def replyData = new MeetingData()
                    def reply = Mobile.createOne2Net(findMeeting.returnChannel)
                    if (meetingMap.containsKey(findMeeting.meetingName)) {
                        // meeting already exists
                        replyData = meetingMap.get(findMeeting.meetingName)
                        replyData.attendees = replyData.attendees + 1
                        meetingMap.put(replyData.meetingName, replyData)
                        println " Meeting: meeting does exist"
                    }
                    else {
                        // meeting needs to be created
                        replyData = findMeeting
                        replyData.attendees = 0  // meeting does not yet exist
                        println " Meeting: meeting does not exist yet"
                    }
                    println " Meeting: existing meetings are-"
                    meetingMap.each {println "Meeting: ${it.key}"}
                    reply.write(replyData)
                    // now enable the NewMeetingClient to be reused
                    fReuse.write(replyData.clientId)
                    break
            } // end of switch
        } // end of while
    } // end of run()
}

    
      
  