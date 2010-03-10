package c21


class Type2 implements Serializable {

  def typeName = "Type2"
    def int typeInstance 
    def int instanceValue
    
    def modify ( value) {
      typeInstance = typeInstance + value
    }    
    
    def String toString(){
  	  def int nodeId = typeInstance / 100000
	  def int typeNumber = (typeInstance - (nodeId*100000)) / 1000
	  def int typeInstanceValue = (typeInstance - (nodeId*100000)) % 1000
	  return " Node: $nodeId, Type: $typeNumber, TypeInstanceValue: $typeInstanceValue, Sequence: $instanceValue"
      //return "Instance of $typeName instance $typeInstance and value $instanceValue"
    }


}