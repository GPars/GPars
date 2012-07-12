package groovyx.gpars.benchmark.akka

import groovy.xml.MarkupBuilder

class HTMLBuilder {
    public void createHTML(String url){
        url= "http://chart.apis.google.com/chart?cht=p3&chs=350x200&chd=t:1,2,3,4,5&chl=1|2|3|4|5"
        FileWriter writer = new FileWriter('Test.html')
        def builder = new MarkupBuilder(writer)

        builder.html {
            body {
                img(src:url, border:0)
            }
        }

    }
    public static void main(String[] args) {

    }
}
