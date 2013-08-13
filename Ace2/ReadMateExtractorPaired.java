package Ace2;

import java.io.*;
import java.util.*;
import net.sf.samtools.*;
import java.util.regex.*;
import java.text.ParseException;

// rpf 2013  import IsoView.RegionParser;

public class ReadMateExtractorPaired {
  // query one or more intervals, writing pairs of reads in order
  // - at least one read of the pair must be mapped
  // - include mapped mates *even if outside query region*
  private File bam;
  private ArrayList<RegionParser> intervals;
  private int FLUSH_CHECK_INTERVAL = 10000;
  private HashMap<String,SAMRecord> mapped_f, mapped_r, unmapped;
  boolean VERBOSE = false;

  private PrintStream ps_fwd, ps_rev;
  // hack

  public ReadMateExtractorPaired() {
    bam = null;
    intervals = new ArrayList<RegionParser>();
    hash_reset();
  }

  private void hash_reset() {
    mapped_f = new HashMap<String,SAMRecord>();
    mapped_r = new HashMap<String,SAMRecord>();
    unmapped = new HashMap<String,SAMRecord>();
  }

  public void set_bam (File bam) {
    this.bam = bam;
  }

  public void add_interval (String interval) throws ParseException {
    RegionParser rp = new RegionParser();
    if (rp.parse(interval)) {
      intervals.add(rp);
    } else {
      throw new ParseException("interval not parsable: " + interval, 0);
    }
  }

  public void extract() throws Exception {
    if (bam == null) throw new Exception("specify bam file");
    SAMFileReader sfr = new SAMFileReader(bam);

    if (intervals.size() == 0) throw new Exception("specify interval(s)");

    String basename = bam.getName();

    WorkingFile wf_fwd = new WorkingFile(basename + ".fwd.fastq");
    WorkingFile wf_rev = new WorkingFile(basename + ".rev.fastq");
    ps_fwd = wf_fwd.getPrintStream();
    ps_rev = wf_rev.getPrintStream();
    // fix me: subclasses for different output types

    ChromosomeDisambiguator cd = new ChromosomeDisambiguator(sfr);

    SAMSequenceDictionary dict = sfr.getFileHeader().getSequenceDictionary();

    for (RegionParser interval : intervals) {
      hash_reset();

      String ref_name_bam = cd.find(interval.reference);
      // translate user-specified name into BAM reference name, if necessary
      SAMRecordIterator query = sfr.queryOverlapping(ref_name_bam,
						     interval.start,
						     interval.end);
      int reads_read = 0;

      while (query.hasNext()) {
	SAMRecord sr = query.next();
	if (sr.getReadUnmappedFlag()) {
	  // stash unmapped
	  unmapped.put(sr.getReadName(), sr);
	} else {
	  // stash mapped
	  if (sr.getReadPairedFlag()) {
	    if (sr.getFirstOfPairFlag()) {
	      mapped_f.put(sr.getReadName(), sr);
	    } else {
	      mapped_r.put(sr.getReadName(), sr);
	    }
	  } else {
	    throw new IOException("paired reads required (can't handle duplicates)");
	    // if unpaired, might have duplicate entries, e.g. bwasw
	  }
	}
	if (++reads_read % FLUSH_CHECK_INTERVAL == 0) flush_check();
      }
      query.close();

      flush_check();
      // finish dumping pairs

      mate_dump(sfr, mapped_f, true);
      mate_dump(sfr, mapped_r, false);
      if (unmapped.size() > 0) System.err.println("WTF: leftover unmapped");  // debug
    }

    wf_fwd.finish();
    wf_rev.finish();
  }

  private void mate_dump(SAMFileReader sfr,
			 HashMap<String,SAMRecord> bucket,
			 boolean is_fwd) {
    for (SAMRecord sr : bucket.values()) {
      SAMRecord sr_f = null;
      SAMRecord sr_r = null;
      if (is_fwd) {
	sr_f = sr;
      } else {
	sr_r = sr;
      }

      SAMRecord mate = sfr.queryMate(sr);
      if (mate == null) {
	System.err.println("gurgle: can't find mate for " + sr.getReadName());  // debug
      } else {
	if (VERBOSE) System.err.println("found mate for " + sr.getReadName());  // debug
	if (is_fwd) {
	  sr_r = mate;
	} else {
	  sr_f = mate;
	}
	if (sr_f != null && sr_r != null) {
	  write_pair(sr_f, sr_r);
	} else {
	  System.err.println("gurgle: mate query doesn't have F + R");  // debug
	}
      }
    }
  }

  private void flush_check() {
    // write out:
    // - mapped reads with mapped mates in region
    // - mapped reads an unmapped mate
    HashSet<String> all_ids = new HashSet<String>();
    all_ids.addAll(mapped_f.keySet());
    all_ids.addAll(mapped_r.keySet());
    all_ids.addAll(unmapped.keySet());

    boolean have_f, have_r, have_unmapped;
    ArrayList<String> prunable = new ArrayList<String>();
    for (String id : all_ids) {
      have_f = mapped_f.containsKey(id);
      have_r = mapped_r.containsKey(id);
      have_unmapped = unmapped.containsKey(id);

      SAMRecord sr_f = null;
      SAMRecord sr_r = null;

      if (have_f && have_r && have_unmapped) {
	System.err.println("that's unpossible!");  // debug
      } else if (have_f && have_r) {
	sr_f = mapped_f.get(id);
	sr_r = mapped_r.get(id);
	//	System.err.println("case 1: " + id);  // debug
      } else if (have_f && have_unmapped) {
	sr_f = mapped_f.get(id);
	sr_r = unmapped.get(id);
	//	System.err.println("case 2: " + id);  // debug
      } else if (have_r && have_unmapped) {
	sr_f = unmapped.get(id);
	sr_r = mapped_r.get(id);
	//	System.err.println("case 3: " + id);  // debug
      } else {
	// - haven't encountered mate yet
	// - mate is out of query range
	//	System.err.println("case 4: " + id);  // debug
      }

      if (sr_f != null && sr_r != null) {
	write_pair(sr_f, sr_r);
	prunable.add(id);
      }
    }

    for (String id : prunable) {
      //      System.err.println("pruning " + id);  // debug
      mapped_f.remove(id);
      mapped_r.remove(id);
      unmapped.remove(id);
    }
  }

  private void add_basic_tags (SAMRecord sr, ArrayList<String> tags) {
    tags.add(sr.getReadName());

    if (sr.getReadUnmappedFlag()) {
      tags.add("unmapped");
    } else {
      tags.add("mapped");
      tags.add(sr.getReferenceName() + ":" + Integer.toString(sr.getAlignmentStart()) + "-" + Integer.toString(sr.getAlignmentEnd()));
    }
  }

  private void write_pair (SAMRecord sr_f, SAMRecord sr_r) {
    ArrayList<String> tags_f = new ArrayList<String>();
    ArrayList<String> tags_r = new ArrayList<String>();

    add_basic_tags(sr_f, tags_f);
    add_basic_tags(sr_r, tags_r);

    rc_fix(sr_f, tags_f);
    rc_fix(sr_r, tags_r);

    SAMUtils.write_fastq(ps_fwd, sr_f, Funk.Str.join(" ", tags_f));
    SAMUtils.write_fastq(ps_rev, sr_r, Funk.Str.join(" ", tags_r));
  }

  private void rc_fix (SAMRecord sr, ArrayList<String> tags) {
    if (!sr.getReadUnmappedFlag() && sr.getReadNegativeStrandFlag()) {
      // BAM alignment transforms all reads into reference space.
      // Undo the reverse-complementation applied to minus-mapped reads.
      String bases_rc = new String(sr.getReadBases());
      String fixed = Funk.Str.reverse_complement(bases_rc);
      char[] fixed_c = fixed.toCharArray();
      byte[] fixed_b = new byte[fixed_c.length];
      for (int i = 0; i < fixed_c.length; i++) {
	fixed_b[i] = (byte) fixed_c[i];
      }
      sr.setReadBases(fixed_b);

      // since we reverse-complemented the sequence,
      // reverse the base quality scores:
      StringBuilder sb = new StringBuilder(sr.getBaseQualityString());
      sr.setBaseQualityString(sb.reverse().toString());
      tags.add("rc_fixed");
    }
  }

  public static void main (String[] argv) {
    try {
      SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
      // STFU

      ReadMateExtractorPaired rmep = new ReadMateExtractorPaired();
      for (int i = 0; i < argv.length; i++) {
	if (argv[i].equals("-bam")) {
	  rmep.set_bam(new File(argv[++i]));
	} else if (argv[i].equals("-interval")) {
	  i++;
	  while (i < argv.length && argv[i].indexOf("-") != 0) {
	    rmep.add_interval(argv[i++]);
	  }
	  i--;
	} else if (argv[i].equals("-v")) {
	  rmep.VERBOSE = true;
	} else {
	  System.err.println("unknown arg " + argv[i]);  // debug
	  System.exit(1);
	}
      }
      rmep.extract();
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
      usage();
      System.exit(1);
    }
  }

  public static void usage() {
    System.err.println("usage:");  // debug
    System.err.println("  -bam whatever.bam");  // debug
    System.err.println("  -interval chrX:1234-5678 [-interval ...] ");  // debug
  }

}
