import org.codehaus.groovy.runtime.InvokerHelper
import org.gparallelizer.enhancer.ActorMetaClass
import org.gparallelizer.actors.pooledActors.PooledActors

/**
 * test execution
 */


class Incrementor{
  int i = 0;
  def increment(){
    i++;
  }
  def status(){
    println("Incrementor counter: $i, thread: "+Thread.currentThread());
  }
}

class SynchronizedIncrementor{
  int i = 0;
  def synchronized increment(){
    i++;
  }
  def synchronized status(){
    println("Incrementor counter: $i, thread: "+Thread.currentThread());
  }
}
println("*** test performance ***");

//long NTIMES = Integer.MAX_VALUE;
//long NTIMES = 5000000;
long NTIMES = 500;

long time = System.currentTimeMillis();
Incrementor i1 = new Incrementor();
for(int i=0;i<NTIMES;i++){
  i1.increment();
}
i1.status();
time = time - System.currentTimeMillis();

println("$NTIMES method invocations without actor $time ms");

//install event driven metaclass
InvokerHelper.metaRegistry.setMetaClass(Incrementor.class, new ActorMetaClass(Incrementor.class, true))


time = System.currentTimeMillis();
i1 = new Incrementor();
for(int i=0;i<NTIMES;i++){
  i1.increment();
}
i1.status();
time = time - System.currentTimeMillis();

println("$NTIMES method invocations with actor $time ms");


InvokerHelper.metaRegistry.setMetaClass(SynchronizedIncrementor.class, new ActorMetaClass(SynchronizedIncrementor.class, true))
time = System.currentTimeMillis();
SynchronizedIncrementor i2 = new SynchronizedIncrementor();
for(int i=0;i<NTIMES;i++){
  i2.increment();
}
i2.status();
time = time - System.currentTimeMillis();

println("$NTIMES method invocations with synchronization $time ms");
