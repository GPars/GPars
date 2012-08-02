/**
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by Hyuk Don Kwon
 * Modified to support graphing using Google Chart
 */

package groovyx.gpars.benchmark.caliper.chart;

import com.google.caliper.model.Instrument;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Result;
import com.google.caliper.model.Run;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.VM;
import com.google.caliper.runner.ResultProcessor;
import com.google.common.base.Function;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.google.common.primitives.Doubles;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

final class ChartBuilder implements ResultProcessor {

    private static final int maxParamWidth = 30;
    private Run run;
    private Table<ScenarioName, AxisName, AxisValue> scenarioLocalVars;
    private Map<ScenarioName, ProcessedResult> processedResults;
    private List<Axis> sortedAxes;
    private ImmutableSortedSet<ScenarioName> sortedScenarioNames;
    private double maxValue;

    @Override
    public void handleResults(final Run run) {
        this.run = run;
        final Map<String, VM> vms = Maps.uniqueIndex(run.vms, VM_LOCAL_NAME_FUNCTION);
        this.scenarioLocalVars = HashBasedTable.create();
        for (final Scenario scenario : run.scenarios) {
            final ScenarioName scenarioName = new ScenarioName(scenario.localName);
            scenarioLocalVars.put(
                    scenarioName, new AxisName("benchmark"), new AxisValue(scenario.benchmarkMethodName));
            scenarioLocalVars.put(
                    scenarioName, new AxisName("vm"), new AxisValue(vms.get(scenario.vmLocalName).vmName));
            for (final Entry<String, String> entry : scenario.userParameters.entrySet()) {
                scenarioLocalVars.put(
                        scenarioName, new AxisName(entry.getKey()), new AxisValue(entry.getValue()));
            }
            for (final Entry<String, String> entry : scenario.vmArguments.entrySet()) {
                scenarioLocalVars.put(
                        scenarioName, new AxisName(entry.getKey()), new AxisValue(entry.getValue()));
            }
        }

        for (final Instrument instrument : run.instruments) {
            displayResults(instrument);
        }
    }

    private void displayResults(final Instrument instrument) {
        processedResults = Maps.newHashMap();
        for (final Result result : run.results) {
            final ScenarioName scenarioLocalName = new ScenarioName(result.scenarioLocalName);
            if (instrument.localName.equals(result.instrumentLocalName)) {
                final ProcessedResult existingResult = processedResults.get(scenarioLocalName);
                if (existingResult == null) {
                    processedResults.put(scenarioLocalName, new ProcessedResult(result));
                } else {
                    processedResults.put(scenarioLocalName, combineResults(existingResult, result));
                }
            }
        }

        double minOfMedians = Double.POSITIVE_INFINITY;
        double maxOfMedians = Double.NEGATIVE_INFINITY;

        for (final ProcessedResult result : processedResults.values()) {
            minOfMedians = Math.min(minOfMedians, result.median);
            maxOfMedians = Math.max(maxOfMedians, result.median);
        }

        final Multimap<AxisName, AxisValue> axisValues = LinkedHashMultimap.create();
        for (final Scenario scenario : run.scenarios) {
            final ScenarioName scenarioName = new ScenarioName(scenario.localName);
            // only include scenarios with data for this instrument
            if (processedResults.keySet().contains(scenarioName)) {
                for (final Entry<AxisName, AxisValue> entry : scenarioLocalVars.row(scenarioName).entrySet()) {
                    axisValues.put(entry.getKey(), entry.getValue());
                }
            }
        }

        final List<Axis> axes = Lists.newArrayList();
        for (final Entry<AxisName, Collection<AxisValue>> entry : axisValues.asMap().entrySet()) {
            final Axis axis = new Axis(entry.getKey(), entry.getValue());
            axes.add(axis);
        }

        /*
        * Figure out how much influence each axis has on the measured value.
        * We sum the measurements taken with each value of each axis. For
        * axes that have influence on the measurement, the sums will differ
        * by value. If the axis has little influence, the sums will be similar
        * to one another and close to the overall average. We take the variance
        * across each axis' collection of sums. Higher variance implies higher
        * influence on the measured result.
        */
        double sumOfAllMeasurements = 0;
        for (final ProcessedResult result : processedResults.values()) {
            sumOfAllMeasurements += result.median;
        }
        for (final Axis axis : axes) {
            final int numValues = axis.numberOfValues();
            final double[] sumForValue = new double[numValues];
            for (final Entry<ScenarioName, ProcessedResult> entry : processedResults.entrySet()) {
                final ScenarioName scenarioLocalName = entry.getKey();
                final ProcessedResult result = entry.getValue();
                sumForValue[axis.index(scenarioLocalName)] += result.median;
            }
            final double mean = sumOfAllMeasurements / sumForValue.length;
            double variance = 0;
            for (final double value : sumForValue) {
                final double distance = value - mean;
                variance += distance * distance;
            }
            axis.variance = variance / numValues;
        }

        this.sortedAxes = new VarianceOrdering().reverse().sortedCopy(axes);
        this.sortedScenarioNames =
                ImmutableSortedSet.copyOf(new ByAxisOrdering(), processedResults.keySet());
        this.maxValue = maxOfMedians;

        buildChart();
    }

    private void buildChart() {
        /* X axis label is User Parameter,
           A parameter is singleton if it has only one value
           Assumption here is there is only one user parameter(numberOfClients)
           and there are multiple scenarios */
        String xLabel = null;

        for (final Axis axis : sortedAxes) {
            if (!axis.isSingleton()) {
                xLabel = axis.name.toString();
                break;
            }
        }

        if (xLabel == null) {
            System.out.println("Need more than 1 scenario to build chart");
            return;
        }

        /* Y axis label is the unit of measurements
           It is ns for latency, and messages per second for throughput */
        final ChartBuilder.ProcessedResult firstResult = processedResults.values().iterator().next();
        final String yLabel = firstResult.responseUnit;

        final ArrayList<Long> yValues = new ArrayList<Long>();
        final ArrayList<String> xValues = new ArrayList<String>();
        for (final ChartBuilder.ScenarioName scenarioLocalName : sortedScenarioNames) {
            final ChartBuilder.ProcessedResult result = processedResults.get(scenarioLocalName);
            yValues.add((long) result.median);
            for (final ChartBuilder.Axis axis : sortedAxes) {
                if (!axis.isSingleton()) {
                    xValues.add(axis.get(scenarioLocalName).toString());
                }
            }
        }

        /* Looking for the previous measurements of this benchmark
           that has the same number of scenarios by
           parsing Json files saved in caliper-results folder.
           Do not change the name of the file nor the name of the benchmark method */
        final File dir = new File("caliper-results");
        final List<ArrayList<Long>> historyYValues = new ArrayList<ArrayList<Long>>();
        final List<ArrayList<String>> historyXValues = new ArrayList<ArrayList<String>>();
        final ArrayList<String> historyNames = new ArrayList<String>();

        /* Pick history files that are most recently created */
        final Queue<File> fileQueue = new PriorityQueue<File>(10, new Comparator<File>() {
            @Override
            public int compare(final File o1, final File o2) {
                final String[] tempList1 = o1.getName().split("\\.");
                final String[] tempList2 = o2.getName().split("\\.");
                Date date1 = null, date2 = null;
                final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ssZ");
                try {
                    date1 = simpleDateFormat.parse(tempList1[tempList1.length - 2]);
                    date2 = simpleDateFormat.parse(tempList2[tempList2.length - 2]);
                    return date2.compareTo(date1);
                } catch (ParseException e) {
                    throw new Error("Cannot parse date of archived reports.", e);
                }
            }
        });

        if (dir.isDirectory()) {
            for (final File file : dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return name.matches(".*" + run.label + ".*" + "\\.json");
                }
            })) {
                final JsonFileParser parser = new JsonFileParser(file);
                final Collection<Long> yVal = parser.getMeasurements();
                if (yVal.size() == yValues.size()) {
                    fileQueue.add(file);
                }
            }
        }

        /* Picks three latest histories.
           Overflows Google Chart if try to plot more than 3 histories.(Total of 4)
         */
        for (int i = 0; i < 3; i++) {
            if (fileQueue.isEmpty()) break;
            final File file = fileQueue.poll();
            final JsonFileParser parser = new JsonFileParser(file);
            historyYValues.add(parser.getMeasurements());
            historyXValues.add(parser.getScenarios());
            final String[] tempList = file.getName().split("\\.");
            historyNames.add(tempList[tempList.length - 2]);
        }

        /* Calculating the range of the cart and the range of each data set.
         */
        final Collection<Long> maxList = new ArrayList<Long>();
        for (final Iterable<Long> medians : historyYValues) {
            long max = -1L;
            for (final long val : medians) {
                max = Math.max(max, val);
            }
            maxList.add(max);
        }

        long globalMax = (long) maxValue;
        for (final long val : maxList) {
            globalMax = Math.max(globalMax, val);
        }

        final HTMLBuilder htmlBuilder = new HTMLBuilder(run);
        htmlBuilder.buildBarGraphURL(xValues, yValues, historyXValues, historyYValues, historyNames, xLabel, yLabel, globalMax);
    }

    private static ProcessedResult combineResults(final ProcessedResult r1, final Result r2) {
        checkArgument(r1.modelResult.instrumentLocalName.equals(r2.instrumentLocalName));
        checkArgument(r1.modelResult.scenarioLocalName.equals(r2.scenarioLocalName));
        r2.measurements = ImmutableList.<Measurement>builder()
                .addAll(r1.modelResult.measurements)
                .addAll(r2.measurements)
                .build();
        return new ProcessedResult(r2);
    }

    /**
     * A scenario variable and the set of values to which it has been assigned.
     */
    private class Axis {
        final AxisName name;
        final ImmutableList<AxisValue> values;
        final int maxLength;
        double variance;

        Axis(final AxisName name, final Collection<AxisValue> values) {
            this.name = name;
            this.values = ImmutableList.copyOf(values);
            checkArgument(!this.values.isEmpty());

            int maxLen = name.toString().length();
            for (final AxisValue value : values) {
                maxLen = Math.max(maxLen, value.toString().length());
            }
            this.maxLength = Math.min(maxLen, maxParamWidth);
        }

        AxisValue get(final ScenarioName scenarioLocalName) {
            return scenarioLocalVars.get(scenarioLocalName, name);
        }

        int index(final ScenarioName scenarioLocalName) {
            // assumes that there are no duplicate values
            return values.indexOf(get(scenarioLocalName));
        }

        int numberOfValues() {
            return values.size();
        }

        boolean isSingleton() {
            return numberOfValues() == 1;
        }
    }

    /**
     * Orders the different axes by their variance. This results
     * in an appropriate grouping of output values.
     */
    private static class VarianceOrdering extends Ordering<Axis> {
        @Override
        public int compare(final Axis a, final Axis b) {
            return Double.compare(a.variance, b.variance);
        }
    }

    /**
     * Orders scenarios by the axes.
     */
    private class ByAxisOrdering extends Ordering<ScenarioName> {
        @Override
        public int compare(final ScenarioName scenarioALocalName, final ScenarioName scenarioBLocalName) {
            for (final Axis axis : sortedAxes) {
                final int aValue = axis.index(scenarioALocalName);
                final int bValue = axis.index(scenarioBLocalName);
                final int diff = aValue - bValue;
                if (diff != 0) {
                    return diff;
                }
            }
            return 0;
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static int floor(final double d) {
        return (int) Math.floor(d);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static int ceil(final double d) {
        return (int) Math.ceil(d);
    }

    private static String truncate(final String s, final int maxLength) {
        if (s.length() <= maxLength) {
            return s;
        } else {
            return s.substring(0, maxLength - 1) + '+';
        }
    }


    private static class ProcessedResult {
        private final Result modelResult;
        private final double[] values;
        private final double min;
        private final double max;
        private final double median;
        private final double mean;
        private final String responseUnit;
        private final String responseDesc;

        private ProcessedResult(final Result modelResult) {
            this.modelResult = modelResult;
            values = getValues(modelResult.measurements);
            min = Doubles.min(values);
            max = Doubles.max(values);
            median = computeMedian(values);
            mean = computeMean(values);
            final Measurement firstMeasurement = modelResult.measurements.get(0);
            responseUnit = firstMeasurement.unit;
            responseDesc = firstMeasurement.description;
        }

        private static double[] getValues(final Collection<Measurement> measurements) {
            final double[] values = new double[measurements.size()];
            int i = 0;
            for (final Measurement measurement : measurements) {
                values[i] = measurement.value / measurement.weight;
                i++;
            }
            return values;
        }

        private static double computeMedian(final double[] values) {
            final double[] sortedValues = values.clone();
            Arrays.sort(sortedValues);
            //noinspection BadOddness
            if (sortedValues.length % 2 == 1) {
                return sortedValues[sortedValues.length / 2];
            } else {
                final double high = sortedValues[sortedValues.length / 2];
                final double low = sortedValues[sortedValues.length / 2 - 1];
                //noinspection MagicNumber
                return (low + high) / 2.0;
            }
        }

        private static double computeMean(final double[] values) {
            double sum = 0;
            for (final double value : values) {
                sum += value;
            }
            return sum / (double) values.length;
        }
    }

    private static class ScenarioName {
        private final String name;

        private ScenarioName(final String name) {
            this.name = checkNotNull(name);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(final Object other) {
            if (other instanceof ScenarioName) {
                final ScenarioName that = (ScenarioName) other;
                return this.name.equals(that.name);
            }
            return false;
        }
    }

    private static class AxisName {
        private final String name;

        private AxisName(final String name) {
            this.name = checkNotNull(name);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(final Object other) {
            if (other instanceof AxisName) {
                final AxisName that = (AxisName) other;
                return this.name.equals(that.name);
            }
            return false;
        }
    }

    private static class AxisValue {
        private final String value;

        private AxisValue(final String value) {
            this.value = checkNotNull(value);
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(final Object other) {
            if (other instanceof AxisValue) {
                final AxisValue that = (AxisValue) other;
                return this.value.equals(that.value);
            }
            return false;
        }
    }

    private static final Function<VM, String> VM_LOCAL_NAME_FUNCTION =
            new Function<VM, String>() {
                @Override
                public String apply(final VM vm) {
                    return vm.localName;
                }
            };
}


