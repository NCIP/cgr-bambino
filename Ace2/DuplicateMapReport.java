package Ace2;

import java.io.*;
import java.util.*;

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;

public class DuplicateMapReport {
  // - find spliced reads with identical mappings
  //   (same reference, alignment start, strand, CIGAR)
  // - also counts max TopHat TH tag

  File bam_file;
  HashMap<Integer,HashMap<String,Integer>> sites = new HashMap<Integer,HashMap<String,Integer>>();
  // ugh: generics

  static int FLUSH_CHECK = 250000;
  static boolean VERBOSE = false;

  int worst_key_count = 0;
  String worst_key = null;

  public DuplicateMapReport (File bam_file) {
    this.bam_file = bam_file;
  }

  private void flush (boolean force, int current_as) {
    HashSet<Integer> delete = new HashSet<Integer>();
    for (Integer site : sites.keySet()) {
      if (force || site < current_as) {
	// site is ready to flush
	delete.add(site);
	HashMap<String,Integer> bucket = sites.get(site);
	for (String key : bucket.keySet()) {
	  int count = bucket.get(key);
	  if (VERBOSE) System.err.println("final " + key + " = " + count);
	  if (count > worst_key_count) {
	    worst_key_count = count;
	    worst_key = key;
	  }
	}
      }
    }

    for (Integer site : delete) {
      sites.remove(site);
    }

    //    System.err.println("flush check");  // debug
  }

  public void report() {
    SAMFileReader sfr = new SAMFileReader(bam_file);
    
    int max_nh = 0;
    // NH tag used by TopHat2 to count total read mappings
    String max_nh_read = "";
    int last_ri = -1;
    int reads = 0;


    Cigar c;

    for (SAMRecord sr : sfr) {
      if (sr.getReadUnmappedFlag()) continue;
      // mapped only

      c = sr.getCigar();
      boolean has_skip = false;
      for (CigarElement ce : c.getCigarElements()) {
	if (ce.getOperator().equals(CigarOperator.SKIPPED_REGION)) {
	  has_skip = true;
	  break;
	}
      }
      if (!has_skip) continue;
      // junction-supporting reads only

      String strand = sr.getReadNegativeStrandFlag() ? "-" : "+";
      int as = sr.getAlignmentStart();

      int ri = sr.getReferenceIndex();
      if (ri != last_ri) {
	System.err.println("processing " + sr.getReferenceName() + "...");  // debug

	flush(true, 0);
	last_ri = ri;
      }
      
      if (++reads % FLUSH_CHECK == 0) {
	System.err.println(as + ": processed " + reads + " ...");  // debug
	flush(false, as);
      }

      try {
	int map_count = sr.getIntegerAttribute("NH");
	if (VERBOSE) {
	  System.err.println("read=" + sr.getReadName() + 
			     " start=" + as + 
			     " strand=" + strand + 
			     " nh="+map_count);  // debug
	}
	if (map_count > max_nh) {
	  max_nh = map_count;
	  max_nh_read = sr.getReadName();
	}
      } catch (RuntimeException ex) {}

      //      String key = sr.getReadName() + "_" + as + "_" + strand + "_" + sr.getCigar();
      String key = sr.getReadName() + "_" + sr.getReferenceName() + "_" + as + "_" + strand + "_" + sr.getCigar();

      HashMap<String,Integer> bucket = sites.get(as);
      if (bucket == null) sites.put(as, bucket = new HashMap<String,Integer>());
      int count = bucket.containsKey(key) ? bucket.get(key) : 0;
      bucket.put(key, count + 1);
    }
    flush(true, 0);

    System.err.println("max NH:" + max_nh + " read:" + max_nh_read);  // debug
    System.err.println("max key:" + worst_key_count + " key:" + worst_key);  // debug

  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    DuplicateMapReport dmr = null;

    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	dmr = new DuplicateMapReport(new File(argv[++i]));
      }
    }

    if (dmr != null) dmr.report();
  }

}
