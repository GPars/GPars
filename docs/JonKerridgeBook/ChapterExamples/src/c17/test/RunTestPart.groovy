package c17.test

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.net.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import org.jcsp.groovy.util.*


class RunTestPart extends GroovyTestCase {

	void testSomething() {
	  
	  Node.info.setDevice(null)
	  
	  Node.getInstance().init(new TCPIPNodeFactory ())
	  
	  NetChannelOutput ordinaryInput = CNS.createOne2Net("ordinaryInput")
	  NetChannelInput scaledOutput = CNS.createNet2One("scaledOutput")

      def collector = new CollectNumbers ( inChannel: scaledOutput)
      def generator = new GenerateNumbers (outChannel: ordinaryInput)
    
      def testList = [ collector, generator]
    
      new PAR(testList).run()
      
      def original = generator.generatedList
      def unscaled = collector.collectedList
      def scaled = collector.scaledList
      assertTrue (original == unscaled)
	  assertTrue (TestUtilities.list1GEList2(scaled, original))

	}

}
