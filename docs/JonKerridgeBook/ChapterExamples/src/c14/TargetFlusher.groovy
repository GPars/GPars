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
            while (targetsInBucket == 0) {
                currentBucket = (currentBucket + 1) % nBuckets
                targetsInBucket = buckets[currentBucket].holding()
            }
            initBarrier.reset(targetsInBucket)
            targetsFlushed.write(targetsInBucket)
            buckets[currentBucket].flush()
            currentBucket = (currentBucket + 1) % nBuckets
        }
    }
}