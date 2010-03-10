package c6
  
import org.jcsp.lang.*
import org.jcsp.groovy.*
import c2.ProduceHW

class HelloWorldTest extends GroovyTestCase {
	
	void testMessage() {
		
		One2OneChannel connect = Channel.createOne2One()
		
		def producer =  new ProduceHW ( outChannel: connect.out() )
		def consumer = new ConsumeHW ( inChannel: connect.in() )

		def processList = [ producer, consumer ]
		new PAR (processList).run()   
		
		def expected = "Hello World!!!"			
		def actual = consumer.message	
		
		assertTrue(expected == actual)
	}
}
