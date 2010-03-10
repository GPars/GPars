package c12.canteen
 
import org.jcsp.lang.*
import org.jcsp.groovy.*
 
Any2OneChannel service = Channel.createAny2One ()
One2AnyChannel deliver = Channel.createOne2Any ()
One2OneChannel supply = Channel.createOne2One ()    
def philosopherList = (0 .. 4).collect{
         i -> return new Philosopher( philosopherId: i, 
                                       service: service.out(), 
                                       deliver: deliver.in())
         }    
def processList = [ new InstantServery ( service:service.in(), 
                                         deliver:deliver.out(), 
                                         supply:supply.in()),
                    new Kitchen (supply:supply.out())
                  ]
processList = processList +  philosopherList    
new PAR ( processList ).run()     