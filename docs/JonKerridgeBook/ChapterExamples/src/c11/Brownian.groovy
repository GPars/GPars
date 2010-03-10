package c11
  
import org.jcsp.groovy.*
import org.jcsp.lang.*
import phw.util.*

Any2OneChannel  connect = Channel.createAny2One()
One2AnyChannel  update = Channel.createOne2Any()

def CSIZE = Ask.Int ("Size of Canvas (200, 600)?: ", 200, 600)
def CENTRE = CSIZE / 2
def PARTICLES = Ask.Int ("Number of Particles (10, 200)?: ", 10, 200)
def INIT_TEMP = 20

def network = []
for ( i in 0..< PARTICLES ) {
  network << new Particle ( id: i, 
                            sendPosition: connect.out(),
                            getPosition: update.in(), 
                            x: CENTRE, 
                            y: CENTRE, 
                            temperature: INIT_TEMP )
}
 
network << ( new ParticleInterface ( inChannel: connect.in(), 
                                     outChannel: update.out(), 
                                     canvasSize: CSIZE,
                                     particles: PARTICLES,
                                     centre: CENTRE,
                                     initialTemp: INIT_TEMP ) )
println "Starting Particle System"
new PAR ( network ).run()
