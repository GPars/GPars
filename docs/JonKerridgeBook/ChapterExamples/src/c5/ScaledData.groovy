package c5;
   
    
class ScaledData implements Serializable {

  def int original
  def int scaled
  
  def String toString () {
	  def s = " " + original + "\t\t" + scaled
	  return s 
  }
	
}