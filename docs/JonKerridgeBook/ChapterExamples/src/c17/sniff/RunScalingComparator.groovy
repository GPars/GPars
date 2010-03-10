package c17.sniff
 
import org.jcsp.lang.*
import org.jcsp.groovy.*

def One2OneChannel Copy2Sniff = Channel.createOne2One()
def One2OneChannel Out2Comp = Channel.createOne2One()

def network = [ new SnifferComparator ( fromCopy: Copy2Sniff.in(),
                                         fromScaler: Out2Comp.in(),
                                         interval: 15000 ), 
                new ScalingSystem ( toSniffer: Copy2Sniff.out(),
                                    toComparator: Out2Comp.out() ) 
             ]

new PAR (network).run()
