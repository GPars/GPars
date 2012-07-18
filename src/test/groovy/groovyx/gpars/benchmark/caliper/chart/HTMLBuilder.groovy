package groovyx.gpars.benchmark.caliper.chart

import com.google.caliper.model.Environment
import com.google.caliper.model.Instrument

import com.google.caliper.model.Run

import groovy.xml.MarkupBuilder

import java.text.DateFormat
import java.text.SimpleDateFormat

import groovyx.gpars.benchmark.caliper.chart.GoogleChartBuilder
import groovyx.gpars.benchmark.caliper.ChartBuilder
import com.google.common.collect.ImmutableSortedSet

class HTMLBuilder {
    private Run run
    private List<ChartBuilder.Axis> sortedAxes
    private Map<ChartBuilder.ScenarioName, ChartBuilder.ProcessedResult> processedResults
    private ImmutableSortedSet<ChartBuilder.ScenarioName> sortedScenarioNames;
    private double minValue;
    private double maxValue;

    public HTMLBuilder(Run run) {
        this.run = run
    }

    public void buildLineGraphURL(ArrayList<String> xValues, ArrayList<Integer> yValues, List<ArrayList<String>> historyXValues,List<ArrayList<Integer>> historyYValues,
                            ArrayList<String> historyNames, ArrayList<Integer> rangeList, String xLabel, String yLabel, int globalMax) {


        def chart = new GoogleChartBuilder()
        String result = chart.lineXY{
            size(w:750, h:400)
            title{
                row(run.label)
            }
            data(encoding:'text', numLines:(historyXValues.size()+1)*2) {
                set(xValues.toList())
                set(yValues.toList())
                for(int i=0; i < historyXValues.size(); i++){
                    set(historyXValues[i].toList())
                    set(historyYValues[i].toList())
                }
            }
            colors{
                Random rand = new Random();

                for(int i=0; i < historyXValues.size()+1; i++){
                    String colorHex = "";
                    for(int j=0; j < 3; j++){
                        char c = 48+(rand.nextInt(9))
                        colorHex = colorHex << c <<c
                    }
                    color(colorHex)
                }
            }
            //lineStyle(line1:[1,6,3])
            legend{
                label("Current")
                historyNames.each{
                    label(it)
                }
            }
            axis( bottom:[],left:[], left2:[yLabel], bottom2:[xLabel])
            range([0:[xValues.get(0).toInteger(),xValues.get(xValues.size()-1).toInteger()], 1:[1,globalMax]])


            dataRange(rangeList.toList())

        }

        buildHTML(result)
    }

    public void buildBarGraphURL(ArrayList<String> xValues, ArrayList<Integer> yValues, List<ArrayList<String>> historyXValues,List<ArrayList<Integer>> historyYValues,
                                  ArrayList<String> historyNames, ArrayList<Integer> rangeList, String xLabel, String yLabel, int globalMax) {


        def chart = new GoogleChartBuilder()
        String result = chart.bar(['vertical', 'grouped']){
            size(w:750, h:400)
            barSize(width:4, space:1)
            title{
                row(run.label)
            }
            data(encoding:'text', numLines:(historyXValues.size()+1)*2) {
//                set(xValues.toList())
                set(yValues.toList())
                for(int i=0; i < historyXValues.size(); i++){
                    set(historyYValues[i].toList())
                }
            }
            colors{
                color('FF9966')
                color('6699FF')
                color('99FF66')
                color('66CC00')
            }
            //lineStyle(line1:[1,6,3])
            legend{
                label("Current")
                historyNames.each{
                    label(it)
                }
            }
            axis( bottom:xValues.toList(),left:[])
            range([0:[xValues.get(0).toInteger(),xValues.get(xValues.size()-1).toInteger()], 1:[1,globalMax]])


            dataRange(rangeList.toList())
            labelOption('b')
        }

        buildHTML(result)
    }

    private void buildHTML(String url) {
        File dir = new File("caliper-charts");
        dir.mkdir();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm");
        Date date = new Date();
        FileWriter writer = new FileWriter(dir.getName()+"/"+run.label + '' + dateFormat.format(date) + '.html')
        def builder = new MarkupBuilder(writer)
        builder.html {
            title '' + run.label + ' ' + run.timestamp
            body {
                table(border: 1) {
                    tr {
                        th("Environments")
                    }
                    for (int i = 0; i < run.environments.size(); i++) {
                        Environment e=run.environments.get(i)
                        SortedMap<String, String> properties=e.@properties
                        tr {
                            td("CPU: "+properties.get("host.cpu.names"))
                            td("Number of Cores: "+properties.get("host.cpus"))
                            td("Memory: "+properties.get("host.memory.physical"))
                            td("OS: "+properties.get("os.name")+" "+properties.get("os.version"))
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th("VMs")
                    }
                    for (int i = 0; i < run.vms.size(); i++) {
                        tr {
                            td(run.vms.get(i).vmName)
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th("Instruments")
                    }
                    for (int i = 0; i < run.instruments.size(); i++) {
                        Instrument instrument=run.instruments.get(i);
                        tr {
                            String s=instrument.className
                            td(s.substring(s.lastIndexOf('.')+1,s.length()))
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th("Number of Actors")
                        th("Measurements")
                    }
                    for (int i = 0; i < run.scenarios.size(); i++) {
                        tr {
                            td(run.scenarios.get(i).userParameters.get("numberOfClients"));
                            td((long)run.results.get(i).measurements.get(0).@value/run.results.get(i).measurements.get(0).weight);
                        }
                    }
                }
                img(src: url, border: 0)

            }
        }
    }
}
