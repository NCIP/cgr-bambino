package Ace2;

import net.sf.samtools.*;
import java.io.*;

public class SEUWriterBAM extends SEUWriter {
  //
  // write a single record to BAM output
  //
  BAMFileWriter bfw = null;

  public SEUWriterBAM (SEUConfig config, SAMFileReader sfr, String base_fn) {
    super(config);
    String bam_fn = base_fn + ".seu.unsorted.bam";
    bfw = new BAMFileWriter(new File(bam_fn));
    SAMFileHeader header_in = sfr.getFileHeader();
    bfw.setSortOrder(SAMFileHeader.SortOrder.valueOf("unsorted"), false);
    bfw.setHeader(header_in);
  }

  public void writeAlignment (SAMRecord sr, SEUInteresting interesting) {
    bfw.addAlignment(sr);
  }

  public void close() {
    bfw.close();
  }

}
