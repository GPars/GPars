import org.gradle.api.file.FileCollection
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

class DemoTask extends DefaultTask {
    @InputFiles FileCollection classpath
    @InputFiles FileCollection demoFiles
    List excludedDemos

    @TaskAction
    def runDemos() {
        def shell = createShell()
        def failed = []
        def ok = 0
        demoFiles.files.each {File file ->
            println "*** starting demo $file.name"
            try {
                shell.evaluate(file)
                ok += 1
            } catch (Exception ex) {
                failed << [file: file, exception: ex]
            }
            println "*** done"
        }
        println "*** cleaning up"
        shell.evaluate("groovyx.gpars.dataflow.Dataflow.DATA_FLOW_GROUP.shutdown()")
        println "=== demos: $ok ok, ${failed.size()} failed"
        failed.each { println "${it.file}\n    ${it.exception}" }
    }

    private GroovyShell createShell() {
        URLClassLoader classloader = new URLClassLoader(
                classpath.files.collect {File classpathElement -> classpathElement.toURL()} as URL[],
                Thread.currentThread().contextClassLoader
        )
        System.metaClass.static.exit = {int returnCode -> println ">>> System.exit($returnCode) suppressed." }
        System.in.metaClass.read = {-> println ">>> System.in.read() suppressed"; 'Automated build' }
        System.in.metaClass.readLines = {-> println ">>> System.in.read() suppressed"; ['Automated build'] }
        new GroovyShell(classloader)
    }
}