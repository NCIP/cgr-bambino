package Ace2;

import java.io.*;
import java.util.*;
import net.sf.samtools.*;
import java.util.regex.*;
import java.text.ParseException;

import IsoView.RegionParser;

public class ReadMateExtractorUnpaired {
  // query one or more intervals, writing pairs of reads in order
  // => underway

  private File unpaired_bam, paired_bam;
  private ArrayList<RegionParser> intervals;
  private HashSet<String> wanted_ids;
  boolean VERBOSE = false;
  boolean STRIP_SEU_SUFFIX = true;
  private Pattern seu_pattern;
  private PrintStream ps_fwd, ps_rev;
  // hack
  boolean QUERY_ALL = false;
  boolean QUERY_ALL_MAPPED = false;
  int READ_REPORT_INTERVAL = 1000000;
  boolean PAIRED_QUERY_ALL = false;

  public ReadMateExtractorUnpaired() {
    unpaired_bam = paired_bam = null;
    intervals = new ArrayList<RegionParser>();
    wanted_ids = new HashSet<String>();
    seu_pattern = Pattern.compile("\\.[FR]\\d+");
  }

  public void set_unpaired_bam (File bam) {
    this.unpaired_bam = bam;
  }

  public void set_paired_bam (File bam) {
    this.paired_bam = bam;
  }

  public void add_interval (String interval) throws ParseException {
    RegionParser rp = new RegionParser();
    if (rp.parse(interval)) {
      intervals.add(rp);
    } else {
      throw new ParseException("interval not parsable: " + interval, 0);
    }
  }

  public boolean extract() throws Exception {
    if (unpaired_bam == null) throw new Exception("specify unpaired bam file (e.g. bwasw)");
    if (paired_bam == null) throw new Exception("specify paired bam file (e.g. bwa paired)");
    SAMFileReader sfr = new SAMFileReader(unpaired_bam);

    if (QUERY_ALL) {
      intervals = new ArrayList<RegionParser>();
      intervals.add(new RegionParser());
      // ignore any specified and create dummy entry.
      // SLOW but does not require that a BAM be sorted/indexed.
    } else if (QUERY_ALL_MAPPED) {
      intervals = new ArrayList<RegionParser>();
      SAMFileHeader sfh = sfr.getFileHeader();
      
      SAMSequenceDictionary dict = sfh.getSequenceDictionary();
      for (SAMSequenceRecord ssr : dict.getSequences()) {
	RegionParser rp = new RegionParser();
	rp.reference = ssr.getSequenceName();
	rp.start = 1;
	rp.end = ssr.getSequenceLength();
	intervals.add(rp);
	System.err.println("will query " + rp);  // debug
      }
    }

    if (intervals.size() == 0) throw new Exception("specify interval(s)");

    System.err.println("stripping SEU suffix?: " + STRIP_SEU_SUFFIX);  // debug

    ChromosomeDisambiguator cd = new ChromosomeDisambiguator(sfr);

    //
    // step 1:
    // find all mapped read IDs in the BAM, stripping bogus 
    // SAMExtractUnmapped suffix.
    //
    for (RegionParser interval : intervals) {
      SAMRecordIterator query;
      if (QUERY_ALL) {
	// hack for single-query mode
	query = sfr.iterator();
      } else {
	String ref_name_bam = cd.find(interval.reference);
	// translate user-specified name into BAM reference name, if necessary
	query = sfr.queryOverlapping(ref_name_bam,
				     interval.start,
				     interval.end);
      }

      int read = 0;
      while (query.hasNext()) {
	if (++read % READ_REPORT_INTERVAL == 0) {
	  System.err.println("read:" + read + " wanted=" + wanted_ids.size());
	}

	SAMRecord sr = query.next();
	if (sr.getReadUnmappedFlag()) continue;
	// only mapped reads

	if (sr.getReadPairedFlag())
	  throw new IOException("unpaired reads required");

	seu_id_strip_check(sr);
	wanted_ids.add(sr.getReadName());
      }
      query.close();
    }

    int count = wanted_ids.size();
    if (count == 0) throw new IOException("no mapped reads found, quitting");
    System.err.println("target IDs: " + count);

    //
    // step 2:
    // scan the associated/original paired BAM 
    // (i.e. the BAM SAMExtractUnmapped was run on),
    // exporting the reads of interest in paired FASTQ.
    //
    String basename = unpaired_bam.getName();
    //    System.err.println("basename="+basename);  // debug
    PairedReadWriter prw = new PairedReadWriter(basename, wanted_ids);

    sfr = new SAMFileReader(paired_bam);
    SAMRecordIterator query;
    if (PAIRED_QUERY_ALL) {
      System.err.println("paired BAM: querying all");  // debug
      query = sfr.iterator();
    } else {
      System.err.println("paired BAM: querying unmapped only");  // debug
      query = sfr.queryUnmapped();
    }
    prw.scan(query);
    return prw.close();
  }

  public static void main (String[] argv) {
    try {
      ReadMateExtractorUnpaired rmeu = new ReadMateExtractorUnpaired();
      for (int i = 0; i < argv.length; i++) {
	if (argv[i].equals("-unpaired-bam")) {
	  rmeu.set_unpaired_bam(new File(argv[++i]));
	} else if (argv[i].equals("-paired-bam")) {
	  rmeu.set_paired_bam(new File(argv[++i]));
	} else if (argv[i].equals("-interval")) {
	  i++;
	  while (i < argv.length && argv[i].indexOf("-") != 0) {
	    rmeu.add_interval(argv[i++]);
	  }
	  i--;
	} else if (argv[i].equals("-all-paired")) {
	  // query all the reads in the paired BAM (default: unmapped only)
	  rmeu.PAIRED_QUERY_ALL = true;
	} else if (argv[i].equals("-all-mapped")) {
	  // query all the mapped reads in the unpaired BAM
	  // (vs. specifying intervals).
	  rmeu.QUERY_ALL_MAPPED = true;
	  //	} else if (argv[i].equals("-all") ||
	} else if (argv[i].equals("-unpaired-no-index")) {
	  // search the entire unpaired BAM file for mapped reads.
	  // does not require the unpaired BAM to be indexed,
	  // but is a lot slower since the entire BAM is scanned
	  // (not just the intervals or mapped reads).
	  rmeu.QUERY_ALL = true;
	} else if (argv[i].equals("-v")) {
	  rmeu.VERBOSE = true;
	} else if (argv[i].equals("-ping")) {
	  rmeu.READ_REPORT_INTERVAL = Integer.parseInt(argv[++i]);
	} else {
	  System.err.println("unknown arg " + argv[i]);  // debug
	  System.exit(1);
	}
      }
      boolean ok = rmeu.extract();
      System.exit(ok ? 0 : 1);
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

  private void seu_id_strip_check (SAMRecord sr) {
    if (STRIP_SEU_SUFFIX) {
      Matcher matcher = seu_pattern.matcher(sr.getReadName());
      if (matcher.find()) {
	// strip SAMExtractUnmapped read suffix
	String name = matcher.replaceAll("");
	sr.setReadName(name);
      }
    }
  }

}
