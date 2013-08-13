package Ace2;

import net.sf.samtools.*;
import java.io.*;
import Funk.StreamingDuplicateFilter;

public abstract class SEUWriter {
  //
  // write a single record to output
  //
  StreamingDuplicateFilter sdf;
  private static final String delim = "&";
  private SEUConfig config;

  public SEUWriter(SEUConfig config) {
    this.config = config;
    sdf = new StreamingDuplicateFilter();
  }

  public void addAlignment(SAMRecord sr, SEUInteresting interesting) {
    boolean usable = true;
    if (!sr.getReadUnmappedFlag()) {
      // mapped read
      String key = sr.getReadName() + delim + 
	sr.getReferenceIndex() + delim + 
	sr.getAlignmentStart() + delim +
	(sr.getReadNegativeStrandFlag() ? "-" : "+");
      //      System.err.println("key="+key);  // debug
      if (sdf.add(key)) usable = false;

      if (config.EXTRACT_UNMAPPED_READS_ONLY) usable = false;
    }

    if (usable) {
      writeAlignment(sr, interesting);
    } else {
      //      System.err.println("SUPPRESS! " + sr.getReadName());  // debug
    }
  }

  public abstract void writeAlignment(SAMRecord sr, SEUInteresting interesting);
  public abstract void close();

}
