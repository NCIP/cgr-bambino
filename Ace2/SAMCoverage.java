package Ace2;

import java.util.*;
import java.io.*;
import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;

public class SAMCoverage {
  public int MIN_QUALITY = 15;
  private String outfile;
  PrintStream ps = System.out;
  private String name = null;
  private boolean verbose_mode = false;

  public void set_outfile (String outfile) {
    this.outfile = outfile;
  }

  public void set_name (String name) {
    this.name = name;
  }

  public void set_verbose (boolean v) {
    verbose_mode = v;
  }

  public void set_min_quality (int q) {
    MIN_QUALITY = q;
  }

  public void setPrintStream (PrintStream ps) {
    this.ps = ps;
  }

  public void find_coverage (SAMResource sres) {
    int start_base = sres.region.range.start;
    int end_base = sres.region.range.end;

    int coverage_len = (end_base - start_base) + 1;
    int i,end, ref_i, read_i, len;

    int[] coverage = new int[coverage_len];
    Arrays.fill(coverage, 0);

    WorkingFile wf = null;
    if (outfile != null) {
      try {
	wf = new WorkingFile(outfile);
	ps = wf.getPrintStream();
      } catch (Exception e) {
	System.err.println("I/O error: " + e);  // debug
	e.printStackTrace();
	System.exit(1);
      }
    }

    try {
      //
      //  gather coverage info:
      //
      CloseableIterator<SAMRecord> iterator = sres.get_iterator();
      int read_count = 0;
      int ref_min = -1;
      int ref_max = -1;

      while (iterator.hasNext()) {
	SAMRecord sr = iterator.next();
	read_count++;

	//	System.err.println(sr.getReadName() + ": " + sr.getAlignmentStart() + "-" + sr.getAlignmentEnd());  // debug

	if (sr.getReadUnmappedFlag()) continue;
	if (sr.getDuplicateReadFlag()) {
	  if (verbose_mode) System.err.println(sr.getReadName() + "." + 
					       (sr.getReadNegativeStrandFlag() ? "R" : "F") +
					       " ignoring, duplicate");
	  continue;
	}

	byte[] read = sr.getReadBases();
	byte[] quals = sr.getBaseQualities();

	for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	  len = ab.getLength();
	  read_i = ab.getReadStart() - 1;
	  ref_i = ab.getReferenceStart() - start_base;

	  if (ref_min == -1 || ref_i < ref_min) ref_min = ref_i;

	  for (i = read_i, end = read_i + len; i < end; i++, ref_i++) {
	    if (ref_i >= 0 && ref_i < coverage_len) {
	      if (quals[i] >= MIN_QUALITY) {
		if (verbose_mode) System.err.println(sr.getReadName() + "." + 
						     (sr.getReadNegativeStrandFlag() ? "R" : "F") +
						     " hit at " + (ref_i + start_base) +
						     " as=" + sr.getAlignmentStart() + 
						     " ae=" + sr.getAlignmentEnd()
						     );
		coverage[ref_i]++;
	      } else if (verbose_mode) {
		System.err.println(sr.getReadName() + "." + 
				   (sr.getReadNegativeStrandFlag() ? "R" : "F") +
				   " qual_reject at " + (ref_i + start_base) +
				   " as=" + sr.getAlignmentStart() + 
				   " ae=" + sr.getAlignmentEnd()
				   );
	      }
	    }
	  }
	  if (ref_max == -1 || ref_i > ref_max) ref_max = ref_i;

	}
      }
      sres.close();
      System.err.println("records:" + read_count + " ref_min:" + (ref_min + start_base) + " ref_max:" + (ref_max + start_base));  // debug

      //
      //  report coverage info:
      //
      for (i=0; i < coverage.length; i++) {
	if (name != null) ps.print(name + ",");
	ps.println((i + start_base) + "," + coverage[i]);  // debug
      }
      if (wf != null) wf.finish();
      
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SAMResource sr = null;
    SAMRegion region = new SAMRegion();
    region.range = new Range();
    region.range.start = -1;
    region.range.end = -1;

    String outfile = null;
    String target_file = null;
    SAMCoverage sc = new SAMCoverage();

    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	sr = new SAMResource();
	//	sr.import_data(SAMResourceTags.SAM_URL, argv[++i]);
	sr.set_file(argv[++i]);
	sr.detect_sample_id();
      } else if (argv[i].equals("-targets")) {
	target_file = argv[++i];
      } else if (argv[i].equals("-tname")) {
	region.tname = argv[++i];
      } else if (argv[i].equals("-verbose")) {
	sc.set_verbose(true);
      } else if (argv[i].equals("-tstart")) {
	region.range.start = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-tend")) {
	region.range.end = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-of")) {
	outfile = argv[++i];
      } else if (argv[i].equals("-min-quality")) {
	sc.set_min_quality(Integer.parseInt(argv[++i]));
      } else {
	System.err.println("error: unknown switch " + argv[i]);  // debug
	System.exit(1);
      }
    }

    String error = null;
    if (sr == null) {
      error = "specify -bam [file]";
    } else if (target_file == null) {
      if (region.tname == null) {
	error = "specify -tname";
      } else if (region.range.start == -1) {
	error = "specify -tstart";
      } else if (region.range.end == -1) {
	error = "specify -tend";
      }
    }

    sr.set_region(region);

    if (error != null) {
      System.err.println("ERROR: " + error);  // debug
    } else if (target_file != null) {
      try {
	File f = new File(target_file);
	BufferedReader br = new BufferedReader(new FileReader(f));
	String line = br.readLine();
	String[] headers = line.split("\t");

	if (headers[0].equals("Name") &&
	    headers[1].equals("Chromosome") &&
	    headers[2].equals("Start") &&
	    headers[3].equals("End")) {
	  
	  WorkingFile wf = null;
	  if (outfile != null) {
	    wf = new WorkingFile(outfile);
	    sc.setPrintStream(wf.getPrintStream());
	  }

	  while (true) {
	    line = br.readLine();
	    if (line == null) {
	      // EOF
	      break;
	    } else {
	      String[] row = line.split("\t");

	      region.tname = row[1];
	      region.range.start = Integer.parseInt(row[2]);
	      region.range.end = Integer.parseInt(row[3]);
	      //	      sc.set_name(new String(row[0]));
	      sc.set_name(new String(row[0]) + "," + new String(row[1]));
	      sc.find_coverage(sr);
	    }
	  }

	  if (outfile != null) wf.finish();

	} else {
	  throw new IOException("file format error");
	}

      } catch (Exception e) {
	System.err.println("ERROR: " + e);  // debug
	e.printStackTrace();
	System.exit(1);
      }
    } else {
      sc.set_outfile(outfile);
      sc.find_coverage(sr);
    }

  }
}

