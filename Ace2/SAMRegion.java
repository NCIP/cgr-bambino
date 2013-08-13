package Ace2;

public class SAMRegion {
  public String tname = null;
  public Range range;
  public String gene_name = null;

  public SAMRegion() {
    //    System.err.println("new SAMRegion");  // debug
    //    new Exception().printStackTrace();
    range = new Range();
  }

  public SAMRegion(String tname, int start, int end) {
    //    System.err.println("new SAMRegion");  // debug
    //    new Exception().printStackTrace();
    this.tname = tname;
    range = new Range(start, end);
  }

  public boolean isValid() {
    return tname != null && range.isValid();
  }

  public String toString() {
    if (isValid()) {
      return tname + ":" + range.start + "-" + range.end;
    } else {
      return null;
    }
  }

  public String get_ucsc_chromosome () {
    Chromosome c = Chromosome.valueOfString(tname);
    return c == null ? null : c.toString();
  }

  public int get_length() {
    return (range.end - range.start) + 1;
  }

  public void set_start (int v) {
    range.start = v;
  }

  public void set_end (int v) {
    range.end = v;
  }

}