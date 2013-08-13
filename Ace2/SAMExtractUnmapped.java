package Ace2;

import net.sf.samtools.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import Funk.Timer;
import Funk.Sys;

public class SAMExtractUnmapped {
  private File sam_file;
  private String report_basename;
  private static int HQ_MISMATCH_EXTRACT_THRESHOLD = 4;
  private static int HQ_MISMATCH_WINDOW_SIZE = HQ_MISMATCH_EXTRACT_THRESHOLD * 3;

  private static String SAM_INTERESTING_TAG = "XU";

  public static final int FLAG_HAS_HQMM = 0x01;
  public static final int FLAG_HAS_HQMM_SC = 0x02;
  public static final int FLAG_HAS_HQMM_EC = 0x04;
  public static final int FLAG_HAS_HQ_INSERTION = 0x08;
  public static final int FLAG_HAS_HQ_DELETION = 0x10;

  private int TRACKER_BUCKET_SIZE = 1000000;
  private int MAX_FILEHANDLES = 100;

  private static boolean VERBOSE = false;
  private static boolean EXTRACT_INDELS = true;
  private static boolean EXTRACT_DUPLICATES = true;
  private static boolean QUERY_UNMAPPED = false;
  private static boolean PASS_1_ONLY = false;
  private static boolean PASS_2_ONLY = false;
  private static boolean WRITE_BAM = false;
  private static boolean ENABLE_READ_LIMIT = false;
  private static int READ_LIMIT = 0;
  private static boolean EXTRACT_INTERESTING_MATES = true;
  // turning this off won't have much effect unless mate map position is
  // far away
  private static boolean REMOVE_INTERMEDIATE_FILES = true;
  private static boolean SORT_RESULTS = true;
  private static String OUT_DIR = null;
  // if a mapped read is "interesting", whether to extract its mapped mate too
  // statics are hacky; should probably be class variables
  
  private String CURRENT_REFERENCE_NAME = null;
  private boolean USABLE_REFERENCE_SEQUENCE;
  private SAMMismatchFilter MMF = null;
  private File temp_dir = null;

  private static String UNMAPPED_IDS_TAG = ".unmapped_ids.";
  private static String INTERESTING_IDS_TAG = ".interesting_ids.";

  public SAMExtractUnmapped (File sam_file, String report_basename) {
    this.sam_file = sam_file;
    this.report_basename = report_basename;
  }

  public void set_temp_dir (File temp_dir) {
    this.temp_dir = temp_dir;
  }

  public void report() throws FileNotFoundException,IOException {
    long read_count=0;
    long duplicate_count = 0;
    long mapped_count=0;
    long unmapped_count=0;
    int unmapped_without_pairing_count = 0;
    int unmapped_with_mapped_mates = 0;

    boolean pass_1_enabled = true;
    boolean pass_2_enabled = true;

    if (PASS_1_ONLY) pass_2_enabled = false;
    if (PASS_2_ONLY) pass_1_enabled = false;

    if (WRITE_BAM && (PASS_1_ONLY || PASS_2_ONLY)) {
      System.err.println("ERROR: bam output must use combined pass");  // debug
      System.exit(1);
    }

    SAMFileReader sfr = new SAMFileReader(sam_file);
    Iterator<SAMRecord> query = QUERY_UNMAPPED ?
      sfr.queryUnmapped() : sfr.iterator();

    HashMap<Integer,Integer> lengths = new HashMap<Integer,Integer>();

    SAMIndelFilter sif = new SAMIndelFilter();

    UniqueReadName urn = new UniqueReadName();
    urn.set_inthash_mode(true, 19);

    String base_fn = null;
    WorkingDirectory wd = null;
    if (temp_dir == null) {
      base_fn = report_basename;
    } else {
      if (PASS_1_ONLY || PASS_2_ONLY) {
	System.err.println("ERROR: -temp-dir not compatible w/partial processing");  // debug
	System.exit(1);
      } else {
	File f = (new File(report_basename)).getCanonicalFile();
	File parent = f.getParentFile();
	File target_dir = parent == null ? new File(".") : parent;
	wd = new WorkingDirectory(temp_dir, target_dir);
	base_fn = wd.get_file(f.getName()).getCanonicalPath();
      }
    }

    MultiplexedWriter mw_paired_reads = null;
    BAMFileWriter bfw = null;
    String bam_fn = null;
    if (WRITE_BAM) {
      bam_fn = base_fn + ".seu.unsorted.bam";
      bfw = new BAMFileWriter(new File(bam_fn));
      SAMFileHeader header_in = sfr.getFileHeader();
      //      bfw.setSortOrder(header_in.getSortOrder(), true);
      // claim pre-sorted output (the cake is a lie)
      // crashes

      //      bfw.setSortOrder(header_in.getSortOrder(), false);

      bfw.setSortOrder(SAMFileHeader.SortOrder.valueOf("unsorted"), false);
      bfw.setHeader(header_in);
    } else {
      mw_paired_reads = new MultiplexedWriter((base_fn + ".mate_mapped.fastq."), false);
    }

    MultiplexedWriter mw_unmapped_ids = new MultiplexedWriter((base_fn + UNMAPPED_IDS_TAG), false);
    mw_unmapped_ids.set_bucket_size(TRACKER_BUCKET_SIZE);
    mw_unmapped_ids.set_max_open_filehandles(MAX_FILEHANDLES);

    // list of unmapped sequence IDs, bucketed by mate reference name
    MultiplexedWriter mw_interesting = new MultiplexedWriter((base_fn + ".interesting_ids."), false);
    mw_interesting.set_bucket_size(TRACKER_BUCKET_SIZE);
    mw_interesting.set_max_open_filehandles(MAX_FILEHANDLES);

    //SAMTagUtil stu = new SAMTagUtil();
    //    SAMTagUtil stu = SAMTagUtil.getSingleton();
    //    short sam_interesting_tag = stu.makeBinaryTag(SAM_INTERESTING_TAG);
    // doesn't seem to work w/Picard 1.53
    // 1.65 OK

    if (pass_1_enabled) {
      // 
      //  first pass:
      //  - extract unmapped reads
      //  - track unmapped reads with mate mapping info for 2nd pass
      //
      System.err.println("starting pass 1");
      //      for (SAMRecord sr : sfr) {
      //      for (SAMRecord sr : query) {
      SAMRecord sr;

    //    WorkingFile wf_mate_unmapped = new WorkingFile(base_fn + ".mate_unmapped.fastq.gz");
      WorkingFile wf_mate_unmapped = null;
      PrintStream ps_mate_unmapped = null;
      if (!WRITE_BAM) {
	wf_mate_unmapped = new WorkingFile(base_fn + ".mate_unmapped.fastq");
	ps_mate_unmapped = wf_mate_unmapped.getPrintStream();
      }

      Timer t = new Timer("time_before_first_unmapped");
      boolean start_time_reported = false;

      HashSet<String> warned_ref = new HashSet<String>();

      while (query.hasNext()) {
	sr = query.next();
	read_count++;

	if (ENABLE_READ_LIMIT && read_count > READ_LIMIT) {
	  System.err.println("read limit enabled: stopping at read #" + read_count);  // debug
	  break;
	}

	if (sr.getDuplicateReadFlag()) {
	  duplicate_count++;
	  if (EXTRACT_DUPLICATES == false) continue;
	}

	String name = sr.getReadName();
	Integer length = sr.getReadLength();
	Integer count = lengths.get(length);
	if (count == null) count = 0;
	lengths.put(length, count + 1);
	//      lengths.put(sr.getReadLength()

	if (sr.getReadUnmappedFlag() == true) {
	  unmapped_count++;

	  if (!start_time_reported) {
	    t.finish();
	    start_time_reported = true;
	  }

	  if (false) {
	    //
	    //  mate/flag info
	    //

	    System.err.println("");  // debug

	    System.err.println("read: " + sr.getReadName());  // debug
	    System.err.println("mate align start: " + sr.getMateAlignmentStart());  // debug
	    System.err.println("mate ref sequence: " + sr.getMateReferenceName());  // debug
	    System.err.println("inferred insert size: " + sr.getInferredInsertSize());

	    System.err.println("read paired: " + sr.getReadPairedFlag());  // debug
	    System.err.println("proper pair: " + sr.getProperPairFlag());  // debug
	    System.err.println("mate unmapped: " + sr.getMateUnmappedFlag());
	    System.err.println("mate - strand: " + sr.getMateNegativeStrandFlag());
	    System.err.println("first of pair: " + sr.getFirstOfPairFlag());
	    System.err.println("second of pair: " + sr.getSecondOfPairFlag());
	    System.err.println("not primary alignment: " + sr.getNotPrimaryAlignmentFlag());
	    System.err.println("fails qual check: " + sr.getReadFailsVendorQualityCheckFlag());
	    System.err.println("duplicate read: " + sr.getDuplicateReadFlag());
	    System.err.println("");  // debug
	  }

	  //
	  //  write unmapped sequence in FASTQ format:
	  //

	  String info = name + "." + urn.get_suffix(name, sr.getReadNegativeStrandFlag());
	  ArrayList<String> stuff = new ArrayList<String>();

	  stuff.add("unmapped");
	  stuff.add("flags");
	  stuff.add(Integer.toString(sr.getFlags()));

	  if (sr.getDuplicateReadFlag()) stuff.add("duplicate");
	  // ever happens for unmapped?

	  if (!sr.getReadPairedFlag()) {
	    // read does not have any pairing info
	    stuff.add("unpaired");
	    unmapped_without_pairing_count++;
	    if (WRITE_BAM) {
	      bfw.addAlignment(sr);
	    } else {
	      SAMUtils.write_fastq(ps_mate_unmapped, sr, info, stuff);
	    }
	  } else if (sr.getMateUnmappedFlag()) {
	    // mate sequence isn't mapped: write to single file
	    stuff.add("mate_unmapped");
	    if (WRITE_BAM) {
	      bfw.addAlignment(sr);
	    } else {
	      SAMUtils.write_fastq(ps_mate_unmapped, sr, info, stuff);
	    }
	  } else {
	    // mate is mapped

	    stuff.add("has_mapped_mate");
	    // mate's mapping information
	    stuff.add(SAMUtils.bucket_reference_name(sr.getMateReferenceName(), false));
	    stuff.add(Integer.toString(sr.getMateAlignmentStart()));
	    stuff.add(sr.getMateNegativeStrandFlag() ? "-" : "+");

	    String mate_refname = SAMUtils.bucket_reference_name(sr.getMateReferenceName(), true);
	    if (mate_refname.equals("other")) {
	      String rname = sr.getMateReferenceName();
	      if (!warned_ref.contains(rname)) {
		//	      System.err.println("non-standard mapped mate reference name " + sr.getMateReferenceName() + " for read " + SAMUtils.get_printable_read_name(sr));  // debug
		System.err.println("non-standard mapped mate reference name " + rname + " for read " + SAMUtils.get_printable_read_name(sr) + " (only warning)");  // debug
		warned_ref.add(rname);
	      }
	    }
	    // save read by bucketized mate refname
	    //	  mw_unmapped_ids.write(mate_refname, name);
	    mw_unmapped_ids.write_bucketed(mate_refname,
					   sr.getMateAlignmentStart(),
					   name);
	    unmapped_with_mapped_mates++;
	    // save unmapped ID by mate refname

	    if (WRITE_BAM) {
	      bfw.addAlignment(sr);
	    } else {
	      SAMUtils.write_fastq(mw_paired_reads.getPrintStream(mate_refname), sr, info, stuff);
	    }
	  }

	  //print $fh "+\n";
	  //	printf $fh "%s\n", $ref->{quality};

	} else {
	  //
	  //  mapped read
	  //
	  if (interesting_check(sr, sif, null) > 0) {
	    if (VERBOSE) System.err.println("interesting mapped read " + sr.getReferenceName() + " " + sr.getAlignmentStart());  // debug
	    mw_interesting.write_bucketed(SAMUtils.bucket_reference_name(sr.getReferenceName(), true),
					  sr.getAlignmentStart(),
					  name);
	    if (EXTRACT_INTERESTING_MATES &&
		sr.getReadPairedFlag() &&
		!sr.getMateUnmappedFlag()) {
	      // read is paired and has a mapped mate
	      if (VERBOSE) System.err.println("  mate mapped to " + sr.getMateReferenceName() + " " + sr.getMateAlignmentStart());  
	      mw_interesting.write_bucketed(SAMUtils.bucket_reference_name(sr.getMateReferenceName(), true),
					    sr.getMateAlignmentStart(),
					    name);
	    }

	    // save read name to two buckets:
	    // 1. mapped chr/position
	    // 2. mate chr/position (if not same bucket)
	  }

	  mapped_count++;
	}
      }

      mw_unmapped_ids.finish();
      mw_interesting.finish();
      System.err.println("pass 1 complete");
      System.err.println("  unmapped count: " + unmapped_count);  // debug
      System.err.println("  unmapped w/mapped mates: " + unmapped_with_mapped_mates);  // debug

      if (wf_mate_unmapped != null) wf_mate_unmapped.finish();
      // unmapped reads w/unmapped mates

    } else {
      System.err.println("pass 1 disabled");
    }

    if (pass_2_enabled) {
      //
      //  pass 2: find mapped reads matching unmapped read mates
      //    also include other mapped reads of interest:
      //    - reads with a certain number of high-quality mismatches (possible undetected deletions)
      //    - indels
      //
      sfr = new SAMFileReader(sam_file);
      //
      // TO DO: restrict to a specific chr (for parallel processing)
      //
      String last_refname = null;
      int last_bucket = -1;
      HashSet<String> unmapped_pair_ids = null;
      HashSet<String> interesting_pair_ids = null;
      PrintStream ps = null;

      HashSet<String> warned_ref = new HashSet<String>();

      read_count = 0;

      for (SAMRecord sr : sfr) {
	if (sr.getReadUnmappedFlag()) continue;
	// only mapped reads in this pass (unmapped reads already extracted)

	read_count++;
	if (ENABLE_READ_LIMIT && read_count > READ_LIMIT) {
	  System.err.println("read limit enabled: stopping at read #" + read_count);  // debug
	  break;
	}
      
	if (EXTRACT_DUPLICATES == false && sr.getDuplicateReadFlag()) continue;

	String refname = SAMUtils.bucket_reference_name(sr.getReferenceName(), true);
	if (refname.equals("other")) {
	  String rname = sr.getReferenceName();
	  if (!warned_ref.contains(rname)) {
	    System.err.println("non-standard reference name " + rname + " for read " + SAMUtils.get_printable_read_name(sr)  + " (only warning)");  // debug
	    warned_ref.add(rname);
	  }
	}

	boolean need_id_update = false;
	int as = sr.getAlignmentStart();
	if (last_refname == null || !last_refname.equals(refname)) {
	  need_id_update = true;
	  if (!WRITE_BAM) ps = mw_paired_reads.getPrintStream(refname);
	  last_refname = refname;
	} else {
	  int bn = mw_unmapped_ids.get_bucket_number(as);
	  if (last_bucket != bn) {
	    need_id_update = true;
	    last_bucket = bn;
	  }
	}

	if (need_id_update) {
	  unmapped_pair_ids = mw_unmapped_ids.getBucketedHashSet(refname, as);
	  interesting_pair_ids = mw_interesting.getBucketedHashSet(refname, as);
	  System.err.println("ID set size for " + refname + " @ " + as + "/" + last_bucket + " unmapped=" + unmapped_pair_ids.size() + " interesting=" + interesting_pair_ids.size());  // debug
	}

	boolean usable = false;

	ArrayList<String> stuff = new ArrayList<String>();
	if (sr.getDuplicateReadFlag()) stuff.add("duplicate");
	if (sr.getReadPairedFlag() && !sr.getMateUnmappedFlag()) {
	  stuff.add("mate");
	  stuff.add(SAMUtils.bucket_reference_name(sr.getMateReferenceName(), false));
	  stuff.add(Integer.toString(sr.getMateAlignmentStart()));
	}

	String name = sr.getReadName();
	if (unmapped_pair_ids.contains(name)) {
	  // mapped read which is a mate of an unmapped read
	  usable = true;
	  stuff.add("has_unmapped_mate");
	  // to distinguish from other types of inclusions below
	}

	if (interesting_pair_ids.contains(name)) usable = true;

	if (usable) {
	  int seu_flags = interesting_check(sr, sif, stuff);
	  // populate tags
	  //	  System.err.println("DEBUG: stuff=" + stuff + " flags=" + seu_flags);  // debug

	  stuff.add("alignEnd");
	  stuff.add(Integer.toString(sr.getAlignmentEnd()));
	  stuff.add("flags");
	  stuff.add(Integer.toString(sr.getFlags()));

	  String tag = null;
	  if (stuff.size() > 0) tag = Funk.Str.join(",", stuff);

	  String info = name + "." + urn.get_suffix(name, sr.getReadNegativeStrandFlag());
	  // can use same UniqueReadName reference since it has never yet been called
	  // for the mapped reads in the file
	  info = info + " mapped," + 
	    SAMUtils.bucket_reference_name(sr.getReferenceName(), false) + "," + 
	    sr.getAlignmentStart() + "," + 
	    (sr.getReadNegativeStrandFlag() ? "-" : "+") + "," +
	    (tag == null ? "" : tag);
	  if (WRITE_BAM) {
	    //	    sr.setAttribute(sam_interesting_tag, Integer.valueOf(seu_flags));
	    // compilation problems w/Picard 1.53 and 1.65, WTF?
	    sr.setAttribute(SAM_INTERESTING_TAG, Integer.valueOf(seu_flags));
	    // I'm sure this is slower but at least it compiles  :/
	    bfw.addAlignment(sr);
	  } else {
	    SAMUtils.write_fastq(ps, sr, info);
	  }
	}
      }
    } else {
      System.err.println("pass 2 disabled");  // debug
    }

    //
    //  close output files:
    // 
    if (mw_paired_reads != null) mw_paired_reads.finish();
    if (bfw != null) bfw.close();

    if (pass_1_enabled && pass_2_enabled) {
      //
      // full processing (single threaded mode)
      //
      if (REMOVE_INTERMEDIATE_FILES) {
	mw_unmapped_ids.delete();
	mw_interesting.delete();
	// delete intermediate files
      }

      if (WRITE_BAM && SORT_RESULTS) {
	String sorted_bam_bn = base_fn + ".seu.sorted";
	ArrayList<String> commands = new ArrayList<String>();
	commands.add("/bin/env samtools sort " + bam_fn + " " + sorted_bam_bn);
	commands.add("/bin/env samtools index " + sorted_bam_bn + ".bam");
	for (String cmd : commands) {
	  int exit = Funk.Sys.exec_command(cmd, true);
	  if (exit != 0) {
	    System.err.println("ERROR: command exited with code " + exit);  // debug
	  }
	}
      }

      //
      // report summary info:
      //
      System.err.println("read count: " + read_count);  // debug
      // won't be accurate if queryUnmapped() is used in 1st pass
      System.err.println("optical/pcr duplicate count: " + duplicate_count);  // debug
      System.err.println("mapped count: " + mapped_count);  // debug
      System.err.println("unmapped count: " + unmapped_count);  // debug
      System.err.println("unmapped reads without pairing info: " + unmapped_without_pairing_count);  // debug

      System.err.println("read lengths:");  // debug
      for (Integer len : lengths.keySet()) {
	System.err.println("  " + len + ": " + lengths.get(len));  // debug
      }
    } else {
      //
      //  processing just 1st or 2nd pass
      //
      System.err.println("only partial processing, so not deleting read ID files");
    }

    if (wd != null) wd.finish();

  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    String bam_file = null;
    boolean span_test = false;
    File temp_dir = null;

    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	bam_file = new String(argv[++i]);
      } else if (argv[i].equals("-nib")) {
	NIB.DEFAULT_NIB_DIR = new String(argv[++i]);
      } else if (argv[i].equals("-query-unmapped")) {
	QUERY_UNMAPPED = true;
      } else if (argv[i].equals("-pass-1-only")) {
	PASS_1_ONLY = true;
      } else if (argv[i].equals("-pass-2-only")) {
	PASS_2_ONLY = true;
      } else if (argv[i].equals("-write-bam")) {
	WRITE_BAM = true;
      } else if (argv[i].equals("-cleanup")) {
	// for multi-pass mode; clean up any temp files
	SAMExtractUnmapped.clean_tempfiles();
	System.exit(0);
      } else if (argv[i].equals("-limit")) {
	ENABLE_READ_LIMIT = true;
	READ_LIMIT = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-no-sort")) {
	SORT_RESULTS = false;
      } else if (argv[i].equals("-no-indels")) {
	EXTRACT_INDELS = false;
      } else if (argv[i].equals("-no-interesting-mates")) {
	EXTRACT_INTERESTING_MATES = false;
      } else if (argv[i].equals("-no-clean")) {
	REMOVE_INTERMEDIATE_FILES = false;
      } else if (argv[i].equals("-no-duplicates")) {
	EXTRACT_DUPLICATES = false;
      } else if (argv[i].equals("-out-dir")) {
	OUT_DIR = new String(argv[++i]);
      } else if (argv[i].equals("-temp-dir")) {
	temp_dir = new File(argv[++i]);
      } else if (argv[i].equals("-tmpdir")) {
	// use $ENV{TMPDIR}, set on clusters
	String dir = System.getenv("TMPDIR");
	if (dir == null) {
	  System.err.println("ERROR: no TMPDIR environment variable; use -temp-dir X to specify manually");  // debug
	  System.exit(1);
	} else {
	  temp_dir = new File(dir);
	}
      } else if (argv[i].equals("-span-test")) {
	span_test = true;
      } else {
	System.err.println("ERROR: unknown argument " + argv[i]);  // debug
	usage();
      }
    }

    if (bam_file == null) {
      System.err.println("ERROR: specify -bam [file] and -nib [directory]");  // debug
      usage();
    } else {
      try {
	File bam = new File(bam_file);
	String basename = bam.getName();
	if (OUT_DIR != null) {
	  File f = new File(new File(OUT_DIR), basename);
	  basename = f.getCanonicalPath();
	}
	SAMExtractUnmapped seu = new SAMExtractUnmapped(bam, basename);
	if (temp_dir != null) {
	  System.err.println("using scratch directory: " + temp_dir);  // debug
	  seu.set_temp_dir(temp_dir);
	}

	if (span_test) {
	  seu.span_test();
	} else {
	  seu.report();
	}
      } catch (Exception e) {
	System.err.println("ERROR: " + e);  // debug
	e.printStackTrace();
      }
    }
  }

  private int interesting_check (SAMRecord sr, SAMIndelFilter sif, ArrayList<String> stuff) throws IOException {
    int result_flag = 0;
    boolean binary_mode = stuff == null;

    String rn = sr.getReferenceName();
    // FIX ME: faster to replace with index?
    if (CURRENT_REFERENCE_NAME == null || !CURRENT_REFERENCE_NAME.equals(rn)) {
      System.err.println("reference sequence: " + rn);  // debug
      CURRENT_REFERENCE_NAME = rn;
      Chromosome c = Chromosome.valueOfString(rn);
      //      if (rn.indexOf("_") != -1) {
      if (c == null) {
	// _random or other nonstandard/unusable name
	MMF = null;
	USABLE_REFERENCE_SEQUENCE = false;
	System.err.println("ignoring reads mapped to reference " + rn);  // debug
      } else {
	NIB nib = new NIB(rn);
	MMF = new SAMMismatchFilter(nib.read_all());
	USABLE_REFERENCE_SEQUENCE = true;
	MMF.get_config().ENABLE_MISMAP_FILTER = false;
	// we're not performing SNP calling, so make sure we're not 
	// tracking suspicious mismatch positions (memory consumption)
      }
    }

    if (USABLE_REFERENCE_SEQUENCE) {
      //
      //  check for reads containing high-quality mismatches to reference seq
      //
      MMF.filter(sr);
      //	System.err.println(sr.getReadName() + ": " + mmf.get_hq_mismatches());  // debug
      int hq = MMF.get_hq_mismatches_window(HQ_MISMATCH_WINDOW_SIZE);

      int hq_start_clip = MMF.get_start_clipped_hq_mismatches(sr);
      int hq_end_clip = MMF.get_end_clipped_hq_mismatches(sr);
      int hq_usable = hq + hq_start_clip + hq_end_clip;

      if (hq_usable >= HQ_MISMATCH_EXTRACT_THRESHOLD) {
	//	    System.err.println("hey now! " + sr.getReadName() + " " + hq);  // debug
	if (binary_mode) return 1;

	stuff.add("hqmm");
	stuff.add(Integer.toString(hq));
	if (hq > 0) result_flag |= FLAG_HAS_HQMM;

	if (hq_start_clip > 0) {
	  stuff.add("hqmm_sc");
	  stuff.add(Integer.toString(hq_start_clip));
	  result_flag |= FLAG_HAS_HQMM_SC;
	}
	if (hq_end_clip > 0) {
	  stuff.add("hqmm_ec");
	  stuff.add(Integer.toString(hq_end_clip));
	  result_flag |= FLAG_HAS_HQMM_EC;
	}
      }

      //
      //  check for reads w/indels meeting quality filters
      //
      if (VERBOSE) System.err.println("read="+SAMUtils.get_printable_read_name(sr));  // debug
      if (EXTRACT_INDELS && sif.filter(sr)) {
	if (VERBOSE) System.err.println("  usable indels");  // debug
	if (binary_mode) return 1;
	for (IndelInfo ii : sif.get_indels()) {
	  if (ii.indel_type.equals(CigarOperator.INSERTION)) {
	    stuff.add("insertion");
	    result_flag |= FLAG_HAS_HQ_INSERTION;
	  } else if (ii.indel_type.equals(CigarOperator.DELETION)) {
	    stuff.add("deletion");
	    result_flag |= FLAG_HAS_HQ_DELETION;
	  } else {
	    System.err.println("ERROR PROCESSING INDEL");  // debug
	  }
	}
      } else {
	if (VERBOSE) System.err.println("  no usable indels");  // debug
      }

    }

    return result_flag;
  }

  private static void usage() {
    System.err.println("  -bam [file]");  // debug
    System.err.println("  -nib [file]");  // debug
    System.err.println("  [-no-indels]");  // debug
    System.err.println("  [-no-duplicates]");  // debug
    System.exit(1);
  }

  public static void clean_tempfiles() {
    File dir = new File(".");
    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      if(files[i].indexOf(UNMAPPED_IDS_TAG) > -1 ||
	 files[i].indexOf(INTERESTING_IDS_TAG) > -1) {
	System.err.println("deleting file "+files[i]);  // debug
	File f = new File(files[i]);
	f.delete();
      }
    }
  }

  public void span_test() {
    SAMFileReader sfr = new SAMFileReader(sam_file);
    SAMFileSpan sfs = sfr.getFilePointerSpanningReads();
    int count = 0;
    while (true) {
      sfs = sfs.getContentsFollowing();
      System.err.println("count=" + ++count);
      // infinite loop???
    }
  }


}