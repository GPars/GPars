package c6

import org.jcsp.lang.*
import org.jcsp.groovy.*
import c5.Queue

class QueueTest extends GroovyTestCase {

	void testQueue() {

		One2OneChannel QP2Q = Channel.createOne2One()
		One2OneChannel Q2QC = Channel.createOne2One()
		One2OneChannel QC2Q = Channel.createOne2One()
		
		def qProducer = new QProducer ( put: QP2Q.out(), iterations: 50 )
		def queue     = new Queue ( put: QP2Q.in(), get: QC2Q.in(), 
				                    receive: Q2QC.out(), elements: 5)
		def qConsumer = new QConsumer ( get: QC2Q.out(), receive: Q2QC.in() )

		def testList = [ qProducer, queue, qConsumer ]
		new PAR ( testList ).run()
		
		def expected = qProducer.sequence
		def actual = qConsumer.outSequence
		assertTrue(expected == actual)
		
	}
}