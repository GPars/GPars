package c17.flagged


class FlaggedSystemData extends SystemData {
  
  def boolean testFlag = false

  def String toString() {
    String s
    s = "Flagged System Data: [" + a + ", " + b + ", " + c + ", " + testFlag + "]"
    return s
  }
}