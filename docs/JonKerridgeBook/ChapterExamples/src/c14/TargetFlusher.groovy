package c14
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

class TargetFlusher implements CSProcess {
  def buckets
  def ChannelOutput targetsFlushed
  def ChannelInput flushNextBucket
  def Barrier initBarrier

  void run() {
    def nBuckets = buckets.size()
    def currentBucket = 0
    def targetsInBucket = 0
    while (true) {
      flushNextBucket.read()
      targetsInBucket = buckets[currentBucket].holding()
      while ( targetsInBucket == 0) {
        currentBucket = (currentBucket + 1) % nBuckets
        targetsInBucket = buckets[currentBucket].holding()        
      }
      initBarrier.reset( targetsInBucket)
      targetsFlushed.write(targetsInBucket)
      buckets[currentBucket].flush()
      currentBucket = (currentBucket + 1) % nBuckets
    }
  }
}