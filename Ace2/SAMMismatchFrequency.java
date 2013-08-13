package Ace2;
// report frequency with which aligned reads mismatch with reference
// MNE 2/2013

import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.*;

import java.util.*;
import java.io.*;

public class SAMMismatchFrequency {
  ReferenceSequence reference;
  SAMFileReader sfr;
  HashMap<String,Integer> ref_lengths;
  long count_bases_compared, count_bases_mismatch, count_ref_skipped, count_bases_indel, count_bases_with_boundary_issues;

  int MIN_QUALITY = 15;
  int MIN_MAPQ = 1;
  boolean COUNT_INDELS_AS_MISMATCHES = true;

  public void set_reference(ReferenceSequence reference) {
    this.reference = reference;
  }

  private void reset() {
    count_bases_compared = 0;
    count_bases_mismatch = 0;
    count_ref_skipped = 0;
    count_bases_indel = 0;
    count_bases_with_boundary_issues = 0;
  }

  public void set_count_indels_as_mismatches (boolean v) {
    COUNT_INDELS_AS_MISMATCHES = v;
  }

  public void set_minimum_quality (int q) {
    MIN_QUALITY = q;
  }

  public void set_minimum_mapq (int q) {
    MIN_MAPQ = q;
  }

  public void process_bam (File bam) throws IOException {
    System.err.println("minimum mapping quality: " + MIN_MAPQ);  // debug
    System.err.println("minimum base quality: " + MIN_QUALITY);  // debug
    System.err.println("count indels as mismatches: " + COUNT_INDELS_AS_MISMATCHES);  // debug


    reset();

    sfr = new SAMFileReader(bam);
    //
    //  references to process
    //  to do: override
    //
    ArrayList<String> refs_to_process = new ArrayList<String>();

    SAMFileHeader h = sfr.getFileHeader();
    SAMSequenceDictionary dict = h.getSequenceDictionary();
    ref_lengths = new HashMap<String,Integer>();
    for (SAMSequenceRecord ssr : dict.getSequences()) {
      String name = ssr.getSequenceName();
      ref_lengths.put(name, ssr.getSequenceLength());
      refs_to_process.add(name);
    }

    //    System.err.println("refs="+refs_to_process);  // debug
    for (String reference : refs_to_process) {
      get_some(reference);
    }
    report();
  }

  public void report() {
    System.err.println("bases compared: " + count_bases_compared);  // debug
    System.err.println("bases mismatch: " + count_bases_mismatch);  // debug
    System.err.println("bases indel: " + count_bases_indel);
    System.err.println("bases mapped outside reference (ignored): " + count_bases_with_boundary_issues);  // debug

    System.err.println("reference bases skipped: " + count_ref_skipped);  // debug

    double mismatch_frequency = (double) count_bases_mismatch / count_bases_compared;
    System.out.println(mismatch_frequency);  // debug
  }


  
  private void get_some(String rname) throws IOException {
    SAMRecordIterator query = sfr.queryOverlapping(rname, 1, ref_lengths.get(rname));

    byte[] reference_sequence = null;

    int len, read_i, ref_i, end;
    byte[] quals, read;
    boolean qual_mapping_problem = false;

    char c_read, c_ref;

    while (query.hasNext()) {
      SAMRecord sr = query.next();
      if (sr.getMappingQuality() < MIN_MAPQ) continue;

      if (reference_sequence == null) {
	// defer so we can skip this for references with no reads in BAM
	// (slows loop though)
	System.err.print("load reference for " + rname + "...");  // debug
	reference_sequence = reference.get_all(rname);
	System.err.println("done");
      }

      quals = sr.getBaseQualities();
      read = sr.getReadBases();

      //
      //  scan blocks of aligned sequence for mismatches:
      //
      for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	len = ab.getLength();
	read_i = ab.getReadStart() - 1;
	ref_i = ab.getReferenceStart() - 1;

	if (read_i < 0) {
	  System.err.println("ERROR: can't include " + sr.getReadName() + ", read_i < 0");
	} else if (ref_i < 0) {
	  System.err.println("ERROR: can't include " + sr.getReadName() + ", ref_i < 0");
	} else {
	  for (end = read_i + len; read_i < end; read_i++, ref_i++) {

	    if (ref_i < 0 || ref_i >= reference_sequence.length) {
	      count_bases_with_boundary_issues++;
	      //	      System.err.println("ERROR: read " + sr.getReadName() + " out of reference bounds");  // debug
	      continue;
	    }

	    if (read_i >= quals.length) {
	      // no quality info / out of range
	      //		System.err.println("SAMStrSNP: no qual info available!");  // debug
	      qual_mapping_problem = true;
	    } else if (quals[read_i] >= MIN_QUALITY) {
	      //
	      // read quality at this position is good enough to track
	      //
	      c_read = Character.toUpperCase((char) read[read_i]);
	      c_ref = Character.toUpperCase((char) reference_sequence[ref_i]);

	      if (
		  c_ref == 'A' ||
		  c_ref == 'C' ||
		  c_ref == 'G' ||
		  c_ref == 'T'
		  ) {
		count_bases_compared++;
		if (c_read != c_ref) count_bases_mismatch++;
	      } else {
		count_ref_skipped++;
	      }

	    }
	  }
	}
      }

      //
      //  add indels to counts:
      //
      CigarOperator co;
      for (CigarElement ce : sr.getCigar().getCigarElements()) {
	co = ce.getOperator();
	if (co.equals(CigarOperator.INSERTION) ||
	    (co.equals(CigarOperator.DELETION))) {
	  //	  System.err.println("adding " + co + " of " + indel_length);  // debug
	  int indel_length = ce.getLength();
	  if (COUNT_INDELS_AS_MISMATCHES) {
	    count_bases_mismatch += indel_length;
	    // TO DO: evaluate quality of insertion bases
	    count_bases_indel += indel_length;
	  }
	}
      }
    }

    query.close();
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    try {
      File bam = null;
      ReferenceSequence refseq = null;
      int min_quality = 15;
      int min_mapq = 1;
      boolean count_indels_as_mismatches = true;

      for (int i=0; i < argv.length; i++) {
	if (argv[i].equals("-bam")) {
	  bam = new File(argv[++i]);
	} else if (argv[i].equals("-min-quality")) {
	  min_quality = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-min-mapq")) {
	  min_mapq = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-no-indel-mismatches")) {
	  count_indels_as_mismatches = false;
	} else if (argv[i].equals("-2bit")) {
	  refseq = new TwoBitFile(argv[++i]);
	} else if (argv[i].equals("-fasta")) {
	  String thing = argv[++i];
	  File f = new File(thing);
	  if (f.isFile()) {
	    // .fai-indexed FASTA file
	    refseq = new FASTAIndexedFAI(thing);
	  } else if (f.isDirectory()) {
	    // directory of individual sequences
	    refseq = new FASTADirectory(thing);
	  } else {
	    System.err.println("ERROR: not a file/directory: " + thing);  // debug
	  }
	}
      }

      if (bam == null || refseq == null) {
	System.err.println("specify -bam [file] and -fasta [file|directory]");  // debug
	System.exit(1);
      } else {
	SAMMismatchFrequency mf = new SAMMismatchFrequency();
	mf.set_reference(refseq);
	mf.set_minimum_mapq(min_mapq);
	mf.set_minimum_quality(min_quality);
	mf.set_count_indels_as_mismatches(count_indels_as_mismatches);
	mf.process_bam(bam);
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
      System.exit(1);
    }
  }

}
