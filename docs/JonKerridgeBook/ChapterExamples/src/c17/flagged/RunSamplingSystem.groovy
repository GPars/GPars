package c17.flagged 

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

One2OneChannel a = Channel.createOne2One()
One2OneChannel b = Channel.createOne2One()
One2OneChannel c = Channel.createOne2One()
One2OneChannel d = Channel.createOne2One()
One2OneChannel e = Channel.createOne2One()
One2OneChannel f = Channel.createOne2One()


def dataGen = new DataGenerator ( outChannel: a.out(), interval: 250)

def sampler = new Sampler ( inChannel: a.in(), outChannel: b.out(), 
                             sampleRequest: e.in() )

def samplingTimer = new SamplingTimer ( sampleRequest: e.out(), 
                                         sampleInterval: 2500 )

def sampledNetwork = new SampledNetwork ( inChannel: b.in(),
                                           outChannel: c.out() )

def gatherer = new Gatherer ( inChannel: c.in(),
                               outChannel: d.out(),
                               gatheredData: f.out() )

def evaluator = new Evaluator (inChannel: f.in()) 

def printResults = new GPrint ( inChannel: d.in(),
                                 heading: "output Values",
                                 delay: 0)

def network = [dataGen, sampler, samplingTimer, sampledNetwork, 
               gatherer, evaluator, printResults ]
                             

new PAR(network).run()               

