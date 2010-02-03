package groovyx.gpars.csp.util


abstract class TestUtilities {

  public static boolean  listContains (list1, list2) {
    
    if (list1.size != list2.size) {
      return false
    }
    else {
      list1.sort()
      list2.sort()
      return (list1 == list2)
    }
  } // end listContains
  
  public static boolean list1GEList2 (list1, list2) {
    
    if (list1.size != list2.size) {
      return false
    }
    else {
      for (i in 0 ..< list1.size ) {
        if (list1[i] < list2[i]) {
          return false
        }
      }
      return true
    }
    
  }
  
  
}
