package c14

import org.jcsp.lang.*
import org.jcsp.groovy.*

class TargetManager implements CSProcess {
  def ChannelInput targetIdFromTarget
  def ChannelInput getActiveTargets
  def ChannelOutput activatedTargets
  def ChannelOutput activatedTargetsToDC
  def ChannelInput targetsFlushed
  def ChannelOutput flushNextBucket
  def Barrier setUpBarrier

   void run() {
    setUpBarrier.sync()
    //println "TM: setup sync"
    while (true) {
      def targetList = [ ]
      getActiveTargets.read()
      flushNextBucket.write(1)
      def targetsRunning = targetsFlushed.read()  
      while (targetsRunning > 0) {
         targetList << targetIdFromTarget.read()
         targetsRunning = targetsRunning - 1
      }
      activatedTargets.write(targetList)
      activatedTargetsToDC.write(targetList)
    }
  }
}