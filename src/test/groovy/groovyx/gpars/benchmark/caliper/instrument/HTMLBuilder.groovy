package groovyx.gpars.benchmark.caliper.instrument

import com.google.caliper.model.Environment
import com.google.caliper.model.Instrument
import com.google.caliper.model.Measurement
import com.google.caliper.model.Result
import com.google.caliper.model.Run
import com.google.caliper.model.Scenario
import com.google.caliper.model.VM
import groovy.xml.MarkupBuilder
import java.text.DateFormat
import java.text.SimpleDateFormat

class HTMLBuilder {
    private List<Environment> environments
    private List<VM> vms
    private List<Instrument> instruments
    private List<Scenario> scenarios
    private List<Result> results
    private String label;
    private long timeStamp;

    public HTMLBuilder(Run run) {
        this.environments = run.environments;
        this.vms = run.vms;
        this.instruments = run.instruments;
        this.scenarios = run.scenarios;
        this.results = run.results;
        this.label = run.label;
        this.timeStamp = run.timestamp;
    }

    public void createHTML(String url) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm");
        Date date = new Date();
        FileWriter writer = new FileWriter(label + '' + dateFormat.format(date) + '.html')
        def builder = new MarkupBuilder(writer)
        builder.html {
            head {
                title label + " " + dateFormat.format(date)
            }
            body {
                table(border: 1) {
                    tr {
                        th(style: "font-size: 0.75em","Number of Actors")
                        th(style: "font-size: 0.75em","Measurements")
                    }
                    for (int i = 0; i < scenarios.size(); i++) {
                        tr {
                            Measurement m = results.get(i).measurements.get(0);
                            td(style: "font-size: 0.75em", scenarios.get(i).userParameters.get("numberOfClients"));
                            td(style: "font-size: 0.75em", ((long) m.@value / m.weight) + " " + m.unit);
                        }
                    }
                    tr{
                        img(src: url, border: 0)
                    }
                }
                table(border: 1) {
                    tr {
                        th(style: "font-size: 0.75em",  "Environments")
                    }
                    for (int i = 0; i < environments.size(); i++) {
                        Environment e = environments.get(i)
                        SortedMap<String, String> properties = e.@properties
                        tr {
                            td(style: "font-size: 0.75em","CPU: " + properties.get("host.cpu.names"))
                            td(style: "font-size: 0.75em","Number of Cores: " + properties.get("host.cpus"))
                            td(style: "font-size: 0.75em","Memory: " + properties.get("host.memory.physical"))
                            td(style: "font-size: 0.75em","OS: " + properties.get("os.name") + " " + properties.get("os.version"))
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th("VMs")
                    }
                    for (int i = 0; i < vms.size(); i++) {
                        tr {
                            td(style: "font-size: 0.75em",vms.get(i).vmName)
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th(style: "font-size: 0.75em","Instruments")
                    }
                    for (int i = 0; i < instruments.size(); i++) {
                        Instrument instrument = instruments.get(i);
                        tr {
                            String s = instrument.className
                            td(style: "font-size: 0.75em",s.substring(s.lastIndexOf('.') + 1, s.length()))
                        }
                    }
                }


            }
        }
    }
}