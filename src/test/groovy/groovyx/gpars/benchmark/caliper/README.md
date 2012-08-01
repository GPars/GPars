# Introduction

A set of benchmarks for the GPars concurrency framework using Google Caliper

# Instructions

1. cd into the GPars project directory
1. run the *runBenchmarks* task using Gradle
1. The benchmarks should take about 40-50 minutes to run

# Results

1. The results are printed to the console and are stored locally as charts in HTML files and as raw data in JSON files
1. The HTML files are stored at *projectDirectory/caliper-charts* and the JSON files are at *projectDirectory/caliper-results*
1. The charts are generated using Google's chart-builder service so an internet connection is required
1. The charts will compare the current benchmark run to the last three benchmark runs

# The Benchmarks

1. The throughput benchmarks measure how long it takes for pairs of actors to send a given number of messages.
    When the results are shown, the number of clients refer to the number of pairs of actors
2. The latency benchmarks measure how long it takes for a message to propagate through groups of actors.
    When the results are shown, the number of clients refer to the number of groups of actors
3. The computation benchmarks are similar to the throughput benchmarks However, each actor will compute the digits of pi when it receives a message
    When the results are shown, the number of clients refer to the number of pairs of actors

Each of the benchmarks are written with DynamicDispatch and StaticDispatch actors
