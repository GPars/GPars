// GPars - Groovy Parallel Systems
//
// Copyright © 2008–10,2012  The original author or authors
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

package groovyx.gpars.samples.actors.stateful

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.group.DefaultPGroup
import java.util.concurrent.CountDownLatch

import groovy.transform.PackageScope

final def group = new DefaultPGroup(23)

//Messages
@PackageScope final class FileToSort {
    String fileName
}
@PackageScope final class SortResult {
    String fileName;
    List<String> words
}

//Worker actor
final class WordSortActor extends DefaultActor {

    def WordSortActor(group) { this.parallelGroup = group }

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
//                        println "Sorting file=${message.fileName} on thread ${Thread.currentThread().name}"
                        reply new SortResult(fileName: message.fileName, words: sortedWords(message.fileName))
                }
            }
        }
    }
}

//Master actor
final class SortMaster extends DefaultActor {

    String docRoot
    int numActors = 1

    List<List<String>> sorted = []
    private CountDownLatch startupLatch = new CountDownLatch(1)
    private CountDownLatch doneLatch

    private void beginSorting() {
        int cnt = sendTasksToWorkers()
        doneLatch = new CountDownLatch(cnt)
    }

    private List createWorkers() {
        return (1..numActors).collect {new WordSortActor(this.parallelGroup).start()}
    }

    private int sendTasksToWorkers() {
        List<Actor> workers = createWorkers()
        int cnt = 0
        new File(this.docRoot).eachFile {
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
        startupLatch.countDown()
        loop {
            react {
                switch (it) {
                    case SortResult:
                        sorted << it.words
                        doneLatch.countDown()
//                        println "Received results for file=${it.fileName}"
                }
            }
        }
    }
}
String docRoot = ''  //set a folder to scan with absolute path

if (docRoot) {
//start the actors to sort words
    def master = new SortMaster(parallelGroup: group, docRoot: docRoot, numActors: 21).start()
    master.waitUntilDone()
    final long t1 = System.currentTimeMillis()
    master = new SortMaster(parallelGroup: group, docRoot: docRoot, numActors: 21).start()
    master.waitUntilDone()
    final long t2 = System.currentTimeMillis()
    println 'Done ' + (t2 - t1)
//println master.sorted
}
