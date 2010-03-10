package c19;
  
import org.jcsp.lang.*
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.mobile.*
import org.jcsp.groovy.*
import phw.util.*

  //CNS_IP = "146.176.163.83" // napier network jon's PC
  //CNS_IP = "169.254.171.140" // jon's laptop
  def CNS_IP = Ask.string("Enter IP address of CNS: ")
  //CNS_IP = ""  // a defualt CNS somewhere on a connected network
  //String CNS_IP = null                   // Default IP of CNS running on a connected network
  // Initialise Mobile package
  if (CNS_IP == null) {
    Mobile.init(Node.getInstance().init(new TCPIPNodeFactory()))
  }
  else {
    Mobile.init(Node.getInstance().init(new TCPIPNodeFactory(CNS_IP)))
  }
  
  def nSize = Ask.Int("Number of Concurrent New Meeting Clients? ", 1, 2)
  def fSize = Ask.Int("Number of Concurrent Find Meeting Clients? ", 1, 3)
  
  // create the net input channels used to create new meetings
  def netChannels = []
  def NMCList = []
  for (i in 0 ..< nSize) { 
    def c = Mobile.createNet2One()
    netChannels << c
    NMCList << new NewMeetingClientProcess(c.getChannelLocation(), i )
  }

  // create the net input channels used to find existing meetings
  def FMCList = []
  for (i in 0 ..< fSize) { 
    def c = Mobile.createNet2One()
    netChannels << c
    FMCList << new FindMeetingClientProcess(c.getChannelLocation(), i )
  }
  
  //connections between newSender and newServer
  def newServe2Send = Channel.createOne2One()
  def newSend2Serve = Channel.createOne2One()
  // reuse connection for new clients
  def newReuse = Channel.createOne2One()
  
  //connections between findSender and findServer
  def findServe2Send = Channel.createOne2One()
  def findSend2Serve = Channel.createOne2One()
  // reuse connection for find clients
  def findReuse = Channel.createOne2One()

  // connection between AccessSender and Server
  def accessConnection = Channel.createOne2One()
  def processList =  [  new AccessSender(toAccessServer:accessConnection.out()),
                        new AccessServer(fromAccessSender:accessConnection.in()),
                        new Server( fromSender:newSend2Serve.in(), 
                                    toSender:newServe2Send.out(), serviceName: "N"),
                        new Sender( toServer:newSend2Serve.out(), 
                                    fromServer:newServe2Send.in(), 
                                    reuse:newReuse.in(), clients: NMCList),
                        new Server( fromSender:findSend2Serve.in(), 
                                    toSender:findServe2Send.out(), serviceName: "F"),
                        new Sender( toServer:findSend2Serve.out(), 
                                    fromServer:findServe2Send.in(), 
                                    reuse:findReuse.in(), clients: FMCList),
                        new Meeting( requestChannels : netChannels, 
                                     nReuse : newReuse.out(), newClients : nSize,
                                     fReuse : findReuse.out(), findClients : fSize ) ]
  println "MO: Starting Meeting Organiser"
  new PAR(processList).run()
  
