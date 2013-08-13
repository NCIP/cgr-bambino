package Ace2;

import net.sf.samtools.*;
import java.util.*;
import java.io.*;

public class SEUInteresting {
  //
  // determine whether a given mapped SAMRecord has features we're interested in.
  // operates within a single reference sequence ONLY.
  //
  // used by SAMExtractIndels2
  //

  SEUConfig config;
  SAMIndelFilter sif;
  SAMMismatchFilter mmf;
  Integer reference_index = null;
  ArrayList<String> stuff = null;

  public SEUInteresting(SEUConfig config, Integer reference_index) {
    this.config = config;
    mmf = null;
    sif = new SAMIndelFilter();
    this.reference_index = reference_index;
  }

  public int interesting_check (SAMRecord sr) throws IOException {
    int result_flag = 0;

    stuff = new ArrayList<String>();
    // for old-school FASTQ output

    if (mmf == null) {
      mmf = new SAMMismatchFilter(config.reference_sequence.get_all(sr.getReferenceName()));
      mmf.get_config().ENABLE_MISMAP_FILTER = false;
      // we're not performing SNP calling, so make sure we're not 
      // tracking suspicious mismatch positions (memory consumption)
    }

    if (!sr.getReferenceIndex().equals(reference_index)) {
      throw new IOException("ERROR: called on a different ref seq mapping");
      // no-no
    }

    //
    //  check for reads containing high-quality mismatches to reference seq
    //
    mmf.filter(sr);
    //	System.err.println(sr.getReadName() + ": " + mmf.get_hq_mismatches());  // debug
    int hq = mmf.get_hq_mismatches_window(config.HQ_MISMATCH_WINDOW_SIZE);

    int hq_start_clip = mmf.get_start_clipped_hq_mismatches(sr);
    int hq_end_clip = mmf.get_end_clipped_hq_mismatches(sr);
    int hq_usable = hq + hq_start_clip + hq_end_clip;

    if (hq_usable >= config.HQ_MISMATCH_EXTRACT_THRESHOLD) {
      //	    System.err.println("hey now! " + sr.getReadName() + " " + hq);  // debug
      stuff.add("hqmm");
      stuff.add(Integer.toString(hq));

      if (hq > 0) result_flag |= config.FLAG_HAS_HQMM;
      if (hq_start_clip > 0) {
	result_flag |= config.FLAG_HAS_HQMM_SC;
	stuff.add("hqmm_sc");
	stuff.add(Integer.toString(hq_start_clip));
      }
      if (hq_end_clip > 0) {
	result_flag |= config.FLAG_HAS_HQMM_EC;
	stuff.add("hqmm_ec");
	stuff.add(Integer.toString(hq_end_clip));

      }
    }

    //
    //  check for reads w/indels meeting quality filters
    //
    if (config.EXTRACT_INDELS && sif.filter(sr)) {
      for (IndelInfo ii : sif.get_indels()) {
	if (ii.indel_type.equals(CigarOperator.INSERTION)) {
	  result_flag |= config.FLAG_HAS_HQ_INSERTION;
	  stuff.add("insertion");
	} else if (ii.indel_type.equals(CigarOperator.DELETION)) {
	  result_flag |= config.FLAG_HAS_HQ_DELETION;
	  stuff.add("deletion");
	} else {
	  System.err.println("ERROR PROCESSING INDEL");  // debug
	}
      }
    }

    sr.setAttribute(config.SAM_INTERESTING_TAG, Integer.valueOf(result_flag));
    // stamp findings in custom SAM tag

    return result_flag;
  }

  public ArrayList<String> get_fastq_tags() {
    // used for olde-style FASTQ output
    return stuff;
  }

}
