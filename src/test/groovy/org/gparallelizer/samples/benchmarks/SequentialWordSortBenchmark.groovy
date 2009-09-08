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

public class SequentialWordSortBenchmark implements Benchmark {

    final String docs = 'C:/dev/TeamCity/logs/'

    public long perform(final int numberOfIterations) {

        final long t1 = System.currentTimeMillis()

        List<List<String>> sorted = []
        new File(docs).eachFile {
            sorted << sortedWords(it.name)
        }

        final long t2 = System.currentTimeMillis()

        sorted = null
        System.gc()
        Thread.sleep 3000
        
        return (t2 - t1)
    }

     private List<String> sortedWords(String fileName) {
        parseFile(fileName).sort {it.toLowerCase()}
    }

    private List<String> parseFile(String fileName) {
        List<String> words = []
        new File(docs + fileName).splitEachLine(' ') {words.addAll(it)}
        return words
    }
}
