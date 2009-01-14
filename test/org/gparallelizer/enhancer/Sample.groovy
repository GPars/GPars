import org.gparallelizer.enhancer.AsynchronousEnhancer

//AsynchronousEnhancer.enhanceClass URL
//
//final String site1='http://www.jroller.com'
//new URL(site1).getText {
//    if (it =~ 'java') {
//        println "The site $site1 talks about Grails today"
//    }
//}
//
//final String site2 = 'http://www.dzone.com'
//new URL(site2).getText {
//    if (it =~ 'java') {
//        println "The site $site2 talks about Grails today"
//    }
//}

//todo report
class MyURL {

}

MyURL.metaClass.methodMissing = {String methodName, args ->
    if (methodName=='retrieve1') return "Result 1"
    else throw new MissingMethodException(methodName, delegate.class, args)
}

println new MyURL().retrieve1()

MyURL.metaClass.methodMissing = {String methodName, args ->
    if (methodName=='retrieve2') return "Result 2"
    else throw new MissingMethodException(methodName, delegate.class, args)
}

println new MyURL().retrieve2()
println new MyURL().retrieve1()

