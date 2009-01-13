import org.gparallelizer.enhancer.AsynchronousEnhancer

AsynchronousEnhancer.enhanceClass URL

final String site1='http://www.jroller.com'
new URL(site1).text {
    if (it =~ 'java') {
        println "The site $site1 talks about Grails today"
    }
}

final String site2 = 'http://www.dzone.com'
new URL(site2).text {
    if (it =~ 'java') {
        println "The site $site2 talks about Grails today"
    }
}


