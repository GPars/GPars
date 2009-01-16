import org.gparallelizer.enhancer.AsynchronousEnhancer
import org.gparallelizer.actors.Actors

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

class MyClass {}
// returns null
println MyClass.metaClass.getMetaMethod("methodMissing", [String, Object] as Object[])

MyClass.metaClass.methodMissing = {String name, args -> println "$name called"}

// returns a not-null meta-method instance
println MyClass.metaClass.getMetaMethod("methodMissing", [String, Object] as Object[])



class MyURL {}

MyURL.metaClass.methodMissing = {String methodName, args ->
    if (methodName=='retrieve1') return "Result 1"
    else throw new MissingMethodException(methodName, delegate.class, args)
}

println new MyURL().retrieve1()

def original1 = MyURL.metaClass.getMetaMethod("methodMissing", [String, Object] as Object[])
println original1


println MyURL.metaClass.getMetaMethods()
def original = MyURL.metaClass.getMetaMethod("methodMissing", [Object, Object] as Object[])
println 'AAAAAAAAAAAAAAAAAAAAa ' + original
MyURL.metaClass.methodMissing = {String methodName, args ->
    if (methodName=='retrieve2') return "Result 2"
    else original.invoke(delegate, methodName, args)
}

println new MyURL().retrieve2()
println new MyURL().retrieve1()

