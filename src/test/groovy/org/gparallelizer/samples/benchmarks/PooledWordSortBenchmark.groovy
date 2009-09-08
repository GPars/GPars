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

package org.gparallelizer.samples.benchmarks

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActor
import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.PooledActors

public class PooledWordSortBenchmark implements Benchmark {

    public long perform(final int numberOfIterations) {
        PooledActors.defaultPooledActorGroup.resize 23

        final long t1 = System.currentTimeMillis()
        final SortMaster master = new SortMaster(numActors: 10, docRoot:'C:/dev/TeamCity/logs/')
        master.start()
        master.waitUntilDone()
        final long t2 = System.currentTimeMillis()
        master.stopAll()
        PooledActors.defaultPooledActorGroup.resetDefaultSize()

        return (t2 - t1)
    }
}
private final class FileToSort { String fileName }
private final class SortResult { String fileName; List<String> words }

class WordSortActor extends AbstractPooledActor {
     private List<String> sortedWords(String fileName) {
        parseFile(fileName).sort {it.toLowerCase()}
    }

    private List<String> parseFile(String fileName) {
        List<String> words = []
        new File(fileName).splitEachLine(' ') {words.addAll(it)}
        return words
    }

    void act() {
        loop {
            react {message ->
                switch (message) {
                    case FileToSort:
                        reply new SortResult(fileName: message.fileName, words: sortedWords(message.fileName))
                }
            }
        }
    }
}

final class SortMaster extends AbstractPooledActor {

    String docRoot = '/'
    int numActors = 1

    List<List<String>> sorted = []
    private CountDownLatch startupLatch = new CountDownLatch(1)
    private CountDownLatch doneLatch
    private List<PooledActor> workers

    private void beginSorting() {
        workers = createWorkers()
        int cnt = sendTasksToWorkers()
        doneLatch = new CountDownLatch(cnt)
        startupLatch.countDown()
    }

    private List createWorkers() {
        return (1..numActors).collect {new WordSortActor().start()}
    }

    private int sendTasksToWorkers() {
        int cnt = 0
        new File(docRoot).eachFile {
            workers[cnt % numActors] << new FileToSort(fileName: it)
            cnt += 1
        }
        return cnt
    }

    public void waitUntilDone() {
        startupLatch.await()
        doneLatch.await()
    }

    void act() {
        beginSorting()
        loop {
            react {
                switch (it) {
                    case SortResult:
                        sorted << it.words
                        doneLatch.countDown()
                }
            }
        }
    }

    public void stopAll() {
        workers.each {it.stop()}
        stop()
        sorted = null
        workers = null
    }
}
