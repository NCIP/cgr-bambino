package Ace2;

import java.io.*;
import java.util.*;

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;

public class SAMReport {
  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SAMResource sres = null;
    int max = 0;

    try {
      CloseableIterator<SAMRecord> iterator = null;

      boolean find_small_alignments = false;
      int fsa_min_size = 0;
      boolean find_non_primary = false;
      boolean scan_mapq = false;
      boolean count_n_tag = false;
      boolean get_run_date = false;
      boolean get_sample_names = false;
      boolean get_assembly = false;
      boolean detect_rna = false;
      boolean find_name = false;

      boolean find_min_aligned = false;
      String outfile = null;
      int max_reads = 0;
      int min_match_length = 0;
      int max_mismatches = 0;
      ReferenceSequence rs = null;
      String bam_file = null;
      boolean mask_matching_sequence = false;
      String search_name = null;

      for (int i=0; i < argv.length; i++) {
	if (argv[i].equals("-bam")) {
	  bam_file = argv[++i];
	  sres = new SAMResource();
	  sres.import_data(SAMResourceTags.SAM_URL, bam_file);
	  sres.detect_sample_id();
	  iterator = sres.get_iterator();
	} else if (argv[i].equals("-2bit")) {
	  rs = new TwoBitFile(argv[++i]);
	} else if (argv[i].equals("-fasta")) {
	  rs = new FASTAIndexedFAI(argv[++i]);
	} else if (argv[i].equals("-find-min-aligned")) {
	  find_min_aligned = true;
	  min_match_length = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-mask")) {
	  mask_matching_sequence = true;
	} else if (argv[i].equals("-max-mismatches")) {
	  max_mismatches = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-find-small-alignments")) {
	  find_small_alignments = true;
	  fsa_min_size = Integer.parseInt(argv[++i]);
	  Integer min_size = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-find-non-primary")) {
	  find_non_primary = true;
	} else if (argv[i].equals("-scan-mapq")) {
	  scan_mapq = true;
	} else if (argv[i].equals("-of") || argv[i].equals("-outfile")) {
	  outfile = argv[++i];
	} else if (argv[i].equals("-count-N-tag")) {
	  count_n_tag = true;
	} else if (argv[i].equals("-get-run-date")) {
	  get_run_date = true;
	} else if (argv[i].equals("-get-assembly")) {
	  get_assembly = true;
	} else if (argv[i].equals("-get-sample-names")) {
	  get_sample_names = true;
	} else if (argv[i].equals("-detect-rna")) {
	  detect_rna = true;
	  max_reads = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-find-read-name")) {
	  find_name = true;
	  search_name = argv[++i];
	} else {
	  System.err.println("ERROR: unknown parameter " + argv[i]);  // debug
	  System.exit(1);
	}
      }

      if (iterator == null) {
	System.err.println("specify -bam");  // debug
      } else if (find_small_alignments) {
	find_small_blocks(iterator, fsa_min_size);
      } else if (find_min_aligned) {
	if (rs == null) {
	  System.err.println("ERROR: specify reference sequence (-2bit / -fasta)");  // debug
	} else if (outfile == null) {
	  System.err.println("ERROR: specify -outfile");  // debug
	} else {
	  find_min_aligned(new File(bam_file), rs, min_match_length, max_mismatches, new File(outfile), mask_matching_sequence);
	}
      } else if (find_non_primary) {
	find_non_primary(iterator);
      } else if (scan_mapq) {
	scan_mapq(iterator, sres.get_file(), outfile);
      } else if (count_n_tag) {
	count_n_tag(iterator);
      } else if (get_run_date) {
	get_run_date(sres.getSAMFileReader());
      } else if (get_sample_names) {
	get_sample_names(sres.getSAMFileReader());
      } else if (get_assembly) {
	get_assembly(sres.getSAMFileReader());
      } else if (detect_rna) {
	detect_rna(iterator, max_reads);
      } else if (find_name) {
	find_read_name(iterator, search_name);
      } else {
	System.err.println("option??");  // debug
      }


    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public static void count_n_tag (CloseableIterator<SAMRecord> iterator) {
    // raw count of reads having an N (region skip) tag
    CigarOperator co;
    long count = 0;
    while (iterator.hasNext()) {
      SAMRecord sr = iterator.next();
      for (CigarElement ce : sr.getCigar().getCigarElements()) {
	co = ce.getOperator();
	if (co.equals(CigarOperator.SKIPPED_REGION)) {
	  count++;
	  break;
	}
      }
    }
    System.out.println(count);
    
  }

  public static void scan_mapq_OLD (CloseableIterator<SAMRecord> iterator, File file) {
    LongCounter lc = new LongCounter(255);
    while (iterator.hasNext()) {
      SAMRecord sr = iterator.next();
      if (sr.getReadUnmappedFlag() ||
	  sr.getDuplicateReadFlag() ||
	  sr.getNotPrimaryAlignmentFlag()) continue;
      lc.increment(sr.getMappingQuality());
    }
    
    try {

      Reporter rpt = new Reporter();
      rpt.add_header("filename");
      rpt.add_header("median");
      for (int percentile = 0; percentile <= 100; percentile += 10) {
	rpt.add_header(Integer.toString(percentile));
      }

      rpt.set_value("filename", file.getAbsolutePath());
      rpt.set_value("median", Integer.toString(lc.get_median()));
    

      for (int percentile = 0; percentile <= 100; percentile += 10) {
	rpt.set_value(Integer.toString(percentile), Integer.toString(lc.get_fraction((double) percentile / 100)));
      }
      rpt.end_row();
      rpt.close();
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }

  }

  public static void scan_mapq (CloseableIterator<SAMRecord> iterator, File file, String outfile) {
    int MAX_VALUE = 255;
    LongCounter lc = new LongCounter(MAX_VALUE);
    while (iterator.hasNext()) {
      SAMRecord sr = iterator.next();
      if (sr.getReadUnmappedFlag() ||
	  sr.getDuplicateReadFlag() ||
	  sr.getNotPrimaryAlignmentFlag()) continue;
      lc.increment(sr.getMappingQuality());
    }
    
    try {
      Reporter rpt = new Reporter();
      if (outfile != null) rpt.set_output_filename(outfile);
      rpt.add_header("mapq");
      rpt.add_header("count");

      long[] tracker = lc.get_array();
      for (int i = 0; i <= MAX_VALUE; i++) {
	rpt.set_value("mapq", Integer.toString(i));
	rpt.set_value("count", Long.toString(tracker[i]));
	rpt.end_row();
      }
      rpt.close();
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }

  }

  public static void find_non_primary (CloseableIterator<SAMRecord> iterator) {
    while (iterator.hasNext()) {
      SAMRecord sr = iterator.next();
      if (sr.getNotPrimaryAlignmentFlag()) {
	System.err.println("non-primary " + SAMUtils.get_printable_read_name(sr) + " at " + sr.getAlignmentStart());  // debug
      }
    }
  }

  public static void find_min_aligned(File bam_in,
				      ReferenceSequence rs,
				      int min_match_length,
				      int max_mismatches,
				      File bam_out,
				      boolean mask
				      ) throws IOException {
    // find alignments having:
    //   - a minimum overlap w/reference
    //   - a maximum number of mismatches
    // mismatches are found manually vs. reference sequence (not tag-dependent)

    SAMFileReader sfr = new SAMFileReader(bam_in);
    BAMFileWriter bfw = new BAMFileWriter(bam_out);
    SAMFileHeader header_in = sfr.getFileHeader();
    bfw.setSortOrder(SAMFileHeader.SortOrder.valueOf("unsorted"), false);
    bfw.setHeader(header_in);

    System.err.println("minimum alignment length: " + min_match_length);  // debug
    System.err.println("      maximum mismatches: " + max_mismatches);

    int last_ri = -1;
    int ri;
    byte[] ref = null;

    int count_usable = 0;
    int count_rejected_length = 0;
    int count_rejected_mismatches = 0;
    int count_unmapped = 0;

    for (SAMRecord sr : sfr) {
      if (sr.getReadUnmappedFlag()) {
	count_unmapped++;
	continue;
      }

      ri = sr.getReferenceIndex();
      if (ri != last_ri) {
	String rn = sr.getReferenceName();
	System.err.println("reading reference sequence: " + rn);
	ref = rs.get_all(rn);
	System.err.println("refence sequence length="+ref.length);  // debug
	last_ri = ri;
      }

      int aligned = 0;
      int mismatches = 0;
      int blen;
      int ref_i, read_i, end;
      byte[] read_bases = sr.getReadBases();
      if (mask) {
	byte[] clone = new byte[read_bases.length];
	System.arraycopy(read_bases, 0, clone, 0, read_bases.length);
	read_bases = clone;
      }

      char ref_base, read_base;
      for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	blen = ab.getLength();
	aligned += blen;
	for (read_i = ab.getReadStart() - 1,
	       ref_i = ab.getReferenceStart() - 1,
	       end = read_i + blen;
	     read_i < end && ref_i < ref.length;
	     read_i++, ref_i++) {
	  ref_base = Character.toUpperCase((char) ref[ref_i]);
	  read_base = Character.toUpperCase((char) read_bases[read_i]);
	  if (ref_base != read_base) mismatches++;
	  if (mask) read_bases[read_i] = (byte) 'N';
	}
      }

      if (aligned < min_match_length) {
	count_rejected_length++;
      } else if (mismatches > max_mismatches) {
	count_rejected_mismatches++;
      } else {
	// usable
	if (mask) sr.setReadBases(read_bases);
	bfw.addAlignment(sr);
	//	System.err.println("ok " + mismatches + " " + sr.getReadName());  // debug
	count_usable++;
      }
    }

    bfw.close();

    System.err.println("usable:" + count_usable + " rejected_mismatches:" + count_rejected_mismatches + " rejected_length:" + count_rejected_length + " unmapped:" + count_unmapped);  // debug

  }

  public static void find_small_blocks(CloseableIterator<SAMRecord> iterator, int min_size) {
    // find alignments where sum of AlignmentBlocks lengths <= min_size
    while (iterator.hasNext()) {
      SAMRecord sr = iterator.next();
      if (sr.getReadUnmappedFlag()) continue;
      int aligned = 0;
      for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	aligned += ab.getLength();
      }
      if (aligned <= min_size) {
	ArrayList<String> stuff = new ArrayList<String>();
	stuff.add(Integer.toString(aligned));
	stuff.add(SAMUtils.get_printable_read_name(sr));
	stuff.add(sr.getReferenceName());
	stuff.add(Integer.toString(sr.getAlignmentStart()));
	System.err.println(Funk.Str.join(",", stuff));  // debug
      }
    }
  }

  public static void get_run_date (SAMFileReader sfr) {
    // return latest date entry mentioned in SAM file header (i.e. read groups)
    SAMFileHeader sfh = sfr.getFileHeader();
    Date run_date = null;
    for (SAMReadGroupRecord srgr : sfh.getReadGroups()) {
      Date date = srgr.getRunDate();
      if (date != null && (run_date == null || date.after(run_date))) {
	run_date = date;
      }
    }
    String result = run_date == null ? "0" : run_date.toString();
    System.out.println(result);  // debug
    // parseable by perl Date::Manip
  }

  public static void get_sample_names (SAMFileReader sfr) {
    // return sample names encoded in the read groups
    SAMFileHeader sfh = sfr.getFileHeader();
    Date run_date = null;
    HashSet<String> saw = new HashSet<String>();

    for (SAMReadGroupRecord srgr : sfh.getReadGroups()) {
      String sample = srgr.getSample();
      if (sample != null && !saw.contains(sample)) {
	System.out.println(sample);
	saw.add(sample);
      }
    }
  }

  public static void get_assembly (SAMFileReader sfr) {
    // return sample names encoded in the read groups
    SAMFileHeader sfh = sfr.getFileHeader();
    SAMSequenceDictionary ssd = sfh.getSequenceDictionary();
    HashSet<String> saw_as = new HashSet<String>();
    HashMap<Chromosome,Integer> lengths = new HashMap<Chromosome,Integer>();
    for (SAMSequenceRecord ssr : ssd.getSequences()) {
      String seq_name = ssr.getSequenceName();
      int seq_len = ssr.getSequenceLength();
      String seq_asm = ssr.getAssembly();

      Chromosome c = Chromosome.valueOfString(seq_name);
      if (c != null) {
	//	System.err.println(seq_name + " => " + c);  // debug
	lengths.put(c, seq_len);
	if (seq_asm != null) {
	  saw_as.add(seq_asm);
	}
	//	System.err.println("hey now " + seq_name + " " + seq_len);  // debug
      }
    }

    KnownGenomeBuild kgb = new KnownGenomeBuild();
    String genome_name = kgb.identify_build(lengths);
    if (genome_name == null) genome_name = "unknown";

    System.out.println("AS," + Funk.Str.join(",", saw_as));
    System.out.println("meta," + genome_name);

  }

  public static void NEW_CALL_STUB (CloseableIterator<SAMRecord> iterator) {
    while (iterator.hasNext()) {
      SAMRecord sr = iterator.next();
    }
  }

  public static void detect_rna (CloseableIterator<SAMRecord> iterator, int max_reads) {
    //
    // guess whether file is RNA by scanning for N tags
    //
    int read_count = 0;
    SAMRecord sr;
    Cigar c;
    boolean is_rna = false;
    CigarOperator co;
    while (iterator.hasNext()) {
      if (max_reads > 0 && read_count++ >= max_reads) break;
      sr = iterator.next();
      c = sr.getCigar();
      for (CigarElement ce : c.getCigarElements()) {
	if (ce.getOperator().equals(CigarOperator.SKIPPED_REGION)) {
	  is_rna = true;
	  break;
	}
      }
      if (is_rna) break;
    }
    System.out.println(is_rna ? "1" : "0");  // debug
  }

  public static void find_read_name (CloseableIterator<SAMRecord> iterator, String id) {
    SAMRecord sr;
    boolean found = false;
    System.err.println("looking for: " + id);  // debug
    while (iterator.hasNext()) {
      sr = iterator.next();
      if (sr.getReadName().equals(id)) {
	found = true;
	System.out.println(sr.getReadName() + " unmapped:" + sr.getReadUnmappedFlag() + " as:" + sr.getAlignmentStart() + " strand:" + (sr.getReadNegativeStrandFlag() ? "-" : "+"));  // debug
      }
    }
    System.err.println("found="+found);  // debug

    
  }
 

}