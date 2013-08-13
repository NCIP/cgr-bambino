package TCGA;

public class GenomicLocation {
  public String chromosome;
  public int start,end;

  public GenomicLocation() {}

  public GenomicLocation (String loc) {
    // UCSC format
    String[] chr_and_pos = loc.split(":");
    chromosome = chr_and_pos[0];
    String[] start_and_end = chr_and_pos[1].split("-");
    start = Integer.parseInt(start_and_end[0]);
    end = Integer.parseInt(start_and_end[1]);
    //    System.err.println("final " + chromosome + " " + start + " " +end);  // debug
  }

  public GenomicLocation (String chromosome, int start, int end) {
    this.chromosome = chromosome;
    this.start=start;
    this.end=end;
  }

  public String get_ucsc_location() {
    return chromosome + ":" + start + "-" + end;
  }

  public String toString() {
    return get_ucsc_location();
  }
  

}
