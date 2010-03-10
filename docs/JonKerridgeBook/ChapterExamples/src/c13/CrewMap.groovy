package c13
 
import java.util.*
import org.jcsp.lang.*

class CrewMap extends  HashMap {
  def theCrew = new Crew()
  
  def Object put ( Object itsKey, Object itsValue ) {
    theCrew.startWrite()
    super.put ( itsKey, itsValue )
    theCrew.endWrite()
  }
  
  def Object get ( Object itsKey ) {
    theCrew.startRead()
    def result = super.get ( itsKey )
    theCrew.endRead()
    return result
  }

}

  