package groovyx.gpars.benchmark.caliper.chart

import com.google.caliper.model.Environment
import com.google.caliper.model.Instrument
import com.google.caliper.model.Measurement
import com.google.caliper.model.Run
import com.google.common.collect.ImmutableSortedSet
import groovy.xml.MarkupBuilder

import java.text.DateFormat
import java.text.SimpleDateFormat

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

    public void buildLineGraphURL(ArrayList<String> xValues, ArrayList<Integer> yValues, List<ArrayList<String>> historyXValues, List<ArrayList<Integer>> historyYValues,
                                  ArrayList<String> historyNames, ArrayList<Integer> rangeList, String xLabel, String yLabel, int globalMax) {


        def chart = new GoogleChartBuilder()
        String result = chart.lineXY {
            size(w: 750, h: 400)
            title {
                row(run.label)
            }
            data(encoding: 'text', numLines: (historyXValues.size() + 1) * 2) {
                set(xValues.toList())
                set(yValues.toList())
                for (int i = 0; i < historyXValues.size(); i++) {
                    set(historyXValues[i].toList())
                    set(historyYValues[i].toList())
                }
            }
            colors {
                Random rand = new Random();

                for (int i = 0; i < historyXValues.size() + 1; i++) {
                    String colorHex = "";
                    for (int j = 0; j < 3; j++) {
                        char c = 48 + (rand.nextInt(9))
                        colorHex = colorHex << c << c
                    }
                    color(colorHex)
                }
            }
            //lineStyle(line1:[1,6,3])
            legend {
                label("Current")
                historyNames.each {
                    label(it)
                }
            }
            axis(bottom: [], left: [], left2: [yLabel], bottom2: [xLabel])
            range([0: [xValues.get(0).toInteger(), xValues.get(xValues.size() - 1).toInteger()], 1: [1, globalMax]])


            dataRange(rangeList.toList())

        }

        buildHTML(result)
    }

    public void buildBarGraphURL(ArrayList<String> xValues, ArrayList<Integer> yValues, List<ArrayList<String>> historyXValues, List<ArrayList<Integer>> historyYValues,
                                 ArrayList<String> historyNames, ArrayList<Integer> rangeList, String xLabel, String yLabel, int globalMax) {


        def chart = new GoogleChartBuilder()
        String result = chart.bar(['vertical', 'grouped']) {
            size(w: 750, h: 400)
            barSize(width: 4, space: 1)
            title {
                row(run.label)
            }
            data(encoding: 'text', numLines: (historyXValues.size() + 1) * 2) {
//                set(xValues.toList())
                set(yValues.toList())
                for (int i = 0; i < historyXValues.size(); i++) {
                    set(historyYValues[i].toList())
                }
            }
            colors {
                color('FF9966')
                color('6699FF')
                color('99FF66')
                color('66CC00')
            }
            //lineStyle(line1:[1,6,3])
            legend {
                label("Current")
                historyNames.each {
                    label(it)
                }
            }
            axis(bottom: xValues.toList(), left: [])
            range([0: [xValues.get(0).toInteger(), xValues.get(xValues.size() - 1).toInteger()], 1: [1, globalMax]])


            dataRange(rangeList.toList())
            labelOption('b')
        }

        buildHTML(result)
    }

    void buildHTML(String url) {
        File dir = new File("caliper-charts");
        dir.mkdir();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm");
        Date date = new Date();
        FileWriter writer = new FileWriter(label + '' + dateFormat.format(date) + '.html')
        def builder = new MarkupBuilder(writer)
        builder.html {
            head {
                title label + " " + dateFormat.format(date)
            }
            body {
                table(border: 1) {//results table
                    tr {
                        th(style: "font-size: 0.75em", "Number of Actors")
                        th(style: "font-size: 0.75em", "Measurements")
                    }
                    for (int i = 0; i < scenarios.size(); i++) {
                        tr {
                            Measurement m = results.get(i).measurements.get(0);
                            td(style: "font-size: 0.75em", scenarios.get(i).userParameters.get("numberOfClients"));
                            td(style: "font-size: 0.75em", ((long) m.@value / m.weight) + " " + m.unit);
                        }
                    }
                    tr {
                        img(src: url, border: 0)
                    }
                }
                table(border: 1) {
                    tr {
                        th(style: "font-size: 0.75em", "Environments")
                    }
                    for (int i = 0; i < environments.size(); i++) {
                        Environment e = environments.get(i)
                        SortedMap<String, String> properties = e.@properties
                        tr {
                            td(style: "font-size: 0.75em", "CPU: " + properties.get("host.cpu.names"))
                            td(style: "font-size: 0.75em", "Number of Cores: " + properties.get("host.cpus"))
                            td(style: "font-size: 0.75em", "Memory: " + properties.get("host.memory.physical"))
                            td(style: "font-size: 0.75em", "OS: " + properties.get("os.name") + " " + properties.get("os.version"))
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th("VMs")
                    }
                    for (int i = 0; i < vms.size(); i++) {
                        tr {
                            td(style: "font-size: 0.75em", vms.get(i).vmName)
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th(style: "font-size: 0.75em", "Instruments")
                    }
                    for (int i = 0; i < instruments.size(); i++) {
                        Instrument instrument = instruments.get(i);
                        tr {
                            String s = instrument.className
                            td(style: "font-size: 0.75em", s.substring(s.lastIndexOf('.') + 1, s.length()))
                        }
                    }
                }
            }
        }
    }
}
