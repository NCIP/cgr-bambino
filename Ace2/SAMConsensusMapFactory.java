package Ace2;

import net.sf.samtools.*;
import java.io.*;
import java.util.*;
import java.net.*;

import static Ace2.Constants.ALIGNMENT_GAP_CHAR;
import static Ace2.Constants.ALIGNMENT_PADDING_CHAR;
import static Ace2.Constants.ALIGNMENT_DELETION_CHAR;
import static Ace2.Constants.ALIGNMENT_SKIPPED_CHAR_F;
import static Ace2.Constants.ALIGNMENT_SKIPPED_CHAR_R;
import static Ace2.Constants.ALIGNMENT_HARD_CLIPPED_CHAR;

public class SAMConsensusMapFactory {
  //
  // generates SAMConsensusMapping objects
  //
  private char[] consensus;
  private byte[] reference_sequence;
  private PadMap pm;
  private int start_offset = 0;
  // offset to apply to all reads (i.e. when generating an assembly from a chromosome subregion)
  private UniqueReadName urn;
  private boolean is_prepadded;

  private static final int MIN_HIGH_QUALITY = 20;
  // minimum quality score to consider a mismatch "high quality"

  private static final int NULL_HACK = -123456;

  private static final boolean VERBOSE = false;
  //  private static final boolean VERBOSE = true;

  public SAMConsensusMapFactory (char[] consensus, byte[] reference_sequence, PadMap pm, int start_offset, boolean is_prepadded) {
    this.consensus=consensus;
    this.reference_sequence = reference_sequence;
    this.pm=pm;
    this.start_offset=start_offset;
    this.is_prepadded = is_prepadded;
    urn = new UniqueReadName();
  }

  public SAMConsensusMapping map (SAMRecord sr) throws IOException {

    if (is_prepadded) {
      //      System.err.println("hey now: prepadded");  // debug
      return map_prepadded(sr);
    }

    //    System.err.println("cigar="+SAMUtils.cigar_to_string(sr.getCigar()));  // debug

    if (VERBOSE) System.err.println("align_start=" +sr.getAlignmentStart() + " uc_start=" + sr.getUnclippedStart());  // debug

    int sou = sr.getUnclippedStart() - start_offset;
    // start offset, unpadded
    //    int sou = sr.getAlignmentStart() - start_offset;

    //    System.err.println("map start="+ (sou + start_offset));  // debug

    int ri = 0;
    // index in read bases

    int ci;
    int first_aligned_ri = NULL_HACK;

    boolean custom_sequence = false;
    Cigar c = sr.getCigar();
    CigarOperator co;

    if (sou < 1) {
      // alignment starts before 1st consensus base
      //      System.err.println("alignment starts before 1st consensus base");  // debug
      //      ci = 1;
      //      ri = 1 - sou;
      ci = sou;
      //      System.err.println("start ci = " + ci);  // debug

      //      System.err.println("WTF: invalid SOU " + sou + " for " + sr.getReadName() + " raw=" + sr.getAlignmentStart() + " so=" + start_offset);  // debug
      custom_sequence = true;
    } else {
      //      ci = pm.get_unpadded_to_padded(sr.getUnclippedStart() - start_offset);
      ci = pm.get_unpadded_to_padded(sou);
      // start alignment position, in consensus space
    }

    for (CigarElement ce : c.getCigarElements()) {
      co = ce.getOperator();
      //      System.err.println("co="+co);  // debug
      if (co.equals(CigarOperator.INSERTION)
	  //	  || co.equals(CigarOperator.SOFT_CLIP) 
	  ) {
	// since insertions are not used in calculating getAlignmentStart(),
	// need to backtrack if a read uses an insertion CIGAR before a MATCH_OR_MISMATCH.
	// example: cigar_p.bat -restrict GS17754-FS3-L03-3:12059438
	ci -= ce.getLength();
	//	System.err.println("HEY NOW: updated ci = " + ci);  // debug
	if (ci != NULL_HACK && ci >= 0) first_aligned_ri = 0;

	//	break;
	// don't quit: there may be multiple tags before alignment starts
	// example: bug_reports\2011_10_04_peter_cock_sam_X_and_equals_tag\test_235.bat
	// CIGAR = 1S2I34M
      } else if (
		 co.equals(CigarOperator.MATCH_OR_MISMATCH) ||
		 co.equals(CigarOperator.HARD_CLIP) ||
		 co.equals(CigarOperator.EQ) ||
		 co.equals(CigarOperator.X)
		 ) {
	// stop once we encounter a tag that's used by getAlignmentStart()
	// or getClippedStart().
	// FIX ME: other tags too??  SKIPPED_REGION??
	break;
      } else if (
		 co.equals(CigarOperator.SOFT_CLIP)
		 ) {
	break;
	// soft clips are not reflected in getAlignmentStart()
	// but they are in getUnclippedStart().
	// If we encounter a soft clip, stop; special insertion handling
	// is only for LEADING insertions.
      } else {
	//	if (VERBOSE) System.err.println("blowing off CIGAR operator " + co);  // debug
	System.err.println("WARNING: blowing off CIGAR operator " + co);  // debug
      }
    }


    SAMConsensusMapping scm = new SAMConsensusMapping(sr);
    scm.asm_start_padded = ci < 1 ? 1 : ci;
    // negative offsets (before asm start) will be compensated for during alignment
    // process below

    ci--;
    // convert to array offset

    scm.suffix = urn.get_suffix(sr.getReadName(), sr.getReadNegativeStrandFlag());
    // generate .F/.R suffix for read

    //
    //  clip start/end setup:
    //
    //    System.err.println("align end = " + sr.getAlignmentEnd());  // debug

    //    System.err.println("clip setup for read "+sr.getReadName() + "." + scm.suffix+ " " + scm.clip_start_padded + " " + scm.clip_end_padded);  // debug
//     sou = sr.getAlignmentStart() - start_offset;
//     int clip_base;
//     if (sou < 1) {
//       clip_base = sou;
//     } else {
//       clip_base = pm.get_unpadded_to_padded(sou);
//     }
//     scm.clip_start_padded = clip_base < 1 ? 1 : clip_base;

    //
    //  map:
    //

    //    System.err.println("ci (0-based)="+ci);  // debug

    byte[] read = sr.getReadBases();
    //    System.err.println("read=" + new String(read));  // debug
    //    System.err.println("cons:" + new String(consensus, ci, 100));
    //    System.err.println(" seq:" + read);  // debug

    if (VERBOSE) System.err.println("mapping " + sr.getReadName() + "." + scm.suffix + " len=" + read.length + " cigar=" + SAMUtils.cigar_to_string(sr.getCigar()) + " bases:" + new String(read));

    int len;

    int i;

    StringBuffer aligned = new StringBuffer();
    
    int first_insertion_cpos = NULL_HACK;
    int last_insertion_cpos = NULL_HACK;

    int first_soft_clip_cpos = NULL_HACK;

    for (CigarElement ce : c.getCigarElements()) {
      co = ce.getOperator();
      len = ce.getLength();
      i = 0;

      if (VERBOSE) {
	System.err.println("processing operator " + co + ", start ci="+(ci+start_offset) + " ri="+ri + " raw_ci:" + ci);  // debug
      }

      if (
	  co.equals(CigarOperator.MATCH_OR_MISMATCH) ||
	  co.equals(CigarOperator.SOFT_CLIP) ||
	  co.equals(CigarOperator.EQ) ||
	  // match to alignment
	  co.equals(CigarOperator.X)
	  // mismatch to alignment
	  ) {
	// normal alignment

	if (co.equals(CigarOperator.SOFT_CLIP) && first_soft_clip_cpos == NULL_HACK) {
	  first_soft_clip_cpos = ci + start_offset;
	}

	for (; i < len; i++) {
	  if (ci >= consensus.length) break;

	  if (ci < 0) {
	    // aligned before our assembly starts
	    //	    System.err.println("align before start, ci=" + ci + " ri="+ri);  // debug
	    ci++;
	    ri++;
	    custom_sequence = true;
	  } else if (consensus[ci++] == ALIGNMENT_GAP_CHAR) {
	    aligned.append(ALIGNMENT_GAP_CHAR);
	    i--;
	    custom_sequence = true;
	  } else {
	    if (ri >= read.length) {
	      System.err.println("WTF: ri past end of read for " + sr.getReadName() + "." + scm.suffix);  // debug
	      break;
	    } else {
	      if (first_aligned_ri == NULL_HACK) first_aligned_ri = ri;
	      aligned.append((char) read[ri++]);
	    }
	  }
	}
      } else if (co.equals(CigarOperator.HARD_CLIP)) {
	// hard-clipped regions do NOT have clipped bases present in 
	// read bases array.  So we don't need to increment read index,
	// but we do need to increment reference sequence position
	// since we started via sr.getUnclippedStart()
	if (VERBOSE) System.err.println("hard clip of " + len);  // debug

	custom_sequence = true;
	for (; i < len; i++) {
	  if (ci >= consensus.length) break;

	  if (ci < 0) {
	    // aligned before our assembly starts
	    ci++;
	  } else if (consensus[ci++] == ALIGNMENT_GAP_CHAR) {
	    // encountered gap from insertion in another read, so re-do this base
	    aligned.append(ALIGNMENT_HARD_CLIPPED_CHAR);
	    i--;
	  } else {
	    // these bases are not present in read array so nothing to add to aligned sequence
	    aligned.append(ALIGNMENT_HARD_CLIPPED_CHAR);
	  }
	}
      } else if (co.equals(CigarOperator.PADDING)) {
	// "silent" padding for (non-provided) padded reference sequence: ignore
	//	System.err.println("PADDING3");  // debug
// 	if (false) {
// 	  custom_sequence = true;
// 	  for (; i < len; i++) {
// 	    if (ci >= consensus.length) {
// 	      // past end
// 	      break;
// 	    } else if (ci >= 0) {
// 	      // if in visible range
// 	      aligned.append(ALIGNMENT_PADDING_CHAR);
// 	    }
// 	    ci++;
// 	  }
// 	}
      } else if (co.equals(CigarOperator.INSERTION)) {
	// insertion

	int start = ci + start_offset;
	int end = ci + (len - 1) + start_offset;
	//	if (first_insertion_cpos == -1) first_insertion_cpos = start;
	if (first_insertion_cpos == NULL_HACK) first_insertion_cpos = start;

	if (end > last_insertion_cpos) last_insertion_cpos = end;

	for (; i < len; i++) {
	  if (ci < 0) {
	    // before alignment starts
	    //	    System.err.println("insertion before start, ci=" + ci + " ri="+ri);  // debug
	    ri++;
	    // insertion is in the read only, consensus position not affected
	    custom_sequence = true;
	  } else {
	    if (first_aligned_ri == NULL_HACK) first_aligned_ri = ri;
	    // if insertion occurs before first aligned block, save 
	    // index (used later for quality mapping)

	    aligned.append((char) read[ri++]);
	    // insertion appears in query sequence

	    if (ci >= consensus.length) break;
	    if (consensus[ci] == ALIGNMENT_GAP_CHAR) {
	      //	      System.err.println("ok");  // debug
	    } else {
	      System.err.println("WTF: insertion not reflected in consensus! at " + (ci + start_offset) + " for " + sr.getReadName());  // debug
	    }

	    ci++;
	    // consensus has already been padded to record this event, so pass on
	  }
	}
      } else if (co.equals(CigarOperator.DELETION) || co.equals(CigarOperator.SKIPPED_REGION)) {
	// deletion from the reference (= pads in query), or skipped region
	// pad characters don't appear in getReadBases()
	custom_sequence = true;
	char marker;
	if (co.equals(CigarOperator.DELETION)) {
	  //	  marker = ALIGNMENT_GAP_CHAR;
	  marker = ALIGNMENT_DELETION_CHAR;
	} else if (sr.getReadNegativeStrandFlag()) {
	  marker = ALIGNMENT_SKIPPED_CHAR_R;
	} else {
	  marker = ALIGNMENT_SKIPPED_CHAR_F;
	}
	for (; i < len; i++, ci++) {
	  if (ci >= consensus.length) break;
	  if (ci < 0) {
	    // before alignment starts
	    //	    System.err.println("deletion before start, ci=" + ci + " ri="+ri);  // debug
 	  } else {
	    if (consensus[ci] == ALIGNMENT_GAP_CHAR) {
	      //
	      // "HWARF!" - Stimpson J. Cat
	      //
	      // While processing a deletion from the reference, encountered 
	      // an insertion in the newly-padded reference sequence. 
	      // Compensate by extending this deletion.
	      //
	      // example: 207/TP53: HWI-EAS289_96837672:3:63:263:1473#0.R1
	      if (false) {
		System.err.println("HWARF disabled");  // debug
	      } else {
		i--;
		// since the reference is padded here due to an insert elsewhere,
		// don't consider this portion of the deletion processed yet
	      }
	      aligned.append(co.equals(CigarOperator.DELETION) ? ALIGNMENT_GAP_CHAR : marker);
	      // don't imply deletion is larger than it really is: only use deletion
	      // character at deletion site, not in gap region
	    } else {
	      aligned.append(marker);
	    }
	  }
	}
      } else {
	throw new IOException("ERROR: unhandled SAM operator " + co);
      }
    }
    //    System.err.println("aligned=" + new String(aligned));  // debug

     //    System.err.println("FIP="+ first_insertion_cpos + " LIP="+last_insertion_cpos);  // debug

     int cs = sr.getAlignmentStart();
     //     System.err.println("cs1="+cs);  // debug
     // we can't just use getUnclippedStart() because this does not account
     // for cases where an insertion precedes the first aligned sequence.
     // example: cigar_p.bat -restrict GS17754-FS3-L03-3:12059438 
     

     //     System.err.println("DEBUG, COMMENTED OUT " + sr.getAlignmentStart() + " " + sr.getUnclippedStart());  // debug

     //     if (VERBOSE) System.err.println("cs=" + cs + " first_insert_cpos="+first_insertion_cpos + " first_soft_cpos="+first_soft_clip_cpos);  // debug
     if (VERBOSE) System.err.println("cs=" + cs + " first_insert_cpos="+first_insertion_cpos + " first_soft_cpos="+first_soft_clip_cpos);  // debug


     if (false) {
       System.err.println("cs shift disabled");  // debug
     } else if ( 
	 //	 false && 
	 first_insertion_cpos != NULL_HACK &&
	 first_insertion_cpos < cs 
	 ) {
       //
       //  An insertion appears before first aligned block of sequence, i.e. getAlignmentStart()
       //  so shift mapping position backwards to show these bases
       //
       if (VERBOSE) System.err.println("hey now 1, adjusting from " + cs + " to " + first_insertion_cpos + " FSCP:" + first_soft_clip_cpos);  // debug
       cs = first_insertion_cpos;
       // still a little buggy, e.g. martin.bat -restrict GQF67IL01DH4RB
       //
       // contrast however with cigar_p.bat -restrict GS17754-FS3-L03-3:12059438 
       // (which is fine)
     }

     if (false && 
	 first_soft_clip_cpos != NULL_HACK &&
	 first_soft_clip_cpos < cs) {
       cs = first_soft_clip_cpos;
       if (VERBOSE) System.err.println("HEY NOW 2, revised cs="+cs);  // debug
     }


     //     System.err.println("cs2="+cs);  // debug
     //    scm.clip_start_padded = translate_clip_base(sr.getAlignmentStart());

     scm.clip_start_padded = translate_clip_base(cs);

     int ce = sr.getAlignmentEnd();
     if (last_insertion_cpos != NULL_HACK && last_insertion_cpos > ce) ce = last_insertion_cpos;
     //    scm.clip_end_padded = translate_clip_base(sr.getAlignmentEnd());
     scm.clip_end_padded = translate_clip_base(ce);

     scm.asm_end_padded = ci;
     // FIX ME: ci - 1?
     char[] padded = aligned.toString().toCharArray();
     scm.padded_sequence = new byte[padded.length];
     for (i=0; i < padded.length; i++) {
       scm.padded_sequence[i] = (byte) padded[i];
     }

     //    System.err.println("finl:" + aligned.toString());  // debug

     //
     //  map quality:
     //
     byte[] quals = sr.getBaseQualities();
     scm.padded_quality = new byte[scm.padded_sequence.length];

     //     System.err.println("fari="+first_aligned_ri);  // debug

     if (first_aligned_ri > 0) custom_sequence = true;
     int qi = first_aligned_ri > 0 ? first_aligned_ri : 0;
     // if part of sequence was aligned before assembly start, this will be > 0

     // System.err.println("start qi="+qi);  // debug

    for (i=0; i < scm.padded_sequence.length; i++) {
      //      System.err.println("nt at " + i + " = " + (char) scm.padded_sequence[i] + " qi=" + qi);
      
      if (scm.padded_sequence[i] == ALIGNMENT_GAP_CHAR ||
	  scm.padded_sequence[i] == ALIGNMENT_HARD_CLIPPED_CHAR ||
	  scm.padded_sequence[i] == ALIGNMENT_DELETION_CHAR ||
	  scm.padded_sequence[i] == ALIGNMENT_PADDING_CHAR ||
	  scm.padded_sequence[i] == ALIGNMENT_SKIPPED_CHAR_F ||
	  scm.padded_sequence[i] == ALIGNMENT_SKIPPED_CHAR_R) {
	scm.padded_quality[i] = -1;
	custom_sequence = true;
      } else if (qi >= quals.length) {
	// out of bounds
	System.err.println("ERROR: quality info out of bounds for " + sr.getReadName());  // debug
	scm.padded_quality[i] = -1;
      } else {
	scm.padded_quality[i] = quals[qi++];
      }
    }

    if (!custom_sequence) {
      scm.padded_sequence = scm.padded_quality = null;
    }

    //    System.err.println("custom?: " + custom_sequence);  // debug

    return scm;
  }

  public SAMConsensusMapping map_prepadded (SAMRecord sr) throws IOException {
    //
    //  map a read to pre-padded reference sequence.
    //

    if (VERBOSE) System.err.println("PREPADDED align_start=" +sr.getAlignmentStart() + " uc_start=" + sr.getUnclippedStart());  // debug

    int sou = sr.getUnclippedStart() - start_offset;
    // start offset, unpadded
    //    int sou = sr.getAlignmentStart() - start_offset;

    //    System.err.println("map start="+ (sou + start_offset));  // debug

    int ri = 0;
    // index in read bases

    int ci;
    int first_aligned_ri = NULL_HACK;

    boolean custom_sequence = false;
    Cigar c = sr.getCigar();
    CigarOperator co;

    if (sou < 1) {
      // alignment starts before 1st consensus base
      System.err.println("alignment starts before 1st consensus base");  // debug
      //      ci = 1;
      //      ri = 1 - sou;
      ci = sou;
      //      System.err.println("start ci = " + ci);  // debug

      //      System.err.println("WTF: invalid SOU " + sou + " for " + sr.getReadName() + " raw=" + sr.getAlignmentStart() + " so=" + start_offset);  // debug
      custom_sequence = true;
    } else {
      //      ci = pm.get_unpadded_to_padded(sr.getUnclippedStart() - start_offset);
      //      ci = pm.get_unpadded_to_padded(sou);
      ci = sou;
      // start alignment position, in consensus space
    }

    //
    //  this section is the same as with unpadded alignments:
    //
    for (CigarElement ce : c.getCigarElements()) {
      co = ce.getOperator();
      //      System.err.println("co="+co);  // debug
      if (co.equals(CigarOperator.INSERTION)
	  //	  || co.equals(CigarOperator.SOFT_CLIP) 
	  ) {
	// since insertions are not used in calculating getAlignmentStart(),
	// need to backtrack if a read uses an insertion CIGAR before a MATCH_OR_MISMATCH.
	// example: cigar_p.bat -restrict GS17754-FS3-L03-3:12059438
	ci -= ce.getLength();
	//	System.err.println("HEY NOW: updated ci = " + ci);  // debug
	if (ci != NULL_HACK && ci >= 0) first_aligned_ri = 0;

	//	break;
	// don't quit: there may be multiple tags before alignment starts
	// example: bug_reports\2011_10_04_peter_cock_sam_X_and_equals_tag\test_235.bat
	// CIGAR = 1S2I34M
      } else if (
		 co.equals(CigarOperator.MATCH_OR_MISMATCH) ||
		 co.equals(CigarOperator.HARD_CLIP) ||
		 co.equals(CigarOperator.EQ) ||
		 co.equals(CigarOperator.X)
		 ) {
	// stop once we encounter a tag that's used by getAlignmentStart()
	// or getClippedStart().
	// FIX ME: other tags too??  SKIPPED_REGION??
	break;
      } else if (
		 co.equals(CigarOperator.SOFT_CLIP)
		 ) {
	break;
	// soft clips are not reflected in getAlignmentStart()
	// but they are in getUnclippedStart().
	// If we encounter a soft clip, stop; special insertion handling
	// is only for LEADING insertions.
      } else {
	//	if (VERBOSE) System.err.println("blowing off CIGAR operator " + co);  // debug
	System.err.println("WARNING: blowing off CIGAR operator " + co);  // debug
      }
    }


    SAMConsensusMapping scm = new SAMConsensusMapping(sr);
    scm.asm_start_padded = ci < 1 ? 1 : ci;
    // negative offsets (before asm start) will be compensated for during alignment
    // process below

    ci--;
    // convert to array offset

    scm.suffix = urn.get_suffix(sr.getReadName(), sr.getReadNegativeStrandFlag());
    // generate .F/.R suffix for read

    //
    //  map:
    //

    //    System.err.println("ci (0-based)="+ci);  // debug

    byte[] read = sr.getReadBases();
    //    System.err.println("read=" + new String(read));  // debug
    //    System.err.println("cons:" + new String(consensus, ci, 100));
    //    System.err.println(" seq:" + read);  // debug

    if (VERBOSE) System.err.println("mapping " + sr.getReadName() + "." + scm.suffix + " len=" + read.length + " cigar=" + SAMUtils.cigar_to_string(sr.getCigar()) + " bases:" + new String(read));

    int len;

    int i;

    StringBuffer aligned = new StringBuffer();
    
    int first_insertion_cpos = NULL_HACK;
    int last_insertion_cpos = NULL_HACK;

    int first_soft_clip_cpos = NULL_HACK;

    for (CigarElement ce : c.getCigarElements()) {
      co = ce.getOperator();
      len = ce.getLength();
      i = 0;

      if (VERBOSE) {
	System.err.println("processing operator " + co + ", start ci="+(ci+start_offset) + " ri="+ri + " raw_ci:" + ci);  // debug
      }

      if (
	  co.equals(CigarOperator.MATCH_OR_MISMATCH) ||
	  co.equals(CigarOperator.SOFT_CLIP) ||
	  co.equals(CigarOperator.EQ) ||
	  // match to alignment
	  co.equals(CigarOperator.X)
	  // mismatch to alignment
	  ) {
	// normal alignment

	if (co.equals(CigarOperator.SOFT_CLIP) && first_soft_clip_cpos == NULL_HACK) {
	  first_soft_clip_cpos = ci + start_offset;
	}

	for (; i < len; i++) {
	  if (ci >= consensus.length) break;

	  if (ci < 0) {
	    // aligned before our assembly starts
	    //	    System.err.println("align before start, ci=" + ci + " ri="+ri);  // debug
	    ci++;
	    ri++;
	    custom_sequence = true;
	  } else if (ri >= read.length) {
	    System.err.println("WTF: ri past end of read for " + sr.getReadName() + "." + scm.suffix);  // debug
	    break;
	  } else {
	    if (first_aligned_ri == NULL_HACK) first_aligned_ri = ri;
	    aligned.append((char) read[ri++]);
	    ci++;
	  }
	}
      } else if (co.equals(CigarOperator.HARD_CLIP)) {
	// hard-clipped regions do NOT have clipped bases present in 
	// read bases array.  So we don't need to increment read index,
	// but we do need to increment reference sequence position
	// since we started via sr.getUnclippedStart()
	if (VERBOSE) System.err.println("hard clip of " + len);  // debug

	custom_sequence = true;
	for (; i < len; i++) {
	  if (ci >= consensus.length) break;

	  if (ci < 0) {
	    // aligned before our assembly starts
	    ci++;
	  } else if (consensus[ci++] == ALIGNMENT_GAP_CHAR) {
	    // encountered gap from insertion in another read, so re-do this base
	    aligned.append(ALIGNMENT_HARD_CLIPPED_CHAR);
	    i--;
	  } else {
	    // these bases are not present in read array so nothing to add to aligned sequence
	    aligned.append(ALIGNMENT_HARD_CLIPPED_CHAR);
	  }
	}
      } else if (co.equals(CigarOperator.PADDING)) {
	// "silent" padding for (non-provided) padded reference sequence: ignore
      } else if (co.equals(CigarOperator.INSERTION)) {
	// insertion
	System.err.println("WTF: insertion in pre-padded alignment!");  // debug
	// "theoretically" there shouldn't be any CIGAR insertions in pre-padded
	// sequence, since these would have all been pre-reconciled with the reference
	// sequence.  Reads with "insertions" now simply have additional sequence
	// other reads don't.  Reads without the insertion will show deletion
	// from the padded reference.
      } else if (co.equals(CigarOperator.DELETION) || co.equals(CigarOperator.SKIPPED_REGION)) {
	// deletion from the reference (= pads in query), or skipped region
	// pad characters don't appear in getReadBases()

	//
	// PROBABLY NEEDS WORK FOR PRE-PADDED SEQUENCE!
	//
	custom_sequence = true;
	char marker;
	if (co.equals(CigarOperator.DELETION)) {
	  //	  marker = ALIGNMENT_GAP_CHAR;
	  marker = ALIGNMENT_DELETION_CHAR;
	} else if (sr.getReadNegativeStrandFlag()) {
	  marker = ALIGNMENT_SKIPPED_CHAR_R;
	} else {
	  marker = ALIGNMENT_SKIPPED_CHAR_F;
	}
	for (; i < len; i++, ci++) {
	  if (ci >= consensus.length) break;
	  if (ci < 0) {
	    // before alignment starts, ignore
	    //	    System.err.println("deletion before start, ci=" + ci + " ri="+ri);  // debug
 	  } else {
	    aligned.append(marker);
	  }
	}
      } else {
	throw new IOException("ERROR: unhandled SAM operator " + co);
      }
    }
    //    System.err.println("aligned=" + new String(aligned));  // debug

     //    System.err.println("FIP="+ first_insertion_cpos + " LIP="+last_insertion_cpos);  // debug

     int cs = sr.getAlignmentStart();
     //     System.err.println("cs1="+cs);  // debug
     // we can't just use getUnclippedStart() because this does not account
     // for cases where an insertion precedes the first aligned sequence.
     // example: cigar_p.bat -restrict GS17754-FS3-L03-3:12059438 
     

     //     System.err.println("DEBUG, COMMENTED OUT " + sr.getAlignmentStart() + " " + sr.getUnclippedStart());  // debug

     //     if (VERBOSE) System.err.println("cs=" + cs + " first_insert_cpos="+first_insertion_cpos + " first_soft_cpos="+first_soft_clip_cpos);  // debug
     if (VERBOSE) System.err.println("cs=" + cs + " first_insert_cpos="+first_insertion_cpos + " first_soft_cpos="+first_soft_clip_cpos);  // debug


     if (false) {
       System.err.println("cs shift disabled");  // debug
     } else if ( 
	 //	 false && 
	 first_insertion_cpos != NULL_HACK &&
	 first_insertion_cpos < cs 
	 ) {
       //
       //  An insertion appears before first aligned block of sequence, i.e. getAlignmentStart()
       //  so shift mapping position backwards to show these bases
       //
       if (VERBOSE) System.err.println("hey now 1, adjusting from " + cs + " to " + first_insertion_cpos + " FSCP:" + first_soft_clip_cpos);  // debug
       cs = first_insertion_cpos;
       // still a little buggy, e.g. martin.bat -restrict GQF67IL01DH4RB
       //
       // contrast however with cigar_p.bat -restrict GS17754-FS3-L03-3:12059438 
       // (which is fine)
     }

     if (false && 
	 first_soft_clip_cpos != NULL_HACK &&
	 first_soft_clip_cpos < cs) {
       cs = first_soft_clip_cpos;
       if (VERBOSE) System.err.println("HEY NOW 2, revised cs="+cs);  // debug
     }


     //     System.err.println("cs2="+cs);  // debug
     //    scm.clip_start_padded = translate_clip_base(sr.getAlignmentStart());

     scm.clip_start_padded = translate_clip_base_prepadded(cs);

     int ce = sr.getAlignmentEnd();
     if (last_insertion_cpos != NULL_HACK && last_insertion_cpos > ce) ce = last_insertion_cpos;
     //    scm.clip_end_padded = translate_clip_base(sr.getAlignmentEnd());
     scm.clip_end_padded = translate_clip_base_prepadded(ce);

     scm.asm_end_padded = ci;
     // FIX ME: ci - 1?
     char[] padded = aligned.toString().toCharArray();
     scm.padded_sequence = new byte[padded.length];
     for (i=0; i < padded.length; i++) {
       scm.padded_sequence[i] = (byte) padded[i];
     }

     //    System.err.println("finl:" + aligned.toString());  // debug

     //
     //  map quality:
     //
     byte[] quals = sr.getBaseQualities();
     scm.padded_quality = new byte[scm.padded_sequence.length];

     //     System.err.println("fari="+first_aligned_ri);  // debug

     if (first_aligned_ri > 0) custom_sequence = true;
     int qi = first_aligned_ri > 0 ? first_aligned_ri : 0;
     // if part of sequence was aligned before assembly start, this will be > 0

     // System.err.println("start qi="+qi);  // debug

    for (i=0; i < scm.padded_sequence.length; i++) {
      //      System.err.println("nt at " + i + " = " + (char) scm.padded_sequence[i] + " qi=" + qi);
      
      if (scm.padded_sequence[i] == ALIGNMENT_GAP_CHAR ||
	  scm.padded_sequence[i] == ALIGNMENT_HARD_CLIPPED_CHAR ||
	  scm.padded_sequence[i] == ALIGNMENT_DELETION_CHAR ||
	  scm.padded_sequence[i] == ALIGNMENT_PADDING_CHAR ||
	  scm.padded_sequence[i] == ALIGNMENT_SKIPPED_CHAR_F ||
	  scm.padded_sequence[i] == ALIGNMENT_SKIPPED_CHAR_R) {
	scm.padded_quality[i] = -1;
	custom_sequence = true;
      } else if (qi >= quals.length) {
	// out of bounds
	System.err.println("ERROR: quality info out of bounds for " + sr.getReadName());  // debug
	scm.padded_quality[i] = -1;
      } else {
	scm.padded_quality[i] = quals[qi++];
      }
    }

    if (!custom_sequence) {
      scm.padded_sequence = scm.padded_quality = null;
    }

    //    System.err.println("custom?: " + custom_sequence);  // debug

    return scm;
  }


  public static void main (String[] argv) {
    try {
//       URL url = new URL("file://localhost/c:/me/work/java2/Ace2/egfr/egfr.fasta");
//       BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
//       FASTASequenceReader fa = new FASTASequenceReader(br, false);
//       StringBuffer sb = fa.get_sequences().get("chr7");
//       System.err.println("len="+sb.length());  // debug

//       SAMConsensusBuilder scb = new SAMConsensusBuilder(sb.toString().toCharArray(), 55054118);

//       String[] files = {"egfr_1.bam", "egfr_2.bam"};

//       ArrayList<SAMRecord> all = new ArrayList<SAMRecord>();

//       for (int fi=0; fi < files.length; fi++) {
// 	SAMRecord[] sams = SAMUtils.load_sams(new File(files[fi]));
// 	scb.add_samrecords(sams);
// 	for (int i = 0; i < sams.length; i++) {
// 	  all.add(sams[i]);
// 	  // hack
// 	}
//       }

//       scb.build_consensus();

//       char[] cons = scb.get_consensus();
//       PadMap pm = new PadMap(cons);

//       //      int pi = 4589 - 1;
//       //      int pi=127482;

//       SAMConsensusMapFactory sm = new SAMConsensusMapFactory(cons, scb.get_reference_sequence(), pm, 55054118);

//       System.err.println("sams:" + all.size());
//       for (SAMRecord sr : all) {
// 	sm.map(sr);
//       }

//       System.err.println("done, sleeping");  // debug
// try {
// System.out.println("killing time...");
// Thread.sleep(100000);
// } catch (InterruptedException e) {}



    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private int translate_clip_base_prepadded (int unpadded_base_num) {
    int clip_base = unpadded_base_num - start_offset;
    return clip_base < 1 ? 1 : clip_base;
  }

  private int translate_clip_base (int unpadded_base_num) {
    int sou = unpadded_base_num - start_offset;
    int clip_base;

    int mup = pm.get_max_unpadded();
    //    System.err.println("mup="+mup + " " + (mup+start_offset));  // debug

    if (sou < 1) {
      clip_base = sou;
    } else if (sou > mup) {
      int mapped = pm.get_unpadded_to_padded(mup);
      if (mapped == PadMap.UNDEF) System.err.println("ERROR: bogus map translation position");  // debug

      //      System.err.println("mup=" + mup + " raw map="+mapped + " prev:" + pm.get_unpadded_to_padded(mup - 1) + " prev2:" + pm.get_unpadded_to_padded(mup - 2));  // debug

      int extra = sou - mup;
      clip_base = mapped + extra;
      //      System.err.println("test me: end clipping pos past end of ref seq " + sou + " " + max_translatable_basenum + " " + mapped + " " + extra);  // debug

      //      System.err.println("req = " + unpadded_base_num);  // debug
      //      System.err.println("max translatable = " + max_translatable_basenum);  // debug
      //      System.err.println("max translated = " + mapped);  // debug
      //      System.err.println("after = " + clip_base);  // debug
    } else {
      clip_base = pm.get_unpadded_to_padded(sou);
      if (clip_base == PadMap.UNDEF) System.err.println("ERROR: bogus map translation position");  // debug
    }
    return clip_base < 1 ? 1 : clip_base;
  }

}