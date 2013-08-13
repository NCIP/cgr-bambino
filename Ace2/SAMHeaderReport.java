package Ace2;

import net.sf.samtools.*;
import java.io.*;
import java.util.*;

public class SAMHeaderReport {

  public void report (String filename, SAMFileReader sfr) {
    SAMFileHeader sfh = sfr.getFileHeader();
    //    System.out.println(" creator: " + sfh.getCreator());  // debug
    //    System.out.println(" version: " + sfh.getVersion());

    SAMSequenceDictionary dict = sfh.getSequenceDictionary();
    for (SAMSequenceRecord ssr : dict.getSequences()) {
      System.err.println("sequence=" + ssr.getSequenceName() +
			 " length=" + ssr.getSequenceLength() + 
			 " assembly=" + ssr.getAssembly());  // debug
    }

    for (SAMProgramRecord rec : sfh.getProgramRecords()) {
      System.out.println(
			 "file=" + filename + "|" + 
  			 "program_name=" + rec.getProgramName() + "|" + 
			 // always seems to be null
			 "version=" + rec.getProgramVersion() + "|" + 
			 "program_group_id=" + rec.getProgramGroupId() + "|" + 
  			 "command_line=" + rec.getCommandLine()
			 );
    }
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    try {
      SAMHeaderReport shi = new SAMHeaderReport();
      String filename = argv[0];
      shi.report(filename, new SAMFileReader(new File(filename)));
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }


}