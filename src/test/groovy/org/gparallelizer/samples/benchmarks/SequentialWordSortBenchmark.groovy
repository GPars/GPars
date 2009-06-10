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
        Thread.sleep 10000
        
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