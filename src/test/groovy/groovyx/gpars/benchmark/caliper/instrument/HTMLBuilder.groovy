package groovyx.gpars.benchmark.caliper.instrument

import com.google.caliper.model.Environment
import com.google.caliper.model.Instrument
import com.google.caliper.model.Result
import com.google.caliper.model.Scenario
import com.google.caliper.model.VM
import groovy.xml.MarkupBuilder

class HTMLBuilder {
    private List<Environment> environments
    private List<VM> vms
    private List<Instrument> instruments
    private List<Scenario> scenarios
    private List<Result> results
    private String label;
    private long timeStamp;

    public HTMLBuilder() {
        this.environments = run.environments;
        this.vms = run.vms;
        this.instruments = run.instruments;
        this.scenarios = run.scenarios;
        this.results = run.results;
        this.label = run.label;
        this.timeStamp = run.timestamp;
    }

    public void createHTML(String url) {
        FileWriter writer = new FileWriter('Test.html')
        def builder = new MarkupBuilder(writer)
        builder.html {
            title '' + label + ' ' + timeStamp
            body {
                table(border: 1) {
                    tr {
                        th("Environments")
                    }
                    for (int i = 0; i < environments.size(); i++) {
                        tr {
                            td(environments.get(i).toString())
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th("VMs")
                    }
                    for (int i = 0; i < vms.size(); i++) {
                        tr {
                            td(vms.get(i).toString())
                        }
                    }
                }
                table(border: 1) {
                    tr {
                        th("Instruments")
                    }
                    for (int i = 0; i < instruments.size(); i++) {
                        tr {
                            td(instruments.get(i).toString())
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
                            scenarios.get(i).toString();
                            results.get(i).toString();
                        }
                    }
                }
                img(src: url, border: 0)
            }
        }
    }
}
