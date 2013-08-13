package Ace2;

import java.io.*;
import java.util.*;
import net.sf.samtools.*;
import static Ace2.SAMMismatchFilter.MASK_CHAR;

public class SAMIndelFilter {

  private ArrayList<IndelInfo> info = null;
  private ArrayList<IndelInfo> info_broad = null;
  private static boolean VERBOSE = false;
  private SNPConfig config;

  public SAMIndelFilter (SNPConfig config) {
    this.config = config;
  }

  public SAMIndelFilter () {
    config = new SNPConfig();
  }

  public boolean filter (SAMRecord sr) throws IOException {
    Cigar c = sr.getCigar();

    // if (sr.getReadName().equals("HWI-ST729_112913646:1:2208:9271:36179")) {
    //   System.err.println("DEBUG: verbosity enabled");  // debug
    //   VERBOSE = true;
    // }

    CigarOperator co;
    int i;

    byte[] read = sr.getReadBases();
    byte[] qual = sr.getBaseQualities();
    byte[] read_masked = null;

    if (qual.length != read.length) {
      System.err.println("ERROR: read " + sr.getReadName() + " length " + sr.getReadLength() + " does not match quality length " + qual.length);  // debug
      return false;
    }


    int read_i = 0;
    //    int ref_i = sr.getAlignmentStart() - 1;
    // 0-based
    int ref_i = sr.getUnclippedStart() - 1;
    // since we are parsing entire CIGAR string, need to use unclipped start

    int len;
    info = info_broad = null;

    for (CigarElement ce : c.getCigarElements()) {
      co = ce.getOperator();
      len = ce.getLength();
      if (co.equals(CigarOperator.MATCH_OR_MISMATCH) ||
	  co.equals(CigarOperator.SOFT_CLIP) ||
	  co.equals(CigarOperator.EQ) ||
	  co.equals(CigarOperator.X)
	  ) {
	// ordinary alignment, no padding involved
	read_i += len;
	ref_i += len;
      } else if (co.equals(CigarOperator.HARD_CLIP)) {
	// hard-clipped bases are not in read array, so read index doesn't change,
	// however reference position must be incremented
	ref_i += len;
      } else if (co.equals(CigarOperator.DELETION)) {
	//
	// deletion from the reference (= pads in query)
	//
	if (read_masked == null) read_masked = SAMMismatchFilter.generate_masked_sequence(read, config);
	// lazy instantiation: may never be needed

	if (VERBOSE) {
	  System.err.println("possible deletion at " + (ref_i + 1));
	  System.err.println("     raw=" + new String(read));
	  System.err.println("  masked=" + new String(read_masked));
	}

	boolean mask_ok = true;
	boolean quality_ok = (qual[read_i - 1] >= config.MIN_QUALITY &&
			      qual[read_i] >= config.MIN_QUALITY);
	// simple check: bases before and after the deletion are of sufficient quality
	if (config.ENABLE_POLY_X_RUN_MASK_INDEL && 
	    len <= config.POLY_X_RUN_INDEL_MAX_LENGTH &&
	    (read_masked[read_i - 1] == MASK_CHAR ||
	     read_masked[read_i] == MASK_CHAR)
	    ) {
	  mask_ok = false;
	}

	IndelInfo ii = new IndelInfo(CigarOperator.DELETION, ref_i, ce.getLength(), sr, read_i);
	  
	if (quality_ok && mask_ok) {
	  if (info_broad == null) info_broad = new ArrayList<IndelInfo>();
	  info_broad.add(ii);
	  // basic/broad tracking
	}

	if (quality_ok && config.MIN_FLANKING_QUALITY_WINDOW > 0) {
	  quality_ok = SAMUtils.flanking_quality_check(qual, read_i, 0, config.MIN_FLANKING_QUALITY, config.MIN_FLANKING_QUALITY_WINDOW, config.ILLUMINA_QUALITY_2_RUN_MODE);
	}

	if (!quality_ok &&
	    config.SKIP_NT_QUALITY_CHECKS_FOR_HIGH_MAPQ_INDELS &&
	    len >= config.HIGH_MAPQ_INDEL_MIN_LENGTH &&
	    sr.getMappingQuality() >= config.HIGH_MAPQ_INDEL_MIN_MAPQ
	    ) {
	  // skip quality checks if indel is long enough and mapq is high enough
	  if (VERBOSE) System.err.println("forced accepting quality-flunked deletion: " + sr.getReadName() + " len="+len + " mapq=" + sr.getMappingQuality());  // debug
	  quality_ok = true;
	}

	if (quality_ok && mask_ok) {
	  // acceptable deletion
	  if (info == null) info = new ArrayList<IndelInfo>();
	  info.add(ii);
	}

	ref_i += len;
	// reference position incremented (since read deletes from the reference)
	// read_i doesn't change (deletion)
      } else if (co.equals(CigarOperator.PADDING)) {
	// "silent" padding for (non-provided) padded reference sequence: ignore
      } else if (co.equals(CigarOperator.SKIPPED_REGION)) {
	ref_i += len;
	// reference position incremented (read skips this region)
	// read_i doesn't change
      } else if (co.equals(CigarOperator.INSERTION)) {
	//
	// insertion to the reference
	//
	if (read_masked == null) read_masked = SAMMismatchFilter.generate_masked_sequence(read, config);
	// lazy instantiation: may never be needed

	IndelInfo ii = new IndelInfo(CigarOperator.INSERTION, ref_i, len, sr, read_i);

	boolean quality_ok = true;
	boolean mask_ok = true;
	int end = read_i + len;
	ii.sequence = new String(read, read_i, len);

	//
	//  check base qualities:
	//
	if (config.AVERAGE_INSERTION_QUALITY) {
	  // average inserted base qualities
	  int total = 0;
	  for (i = read_i; i < end; i++) {
	    if (VERBOSE) System.err.println(sr.getReadName() + " i=" + read_i + " q="+qual[i] + " mq="+config.MIN_QUALITY);  // debug
	    total += qual[i];
	    if (config.ENABLE_POLY_X_RUN_MASK_INDEL &&
		len <= config.POLY_X_RUN_INDEL_MAX_LENGTH &&
		read_masked[i] == MASK_CHAR) {
	      // always flunk if poly-X masked region hit
	      if (VERBOSE) System.err.println("mask flunk");  // debug
	      mask_ok = false;
	      break;
	    }
	  }
	  int avg = total / len;
	  if (avg < config.MIN_QUALITY) {
	    if (VERBOSE) System.err.println("flunk");  // debug
	    quality_ok = false;
	  } else {
	    //	    System.err.println("===> pass! avg="+avg);  // debug
	  }
	} else {
	  // check all inserted base qualities
	  for (i = read_i; i < end; i++) {
	    if (VERBOSE) System.err.println(sr.getReadName() + " i=" + read_i + " q="+qual[i] + " mq="+config.MIN_QUALITY);  // debug
	    if (qual[i] < config.MIN_QUALITY) quality_ok = false;
	    if (config.ENABLE_POLY_X_RUN_MASK_INDEL &&
		len <= config.POLY_X_RUN_INDEL_MAX_LENGTH &&
		read_masked[i] == MASK_CHAR) {
	      // always flunk if poly-X masked region hit
	      if (VERBOSE) System.err.println("mask flunk");  // debug
	      mask_ok = false;
	      break;
	    }
	  }
	}

	if (quality_ok && mask_ok) {
	  // broad tracking: only check basic quality info
	  if (info_broad == null) info_broad = new ArrayList<IndelInfo>();
	  info_broad.add(ii);
	}

	if (quality_ok && config.MIN_FLANKING_QUALITY_WINDOW > 0) {
	  //
	  //  check flanking qualities:
	  //
	  if (VERBOSE) System.err.println("before flank qual check: " + quality_ok + " mfq="+config.MIN_FLANKING_QUALITY + " win="+config.MIN_FLANKING_QUALITY_WINDOW);  // debug
	  quality_ok = SAMUtils.flanking_quality_check(qual, read_i, len, config.MIN_FLANKING_QUALITY, config.MIN_FLANKING_QUALITY_WINDOW, config.ILLUMINA_QUALITY_2_RUN_MODE);
	  if (VERBOSE) System.err.println("after flank qual check: " + quality_ok);  // debug
	}

	if (!quality_ok &&
	    config.SKIP_NT_QUALITY_CHECKS_FOR_HIGH_MAPQ_INDELS &&
	    len >= config.HIGH_MAPQ_INDEL_MIN_LENGTH &&
	    sr.getMappingQuality() >= config.HIGH_MAPQ_INDEL_MIN_MAPQ
	    ) {
	  // skip quality checks if indel is long enough and mapq is high enough
	  if (VERBOSE) System.err.println("forced accepting quality-flunked insertion: " + sr.getReadName() + " len="+len + " mapq=" + sr.getMappingQuality());  // debug
	  quality_ok = true;
	  // force acceptance
	  //	  System.exit(1);
	}

	if (quality_ok && mask_ok) {
	  if (info == null) info = new ArrayList<IndelInfo>();
	  info.add(ii);
	} else {
	  if (VERBOSE) System.err.println("insertion quality flunk for " + sr.getReadName());  // debug
	}

	read_i += len;
	// ref_i doesn't change (insertion)
      } else {
	throw new IOException("ERROR: unhandled SAM CIGAR operator " + co + " for " + sr.getReadName() + " at " + sr.getReferenceName() + " " + sr.getAlignmentStart());
      }
    }

    return info != null;
  }

  public ArrayList<IndelInfo> get_indels() {
    return info;
  }

  public ArrayList<IndelInfo> get_broad_indels() {
    return info_broad;
  }


}