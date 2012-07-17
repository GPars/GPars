package groovyx.gpars.benchmark.caliper.instrument

import com.google.caliper.model.Environment
import com.google.caliper.model.Instrument
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
        System.out.println();
        FileWriter writer = new FileWriter(label + '' + dateFormat.format(date) + '.html')
        def builder = new MarkupBuilder(writer)
        builder.html {
            title '' + label + ' ' + timeStamp
            body {
                table(border: 1) {
                    tr {
                        th("Environments")
                    }
                    for (int i = 0; i < environments.size(); i++) {
                        Environment e=environments.get(i)
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
                    for (int i = 0; i < vms.size(); i++) {
                        tr {
                            td(vms.get(i).vmName)
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th("Instruments")
                    }
                    for (int i = 0; i < instruments.size(); i++) {
                        Instrument instrument=instruments.get(i);
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
                    for (int i = 0; i < scenarios.size(); i++) {
                        tr {
                            td(scenarios.get(i).userParameters.get("numberOfClients"));
                            td((long)results.get(i).measurements.get(0).@value/results.get(i).measurements.get(0).weight);
                        }
                    }
                }
                img(src: url, border: 0)

            }
        }
    }
}
