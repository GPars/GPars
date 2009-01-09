package org.gparallelizer.actors

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class MixinTest extends GroovyTestCase {
    public void testMixin() {
        Company.metaClass {
            mixin DefaultActor

            def act = {->
                println "Listening"
                receive {
                    println "AAAAAAAAAAAAAAAAAAA $it"
                }
            }
        }

        final Company company = new Company(name: 'Company1', employees: ['Joe', 'Dave', 'Alice'])

        //todo enable mixins
//        company.start()
//        company.send("Message")

    }

}

class Company {
    String name
    List<String> employees

}