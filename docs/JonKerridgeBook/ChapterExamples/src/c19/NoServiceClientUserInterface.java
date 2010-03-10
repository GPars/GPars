package c19;

import org.jcsp.lang.*;
import org.jcsp.awt.*;
import java.awt.*;


public class  NoServiceClientUserInterface implements CSProcess {
  private ChannelOutput responseEvent ;
  private ChannelInput messageConfigure ;
  
  public NoServiceClientUserInterface(ChannelOutput responseEvent, ChannelInput messageConfigure) {
	this.responseEvent = responseEvent;
	this.messageConfigure = messageConfigure;
}

public void run() {
    System.out.println ( "No service client user interface started " );
    final ActiveFrame root = new ActiveFrame ( null, responseEvent, "JK Meeting Service" );
    root.setSize ( 320, 480 );
    root.setLayout ( new BorderLayout () );
    final ActiveLabel message = new ActiveLabel ( messageConfigure, "                         " );
    final ActiveButton retry = new ActiveButton (null, responseEvent, "Retry")   ;
    final ActiveButton close = new ActiveButton (null, responseEvent, "Close")  ; 
    root.add ( message, BorderLayout.NORTH );
    root.add ( retry, BorderLayout.CENTER ) ;
    root.add ( close, BorderLayout.SOUTH ) ;
    root.pack();
    System.out.println ( "NSCUI:  packed" );
    root.setVisible(true);
    System.out.println ( "NSCUI:  setvisible" );
    final CSProcess [] network = {
    		message, 
    		retry, 
    		close 
    };
    System.out.println ( "NSCUI:  about to run" );
    new Parallel ( network ).run();
  }
}