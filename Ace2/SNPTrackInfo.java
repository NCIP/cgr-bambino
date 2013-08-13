package Ace2;

import net.sf.samtools.*;

public class SNPTrackInfo {
  SAMRecord sr;
  byte tumor_normal;
  private short read_i = -1;

  public SNPTrackInfo (SAMRecord sr, int read_i, byte tumor_normal) {
    //   System.err.println("new STI");  // debug
    this.sr = sr;
    this.read_i = (short) read_i;
    this.tumor_normal = tumor_normal;
  }

  public byte get_quality () {
    // base quality
    if (read_i == -1) {
      System.err.println("ERROR: get_quality(): no read index specified!");  // debug
      System.exit(1);
    }
    return sr.getBaseQualities()[read_i];
  }

  public int get_minimum_flanking_sequence() {
    if (read_i == -1) {
      System.err.println("ERROR: get_minimum_flanking_sequence(): no read index specified!");  // debug
      System.exit(1);
    }
    int r = sr.getReadLength() - read_i - 1;
    return read_i < r ? read_i : r;
  }


}
