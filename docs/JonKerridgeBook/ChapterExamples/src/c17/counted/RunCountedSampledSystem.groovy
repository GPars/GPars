package c17.counted 
  
import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

One2OneChannel a = Channel.createOne2One()
One2OneChannel b = Channel.createOne2One()
One2OneChannel c = Channel.createOne2One()
One2OneChannel d = Channel.createOne2One()
One2OneChannel e = Channel.createOne2One()
One2OneChannel f = Channel.createOne2One()
One2OneChannel g = Channel.createOne2One()
One2OneChannel h = Channel.createOne2One()

def dataGen = new GNumbers ( outChannel: a.out() )

def sampler = new CountingSampler ( inChannel: a.in(), 
                                     outChannel: b.out(), 
                                     sampleRequest: e.in(),
                                     countReturn: g.out() )

def samplingTimer = new CountedSamplingTimer ( sampleRequest: e.out(), 
                                                sampleInterval: 2500,
                                                countReturn: g.in(),
                                                countToGatherer: h.out() )

def sampledNetwork = new CountedSampledNetwork ( inChannel: b.in(),
                                                  outChannel: c.out() )

def gatherer = new CountingGatherer ( inChannel: c.in(),
                                       outChannel: d.out(),
                                       gatheredData: f.out(),
                                       countInput: h.in() )

def evaluator = new CountedEvaluator (inChannel: f.in()) 

def printResults = new GPrint ( inChannel: d.in(),
                                 heading: "output Values",
                                 delay: 0)

def network = [dataGen, sampler, samplingTimer, sampledNetwork, 
               gatherer, evaluator, printResults ]
                             

new PAR(network).run()               

