package c19;

import org.jcsp.lang.*;
import org.jcsp.net.mobile.*;
import org.jcsp.util.*;

public class NoServiceClientProcess extends MobileProcess {
  
  static final long serialVersionUID = 9;
  
  public void run () {
    System.out.println ( "No service client process started " );
    final Any2OneChannel responseEvent = Channel.createAny2One(new OverWriteOldestBuffer (10) );

    final One2OneChannel messageConfigure = Channel.createOne2One(new OverWriteOldestBuffer (10) );
    
    final CSProcess [] network = { 
    		new NoServiceClientCapability (
                       responseEvent.in(),
                       messageConfigure.out() ),
            new NoServiceClientUserInterface (
                       responseEvent.out(),      
                       messageConfigure.in() ) 
    };
    new Parallel ( network ).run();
  }
}

