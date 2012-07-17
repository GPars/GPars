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
 */

package groovyx.gpars.benchmark.caliper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.caliper.model.Instrument;
import com.google.caliper.model.Measurement;
import com.google.caliper.model.Result;
import com.google.caliper.model.Run;
import com.google.caliper.model.Scenario;
import com.google.caliper.model.VM;
import com.google.caliper.runner.ResultProcessor;
import com.google.caliper.util.LinearTranslation;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.google.common.primitives.Doubles;
import groovyx.gpars.benchmark.caliper.instrument.HTMLBuilder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class ChartBuilder implements ResultProcessor {

    private static final int maxParamWidth = 30;



    // TODO(schmoe): these feel dirty - they assume a ConsoleResultProcessor is single-use, or at
    // least non-overlapping-use, which is only sorta necessary. Seems like handleResults should
    // create a one-per-call state object containing these fields and pass that around instead of
    // having these as member vars.

    private Run run;
    private Table<ScenarioName, AxisName, AxisValue> scenarioLocalVars;
    private Map<ScenarioName, ProcessedResult> processedResults;
    private List<Axis> sortedAxes;
    private ImmutableSortedSet<ScenarioName> sortedScenarioNames;

    private double minValue;
    private double maxValue;



    @Override public void handleResults(Run run) {
        this.run = run;
        Map<String, VM> vms = Maps.uniqueIndex(run.vms, VM_LOCAL_NAME_FUNCTION);
        this.scenarioLocalVars = HashBasedTable.create();
        for (Scenario scenario : run.scenarios) {
            ScenarioName scenarioName = new ScenarioName(scenario.localName);
            scenarioLocalVars.put(
                    scenarioName, new AxisName("benchmark"), new AxisValue(scenario.benchmarkMethodName));
            scenarioLocalVars.put(
                    scenarioName, new AxisName("vm"), new AxisValue(vms.get(scenario.vmLocalName).vmName));
            for (Entry<String, String> entry : scenario.userParameters.entrySet()) {
                scenarioLocalVars.put(
                        scenarioName, new AxisName(entry.getKey()), new AxisValue(entry.getValue()));
            }
            for (Entry<String, String> entry : scenario.vmArguments.entrySet()) {
                scenarioLocalVars.put(
                        scenarioName, new AxisName(entry.getKey()), new AxisValue(entry.getValue()));
            }
        }

        for (Instrument instrument : run.instruments) {
            displayResults(instrument);
        }
    }

    private void displayResults(Instrument instrument) {
        processedResults = Maps.newHashMap();
        for (Result result : run.results) {
            ScenarioName scenarioLocalName = new ScenarioName(result.scenarioLocalName);
            if (instrument.localName.equals(result.instrumentLocalName)) {
                ProcessedResult existingResult = processedResults.get(scenarioLocalName);
                if (existingResult == null) {
                    processedResults.put(scenarioLocalName, new ProcessedResult(result));
                } else {
                    processedResults.put(scenarioLocalName, combineResults(existingResult, result));
                }
            }
        }

        double minOfMedians = Double.POSITIVE_INFINITY;
        double maxOfMedians = Double.NEGATIVE_INFINITY;

        for (ProcessedResult result : processedResults.values()) {
            minOfMedians = Math.min(minOfMedians, result.median);
            maxOfMedians = Math.max(maxOfMedians, result.median);
        }

        Multimap<AxisName, AxisValue> axisValues = LinkedHashMultimap.create();
        for (Scenario scenario : run.scenarios) {
            ScenarioName scenarioName = new ScenarioName(scenario.localName);
            // only include scenarios with data for this instrument
            if (processedResults.keySet().contains(scenarioName)) {
                for (Entry<AxisName, AxisValue> entry : scenarioLocalVars.row(scenarioName).entrySet()) {
                    axisValues.put(entry.getKey(), entry.getValue());
                }
            }
        }

        List<Axis> axes = Lists.newArrayList();
        for (Entry<AxisName, Collection<AxisValue>> entry : axisValues.asMap().entrySet()) {
            Axis axis = new Axis(entry.getKey(), entry.getValue());
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
        for (ProcessedResult result : processedResults.values()) {
            sumOfAllMeasurements += result.median;
        }
        for (Axis axis : axes) {
            int numValues = axis.numberOfValues();
            double[] sumForValue = new double[numValues];
            for (Entry<ScenarioName, ProcessedResult> entry : processedResults.entrySet()) {
                ScenarioName scenarioLocalName = entry.getKey();
                ProcessedResult result = entry.getValue();
                sumForValue[axis.index(scenarioLocalName)] += result.median;
            }
            double mean = sumOfAllMeasurements / sumForValue.length;
            double variance = 0;
            for (double value : sumForValue) {
                double distance = value - mean;
                variance += distance * distance;
            }
            axis.variance = variance / numValues;
        }

        this.sortedAxes = new VarianceOrdering().reverse().sortedCopy(axes);
        this.sortedScenarioNames =
                ImmutableSortedSet.copyOf(new ByAxisOrdering(), processedResults.keySet());
        this.maxValue = maxOfMedians;
        this.minValue = minOfMedians;

        buildChart();
    }

    private void buildChart(){
        /* X axis label is User Parameter,
           A parameter is singleton if it has only one value
           Assumption here is there is only one user parameter(numberOfClients)
           and there are multiple scenarios */
        String xLabel = null;

        for (Axis axis : sortedAxes) {
            if (!axis.isSingleton()) {
                xLabel = axis.name.toString();
                break;
            }
        }

        if(xLabel.equals(null)){
            System.out.println( "Need more than 1 scenario to build chart");
            return;
        }

        /* Y axis label is the unit of measurements
           It is ns for latency, and messages per second for throughput */
        ChartBuilder.ProcessedResult firstResult = processedResults.values().iterator().next();
        String yLabel = firstResult.responseUnit;

        ArrayList<Integer> yValues = new ArrayList<Integer>();
        ArrayList<String> xValues = new ArrayList<String>();
        for (ChartBuilder.ScenarioName scenarioLocalName : sortedScenarioNames) {
            ChartBuilder.ProcessedResult result = processedResults.get(scenarioLocalName);
            yValues.add((int)result.median);
            for (ChartBuilder.Axis axis : sortedAxes) {
                if (!axis.isSingleton()) {
                    xValues.add(axis.get(scenarioLocalName).toString());
                }
            }
        }

        /* Looking for the previous measurements of this benchmark
           that has the same number of scenarios by
           parsing Json files saved in caliper-results folder.
           Do not change the name of the file nor the name of the benchmark method */
        File dir = new File("caliper-results");
        List<ArrayList<Integer>> historyYValues = new ArrayList<ArrayList<Integer>>();
        List<ArrayList<String>> historyXValues = new ArrayList<ArrayList<String>>();
        ArrayList<String> historyNames = new ArrayList<String>();
        int counter =0;
        for(File file : dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                if(name.matches(".*"+run.label+".*"+"\\.json")){
                    return true;
                }
                return false;
            }
        })){
            JsonFileParser parser = new JsonFileParser(file);
            ArrayList<Integer> yVal = parser.getMeasurements();
            if(yVal.size() == yValues.size()){
                historyYValues.add(parser.getMeasurements());
                historyXValues.add(parser.getScenarios());
                String [] tempList = file.getName().split("\\.");
                historyNames.add(tempList[tempList.length -2]);
                if(++counter >= 3) break;
            }

        }

        /* Calculating the range of the cart and the range of each data set.
         */
        List<Integer> maxList = new ArrayList<Integer>();
        for(ArrayList<Integer> medians: historyYValues){
            int max = -1;
            for(int val: medians){
                max = Math.max(max, val);
            }
            maxList.add(max);
        }

        ArrayList<Integer> rangeList = new ArrayList<Integer>();
        rangeList.add(new Integer(xValues.get(0)));
        rangeList.add(new Integer(xValues.get(xValues.size()-1)));
        rangeList.add(1);
        rangeList.add((int)maxValue);

        int globalMax = (int)maxValue;
        for(int val: maxList){
            globalMax = Math.max(globalMax, val);
//            rangeList.add(new Integer(xValues.get(0)));
//            rangeList.add(new Integer(xValues.get(xValues.size()-1)));
            rangeList.add(1);
            rangeList.add(val);
        }

        HTMLBuilder htmlBuilder = new HTMLBuilder(run);
        htmlBuilder.buildBarGraphURL(xValues,yValues,historyXValues,historyYValues,historyNames,rangeList,xLabel,yLabel,globalMax);
    }

    private ProcessedResult combineResults(ProcessedResult r1, Result r2) {
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

        Axis(AxisName name, Collection<AxisValue> values) {
            this.name = name;
            this.values = ImmutableList.copyOf(values);
            checkArgument(!this.values.isEmpty());

            int maxLen = name.toString().length();
            for (AxisValue value : values) {
                maxLen = Math.max(maxLen, value.toString().length());
            }
            this.maxLength = Math.min(maxLen, maxParamWidth);
        }

        AxisValue get(ScenarioName scenarioLocalName) {
            return scenarioLocalVars.get(scenarioLocalName, name);
        }

        int index(ScenarioName scenarioLocalName) {
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
        public int compare(Axis a, Axis b) {
            return Double.compare(a.variance, b.variance);
        }
    }

    /**
     * Orders scenarios by the axes.
     */
    private class ByAxisOrdering extends Ordering<ScenarioName> {
        public int compare(ScenarioName scenarioALocalName, ScenarioName scenarioBLocalName) {
            for (Axis axis : sortedAxes) {
                int aValue = axis.index(scenarioALocalName);
                int bValue = axis.index(scenarioBLocalName);
                int diff = aValue - bValue;
                if (diff != 0) {
                    return diff;
                }
            }
            return 0;
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static int floor(double d) {
        return (int) Math.floor(d);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static int ceil(double d) {
        return (int) Math.ceil(d);
    }

    private static String truncate(String s, int maxLength) {
        if (s.length() <= maxLength) {
            return s;
        } else {
            return s.substring(0, maxLength - 1) + "+";
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

        private ProcessedResult(Result modelResult) {
            this.modelResult = modelResult;
            values = getValues(modelResult.measurements);
            min = Doubles.min(values);
            max = Doubles.max(values);
            median = computeMedian(values);
            mean = computeMean(values);
            Measurement firstMeasurement = modelResult.measurements.get(0);
            responseUnit = firstMeasurement.unit;
            responseDesc = firstMeasurement.description;
        }

        private static double[] getValues(Collection<Measurement> measurements) {
            double[] values = new double[measurements.size()];
            int i = 0;
            for (Measurement measurement : measurements) {
                values[i] = measurement.value / measurement.weight;
                i++;
            }
            return values;
        }

        // TODO(schmoe): consider copying com.google.math.Sample into caliper.util
        private static double computeMedian(double[] values) {
            double[] sortedValues = values.clone();
            Arrays.sort(sortedValues);
            if (sortedValues.length % 2 == 1) {
                return sortedValues[(int) sortedValues.length / 2];
            } else {
                double high = sortedValues[(int) sortedValues.length / 2];
                double low = sortedValues[((int) sortedValues.length / 2) - 1];
                return (low + high) / 2;
            }
        }

        private static double computeMean(double[] values) {
            double sum = 0;
            for (double value : values) {
                sum += value;
            }
            return sum / values.length;
        }
    }

    // TODO(fry): eventually migrate to a Java data model b/5895975

    private static class ScenarioName {
        private final String name;

        public ScenarioName(String name) {
            this.name = checkNotNull(name);
        }

        @Override public String toString() {
            return name;
        }

        @Override public int hashCode() {
            return name.hashCode();
        }

        @Override public boolean equals(Object other) {
            if (other instanceof ScenarioName) {
                ScenarioName that = (ScenarioName) other;
                return this.name.equals(that.name);
            }
            return false;
        }
    }

    private static class AxisName {
        private final String name;

        public AxisName(String name) {
            this.name = checkNotNull(name);
        }

        @Override public String toString() {
            return name;
        }

        @Override public int hashCode() {
            return name.hashCode();
        }

        @Override public boolean equals(Object other) {
            if (other instanceof AxisName) {
                AxisName that = (AxisName) other;
                return this.name.equals(that.name);
            }
            return false;
        }
    }

    private static class AxisValue {
        private final String value;

        public AxisValue(String value) {
            this.value = checkNotNull(value);
        }

        @Override public String toString() {
            return value;
        }

        @Override public int hashCode() {
            return value.hashCode();
        }

        @Override public boolean equals(Object other) {
            if (other instanceof AxisValue) {
                AxisValue that = (AxisValue) other;
                return this.value.equals(that.value);
            }
            return false;
        }
    }

    private static final Function<VM, String> VM_LOCAL_NAME_FUNCTION =
            new Function<VM, String>() {
                @Override public String apply(VM vm) {
                    return vm.localName;
                }
            };


}


