package Ace2;

import net.sf.samtools.*;
import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.*;

public class SAMUtils {
  private final static boolean DEBUG_RESTRICT_LOAD = false;
  // private final static boolean DEBUG_RESTRICT_LOAD = false;

  public static SAMRecord[] load_sams(SAMFileReader inputSam) {

    //    (new Exception()).printStackTrace();
    Funk.Timer timer = new Funk.Timer("sam load");

    ArrayList<SAMRecord> sams = new ArrayList<SAMRecord>();
    int unmapped_reads=0;

    HashSet<String> wanted = new HashSet<String>();

    if (DEBUG_RESTRICT_LOAD) {
      System.err.println("DEBUG: only loading subset of SAM records!");  // debug
      wanted.add("302P7AAXX090507:5:13:860:1053#0");
      wanted.add("302P7AAXX090507:5:29:240:1721#0");
      // 2 sequences: one with native insert to consensus, another with perfect match
      // but which overlaps with new consensus insertion
    }

    for (SAMRecord samRecord : inputSam) {
      //      if (!samRecord.getReadName().equals("302P7AAXX090507:5:29:240:1721#0")) continue;
      // insert of 1

      if (DEBUG_RESTRICT_LOAD && !wanted.contains(samRecord.getReadName())) continue;

      //       if (!(
      // 	    samRecord.getReadName().equals("302P7AAXX090507:5:56:245:1093#0") ||
      // 	    //	    false
      // 	    //	    false || 
      // 	    samRecord.getReadName().equals("302P7AAXX090507:5:18:1442:209#0")
      // 	    )) continue;
      // 2 neighboring gaps

      if (samRecord.getReadUnmappedFlag() == true) {
	unmapped_reads++;
	continue;
      }

      // DEBUG

      sams.add(samRecord);
    }
    inputSam.close();

    timer.finish();
    System.err.println("loaded " + sams.size() + " reads");  // debug

    if (unmapped_reads > 0) System.err.println("unmapped reads: " + unmapped_reads);  // debug
    //    System.err.println("loaded reads: " + sams.size());  // debug

    
    SAMRecord[] sa = new SAMRecord[sams.size()];
    int i = 0;
    for (SAMRecord sr : sams) {
      sa[i++] = sr;
    }
    return sa;
  }

  public static SAMRecord[] load_sams(File f) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU
    SAMFileReader inputSam = new SAMFileReader(f);
    return load_sams(inputSam);
  }

  public static SAMRecord[] load_sams(URL url) throws IOException {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU
    System.err.println("SAM load via URL " + url);  // debug
    SAMFileReader inputSam = new SAMFileReader(url.openStream());
    return load_sams(inputSam);
  }

  public static String cigar_to_string (Cigar c) {
    StringBuffer sb = new StringBuffer();
    for (CigarElement ce : c.getCigarElements()) {
      CigarOperator co = ce.getOperator();
      sb.append(co);
      sb.append(ce.getLength());
    }
    return sb.toString();
  }

  public static void write_fastq(PrintStream ps, SAMRecord sr, String info) {
    byte[] bases = sr.getReadBases();
    String qual = sr.getBaseQualityString();

    String error = null;
    if (bases.length == 0) {
      error = "0-length bases";
    } else if (qual.length() == 0) {
      error = "0-length quality";
    } else if (bases.length != qual.length()) {
      error = "sequence/quality length mismatch";
    }

    if (error != null) {
      System.err.println("ERROR: not writing FASTQ, " + error + " for " + sr.getReadName());
    } else {
      ps.println("@" + info);
      ps.println(new String(bases));
      ps.println("+");
      ps.println(qual);
    }
  }

  public static void write_fastq(PrintStream ps, SAMRecord sr, String info, ArrayList<String> tags) {
    write_fastq(ps, sr, info + " " + Funk.Str.join(",", tags));
  }
  
  public static String bucket_reference_name (String ref_name, boolean bucket_non_chr) {
    if (ref_name == null || ref_name.length() == 0) {
      ref_name = "unknown";
    } else if (ref_name.indexOf("chr") == 0) {
      // strip leading "chr" if present
      ref_name = ref_name.substring(3);
    }
    if (bucket_non_chr) {
      // ref_name = ref_name.length() <= 2 ? ref_name : "other";
      Chromosome c = Chromosome.valueOfString(ref_name);
      if (c == null) {
	//	System.err.println("bogus reference seq name " + ref_name);  // debug
	ref_name = "other";
      }
    }
    return ref_name;
  }

  public static boolean flanking_quality_check(byte[] quals, int read_i, int site_size, int min_qual, int window_size, boolean illumina_q2_mode) {
    // site_size --
    //    deletion: 0
    //         SNP: 1
    //   insertion: >= 1
    int start, end, i;
    boolean result = true;

    if (site_size == 0) {
      //
      // deletion: flanking sequence is just 1 range
      //
      start = read_i - window_size;
      end = read_i + site_size + window_size;

      //      if (true) {
	// original approach: if not enough nt, just use those available.
	// 3/2010: ...aaaaaand we're back to this approach.  New mismap filter should help with 
	// false positives where "banding" is shown in other reads.
      if (start < 0) start = 0;
      if (end > quals.length) end = quals.length;
	//      } else {
	//	if (start < 0) return false;
	//	if (end > quals.length) return false;
	// new approach: REQUIRE full window coverage.
	// - positive: reduces false positives from junky end-of-read alignments
	// - negative: discards SNP evidence near end of reads!
	//   example: chr14.93116306, 257_chr14_93116176_93116437.markup
	//      }

      for (i = start; i < end; i++) {
	//      System.err.println("  q="+quals[i]);  // debug
	if (quals[i] < min_qual && (illumina_q2_mode ? quals[i] != 2 : true)) {
	  result = false;
	  break;
	}
      }
    } else {
      //
      // insertion or SNP, scan before/after sequence separately
      //

      //      
      // flanking sequence before site:
      //
      start = read_i - window_size;
      end = read_i;
      //      System.err.println("site:" + read_i + " size=" + site_size + " before start="+start + " end="+end);  // debug

      if (start < 0) start = 0;
      if (end > quals.length) end = quals.length;
      for (i = start; i < end; i++) {
	//      System.err.println("  q="+quals[i]);  // debug
	//	if (quals[i] < min_qual) {
	if (quals[i] < min_qual && (illumina_q2_mode ? quals[i] != 2 : true)) {
	  result = false;
	  break;
	}
      }

      //      
      // flanking sequence after site:
      //
      start = read_i + site_size;
      end = start + window_size;
      //      System.err.println("               after start="+start + " end="+end);  // debug
      if (start < 0) start = 0;
      if (end > quals.length) end = quals.length;
      for (i = start; i < end; i++) {
	//      System.err.println("  q="+quals[i]);  // debug
	//	if (quals[i] < min_qual) {
	if (quals[i] < min_qual && (illumina_q2_mode ? quals[i] != 2 : true)) {
	  result = false;
	  break;
	}
      }
    }

    return result;
  }

  public static Exception sam_config_setup (AceViewerConfig avc, SAMRegion region) throws IOException,FileNotFoundException {

    Exception exception = null;

    //
    // prepare AceViewerConfig to load a particular SAMRegion
    //
    //    System.err.println("config setup: range="+region);  // debug
    if (region.isValid()) {
      //
      //  startup region specified
      //
      ArrayList<String> files = new ArrayList<String>();
      for (SAMResource sre : avc.sams) {
	sre.set_region(region);
	if (sre.file != null) files.add(sre.file.getName());
      }
      String title = "";
      if (files.size() > 0) title = Funk.Str.join(", ", files);
      if (title.length() > 0) title = title + ": ";
      title = title + region.toString();
      avc.title = title;

      avc.ruler_start = region.range.start - 1;
      // set/override ruler start position
	    
      //
      // get reference sequence chunk:
      //
      Chromosome c = Chromosome.valueOfString(region.tname);

      if (c == null) {
	//	System.err.println("NOTE: non-chromosome-format reference sequence: " + region.tname);
	avc.CONSENSUS_TAG = region.tname;
      } else {
	avc.CONSENSUS_TAG = c.toString();
      }

      if (avc.GENERATE_CONSENSUS) {
	avc.CONSENSUS_TAG = avc.CONSENSUS_TAG.concat("_SAM_consensus");
	//	System.err.println("hey now: generating cons");  // debug
	SAMConsensusGenerator scg = new SAMConsensusGenerator(avc);
	avc.target_sequence = scg.generate_consensus();
      } else if (avc.reference_sequence != null) {
	System.err.println("loading reference from " + avc.reference_sequence);  // debug
	avc.target_sequence = avc.reference_sequence.get_region(region.tname,
								region.range.start,
								region.get_length());
	if (avc.target_sequence == null) {
	  JOptionPane.showMessageDialog(null,
					"Can't retrieve reference sequence for \"" + region.tname + "\"; please check reference and/or BAM data.  Using generated reference sequence.",
					"Error",
					JOptionPane.ERROR_MESSAGE);
	  SAMConsensusGenerator scg = new SAMConsensusGenerator(avc);
	  avc.target_sequence = scg.generate_consensus();
	}
	//	  System.err.println("target seq = " + avc.target_sequence);  // debug
      } else if (avc.target_sequence == null) {
	System.err.println("ERROR: no ReferenceSequence accessor available");  // debug
      }
      
    }
    
    return exception;
  }

  public static String get_printable_read_name(SAMRecord sr) {
    return sr.getReadName() + "." + (sr.getReadNegativeStrandFlag() ? "R" : "F");
  }

  public static String get_standardized_refname (String s) {
    if (s.toLowerCase().indexOf("chr") == 0) {
      return(s.substring(3));
    } else {
      return s;
    }
  }

  public static ArrayList<String> get_refname_alternates (String s) {
    ArrayList<String> results = new ArrayList<String>();
    HashSet<String> saw = new HashSet<String>();
    conditional_add(results, saw, s);
    // unique ordered list, always returning given string first

    String std = get_standardized_refname(s);
    conditional_add(results, saw, std);
    conditional_add(results, saw, "chr" + std);

    if (std.toUpperCase().equals("M")) {
      conditional_add(results, saw, "MT");
      conditional_add(results, saw, "chrMT");
    } else if (std.toUpperCase().equals("MT")) {
      conditional_add(results, saw, "M");
      conditional_add(results, saw, "chrM");
    }
    
    //    System.err.println("input="+s + " out=" + results);  // debug

    return results;
  }

  public static void main (String[] argv) {
    for (String s : get_refname_alternates("MT")) {
      System.err.println("alt="+s);  // debug
    }
  }


  private static void conditional_add (ArrayList<String> list, HashSet<String> saw, String s) {
    if (!saw.contains(s)) {
      list.add(s);
      saw.add(s);
    }
  }

  public static boolean is_bam_indexed (File f) {
    SAMFileReader sfr = new SAMFileReader(f);
    return sfr.hasIndex();
  }

  public static void bam_index(File bam) {
    SAMFileReader sfr = new SAMFileReader(bam);
    sfr.enableFileSource(true);

    BAMIndexer ix = new BAMIndexer(new File(bam.getName() + ".bai"),
				   sfr.getFileHeader());
    for (SAMRecord sr : sfr) {
      ix.processAlignment(sr);
    }
    ix.finish();
  }
    
}
