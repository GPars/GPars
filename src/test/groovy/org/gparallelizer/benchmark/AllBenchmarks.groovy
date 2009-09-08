//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.benchmark

import org.gparallelizer.samples.dataflow.BenchmarkManyDataFlowVariables

long measure (int limit, Script worker) {
    print "running ${worker.class.name} with limit $limit ... "
    worker.binding = new Binding([limit: limit])
    def begin = System.nanoTime()
    worker.run()
    def end = System.nanoTime()
    println 'done'
    (end - begin) / 1000000
}

def dfv = new BenchmarkManyDataFlowVariables()
def limits = [1, 10, 100, 1000, 10000, 100000, 200000, 500000, 1000000, 2500000] * 3
def times = limits.sort().collect { sleep 100; measure (it, dfv) }

for (i in 0..<limits.size()) {
    printf "%7d: %5.3f\n", limits[i], times[i]/1000
}

/* measures 6.9.09, Java 1.5 -Xmx512M, Groovy 1.6, MacBookPro Core 2 Duo 2.8 GHz
      1: 0,570
      1: 0,001
      1: 0,001
     10: 0,003
     10: 0,002
     10: 0,002
    100: 0,006
    100: 0,004
    100: 0,007
   1000: 0,021
   1000: 0,023
   1000: 0,004
  10000: 0,032
  10000: 0,071
  10000: 0,032
 100000: 0,440
 100000: 0,337
 100000: 0,326
 200000: 0,680
 200000: 0,619
 200000: 0,573
 500000: 1,666
 500000: 1,460
 500000: 1,376
1000000: 3,119
1000000: 2,794
1000000: 2,733
2500000: 8,102
2500000: 7,171
2500000: 6,995
*/

/* measures 6.9.09, Java 1.5, Groovy 1.6, MacBookPro Core 2 Duo 2.8 GHz
      1:  0,103
      1:  0,001
      1:  0,001
     10:  0,012
     10:  0,004
     10:  0,004
    100:  0,049
    100:  0,021
    100:  0,015
   1000:  0,148
   1000:  0,137
   1000:  0,065
  10000:  0,712
  10000:  0,579
  10000:  0,557
 100000:  5,223
 100000:  5,100
 100000:  5,011
 200000:  9,824
 200000: 11,834
 200000:  9,856
*/

