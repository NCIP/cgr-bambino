package Ace2;

import net.sf.samtools.*;
import java.io.*;
import java.util.*;

public class BAMConcat {

  public static void concatenate_bams (ArrayList<File> in_files, File out_file) {
    SAMFileReader sfr = new SAMFileReader(in_files.get(0));
    BAMFileWriter bfw = new BAMFileWriter(out_file);
    SAMFileHeader header_in = sfr.getFileHeader();
    bfw.setSortOrder(SAMFileHeader.SortOrder.valueOf("unsorted"), false);
    bfw.setHeader(header_in);
    sfr.close();
    for (File bam : in_files) {
      System.err.println("processing " + bam);  // debug
      sfr = new SAMFileReader(bam);
      for (SAMRecord sr : sfr) {
	bfw.addAlignment(sr);
      }
    }
    bfw.close();
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    ArrayList<File> in_files = new ArrayList<File>();

    File out_file = null;
    boolean use_tmpdir = false;
    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-in")) {
	in_files.add(new File(argv[++i]));
      } else if (argv[i].equals("-out")) {
	out_file = new File(argv[++i]);
      } else if (argv[i].equals("-tmpdir")) {
	use_tmpdir = true;
      }
    }

    if (in_files.size() > 0 && out_file != null) {

      try {
      
	WorkingDirectory wd = null;
	if (use_tmpdir) {
	  String temp_dir = System.getenv("TMPDIR");
	  if (temp_dir == null) {
	    System.err.println("ERROR: no TMPDIR environment variable; use -temp-dir X to specify manually");  // debug
	    System.exit(1);
	  } else {
	    // write files to a temporary/working directory
	    // then move to output dir
	    File f = out_file.getCanonicalFile();
	    File parent = f.getParentFile();
	    File target_dir = parent == null ? new File(".") : parent;
	    wd = new WorkingDirectory(new File(temp_dir), target_dir);
	    System.err.println("raw outfile="+out_file);  // debug
	    out_file = wd.get_file(f.getName());
	    System.err.println("working directory outfile="+out_file);  // debug
	  }
	}
      
	concatenate_bams(in_files, out_file);

	if (wd != null) wd.finish();
      } catch (Exception e) {
	System.err.println("ERROR: " + e);  // debug
      }

    } else {
      System.err.println("ERROR specify -in [bam] [-in [bam]...] + -out [bam]");  // debug
    }
  }

}
