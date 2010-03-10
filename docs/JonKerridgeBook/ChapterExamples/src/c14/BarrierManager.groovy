package c14

import org.jcsp.lang.*
import org.jcsp.groovy.*


class BarrierManager implements CSProcess{
  def AltingBarrier timeAndHitBarrier
  def AltingBarrier finalBarrier
  def Barrier goBarrier
  def Barrier setUpBarrier

  void run() {
    def timeHitAlt = new ALT ([timeAndHitBarrier])
    def finalAlt = new ALT ([finalBarrier])
    setUpBarrier.sync()
    //println "    BM: synced on setup awaiting go"
    while (true){
      goBarrier.sync()
      //println "    BM: synced on go "
      def t = timeHitAlt.select()
      //println "    BM: barriered on timeout or hit "
      def f = finalAlt.select()
      //println "    BM: has barriered on final"
    }
  }

}