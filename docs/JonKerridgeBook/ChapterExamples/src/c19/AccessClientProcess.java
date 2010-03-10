package c19;

import org.jcsp.lang.*;
import org.jcsp.net.mobile.*;


public class AccessClientProcess extends MobileProcess {
  
  static final long serialVersionUID = 9;
	
  public void run () {
    System.out.println ( "MeetingAccessClient has started" );
    final Any2OneChannel events = Channel.createAny2One();
    final CSProcess[] network = { 
    		new AccessClientCapability ( events.in() ) ,
        new AccessClientUserInterface ( events.out() ) 
    };                 
    new Parallel (network).run();
  }
}
