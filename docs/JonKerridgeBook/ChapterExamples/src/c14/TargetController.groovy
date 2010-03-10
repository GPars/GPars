package c14

import org.jcsp.lang.*
import org.jcsp.awt.*
import org.jcsp.groovy.*

class TargetController implements CSProcess {
  def ChannelOutput getActiveTargets
  def ChannelInput activatedTargets
  
  def ChannelInput receivePoint
  
  def ChannelOutputList sendPoint
  
  def Barrier setUpBarrier
  def Barrier goBarrier
  def AltingBarrier timeAndHitBarrier
  
  def int targets = 16

   void run() {
     def POINT = 1
     def BARRIER = 0

     def controllerAlt = new ALT ( [ timeAndHitBarrier, receivePoint] )
    
     setUpBarrier.sync()
     //println "        TC: after setup sync"
     while (true) {
       getActiveTargets.write(1)
       def activeTargets = activatedTargets.read()  // a non-zero list of activated targets
       def runningTargets = activeTargets.size      // greater than zero
       //println "        TC: $runningTargets targets $activeTargets running"
       def ChannelOutputList sendList = []
       for ( t in activeTargets) {
         sendList.append(sendPoint[t])
       }
       def active = true
       goBarrier.sync()  //sync here  between DC BM TC and running targets
       //println "TC:  goBarrier sync"
       while (active) {
         switch ( controllerAlt.priSelect() ) {
           case BARRIER:
             active = false
             //println "        TC:  has barriered on timeout or hit"
             break
           case POINT:
             def point = receivePoint.read()
             sendList.write(point)
             break
         }
       }
     }
  }
}