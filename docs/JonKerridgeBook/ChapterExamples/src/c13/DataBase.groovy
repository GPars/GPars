package c13
  
import org.jcsp.lang.*
import org.jcsp.groovy.*

class DataBase implements CSProcess {
  def inChannels
  def outChannels
  def int readers
  def int writers
  
  void run () {
    println "DataBase has started"
    def crewDataBase = new CrewMap()
    for ( i in 0 ..< 10 ) {
      crewDataBase.put ( i, 100 + i)
    }
    for ( i in 0 ..< 10 ) {
      println "DB: Location ${i} contains ${crewDataBase.get( i )} "
    }   
    def processList = [] 
    for (i in 0..< readers) { 
      processList.putAt (i, new ReadClerk ( cin: inChannels[i], 
                                            cout: outChannels[i],
                                            data: crewDataBase) )
    }
    for ( i in 0 ..< writers ) {
      processList.putAt ( ( i + readers), new WriteClerk ( cin: inChannels[i + readers], 
                                                           cout: outChannels[i + readers],
                                                           data: crewDataBase ) )
    }
    new PAR (processList).run()
  }
}

    
      