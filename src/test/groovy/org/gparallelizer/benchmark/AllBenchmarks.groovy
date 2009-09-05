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
def limits = [1, 10, 100, 1000, 10000, 100000, 200000] * 3
def times = limits.sort().collect { sleep 100; measure (it, dfv) }

for (i in 0..<limits.size()) {
    printf "%7d: %5.3f\n", limits[i], times[i]/1000
}

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