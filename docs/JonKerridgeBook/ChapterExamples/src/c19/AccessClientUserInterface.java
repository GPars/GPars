package c19;

import org.jcsp.lang.*;
import java.awt.*;
import org.jcsp.awt.*;


/**
 * @author JM Kerridge
 *
 */
public class AccessClientUserInterface implements CSProcess {
  private  ChannelOutput buttonEvent;
  
  /**
 * @param buttonEvent
 */
public AccessClientUserInterface(ChannelOutput buttonEvent) {
	this.buttonEvent = buttonEvent;
}

  public void run() {
    final ActiveClosingFrame root = new ActiveClosingFrame ("Jon's Meeting Service");
    final Frame mainFrame = root.getActiveFrame();
    mainFrame.setSize (320, 480);
    mainFrame.setLayout ( new BorderLayout() );
    final ActiveButton newButton = new ActiveButton ( null, buttonEvent, "Create New Meeting" );
    final ActiveButton findButton = new ActiveButton ( null, buttonEvent, "Find Existing Meeting" );
    final Container buttonContainer = new Container();
    buttonContainer.setSize(320,480);
    buttonContainer.setLayout ( new GridLayout ( 2,1 ) );
    buttonContainer.add ( newButton );
    buttonContainer.add ( findButton );
    mainFrame.add ( buttonContainer, BorderLayout.CENTER );
    mainFrame.pack();
    mainFrame.setVisible(true);
    final CSProcess []network = {
    		root, 
    		newButton, 
    		findButton 
    };
    System.out.println ( "MeetingAccessFrame about to invoke PAR" );
    new Parallel ( network ).run();
  }
}

 
