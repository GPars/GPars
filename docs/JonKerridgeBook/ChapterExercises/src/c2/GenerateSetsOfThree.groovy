package c2

import org.jcsp.lang.*

class GenerateSetsOfThree implements CSProcess {
	
	def ChannelOutput outChannel
	
	void run(){
		def threeList = [
		                 [1, 2, 3], 
		                 [4, 5, 6], 
		                 [7, 8, 9], 
		                 [10, 11, 12], 
		                 [13, 14, 15], 
		                 [16, 17, 18],
		                 [19, 20, 21], 
		                 [22, 23, 24] ]
		for ( i in 0 ..< threeList.size)outChannel.write(threeList[i])
		//write the terminating List as per exercise definition
	}
}