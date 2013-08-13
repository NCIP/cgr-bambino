package Ace2;
//
//  single-pass SNP/indel detector for BAM files
//

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.text.NumberFormat;

import Funk.Timer;
import Funk.NumberComparator;
import Funk.LogicalComparator;

import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.*;

import static Ace2.TumorNormal.TUMOR_BYTE;
import static Ace2.TumorNormal.NORMAL_BYTE;

import net.maizegenetics.pal.statistics.FisherExact;

public class SAMStreamingSNPFinder {
  public static boolean VERBOSE = false;

  Iterable<SAMRecord> sam;

  //  private int READ_PING_INTERVAL = 1000000;
  private static final int SNP_FLANK_LEN = 20;

  //  private char[] reference_sequence = null;
  byte[] reference_sequence = null;

  private int start_offset = 0;
  private boolean limited_refseq = false;

  private String output_filename = null;
  private String current_reference_name, current_reference_label;
  private boolean single_sam = false;
  private boolean has_samples = false;
  Sample[] samples = null;

  int current_ref_index = 0;
  AbstractMap<Integer,BaseCounter3> tracker;
  // 0-based index

  HashMap<Integer,EnumMap<Base,EnumMap<Strand,EnumMap<TumorNormal,Integer>>>> tracker_broad;
  // track nucleotide counts meeding a certain quality threshold by 
  //   consensus base index -> nucleotide -> strand -> tumor/normal -> count
  // $tracker{consensus_base_number}{base}{strand}{tumor_normal_status} = $count;

  HashMap<Integer,HashMap<String,EnumMap<Strand,EnumMap<TumorNormal,Integer>>>> tracker_broad_indels;


  AbstractMap<Integer,HashMap<String,ArrayList<IndelInfo>>> indel_tracker;
  private SAMPooledIterator spi;
  private SAMMismatchFilter mmf;

  private Reporter rpt = null;
  private Reporter rpt_reads = null;

  private boolean use_dbsnp = true;
  private dbSNPDB dbsnp = null;
  private SNPConfig config;
  private ArrayList<SNP2> results = null;
  private ArrayList<SAMResource> srs = null;
  private ReferenceSequence refseq = null;
  private long PROCESSED;
  private ArrayList<SAMFileReader> sfrs;
  private Counter counter;

  public static final String HEADER_NORMAL_SAMPLE = "NormalSample";
  public static final String HEADER_TUMOR_SAMPLE = "TumorSample";
  public static final String HEADER_NAME = "Name";
  public static final String HEADER_CHR = "Chr";
  public static final String HEADER_POS = "Pos";
  public static final String HEADER_TYPE = "Type";
  public static final String HEADER_SIZE = "Size";
  public static final String HEADER_COVERAGE = "Coverage";
  public static final String HEADER_PCT_ALT_ALLELE = "Percent_alternative_allele";
  public static final String HEADER_CHR_ALLELE = "Chr_Allele";
  public static final String HEADER_ALT_ALLELE = "Alternative_Allele";
  //  public static final String HEADER_P_VALUE = "P-value";
  public static final String HEADER_P_VALUE = "Score";
  public static final String HEADER_TEXT = "Text";
  public static final String HEADER_UNIQUE_ALT_READS = "unique_alternative_ids";

  public static final String HEADER_REF_NORMAL_COUNT = "reference_normal_count";
  public static final String HEADER_REF_TUMOR_COUNT = "reference_tumor_count";
  public static final String HEADER_ALT_NORMAL_COUNT = "alternative_normal_count";
  public static final String HEADER_ALT_TUMOR_COUNT = "alternative_tumor_count";

  public static final String HEADER_COUNT_REF_NORMAL_FWD = "count_ref_normal_fwd";
  public static final String HEADER_COUNT_REF_NORMAL_REV = "count_ref_normal_rev";
  public static final String HEADER_COUNT_VAR_NORMAL_FWD = "count_var_normal_fwd";
  public static final String HEADER_COUNT_VAR_NORMAL_REV = "count_var_normal_rev";
  public static final String HEADER_COUNT_REF_TUMOR_FWD = "count_ref_tumor_fwd";
  public static final String HEADER_COUNT_REF_TUMOR_REV = "count_ref_tumor_rev";
  public static final String HEADER_COUNT_VAR_TUMOR_FWD = "count_var_tumor_fwd";
  public static final String HEADER_COUNT_VAR_TUMOR_REV = "count_var_tumor_rev";

  public static final String HEADER_ALT_FWD_COUNT = "alternative_fwd_count";
  public static final String HEADER_ALT_REV_COUNT = "alternative_rev_count";
  public static final String HEADER_ALT_HAS_RC = "alternative_bidirectional_confirmation";

  public static final String HEADER_BROAD_COVERAGE = "broad_coverage";
  public static final String HEADER_BROAD_REF_NORMAL_COUNT = "broad_reference_normal_count";
  public static final String HEADER_BROAD_REF_TUMOR_COUNT = "broad_reference_tumor_count";
  public static final String HEADER_BROAD_ALT_NORMAL_COUNT = "broad_alternative_normal_count";
  public static final String HEADER_BROAD_ALT_TUMOR_COUNT = "broad_alternative_tumor_count";

  public static final String HEADER_UNIQUE_ALT_READ_START = "unique_alt_read_starts";
  public static final String HEADER_UNIQUE_ALT_READ_START_F = "unique_alt_read_starts_fwd";
  public static final String HEADER_UNIQUE_ALT_READ_START_R = "unique_alt_read_starts_rev";
  
  public static final String HEADER_AVG_MAPQ_ALTERNATIVE = "avg_mapq_alternative";
  // average mapping quality for reads showing variant allele

  public static final String HEADER_SAM_TAGS_ALTERNATIVE = "alt_sam_tags";

  //
  //  read report headers:
  //
  public static final String HEADER_READ_NAME = "read_name";
  public static final String HEADER_STRAND = "strand";
  public static final String HEADER_REFERENCE_OR_VARIANT = "reference_or_variant";
  public static final String HEADER_TN = "tumor_normal";
  public static final String HEADER_SAM_FLAGS = "sam_flags";

  //
  //  Somatic/Germline/LOH headers:
  //
  public static final String HEADER_SOMATIC_OR_GERMLINE = "somatic_or_germline";
  public static final String HEADER_LOH_FLAG = "loh_flag";
  public static final String HEADER_ALT_RATIO_NORMAL = "alt_to_ref_ratio_normal";
  public static final String HEADER_ALT_RATIO_TUMOR = "alt_to_ref_ratio_tumor";
  public static final String HEADER_ALT_RATIO_NORMAL_TUMOR_DIFF = "alt_to_ref_ratio_diff_normal_tumor";

  public static final String HEADER_STRAND_SKEW = "strand_skew";
  

  public SAMStreamingSNPFinder (SNPConfig config) {
    this.config = config;
  }

  public SAMStreamingSNPFinder () {
    config = new SNPConfig();
  }

  public void set_genome_version_check (boolean v) {
    config.CHECK_GENOME_VERSION = v;
  }

  public void extent_setup (AceViewerConfig avc) throws IOException {
    // initialize from AceViewer instance (limited data)
    // init input data:
    set_resources(avc.sams);

    // init reference sequence:
    // FIX ME: this will explode if > 1 reference sequence (or changes)
    limited_refseq = true;
    start_offset = avc.ruler_start;
    reference_sequence = avc.target_sequence;
    current_reference_name = current_reference_label = avc.CONSENSUS_TAG;
  }

  public void set_readers (ArrayList<SAMFileReader> sfrs) {
    //    System.err.println("SFRS size:"+sfrs.size());  // debug
    this.sfrs = sfrs;
    boolean use_internal_iterators = false;
    if (srs != null) {
      use_internal_iterators = true;
      for (SAMResource sr : srs) {
	if (!sr.has_maps()) use_internal_iterators = false;
      }
      //      System.err.println("use internal: " + use_internal_iterators);  // debug
    }

    if (false) {
      System.err.println("internal disabled");  // debug
      use_internal_iterators = false;
    }

    if (sfrs.size() == 1) {
      single_sam = true;
      if (use_internal_iterators) {
	sam = srs.get(0).get_samrecord_iterable();
      } else {
	sam = sfrs.get(0);
      }
    } else if (config.STREAMING_MODE) {
      single_sam = false;
      spi = new SAMPooledIterator();
      spi.set_genome_version_check(config.CHECK_GENOME_VERSION);
      spi.addAll(sfrs);

      if (use_internal_iterators) {
	ArrayList<Iterable<SAMRecord>> its = new ArrayList<Iterable<SAMRecord>>();
	for (SAMResource sr : srs) {
	  its.add(sr.get_samrecord_iterable());
	}
	spi.set_custom_iterables(its);
      }

      if (spi.prepare()) {
	sam = spi;
      } else {
	System.err.println("ERROR: BAM files can't be pooled in streaming mode because layouts are incompatible.  Try using the -query-mode command-line flag (see documentation)");  // debug
	System.exit(1);
      }
    } else {
      // query mode:
      // delay iterator setup as we'll be doing it separately for each chromosome
      single_sam = false;
    }
  }
  
  public void set_resources(ArrayList<SAMResource> srs) throws IOException {
    this.srs = srs;
    ArrayList<SAMFileReader> sfrs = new ArrayList<SAMFileReader>();
    samples = new Sample[srs.size()];
    int i=0;
    has_samples = true;

    for (SAMResource sr : srs) {
      samples[i++] = sr.sample;
      SAMFileReader sfr = sr.getSAMFileReader();

      if (config.CHECK_GENOME_VERSION) {
	if (refseq == null) {
	  System.err.println("ERROR: no ReferenceSequence ref");  // debug
	  System.exit(1);
	}

	SAMFileHeader h = sfr.getFileHeader();
	SAMSequenceDictionary dict = h.getSequenceDictionary();

	for (SAMSequenceRecord ssr : dict.getSequences()) {
	  if (false) {
	    if (!ssr.getSequenceName().equals("chr7")) {
	      System.err.println("DEBUG");  // debug
	      continue;
	    }
	  }
	  //	System.err.println("processing " + ssr.getSequenceName());  // debug

	  String normalized_name = Chromosome.standardize_name(ssr.getSequenceName());
	  int rs_len = refseq.get_length(normalized_name);

	  if (rs_len != -1) {
	    //
	    // reference sequence model may not support all IDs in .bam file
	    //
	    int diff = Math.abs(ssr.getSequenceLength() - rs_len);
	    //	    System.err.println("chr=" + c + " bam_ref_len=" + ssr.getSequenceLength() + " rs_len=" + rs_len);  // debug
	    if (diff > 1) {
	      // allow off-by-one due to NIB header weirdness
	      String msg = "possible genome version mismatch for " + normalized_name + ": BAM_header=" + ssr.getSequenceLength() + " reference_sequence=" + rs_len + " file=" + sr.get_url();  // debug
	      throw new IOException(msg);
	    }
	  }
	}
      }

      sfrs.add(sfr);
    }
    set_readers(sfrs);
  }


  public void set_dbsnp (boolean v) {
    // FIX ME: move to SNPConfig?
    use_dbsnp = v;
  }

  public void set_output_file (String s) {
    // FIX ME: move to SNPConfig?
    output_filename = s;
  }

  private void call_snps (boolean flush_all) throws IOException {
    //
    //  call SNPs and release resources for sites where reads are completely loaded
    //
    ArrayList<Integer> indel_flush_queue = new ArrayList<Integer>();
    HashSet<String> called_indels = new HashSet<String>();

    if (VERBOSE) System.err.println("call_snps(): CRI="+current_ref_index);  // debug

    //
    //  process indels:
    //
    Iterable<Integer> it_keys;
    if (config.USE_TREEMAP) {
      it_keys = indel_tracker.keySet();
    } else if (indel_tracker == null) {
      ArrayList<Integer> empty = new ArrayList<Integer>();
      it_keys = empty;
    } else {
      ArrayList al = new ArrayList<Integer>(indel_tracker.keySet());
      Collections.sort(al);
      it_keys = al;
    }
    //    for (Integer ci : indel_tracker.keySet()) {

    if (!flush_all) {
      // Deletions require special handling because they may extend
      // beyond the current processing site, i.e.  if we process and
      // flush them too soon, we will lose tracking information needed
      // for the mismapped deletion filter in SNP calling (below).
      for (Integer ci : it_keys) {
	if (ci >= current_ref_index) break;
	HashMap<String,ArrayList<IndelInfo>> cons_bucket = indel_tracker.get(ci);

	for (String indel_key : cons_bucket.keySet()) {
	  for (IndelInfo ii : cons_bucket.get(indel_key)) {
	    if (ii.indel_type.equals(CigarOperator.DELETION)) {
	      int end_i = ci + ii.length - 1;
	      if (end_i >= current_ref_index) {
		// current_ref_index is 0-based alignment start of
		// reads currently being parsed (i.e. do NOT call any
		// variants touching this site or later because all
		// reads may not have been read yet).
		//
		// So, in this case the deletion extends beyond max
		// processing border.  If we process and flush it now
		// we will breaking the mismapped deletion filter for
		// any SNP at a later index because tracking info
		// will have been lost.
		if (VERBOSE) System.err.println("indel of " +indel_key + " at " + ci + " extends to " + end_i + ", CRI=" + current_ref_index + ", postponing calling");  // debug

		// My first attempt at fixing this was to simply
		// flag and postpone processing of tainted bases.
		// This can still break however if another indel 
		// is encountered: the new indel will postpone SNP
		// processing while the previously postponed indel
		// is (wrongly) now considered safe to process:
		//
		// consider:
		//
		//    1234
		// A: --
		// B:  T
		// C:  ----
		//
		// - at base 2, indel A postpones processing of SNP T at B (ok)
		// - at base 3, indel C postpones processing of SNP T at B (ok)
		// - at base 3, indel A is now considered safe to call and flush
		//   (WRONG, because it interacts with base 2, which
		//   is now locked and so won't be processed.)
		//
		// Simpler solution is just to return from sub
		// and try again later once coast is clear.

		return;
		// "bye for now." -ETM
	      }
	    }
	  }
	}
      }
    }

    for (Integer ci : it_keys) {
      if (!flush_all && ci >= current_ref_index) break;

      int i = ci - start_offset;

      if (i < 0 || i >= reference_sequence.length) {
	// indel out of range
	indel_flush_queue.add(ci);
	continue;
      }
      
      HashMap<String,ArrayList<IndelInfo>> cons_bucket = indel_tracker.get(ci);

      for (String indel_key : cons_bucket.keySet()) {
	ArrayList<IndelInfo> infos = cons_bucket.get(indel_key);
	int indel_count = infos.size();

	BaseCounter3 bc3 = tracker.get(ci);

	int bc_count = bc3 == null ? 0 : bc3.count_sequences();
	// indel events occur *between* reference sequence base numbers.
	// 
	// - bc3 *probably does* include counts for inserted regions, because an
	//   adjacent reference base number is used for tracking, and this base
	//   will be part of the read.
	//
	// - bc3 *probably doesn't* include counts for deleted regions, because 
	//   deleted bases aren't included in aligned blocks which generate the counts.
	//
	//	System.err.println("FIX ME: coverage at indel site " + (ci + 1) + " for " + indel_key);  // debug
	//	System.err.println("bc_raw="+(bc3 == null ? -1 : bc3.count_sequences()) + " ic=" + indel_count);
	if (infos.get(0).indel_type.equals(CigarOperator.INSERTION)) {
	  //	  System.err.println("adjusting");  // debug
	  bc_count -= indel_count;
	}
	if (bc_count < 0) bc_count = 0;
	//	System.err.println("  bc_cooked: " + bc_count);  // debug

	int coverage = bc_count + indel_count;
	// - BaseCounter will track the normally-aligned portion of the read
	//   (i.e. portion after the indel) at this consensus position, so be sure
	//   not to double-count reads!
	// - may be null if only indel sequences aligned at this position
	// - may be slightly different than TumorNormalReferenceTracker count, WTF???

	float indel_freq = (float) indel_count / coverage;
	Base cons_nt = Base.valueOf((char) reference_sequence[ci - start_offset]);

	boolean usable = coverage >= config.MIN_COVERAGE &&
	  indel_count >= config.MIN_ALT_ALLELE_COUNT &&
	  indel_freq >= config.MIN_MINOR_ALLELE_FREQUENCY &&
	  !cons_nt.equals(Base.BASE_UNKNOWN);

	if (VERBOSE) System.err.println("candidate indel at: " + (ci + 1) + " usable=" + usable);

	if (usable &&
	    indel_count >= config.MIN_ALT_ALLELE_COUNT_FOR_FILTER_ENABLE &&
	    config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE > 0) {
	  // false positives from sites where variant occurs near start/end of read only,
	  // and is never observed in the middle of a read.  Could be reads where
	  // problematic site is near the start/end and so evades mismatch filters,
	  // either due to nucleotide counts or quality levels.  Or: possibly linker sequence?
	  // ( see snp_error.bat for example )
	  if (VERBOSE) System.err.println("at: " + (ci + 1) + " size=" + infos.size());
	  
	  int ok_count = 0;
	  for (IndelInfo ii : infos) {
	    if (VERBOSE) System.err.println("   " + SAMUtils.get_printable_read_name(ii.sr) + " flank=" + ii.minimum_flanking_sequence + " len="+ ii.length);  // debug
	    if (ii.minimum_flanking_sequence >= config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE_WINDOW) {
	      if (++ok_count >= config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE) break;
	    }
	  }
	  if (ok_count < config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE) {
	    if (VERBOSE) System.err.println("indel flank flunk @ " + (ci + 1));  // debug
	    usable = false;
	  }
	}

	//
	// FIX ME:
	// clean up / centralize this code between SNPs and indels
	//
	if (usable &&
	    indel_count >= config.MIN_ALT_ALLELE_COUNT_FOR_FILTER_ENABLE &&
	    config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE > 1) {
	  UniqueStartTracker ust = new UniqueStartTracker(config, infos);
	  int unique_alt_read_start_positions = ust.get_unique_read_start_positions();
	  if (VERBOSE) System.err.println("unique starts for indel at " + (ci + 1) + " = " + unique_alt_read_start_positions);
	  if (!ust.are_counts_ok()) {
	    if (VERBOSE) System.err.println("unique start flunk @ " + (ci + 1));  // debug
	    usable = false;
	  }
	  if (VERBOSE) System.err.println("usable min_unique_start_positions_for_alt_allele check: " + usable);  // debug
	}

	if (usable) {
	  //
	  // usable indel, report
	  //
	  String save_key = Integer.toString(ci) + indel_key;
	  called_indels.add(save_key);

	  if (VERBOSE) {
	    System.err.println("indel of " + indel_key+ " at " + (ci + 1) + " reads=" + indel_count + " mfreq="+indel_freq + " cons_nt:" + reference_sequence[ci - start_offset]);
	    System.err.println("indel reads:");  // debug
	    for (IndelInfo ii : infos) {
	      System.err.println("  " + ii.sr.getReadName());  // debug
	    }

	    System.err.println("other reads");  // debug
	    // reads 
	    if (bc3 == null) {
	      System.err.println("  [none aligned]");  // debug
	    } else {
	      ArrayList<Base> bases = new ArrayList<Base>();
	      bases.add(Base.BASE_A);
	      bases.add(Base.BASE_C);
	      bases.add(Base.BASE_G);
	      bases.add(Base.BASE_T);

	      for (Base b : bases) {
		BaseCountInfo bci = bc3.get_info(b);
		if (bci != null) {
		  for (Object o : bci.sequences) {
		    SNPTrackInfo sti = (SNPTrackInfo) o;
		    System.err.println("  " + sti.sr.getReadName());  // debug
		  }
		}
	      }
	    }
	  }  // VERBOSE

	  //
	  //  get tumor/normal counts for indel reads
	  //
	  TumorNormalReferenceTracker tnrt = new TumorNormalReferenceTracker();
	  tnrt.addAll(infos);

	  boolean DEBUG_INDEL = false;
	  if (DEBUG_INDEL) {
	    // debug
	    System.err.println("indel at ref base " + (ci + 1));  // debug
	    System.err.println("indel reads:");  // debug
	    for (IndelInfo ii : infos) {
	      System.err.println("  " + SAMUtils.get_printable_read_name(ii.sr));  // debug
	    }
	  }

	  //
	  //  get tumor/normal counts at consensus site
	  //  (base immediately following indel event)
	  //
	  if (DEBUG_INDEL) System.err.println("normal reads:");  // debug
	  if (bc3 != null) {
	    BaseCountInfo bc = bc3.get_info(cons_nt);
	    if (bc == null) {
	      System.err.println("urgh: undef bc");  // debug
	      // can happen in config.MINOR_ALLELE_MODE if there are no
	      // observations of the reference (not initialized in that mode)
	    } else {
	      for (Object o : bc.sequences) {
		SNPTrackInfo sti = (SNPTrackInfo) o;
		if (DEBUG_INDEL) System.err.println("  " + SAMUtils.get_printable_read_name(sti.sr));  // debug
		tnrt.add(ReferenceOrVariant.REFERENCE, sti);
	      }
	    }
	  }
	  tnrt.analyze();

	  if (!unique_alt_reads_check(tnrt)) usable = false;

	  starts_populate(rpt, new UniqueStartTracker(config, infos));
	  mapq_populate(rpt, new MapQTracker(infos));
	  if (config.REPORT_ALT_ALLELE_SAM_TAGS) sam_tag_populate(rpt, new SAMTagTracker(infos));

	  //
	  //  quality score:
	  //
	  JointPolymorphismScore jps = new JointPolymorphismScore();
	  jps.set_variant_probability(infos);
	  ArrayList<SNPTrackInfo> ref_sti = tnrt.get_sti(ReferenceOrVariant.REFERENCE);
	  // after tnrt.analyze(), list is pruned of indel reads
	  if (ref_sti == null || ref_sti.size() == 0) {
 	    // only variant allele observed (i.e. germline variant).
 	    jps.set_reference_probability_from_quality(50);
	    System.err.println("TEST ME: indel at " + (ci + 1) + " germline only");  // debug
	  } else {
	    jps.set_reference_probability(ref_sti);
	  }
	  //	  rpt.set_value(HEADER_P_VALUE, Double.toString(jps.calculate_score()));
	  double score = jps.calculate_score();
	  if (score < config.MIN_SCORE) usable = false;

	  rpt.set_value(HEADER_P_VALUE, Funk.Str.float_decimal_format(score, config.REPORT_DECIMAL_ROUNDING));

	  IndelInfo ii = infos.get(0);

	  int ref_base_num = ci + 1;
	  rpt.set_value(HEADER_NAME, current_reference_label + "." + ref_base_num);
	  rpt.set_value(HEADER_CHR, current_reference_label);
	  //	  System.err.println("chrLA="+current_reference_label);  // debug
	  //	  System.err.println("get="+rpt.get_value(HEADER_CHR));  // debug

	  rpt.set_value(HEADER_POS, Integer.toString(ref_base_num));

	  String chr_allele = "";
	  String alternative_allele = "";

	  if (ii.indel_type.equals(CigarOperator.INSERTION)) {
	    rpt.set_value(HEADER_TYPE, "insertion");
	    chr_allele = "";
	    alternative_allele = ii.sequence;
	  } else if (ii.indel_type.equals(CigarOperator.DELETION)) {
	    rpt.set_value(HEADER_TYPE, "deletion");
	    int end_i = ii.reference_i + (ii.length - 1);
	    if ((end_i - start_offset) >= reference_sequence.length) {
	      System.err.println("WTF: indel beyond end of reference sequence");  // debug
	      chr_allele = "?";
	    } else {
	      chr_allele = new String(reference_sequence, ii.reference_i - start_offset, ii.length);	      
	    }
	    alternative_allele = "";
	  } else {
	    rpt.set_value(HEADER_TYPE, "error");
	  }

	  rpt.set_value(HEADER_CHR_ALLELE, chr_allele);
	  rpt.set_value(HEADER_ALT_ALLELE, alternative_allele);

	  rpt.set_value(HEADER_SIZE, Integer.toString(ii.length));
	  rpt.set_value(HEADER_COVERAGE, Integer.toString(coverage));
	  //	  rpt.set_value(HEADER_PCT_ALT_ALLELE, Float.toString(indel_freq));
	  rpt.set_value(HEADER_PCT_ALT_ALLELE, Funk.Str.float_decimal_format(indel_freq, config.REPORT_DECIMAL_ROUNDING));

	  String before = get_flanking_sequence(ci, true);
	  String after = get_flanking_sequence(ci, false);
	  String chunk = before+"["+chr_allele + "/" + alternative_allele + "]" + after;

	  //	  rpt.set_value("Text", chunk);
	  rpt.set_value(HEADER_TEXT, "");
	  // BROKEN

	  tnrt_report_populate(tnrt);
	  if (!skew_populate(tnrt) && !config.FISHERS_EXACT_STRAND_BIAS_ENABLE) {
	    System.err.println("rejecting indel due to skew at " + rpt.get_value(HEADER_CHR) + "." + rpt.get_value(HEADER_POS));
	    usable = false;
	  }
	  if (config.FISHERS_EXACT_STRAND_BIAS_ENABLE &&
	      !fishers_exact_strand_bias_check(tnrt)) {
	    if (VERBOSE) System.err.println("rejecting indel due to Fisher's Exact strand bias at " + rpt.get_value(HEADER_CHR) + "." + rpt.get_value(HEADER_POS));
	    usable = false;
	  }

	  if (config.ENABLE_BROAD_BASE_TRACKING) {
	    tnrt = new TumorNormalReferenceTracker();

	    EnumMap<Base,EnumMap<Strand,EnumMap<TumorNormal,Integer>>> base2strand = tracker_broad.get(ci);
	    if (base2strand == null) {
	      // happens if no normally-aligned reads at site
	      // (i.e. reads align just to indels or skipped regions)
	      //	      System.err.println("check: no counts at " + (ci + 1));  // debug
	    } else {
	      gather_counts(base2strand, cons_nt, ReferenceOrVariant.REFERENCE, tnrt);
	      // use ordinary broad-base tracker to get coverage for reference nucleotide
	    }

	    HashMap<String,EnumMap<Strand,EnumMap<TumorNormal,Integer>>> base2strand_i = tracker_broad_indels.get(ci);
	    System.err.println("ci="+ci);  // debug
	    gather_counts2(base2strand_i, ii.getTypeHashString(), ReferenceOrVariant.VARIANT, tnrt);

 	    int rnc = tnrt.get_count(ReferenceOrVariant.REFERENCE, TumorNormal.NORMAL);
 	    int rtc = tnrt.get_count(ReferenceOrVariant.REFERENCE, TumorNormal.TUMOR);
 	    int vnc = tnrt.get_count(ReferenceOrVariant.VARIANT, TumorNormal.NORMAL);
	    int vtc = tnrt.get_count(ReferenceOrVariant.VARIANT, TumorNormal.TUMOR);

	    if (ii.indel_type.equals(CigarOperator.INSERTION)) {
	      rnc -= vnc;
	      rtc -= vtc;
	      // indel events occur *between* reference sequence base positions.
	      //
	      // insertions are likely to be double-counted, since rnc
	      // and rtc use coverage counts for the reference base
	      // *adjacent* to the insertion itself.  Compensate by
	      // subtracting the insertion counts from the reference
	      // counts.  This is hacky but the best we can do given
	      // we don't have read-ID-level tracking available to
	      // reconcile the sequence ID lists.
	      //
	      // deletions don't need this compensation, since reads with 
	      // the deletion will not appear in the reference base
	      // coverage counts.
	      //
	      // ...where's the Tylenol?
	    }
	    
	    int broad_coverage = rnc + rtc + vnc + vtc;

 	    rpt.set_value(HEADER_BROAD_COVERAGE, Integer.toString(broad_coverage));
 	    rpt.set_value(HEADER_BROAD_REF_NORMAL_COUNT, Integer.toString(rnc));
 	    rpt.set_value(HEADER_BROAD_REF_TUMOR_COUNT, Integer.toString(rtc));
 	    rpt.set_value(HEADER_BROAD_ALT_NORMAL_COUNT, Integer.toString(vnc));
 	    rpt.set_value(HEADER_BROAD_ALT_TUMOR_COUNT, Integer.toString(vtc));
	  }


	  // rpt.set_value(HEADER_REF_NORMAL_COUNT, Integer.toString(tnrt.get_reference_normal_count()));
	  // note that these counts may not add up to "coverage" number
	  // as coverage includes counts for all base types (not just
	  // reference base and primary variant base, but ALL variants)

	  //	  rpt.set_value("Somatic_alteration", ask.is_somatic_variation() ? "1" : "0");
	  
	  //	  rpt.set_value("dbSNP", dbsnp_match);
	  //	  rpt.set_value("genotypes", gt.get_summary());

	  //	  rpt.set_value("Somatic_alteration", Integer.toString(ask.get_somatic_variation_int()));

	  //	  rpt.set_value("Normal_alleles", report_bases(ask.get_info_for(TumorNormal.NORMAL)));
	  //	  rpt.set_value("Tumor_alleles", report_bases(ask.get_info_for(TumorNormal.TUMOR)));

	  //	  rpt.set_value("Somatic_detail", ask.toString());

	  // 	  int map_error_code = 0;
	  // 	  if (cdp.has_mapping_error()) {
	  // 	    map_error_code = 1;
	  // 	    for (RefGene rg : config.refgenes) {
	  // 	      if (rg.is_rc()) map_error_code = 2;
	  // 	    }
	  // 	  }
	  // 	  rpt.set_value("cds_mapping_error", Integer.toString(map_error_code));

	  // 	  rpt.set_value("cds_change", Integer.toString(cdp.get_any_change_int()));
	  // 	  rpt.set_value("cds_change_detail", cdp.toString(","));

	  populate_sample();
	  somatic_germline_populate();

	  if (usable) {
	    end_row();
	  } else {
	    rpt.reset_row();
	  }

	  if (VERBOSE) {
	    for (IndelInfo inf : infos) {
	      System.err.println("  " + SAMUtils.get_printable_read_name(inf.sr));
	    }
	  }

	}  // usable
      }  // for indel_key

      indel_flush_queue.add(ci);
    }

    //
    //  process SNPs:
    //
    if (VERBOSE) System.err.println("calling SNPs...");  // debug
    ArrayList<Integer> flush_queue = new ArrayList<Integer>();

    Iterable<Integer> t_keys;
    if (config.USE_TREEMAP) {
      t_keys = tracker.keySet();
    } else if (tracker == null) {
      ArrayList<Integer> empty = new ArrayList<Integer>();
      t_keys = empty;
    } else {
      ArrayList al = new ArrayList<Integer>(tracker.keySet());
      Collections.sort(al);
      t_keys = al;
    }

    //    for (Integer ci : tracker.keySet()) {
    for (Integer ci : t_keys) {
      //      System.err.println("ci="+ci);  // debug

      if (!flush_all && ci >= current_ref_index) break;
      // halt when we reach region still being processed

      //
      // at this point we know we have all reads aligned to the candidate SNP site
      //
      BaseCounter3 bc = tracker.get(ci);

      //
      // evaluate SNP:
      //
      Base cons_nt = Base.valueOf((char) reference_sequence[ci - start_offset]);
      int coverage = bc.count_sequences();

      if (VERBOSE) System.err.println("reference at " + (ci + 1) + " is " + cons_nt + ", coverage=" + coverage + " bases:" + bc.count_bases());

      if (!cons_nt.equals(Base.BASE_UNKNOWN) &&
	  bc.count_bases() > 1 &&
	  coverage >= config.MIN_COVERAGE) {
	Base[] freq = bc.get_bases_by_frequency();

	if (config.MINOR_ALLELE_MODE) {
	  // Normal processing is oriented towards identifying variants
	  // vs. the reference allele.  This mode by contrast is 
	  // explicitly focused on the minor allele, regardless
	  // of the reference sequence.
	  //
	  // Redefines the "reference" allele as the most
	  // frequently-observed allele.
	  if (cons_nt != freq[0]) {
	    System.err.println("minor-allele-mode: replacing ref " + cons_nt + " with more-frequent " + freq[0] + " at " + (ci + 1));  // debug
	    cons_nt = freq[0];
	  }
	}


	boolean snp_vs_consensus = freq.length < 2;

	BaseCountInfo[] bcis = new BaseCountInfo[2];
	int max = freq.length > 2 ? 2 : freq.length;
	int alt_i = -1;
	for (int i=0; i < max; i++) {
	  bcis[i] = bc.get_info(freq[i]);
	  //	    if (freq[i] != cons_nt && alt_nt == 0) alt_nt = freq[i];
	  if (freq[i] != cons_nt && alt_i == -1) alt_i = i;
	  // index of the highest-frequency non-consensus base (alternative allele)

	  if (VERBOSE) {
	    System.err.println(i + " => " + freq[i] + " => " + bcis[i].count);
	    for (Object o : bcis[i].sequences) {
	      SNPTrackInfo sti = (SNPTrackInfo) o;
	      System.err.println("  " + sti.sr.getReadName() + " " + 
				 SAMUtils.get_printable_read_name(sti.sr) + " min_flank=" + sti.get_minimum_flanking_sequence());
	    }
	  }
	}

	if (freq[0] != cons_nt) {
	  // the highest-frequency (but not necessarily unique!)
	  // allele is also likely to be a SNP if it's different
	  // from the reference sequence.  In assemblies containing
	  // data from only one sample, we're unlikely to see much
	  // variation in major/minor allele frequencies -- the SNP
	  // is likely to dominate the observed calls.  However this
	  // majority allele will be a different nucleotide than in
	  // the canonical/reference/chromosome sequence.
	  alt_i = 0;
	  snp_vs_consensus = true;
	}

	if (config.MINOR_ALLELE_MODE && snp_vs_consensus) {
	  // should never happen
	  System.err.println("EPIC FAIL: snp_vs_consensus active even in population freq mode");
	  System.exit(1);
	}

	if (config.ENABLE_MATE_PAIR_DISAGREEMENT_FILTER &&
	    bcis[0] != null && bcis[1] != null) {
	  //
	  // exclude reads where calls from same mate pair disagree
	  //
	  //	  System.err.println("mate pair QA start at " + (ci + 1) + " " + bcis[0].base + " " + bcis[1].base);  // debug
	  if (!mate_pair_qa(bcis[0], bcis[1])) {
	    System.err.println("mate pair QA fail at " + (ci + 1));  // debug
	  }
	}

	if (config.ENABLE_MISMAPPED_DELETION_FILTER) {
	  if (VERBOSE) {
	    System.err.println("itracker: keys=" + indel_tracker.keySet());
	    System.err.println("called_indels: keys=" + called_indels);  // debug
	  }

	  for (Integer ipos : indel_tracker.keySet()) {
	    HashMap<String,ArrayList<IndelInfo>> bucket = indel_tracker.get(ipos);
	    for (String itype : bucket.keySet()) {
	      ArrayList<IndelInfo> iis = bucket.get(itype);
	      IndelInfo ii = iis.get(0);
	      String called_key = ipos.toString() + itype;
	      if (ii.indel_type.equals(CigarOperator.DELETION)
		  && called_indels.contains(called_key)
		  ) {
		// indel is a deletion and was called
		if (VERBOSE) {
		  System.err.println("itracker: called del at " + ipos + ", len=" + ii.length);  // debug

		}

		if (ci >= ipos && ci <= (ipos + (ii.length - 1))) {
		  // the deletion overlaps this putative SNP position
		  //		  System.err.println("mismapped deletion check at " + (ipos + 1) + " " + itype);  // debug
		  ArrayList<SNPTrackInfo> remove = new ArrayList<SNPTrackInfo>();
		  for (Object o : bcis[alt_i].sequences) {
		    SNPTrackInfo sti = (SNPTrackInfo) o;
		    //		    System.err.println("  id:" + SAMUtils.get_printable_read_name(sti.sr) + " mfseq:" + (int) sti.minimum_flanking_sequence);  // debug
		    if (sti.get_minimum_flanking_sequence() < 10) {
		      // FIX ME: CONSTANT: MOVE TO CONFIG
		      //		      System.err.println("    removing!");  // debug
		      remove.add(sti);
		    }
		  }
		  if (remove.size() > 0) {
		    System.err.println("mismapped deletion filter: removing " + remove.size() + " near-end reads at " + (ipos + 1) + " within deletion site");  // debug
		    if (VERBOSE) {
		      for (SNPTrackInfo sti : remove) {
			System.err.println("  removing:" + SAMUtils.get_printable_read_name(sti.sr));
		      }
		    }
		    bcis[alt_i].sequences.removeAll(remove);
		    bcis[alt_i].count -= remove.size();
		  }
		}
	      }
	    }
	  }
	}

	int total_count = 0;
	for (int i=0; i < max; i++) {
	  total_count += bcis[i].count;
	}

	int alt_count = bcis[alt_i].count;
	float alt_freq = (float) alt_count / total_count;
	if (VERBOSE) System.err.println("alt_count=" + alt_count + " alt_freq=" + alt_freq);
	
	boolean usable = alt_count >= config.MIN_ALT_ALLELE_COUNT && alt_freq >= config.MIN_MINOR_ALLELE_FREQUENCY;
	if (VERBOSE) System.err.println("usable after count/freq check: " + usable);  // debug

	if (usable && 
	    alt_count >= config.MIN_ALT_ALLELE_COUNT_FOR_FILTER_ENABLE &&
	    config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE > 0) {
	  // false positives from sites where variant occurs near start/end of read only,
	  // and is never observed in the middle of a read.  Could be reads where
	  // problematic site is near the start/end and so evades mismatch filters,
	  // either due to nucleotide counts or quality levels.  Or: possibly linker sequence?
	  // ( see snp_error.bat for example )
	  int ok_count = 0;
	  for (Object o : bcis[alt_i].sequences) {
	    SNPTrackInfo sti = (SNPTrackInfo) o;
	    if (sti.get_minimum_flanking_sequence() >= config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE_WINDOW) {
	      if (++ok_count >= config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE) break;
	    }
	  }
	  if (ok_count < config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE) {
	    if (VERBOSE) System.err.println("SNP flank flunk @ " + (ci + 1));  // debug
	    usable = false;
	  }
	}
	if (VERBOSE) System.err.println("usable after min_alt_reads_with_flanking_sequence check: " + usable);  // debug

	if (usable &&
	    alt_count >= config.MIN_ALT_ALLELE_COUNT_FOR_FILTER_ENABLE &&
	    config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE > 1) {
	  UniqueStartTracker ust = new UniqueStartTracker(config, bcis[alt_i].sequences);

	  int unique_alt_read_start_positions = ust.get_unique_read_start_positions();

	  if (VERBOSE) System.err.println("unique starts for alt allele at " + (ci + 1) + " = " + unique_alt_read_start_positions);

	  if (!ust.are_counts_ok()) {
	    if (VERBOSE) System.err.println("unique start flunk @ " + (ci + 1));  // debug
	    usable = false;
	  }
	}
	if (VERBOSE) System.err.println("usable min_unique_start_positions_for_alt_allele check: " + usable);  // debug

	if (usable && config.ENABLE_MISMAP_FILTER) {
	  if (VERBOSE) System.err.println("mismap test for " + (ci + 1));  // debug
	  EnumMap<Base,Integer> hq_mismatches = mmf.get_hq_mismatches(ci + 1);
	  if (hq_mismatches != null) {
	    // 	    for (Base b : hq_mismatches.keySet()) {
	    // 	      System.err.println("base:" + b + " count:" + hq_mismatches.get(b));  // debug
	    // 	    }
	    Integer suspicious_count = hq_mismatches.get(freq[alt_i]);
	    if (suspicious_count != null) {
	      int usable_count = bcis[alt_i].count;
	      float suspicious_ratio = (float) suspicious_count / usable_count;
	      if (VERBOSE) System.err.println("usable count for " + freq[alt_i] + ":" + usable_count + " suspicious=" + suspicious_count + " ratio:" + suspicious_ratio);  // debug

	      if (suspicious_ratio >= config.MISMAP_BASE_FREQUENCY_THRESHOLD) {
		System.err.println("rejecting SNP at " + (ci + 1) + ", mismap filter base ratio of " + suspicious_ratio);  // debug
		usable = false;
	      } else if (suspicious_ratio >= 0.2 && usable_count > 3) {
		// debug/tuning
		System.err.println("allowing SNP at " + (ci + 1) + " w/mismap ratio of " + suspicious_ratio + ", usable_count=" +usable_count);  // debug		
	      }
	      
	    }
	  }
	}
	if (VERBOSE) System.err.println("usable after mismap filter: " + usable);  // debug

	double score = -1;
	if (usable) {
	  // 
	  // generate Conscript-style SNP score
	  //
	  JointPolymorphismScore jps = new JointPolymorphismScore();
	  jps.set_variant_probability(bcis[alt_i]);
	  if (snp_vs_consensus) {
	    // only variant allele observed (i.e. germline variant).
	    jps.set_reference_probability_from_quality(50);
	    // since the reference sequence is known, act as if we've observed
	    // a high quality example of that base.
	  } else {
	    //	    System.err.println("2-allele snp at " + (ci + 1) + "  alt_i = " + alt_i);  // debug
	    int reference_i = alt_i == 1 ? 0 : 1;
	    jps.set_reference_probability(bcis[reference_i]);
	  }
	  score = jps.calculate_score();
	  if (score < config.MIN_SCORE) usable = false;
	}

	if (usable) {
	  Base alt_nt = freq[alt_i];
	  // variant allele

	  //
	  // extract flanking sequence for SNP:
	  //
	  int blen = SNP_FLANK_LEN;
	  int bi = ci - blen;
	  if (bi < 0) {
	    blen += bi;
	    bi = 0;
	  }
	  String before = get_flanking_sequence(ci, true);
	  String after = get_flanking_sequence(ci, false);
	  String chunk = before+"["+cons_nt + "/" + alt_nt + "]" + after;
	  int ref_base_num = ci + 1;
	  if (VERBOSE) System.err.println("SNP:" +chunk + " at " + ref_base_num);  // debug

	  TumorNormalReferenceTracker tnrt = new TumorNormalReferenceTracker();
	  BaseCountInfo bci = bc.get_info(cons_nt);

	  //	  System.err.println("at: " + current_reference_label + "." + ref_base_num);
	  //System.err.println("bci_null:" + (bci == null));
	  //System.err.println("bci.sequences null:" + (bci.sequences == null));

	  //	  System.err.println("bci="+bci);  // debug
	  //	  System.err.println("bci.sequences="+bci.sequences);  // debug

	  for (Object o : bci.sequences) {
	    tnrt.add(ReferenceOrVariant.REFERENCE, (SNPTrackInfo) o);
	  }
	  bci = bc.get_info(alt_nt);
	  for (Object o : bci.sequences) {
	    SNPTrackInfo sti = (SNPTrackInfo) o;
	    tnrt.add(ReferenceOrVariant.VARIANT, sti);
	  }
	  tnrt.analyze();
	  if (!unique_alt_reads_check(tnrt)) usable = false;

	  starts_populate(rpt, new UniqueStartTracker(config, bci.sequences));
	  mapq_populate(rpt, new MapQTracker(bci.sequences));
	  if (config.REPORT_ALT_ALLELE_SAM_TAGS) sam_tag_populate(rpt, new SAMTagTracker(bci.sequences));

	  //
	  //  report:
	  //

	  if (config.ENABLE_BROAD_BASE_TRACKING) {
	    int broad_coverage = 0;
	    TumorNormalReferenceTracker btnrt = new TumorNormalReferenceTracker();

	    EnumMap<Base,EnumMap<Strand,EnumMap<TumorNormal,Integer>>> base2strand = tracker_broad.get(ci);
	    if (base2strand != null) {
	      broad_coverage += gather_counts(base2strand, cons_nt, ReferenceOrVariant.REFERENCE, btnrt);
	      broad_coverage += gather_counts(base2strand, alt_nt, ReferenceOrVariant.VARIANT, btnrt);
	    }

	    rpt.set_value(HEADER_BROAD_COVERAGE, Integer.toString(broad_coverage));

	    rpt.set_value(HEADER_BROAD_REF_NORMAL_COUNT, Integer.toString(btnrt.get_count(ReferenceOrVariant.REFERENCE, TumorNormal.NORMAL)));
	    rpt.set_value(HEADER_BROAD_REF_TUMOR_COUNT, Integer.toString(btnrt.get_count(ReferenceOrVariant.REFERENCE, TumorNormal.TUMOR)));
	    rpt.set_value(HEADER_BROAD_ALT_NORMAL_COUNT, Integer.toString(btnrt.get_count(ReferenceOrVariant.VARIANT, TumorNormal.NORMAL)));
	    rpt.set_value(HEADER_BROAD_ALT_TUMOR_COUNT, Integer.toString(btnrt.get_count(ReferenceOrVariant.VARIANT, TumorNormal.TUMOR)));
	  }

	  rpt.set_value(HEADER_NAME, current_reference_label + "." + ref_base_num);
	  rpt.set_value(HEADER_CHR, current_reference_label);
	  rpt.set_value(HEADER_POS, Integer.toString(ref_base_num));
	  rpt.set_value(HEADER_TYPE, "SNP");
	  rpt.set_value(HEADER_SIZE, "1");

	  rpt.set_value(HEADER_COVERAGE, Integer.toString(coverage));

	  //	  rpt.set_value(HEADER_PCT_ALT_ALLELE, Float.toString(alt_freq));
	  rpt.set_value(HEADER_PCT_ALT_ALLELE, Funk.Str.float_decimal_format(alt_freq, config.REPORT_DECIMAL_ROUNDING));

	  rpt.set_value(HEADER_CHR_ALLELE, cons_nt.toString());
	  rpt.set_value(HEADER_ALT_ALLELE, alt_nt.toString());
	  //	  rpt.set_value(HEADER_P_VALUE, Double.toString(score));
	  rpt.set_value(HEADER_P_VALUE, Funk.Str.float_decimal_format(score, config.REPORT_DECIMAL_ROUNDING));
	  rpt.set_value(HEADER_TEXT, chunk);

	  if (dbsnp != null) {
	    dbSNPEntry snp = dbsnp.find(ref_base_num, cons_nt, alt_nt);
	    if (snp != null) {
	      rpt.set_value("dbSNP", snp.get_name());
	    }
	  }

	  //	  rpt.set_value("Somatic_alteration", ask.is_somatic_variation() ? "1" : "0");
	  
	  //	  rpt.set_value("dbSNP", dbsnp_match);
	  //	  rpt.set_value("genotypes", gt.get_summary());

	  //	  rpt.set_value("Somatic_alteration", Integer.toString(ask.get_somatic_variation_int()));

	  //	  rpt.set_value("Normal_alleles", report_bases(ask.get_info_for(TumorNormal.NORMAL)));
	  //	  rpt.set_value("Tumor_alleles", report_bases(ask.get_info_for(TumorNormal.TUMOR)));

	  //	  rpt.set_value("Somatic_detail", ask.toString());

	  // 	  int map_error_code = 0;
	  // 	  if (cdp.has_mapping_error()) {
	  // 	    map_error_code = 1;
	  // 	    for (RefGene rg : config.refgenes) {
	  // 	      if (rg.is_rc()) map_error_code = 2;
	  // 	    }
	  // 	  }
	  // 	  rpt.set_value("cds_mapping_error", Integer.toString(map_error_code));

	  // 	  rpt.set_value("cds_change", Integer.toString(cdp.get_any_change_int()));
	  // 	  rpt.set_value("cds_change_detail", cdp.toString(","));
	    
	  tnrt_report_populate(tnrt);
	  if (!skew_populate(tnrt) && !config.FISHERS_EXACT_STRAND_BIAS_ENABLE) {
	    System.err.println("rejecting SNP due to skew at " + rpt.get_value(HEADER_CHR) + "." + rpt.get_value(HEADER_POS));  // debug
	    usable = false;
	  }

	  if (VERBOSE) System.err.println("before FE, at " + rpt.get_value(HEADER_CHR) + "." + rpt.get_value(HEADER_POS));  // debug

	  if (config.FISHERS_EXACT_STRAND_BIAS_ENABLE &&
	      !fishers_exact_strand_bias_check(tnrt)) {
	    if (true) System.err.println("rejecting SNP due to Fisher's Exact strand bias at " + rpt.get_value(HEADER_CHR) + "." + rpt.get_value(HEADER_POS));
	    usable = false;
	  }

	  populate_sample();
	  somatic_germline_populate();

	  if (VERBOSE) System.err.println("final usable=" + usable);  // debug

	  if (usable) {
	    end_row();
	  } else {
	    rpt.reset_row();
	  }

	} // frequency
      } // count and coverage


      flush_queue.add(ci);
      // mark bucket for deletion
      // can't remove while iterating (ConcurrentModificationException)
    }

    int max_flush = -1;
    for (Integer ci : flush_queue) {
      // release resources (reads, etc.) tied to this SNP site
      //      System.err.println("flushing " + ci);  // debug
      if (ci > max_flush) max_flush = ci;
      tracker.remove(ci);
    }
    //    clean_intmap_through(tracker, max_flush);
    
    // clean indel tracker:
    for (Integer ci : indel_flush_queue) {
      if (VERBOSE) {
	System.err.println("itracker: flushing " + ci);  // debug
      }
      indel_tracker.remove(ci);
    }


    

    //    System.err.println("flush queue: " + flush_queue);  // debug

    if (config.ENABLE_BROAD_BASE_TRACKING && max_flush >= 0) {
      // unlikely but possible that main tracker will not contain all entries
      // that broad tracker does (if main tracker never populated due to quality checks).
      // on the other hand, tracker has at least 1 entry for reference sequence,
      // so maybe it should be identical?  whatever (safe > sorry).
      clean_intmap_through(tracker_broad, max_flush);
      clean_intmap_through(tracker_broad_indels, max_flush);
    }
    //    System.err.println("broad flush queue: " + broad_flush_queue);  // debug

    if (config.ENABLE_MISMAP_FILTER && mmf != null) {
      mmf.clean_hq_mismatch_tracker_through(max_flush + 1);
    }

    if (config.LIMIT_READ_TRACKING) prune_tracker();

    if (config.DEBUG_SNP_MEMORY_USAGE) {
      //
      // debug: mongo RAM usage
      //
      System.err.println("end of call_snps() at " + current_ref_index + ", tracker keys:" + tracker.keySet().size());
      int seqs_tracked = 0;
      for (Integer pos : tracker.keySet()) {
	BaseCounter3 bc = tracker.get(pos);
	EnumMap<Base,BaseCountInfo> saw_bases = bc.get_saw_bases();
	for (Base b : saw_bases.keySet()) {
	  BaseCountInfo bci = saw_bases.get(b);
	  int count = bci.sequences.size();
	  seqs_tracked += count;
	  System.err.println("  seq count for ref base " + (pos + 1) + ", " + b + " = " + count);
	}
      }
      System.err.println("total tracker objects: " + seqs_tracked);  // debug

      seqs_tracked = 0;
      for (Integer pos : indel_tracker.keySet()) {
	HashMap<String,ArrayList<IndelInfo>> bucket = indel_tracker.get(pos);
	for (String ikey : bucket.keySet()) {
	  ArrayList<IndelInfo> il = bucket.get(ikey);
	  int count = il.size();
	  seqs_tracked += count;
	  System.err.println("  ii count at " + (pos + 1) + " = " + count);  // debug
	}
      }
      System.err.println("total indel_tracker objects: " + seqs_tracked);  // debug

    }
  }


  public void find_snps() {

    mmf = null;
    if (limited_refseq) {
      // reference sequence is fixed to a particular region (i.e. viewer scope)
      mmf = new SAMMismatchFilter(config, reference_sequence, start_offset);
      // FIX ME: move to SHARED REFSEQ SETUP CODE
    } else {
      // load full reference sequences as we go
      reference_sequence = null;
      current_reference_name = null;
    }

    if (false) {
      // debug to test filter
      System.err.println("DEBUG: min alt allele sanity check disabled");  // debug
    } else if (config.MIN_ALT_ALLELE_COUNT < config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE) {
      System.err.println("Note: setting minimum unique start positions for alt allele to " + config.MIN_ALT_ALLELE_COUNT + " because min_alt_allele_count is set to " + config.MIN_ALT_ALLELE_COUNT);  // debug
      config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE = config.MIN_ALT_ALLELE_COUNT;
    }


    System.err.println("SNP detection configuration:");
    System.err.println("  min_quality: " + config.MIN_QUALITY);
    System.err.println("  min_alt_allele_count: " + config.MIN_ALT_ALLELE_COUNT);
    System.err.println("  min_minor_allele_frequency: " + config.MIN_MINOR_ALLELE_FREQUENCY);


    System.err.println("  if alt coverage >= " + config.MIN_ALT_ALLELE_COUNT_FOR_FILTER_ENABLE + ":");  // debug

    System.err.println("    min_alt_reads_w_flanking: " + config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE + ", window=" + config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE_WINDOW);
    System.err.println("    minimum unique start positions for alt allele: " + config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE);  // debug


    System.err.println("  min mapping quality: " + config.MIN_MAPPING_QUALITY);  // debug
    System.err.println("  min unique read names for alternative allele: " + config.MIN_UNIQUE_READ_NAMES_FOR_ALT_ALLELE);
    System.err.println("  min unique read start posotions for alternative allele: " + config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE);

    System.err.println("  min_flanking_quality: " + config.MIN_FLANKING_QUALITY + ", window=" + config.MIN_FLANKING_QUALITY_WINDOW);  // debug

    System.err.println("  exclude reads w/non-primary alignments: " + config.SKIP_NONPRIMARY_ALIGNMENTS);  // debug

    System.err.println("  mismatch filter: " + (config.ENABLE_MISMATCH_FILTER ? "enabled" : "disabled"));
    if (config.ENABLE_MISMATCH_FILTER) {
      System.err.println("    max high-quality reference mismatches: " + config.MISMATCH_FILTER_MAX_HQ_MISMATCH_COUNT + " of quality " + config.MISMATCH_FILTER_MIN_HIGH_QUALITY + "+");  // debug
      System.err.println("    max low-quality mismatches: " + config.MISMATCH_FILTER_MAX_LQ_MISMATCH_COUNT + " of quality " + config.MISMATCH_FILTER_MIN_LOW_QUALITY + "+");  // debug
    }
    System.err.println("  mismap filter: " + (config.ENABLE_MISMAP_FILTER ? "enabled" : "disabled") + ", frequency threshold=" + config.MISMAP_BASE_FREQUENCY_THRESHOLD);

    System.err.println("  broad base tracking:" + config.ENABLE_BROAD_BASE_TRACKING + " min_quality:" + config.BROAD_BASE_TRACKING_MIN_QUALITY);  // debug
    System.err.println("  read-end mismatch filter: " + config.ENABLE_END_MISMATCH_FILTER);

    System.err.println("  strand skew filter: " + config.STRAND_SKEW_FILTER_ENABLE);  // debug
    if (config.STRAND_SKEW_FILTER_ENABLE) {
      System.err.println("    to generate skew call: " + config.STRAND_SKEW_CALL_MIN_STRAND_COVERAGE + "+ reads/strand, " + config.STRAND_SKEW_CALL_MIN_VARIANT_COUNT + "+ variant observations");  // debug
      System.err.println("    to reject variant: " + config.STRAND_SKEW_FILTER_MIN_STRAND_COVERAGE + "+ reads/strand, " + config.STRAND_SKEW_FILTER_MIN_VARIANT_COUNT + "+ variant observations, " + (Math.round(config.STRAND_SKEW_FILTER_MIN_STRAND_PERCENT_TO_CONSIDER_SKEWED * 100)) + "%+ skewed");  // debug


    }



    System.err.println("  read_limit_tracking: " + config.LIMIT_READ_TRACKING + " " + (config.LIMIT_READ_TRACKING ? "(" + config.READ_TRACKING_LIMIT + ", trigger at " + config.READ_TRACKING_LIMIT_TRIGGER + ")" : ""));  // debug
    
    System.err.println("  insertions: " + (config.AVERAGE_INSERTION_QUALITY ? "average inserted base qualities for quality check" : "require each base to meet minimum quality"));  // debug

    System.err.println("  tracker map implementation: " + (config.USE_TREEMAP ? "TreeMap" : "HashMap"));  // debug

    System.err.println("  poly-X runs:");  // debug
    System.err.println("    minimum run length: " + config.POLY_X_MIN_RUN_LENGTH);  // debug
    System.err.println("    indel filter enabled: " + config.ENABLE_POLY_X_RUN_MASK_INDEL);  // debug
    System.err.println("    SNPs: low-quality mask filter enabled: " + config.ENABLE_POLY_X_RUN_MASK_SNP);  // debug

    if (!config.REPORT_RESULTS) results = new ArrayList<SNP2>();

    if (use_dbsnp) {
      boolean usable = false;

      if (config.DBSNP_BLOB_FILE != null) {
	File f = new File(config.DBSNP_BLOB_FILE);
	if (f.exists() && f.canRead()) {
	  dbsnp = new dbSNPDB(config.DBSNP_BLOB_FILE);
	  dbsnp.set_caching(false);
	  // only keep SNPs for current chromosome in memory
	  // beacoup memory savings
	  config.snp_query = new dbSNPQueryCacher(dbsnp);
	  usable = true;
	} else {
	  System.err.println("WARNING: dbSNP flatfile not found or not readable, can't perform dbSNP lookups: " + config.DBSNP_BLOB_FILE);  // debug
	}
      }

      use_dbsnp = usable;
    }

    rpt = new Reporter();
    if (config.ENABLE_READ_REPORT) rpt_reads = new Reporter();

    Timer call_timer = new Funk.Timer("SNP calling");

    try {
      if (output_filename != null) rpt.set_output_filename(output_filename);

      rpt.add_header(HEADER_NORMAL_SAMPLE);
      rpt.add_header(HEADER_TUMOR_SAMPLE);
      rpt.add_header(HEADER_NAME);
      rpt.add_header(HEADER_CHR);
      rpt.add_header(HEADER_POS);
      rpt.add_header(HEADER_TYPE);
      rpt.add_header(HEADER_SIZE);
      rpt.add_header(HEADER_COVERAGE);
      rpt.add_header(HEADER_PCT_ALT_ALLELE);
      rpt.add_header(HEADER_CHR_ALLELE);
      rpt.add_header(HEADER_ALT_ALLELE);
      rpt.add_header(HEADER_P_VALUE);
      rpt.add_header(HEADER_TEXT);
      rpt.add_header(HEADER_UNIQUE_ALT_READS);
      rpt.add_header(HEADER_REF_NORMAL_COUNT);
      // counts of reference allele for normal reads
      rpt.add_header(HEADER_REF_TUMOR_COUNT);
      // counts of reference allele in tumor reads
      rpt.add_header(HEADER_ALT_NORMAL_COUNT);
      // counts of alternative allele for normal reads
      rpt.add_header(HEADER_ALT_TUMOR_COUNT);
      // counts of alternative allele in tumor reads

      if (true) {
	rpt.add_header(HEADER_COUNT_REF_NORMAL_FWD);
	rpt.add_header(HEADER_COUNT_REF_NORMAL_REV);
	rpt.add_header(HEADER_COUNT_REF_TUMOR_FWD);
	rpt.add_header(HEADER_COUNT_REF_TUMOR_REV);
	rpt.add_header(HEADER_COUNT_VAR_NORMAL_FWD);
	rpt.add_header(HEADER_COUNT_VAR_NORMAL_REV);
	rpt.add_header(HEADER_COUNT_VAR_TUMOR_FWD);
	rpt.add_header(HEADER_COUNT_VAR_TUMOR_REV);
      }

      rpt.add_header(HEADER_ALT_FWD_COUNT);
      rpt.add_header(HEADER_ALT_REV_COUNT);
      rpt.add_header(HEADER_ALT_HAS_RC);
      // reverse-confirmation section

      if (config.ENABLE_BROAD_BASE_TRACKING) {
	rpt.add_header(HEADER_BROAD_COVERAGE);
	rpt.add_header(HEADER_BROAD_REF_NORMAL_COUNT);
	rpt.add_header(HEADER_BROAD_REF_TUMOR_COUNT);
	rpt.add_header(HEADER_BROAD_ALT_NORMAL_COUNT);
	rpt.add_header(HEADER_BROAD_ALT_TUMOR_COUNT);
      }

      if (dbsnp != null) rpt.add_header("dbSNP");

      rpt.add_header(HEADER_UNIQUE_ALT_READ_START);
      rpt.add_header(HEADER_UNIQUE_ALT_READ_START_F);
      rpt.add_header(HEADER_UNIQUE_ALT_READ_START_R);

      rpt.add_header(HEADER_AVG_MAPQ_ALTERNATIVE);

      rpt.add_header(HEADER_SOMATIC_OR_GERMLINE);
      rpt.add_header(HEADER_LOH_FLAG);
      rpt.add_header(HEADER_ALT_RATIO_NORMAL);
      rpt.add_header(HEADER_ALT_RATIO_TUMOR);
      rpt.add_header(HEADER_ALT_RATIO_NORMAL_TUMOR_DIFF);

      rpt.add_header(HEADER_STRAND_SKEW);


      if (config.REPORT_ALT_ALLELE_SAM_TAGS) rpt.add_header(HEADER_SAM_TAGS_ALTERNATIVE);

      //      rpt.add_header("genotypes");
      //      rpt.add_header("Somatic_alteration");
      //      rpt.add_header("Normal_alleles");
      //      rpt.add_header("Tumor_alleles");
      //      rpt.add_header("Somatic_detail");
      //      rpt.add_header("cds_mapping_error");
      //      rpt.add_header("cds_change");
      //      rpt.add_header("cds_change_detail");


      if (config.ENABLE_READ_REPORT) {
	rpt_reads.set_output_filename(config.READ_REPORT_FILENAME);
	rpt_reads.add_header(HEADER_CHR);
	rpt_reads.add_header(HEADER_POS);
	rpt_reads.add_header(HEADER_TYPE);
	rpt_reads.add_header(HEADER_SIZE);
	rpt_reads.add_header(HEADER_READ_NAME);
	rpt_reads.add_header(HEADER_STRAND);
	rpt_reads.add_header(HEADER_TN);
	rpt_reads.add_header(HEADER_REFERENCE_OR_VARIANT);
	rpt_reads.add_header(HEADER_SAM_FLAGS);
	if (config.READ_REPORT_TAGS != null) {
	  ArrayList<String> tags = new ArrayList<String>(config.READ_REPORT_TAGS);
	  Collections.sort(tags);
	  for (String tag : tags) {
	    rpt_reads.add_header("tag_" + tag);
	  }
	}
      }

      if (limited_refseq) init_trackers();

      PROCESSED = 0;
      counter = new Counter();

      //      if (config.STREAMING_MODE || single_sam) {
      if (config.STREAMING_MODE) {
	// if in streaming mode, no chromosome-by-chromosome setup needed
	// (all data will appear ordered properly, by definition)
	get_some();
      } else {
	// query mode:
	// process chromosomes one at a time, pooling iterators for each
	// (and accounting for naming convention clashes)
	// FIX ME:
	// populate chr list from HashSet of .bam-contained chromosomes?

	if (config.SNP_SEARCH_CHR_LIST == null) {
	  HashSet<String> saw_refs = new HashSet<String>();

	  for (SAMFileReader sfr : sfrs) {
	    SAMFileHeader sfh = sfr.getFileHeader();
	    SAMSequenceDictionary ssd = sfh.getSequenceDictionary();
	    HashMap<Chromosome,Integer> lengths = new HashMap<Chromosome,Integer>();
	    for (SAMSequenceRecord ssr : ssd.getSequences()) {
	      String seq_name = ssr.getSequenceName();
	      String std = Chromosome.standardize_name(seq_name);
	      if (config.SKIP_RANDOM_REFERENCE_SEQUENCES && std.toLowerCase().indexOf("_random") > -1) {
		continue;
	      }
	      saw_refs.add(std);
	    }
	  }
	  config.SNP_SEARCH_CHR_LIST = new ArrayList<String>(saw_refs);
	}

	//
	//  check reference sequence list to make sure we have local copies of each
	//
	ArrayList<String> passed = new ArrayList<String>();
	for (String name : config.SNP_SEARCH_CHR_LIST) {
	  int ref_len = ReferenceSequence.NULL_LENGTH;
	  System.err.print("checking local refseq availability for " + name + ": ");  // debug

	  try {
	    ref_len = refseq.get_length(name);
	  } catch (IOException ex) {
	    // error (FAIL)
	  }

	  if (ref_len == ReferenceSequence.NULL_LENGTH) {
	    System.err.println("missing; will NOT search for this sequence.");  // debug
	  } else {
	    System.err.println("ok (" + ref_len + " bytes)");  // debug
	    passed.add(name);
	  }
	}

	if (passed.size() == 0) {
	  System.err.println("ERROR: no local reference sequences available, does your reference database provide them?");  // debug
	  System.exit(1);
	}

	config.SNP_SEARCH_CHR_LIST = passed;
	Collections.sort(config.SNP_SEARCH_CHR_LIST);

	//	System.err.println("query set="+config.SNP_SEARCH_CHR_LIST);  // debug

	for (String ref_name : config.SNP_SEARCH_CHR_LIST) {
	  System.err.println("query mode: processing " + ref_name);  // debug

	  reference_sequence = null;
	  current_reference_name = null;
	  // bleh

	  spi = new SAMPooledIterator();
	  spi.set_genome_version_check(config.CHECK_GENOME_VERSION);
	  // compatibility??
	  spi.set_restrict_reference(ref_name);

	  if (config.SNP_SEARCH_RANGE != null) {
	    // searching a target region only
	    if (config.SNP_SEARCH_RANGE.isValid()) {
	      System.err.println("valid restrict range");  // debug
	      spi.set_restrict_range(config.SNP_SEARCH_RANGE);
	    } else {
	      System.err.println("ERROR: invalid search region!: " + config.SNP_SEARCH_RANGE.start + " " + config.SNP_SEARCH_RANGE.end);  // debug
	      System.exit(1);
	    }
	  }

	  spi.addAll(sfrs);
	  if (spi.prepare()) {
	    if (spi.hasNext()) {
	      // only attempt to process this reference sequence if data available
	      sam = spi;
	      get_some();
	      call_snps(true);
	    }
	    spi.close();
	  } else {
	    System.err.println("ERROR: can't pool iterators for given files!");  // debug
	    System.exit(1);
	  }
	}
      }

      call_snps(true);
      // flush any last pending results

      rpt.close();
      // finish report
      if (config.ENABLE_READ_REPORT) rpt_reads.close();

      if (counter.get_total() > 0) {
	HashMap<String,Long> counts = counter.get_counts();
	ArrayList<String> labels = new ArrayList<String>(counts.keySet());
	Collections.sort(labels);
	System.err.println("skipped reads summary:");  // debug
	for (String label : labels) {
	  System.err.println("  " + label + ": " + counts.get(label));  // debug
	}
      } else {
	System.err.println("no skipped reads");  // debug
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }

    call_timer.finish();

  }

  public SNPConfig get_config() {
    return config;
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SAMStreamingSNPFinder sf = new SAMStreamingSNPFinder();
    ArrayList<SAMResource> srs = new ArrayList<SAMResource>();

    SNPConfig config = sf.get_config();

    ReferenceSequence refseq = null;
    
    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	SAMResource sr = new SAMResource();
	sr.import_data(SAMResourceTags.SAM_URL, argv[++i]);
	sr.detect_sample_id();
	srs.add(sr);
      } else if (argv[i].equals("-convention")) {
	SampleNamingConvention.import_convention(argv[++i]);
      } else if (argv[i].equals("-alt-sam-report")) {
	config.REPORT_ALT_ALLELE_SAM_TAGS = true;
      } else if (argv[i].equals("-jz")) {
	config.MIN_QUALITY = 15;
	config.MIN_ALT_ALLELE_COUNT = 2;
	config.MIN_MINOR_ALLELE_FREQUENCY = 0f;
      } else if (argv[i].equals("-flush")) {
	config.READ_FLUSH_INTERVAL = Integer.parseInt(argv[++i]);
	System.err.println("setting read flush interval to " + config.READ_FLUSH_INTERVAL);
      } else if (argv[i].equals("-dbsnp-file")) {
	config.DBSNP_BLOB_FILE = argv[++i];
      } else if (argv[i].equals("-query-mode")) {
	config.STREAMING_MODE = false;
      } else if (argv[i].equals("-min-quality")) {
	config.MIN_QUALITY = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-min-score")) {
	config.MIN_SCORE = Float.parseFloat(argv[++i]);
	// FIX ME: implementation unfinished
      } else if (argv[i].equals("-min-mapq")) {
	config.MIN_MAPPING_QUALITY = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-unique-filter-coverage")) {
	config.MIN_ALT_ALLELE_COUNT_FOR_FILTER_ENABLE = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-min-unique-alt-reads")) {
	config.MIN_UNIQUE_READ_NAMES_FOR_ALT_ALLELE = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-min-unique-alt-read-start")) {
	config.MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-skip-non-primary")) {
	config.SKIP_NONPRIMARY_ALIGNMENTS = true;
      } else if (argv[i].equals("-allow-non-primary")) {
	config.SKIP_NONPRIMARY_ALIGNMENTS = false;
      } else if (argv[i].equals("-min-coverage")) {
	config.MIN_COVERAGE = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-min-flanking-quality")) {
	config.MIN_FLANKING_QUALITY = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-min-flanking-quality-window")) {
	config.MIN_FLANKING_QUALITY_WINDOW = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-min-alt-allele-count")) {
	config.MIN_ALT_ALLELE_COUNT = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-min-alt-flanking-reads")) {
	config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-min-alt-flanking-reads-window")) {
	config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE_WINDOW = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-min-minor-frequency")) {
	config.MIN_MINOR_ALLELE_FREQUENCY = Float.parseFloat(argv[++i]);
	if (config.MIN_MINOR_ALLELE_FREQUENCY > 1 || config.MIN_MINOR_ALLELE_FREQUENCY < 0) {
	  System.err.println("-min-minor-frequency must be >= 0 and <= 1");  // debug
	  System.exit(1);
	}

      } else if (argv[i].equals("-read-report")) {
	config.ENABLE_READ_REPORT = true;
	config.READ_REPORT_FILENAME = new String(argv[++i]);
      } else if (argv[i].equals("-rr-tags")) {
	config.READ_REPORT_TAGS = new HashSet<String>();
	String[] list = argv[++i].split(",");
	for (int li=0; li < list.length; li++) {
	  config.READ_REPORT_TAGS.add(new String(list[li]));
	}
	//	System.err.println("tags="+ config.READ_REPORT_TAGS);  // debug
      } else if (argv[i].equals("-require-tags")) {
	if (config.sam_tag_filter != null) {
	  System.err.println("ERROR: only one of -require-tags and -optional-tags may be specified");  // debug
	  System.exit(1);
	}
	config.sam_tag_filter = new SAMTagFilter(true);
	config.sam_tag_filter.parse(argv[++i]);
      } else if (argv[i].equals("-optional-tags")) {
	if (config.sam_tag_filter != null) {
	  System.err.println("ERROR: only one of -require-tags and -optional-tags may be specified");  // debug
	  System.exit(1);
	}
	config.sam_tag_filter = new SAMTagFilter(false);
	config.sam_tag_filter.parse(argv[++i]);
      } else if (argv[i].equals("-mmf-disable")) {
	config.ENABLE_MISMATCH_FILTER = false;
      } else if (argv[i].equals("-mmf-max-hq-mismatches")) {
	config.MISMATCH_FILTER_MAX_HQ_MISMATCH_COUNT = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-mmf-min-hq-quality") ||
		 argv[i].equals("-mmf-min-quality")
		 ) {
	if (argv[i].equals("-mmf-min-quality")) System.err.println("WARNING: -mmf-min-quality obsolete, use -mmf-min-hq-quality");  // debug
	config.MISMATCH_FILTER_MIN_HIGH_QUALITY = Integer.parseInt(argv[++i]);

      } else if (argv[i].equals("-mmf-min-lq-quality")) {
	config.MISMATCH_FILTER_MIN_LOW_QUALITY = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-mmf-max-lq-mismatches") ||
		 argv[i].equals("-mmf-max-any-mismatches")
		 ) {
	if (argv[i].equals("-mmf-max-any-mismatches")) System.err.println("WARNING: -mmf-max-any-mismatches obsolete, use -mmf-max-lq-mismatches");  // debug
	config.MISMATCH_FILTER_MAX_LQ_MISMATCH_COUNT = Integer.parseInt(argv[++i]);

      } else if (argv[i].equals("-no-mismap-filter")) {
	config.ENABLE_MISMAP_FILTER = false;
      } else if (argv[i].equals("-mismap-frequency")) {
	config.MISMAP_BASE_FREQUENCY_THRESHOLD = Float.parseFloat(argv[++i]);
      } else if (argv[i].equals("-nib")) {
	NIB.DEFAULT_NIB_DIR = new String(argv[++i]);
	refseq = new NIB();
      } else if (argv[i].equals("-ref-byte")) {
	// debug reference sequence: a single nucleotide in a single chromosome
	// (for test harnesses, to avoid overhead of loading entire chromosome)
	refseq = new ReferenceSequenceByte(argv[++i]);
      } else if (argv[i].equals("-2bit")) {
	try {
	  //	  refseq = new TwoBitFile(argv[++i]);
	  refseq = new TwoBitFileLite(argv[++i]);
	} catch (Exception e) {
	  System.err.println("ERROR initializing reference sequence: " + e);  // debug
	  e.printStackTrace();
	  System.exit(1);
	}
      } else if (argv[i].equals("-fasta")) {
	try {
	  String thing = argv[++i];
	  File f = new File(thing);
	  if (f.isFile()) {
	    // .fai-indexed FASTA file
	    refseq = new FASTAIndexedFAI(thing);
	  } else if (f.isDirectory()) {
	    refseq = new FASTADirectory(thing);
	  } else {
	    System.err.println("ERROR: not a file/directory: " + thing);  // debug
	  }
	} catch (Exception ex) {
	  System.err.println("ERROR: " + ex);  // debug
	  System.exit(1);
	}
      } else if (argv[i].equals("-tree-map")) {
	config.USE_TREEMAP = true;
      } else if (argv[i].equals("-hash-map")) {
	config.USE_TREEMAP = false;
      } else if (argv[i].equals("-no-strand-skew-filter")) {
	config.STRAND_SKEW_FILTER_ENABLE = false;
      } else if (argv[i].equals("-strand-skew-filter-verbose")) {
	StrandSkewFilter.VERBOSE = true;
      } else if (argv[i].equals("-strand-skew-filter-config")) {
	boolean error = false;
	if (i >= argv.length - 1) {
	  error = true;
	} else {
	  // parse
	  String[] things = argv[++i].split(",");
	  if (things.length == 5) {
	    config.STRAND_SKEW_CALL_MIN_STRAND_COVERAGE = Integer.parseInt(things[0]);
	    config.STRAND_SKEW_CALL_MIN_VARIANT_COUNT = Integer.parseInt(things[1]);
	    config.STRAND_SKEW_FILTER_MIN_STRAND_COVERAGE = Integer.parseInt(things[2]);
	    config.STRAND_SKEW_FILTER_MIN_VARIANT_COUNT = Integer.parseInt(things[3]);
	    config.STRAND_SKEW_FILTER_MIN_STRAND_PERCENT_TO_CONSIDER_SKEWED = Double.parseDouble(things[4]);

	    if (config.STRAND_SKEW_FILTER_MIN_STRAND_PERCENT_TO_CONSIDER_SKEWED < 0 ||
		config.STRAND_SKEW_FILTER_MIN_STRAND_PERCENT_TO_CONSIDER_SKEWED > 1) error = true;
	  } else {
	    error = true;
	  }
	}

	if (error) {
	  System.err.println("-strand-skew-filter-config requires comma-delimited list of call_min_strand_coverage,call_min_variant_count,filter_min_strand_coverage,filter_min_variant_count,skew_fraction_to_filter");
	  System.exit(1);
	}
      } else if (argv[i].equals("-no-broad-base-tracking")) {
	config.ENABLE_BROAD_BASE_TRACKING = false;
      } else if (argv[i].equals("-broad-min-quality")) {
	config.BROAD_BASE_TRACKING_MIN_QUALITY = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-no-dbsnp")) {
	sf.set_dbsnp(false);
      } else if (argv[i].equals("-verbose")) {
	SAMStreamingSNPFinder.VERBOSE = true;
      } else if (argv[i].equals("-no-version-check")) {
	sf.set_genome_version_check(false);
      } else if (argv[i].equals("-limit")) {
	config.LIMIT_READ_TRACKING = true;
	config.READ_TRACKING_LIMIT = Integer.parseInt(argv[++i]);
	config.READ_TRACKING_LIMIT_TRIGGER = (int) (config.READ_TRACKING_LIMIT * 1.33);
	System.err.println("trigger="+config.READ_TRACKING_LIMIT_TRIGGER);  // debug
      } else if (argv[i].equals("-debug-memory")) {
	config.DEBUG_SNP_MEMORY_USAGE = true;
      } else if (argv[i].equals("-debug-start-base")) {
	SNPConfig.QUERY_START_BASE = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-ping")) {
	config.READ_PING_INTERVAL = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-chr")) {
	config.SNP_SEARCH_CHR_LIST = new ArrayList<String>();
	String[] stuff = argv[++i].split(",");
	for (String name : stuff) {
	  config.SNP_SEARCH_CHR_LIST.add(Chromosome.standardize_name(name));
	}
	config.STREAMING_MODE = false;
	// implied
	System.err.println("searching reads mapped to: " + config.SNP_SEARCH_CHR_LIST.get(0));  // debug
      } else if (argv[i].equals("-fast-target-refseq")) {
	config.ENABLE_FAST_TARGET_REFSEQ = true;
      } else if (argv[i].equals("-start") || argv[i].equals("-end")) {
	int value = Integer.parseInt(argv[i + 1]);
	System.err.println("val " + value);  // debug
	if (config.SNP_SEARCH_RANGE == null) config.SNP_SEARCH_RANGE = new Range();
	if (argv[i].equals("-start")) {
	  config.SNP_SEARCH_RANGE.start = value;
	} else {
	  config.SNP_SEARCH_RANGE.end = value;
	}
	System.err.println("range valid?: " + config.SNP_SEARCH_RANGE.isValid());  // debug

	i++;
      } else if (argv[i].equals("-tcga-sample-fields")) {
	int fields = Integer.parseInt(argv[++i]);
	if (fields == 3 || fields == 4) {
	  System.err.println("using TCGA sample names of " + fields + " fields");  // debug
	  SNPConfig.TCGA_SAMPLE_FIELD_COUNT = fields;
	} else {
	  System.err.println("invalid TCGA sample field count (3 or 4)");
	  System.exit(1);
	}
      } else if (argv[i].equals("-sample")) {
	if (srs.size() > 0) {
	  String sid = argv[++i];
	  //	  System.err.println("importing sample " + sid);  // debug
	  srs.get(srs.size() - 1).import_data(SAMResourceTags.SAM_SAMPLE, sid);
	} else {
	  System.err.println("ERROR: must specify -sample after -bam");  // debug
	  System.exit(1);
	}
      } else if (argv[i].equals("-tn")) {
	// specifies tumor or normal for preceding SAMResource
	if (srs.size() > 0) {
	  SAMResource sr = srs.get(srs.size() - 1);
	  sr.set_tumor_normal(argv[++i]);
	} else {
	  System.err.println("ERROR: -tn must be specified after -bam");  // debug
	}
      } else if (argv[i].equals("-average-insertion-quality")) {
	config.AVERAGE_INSERTION_QUALITY = true;
      } else if (argv[i].equals("-of")) {
	sf.set_output_file(argv[++i]);
      } else if (argv[i].equals("-force-indel-accept-mapq")) {
	config.SKIP_NT_QUALITY_CHECKS_FOR_HIGH_MAPQ_INDELS = true;
	config.HIGH_MAPQ_INDEL_MIN_MAPQ = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-force-indel-accept-length")) {
	config.SKIP_NT_QUALITY_CHECKS_FOR_HIGH_MAPQ_INDELS = true;
	config.HIGH_MAPQ_INDEL_MIN_LENGTH = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-markup")) {
	try {
	  MarkupReader mr = new MarkupReader(argv[++i]);
	  srs = mr.get_config().sams;
	} catch (Exception e) {
	  System.err.println("error: " + e);  // debug
	  e.printStackTrace();
	  System.exit(1);
	}
      } else if (argv[i].equals("-version")) {
	AceViewer.print_version();
	System.exit(0);
      } else if (argv[i].equals("-no-poly-x-indel-filter")) {
	config.ENABLE_POLY_X_RUN_MASK_INDEL = false;
      } else if (argv[i].equals("-poly-x-min-run-length")) {
	config.POLY_X_MIN_RUN_LENGTH = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-no-poly-x-snp-filter")) {
	config.ENABLE_POLY_X_RUN_MASK_SNP = false;
      } else if (argv[i].equals("-illumina-q2")) {
	int value = Integer.parseInt(argv[++i]);
	config.AUTODETECT_ILLUMINA_QUALITY_2_RUNS = false;
	if (value == 0) {
	  config.ILLUMINA_QUALITY_2_RUN_MODE = false;
	} else if (value == 1) {
	  config.ILLUMINA_QUALITY_2_RUN_MODE = true;
	} else {
	  System.err.println("ERROR: -illumina-q2 must be 0 or 1");  // debug
	  System.exit(1);
	}
      } else if (argv[i].equals("-minor-allele-mode")) {
	config.MINOR_ALLELE_MODE = true;
      } else if (argv[i].equals("-precision")) {
	config.REPORT_DECIMAL_ROUNDING = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-fisher-strand-enable")) {
	config.FISHERS_EXACT_STRAND_BIAS_ENABLE = true;
      } else if (argv[i].equals("-ignore-first-base-mismatches")) {
	config.IGNORE_MISMATCHES_IN_FIRST_READ_BASE = true;
      } else if (argv[i].equals("-ignore-last-base-mismatches")) {
	config.IGNORE_MISMATCHES_IN_LAST_READ_BASE = true;
      } else {
	System.err.println("ERROR: unknown parameter " + argv[i]);  // debug
	System.exit(1);
      }
    }

    if (config.SKIP_NT_QUALITY_CHECKS_FOR_HIGH_MAPQ_INDELS) {
      if (config.HIGH_MAPQ_INDEL_MIN_MAPQ == -1 || 
	  config.HIGH_MAPQ_INDEL_MIN_LENGTH == -1) {
	System.err.println("ERROR: must specify both -force-indel-accept-length and -force-indel-accept-mapq");
	System.exit(1);
      }
    }


    if (refseq == null) {
      System.err.println("ERROR: no reference sequence provided (specify with -2bit or -nib)");  // debug
      System.exit(1);
    } else {
      sf.set_reference_sequence(refseq);
    }

	//	sf.set_reference_sequence(new NIB());


    for (SAMResource sr : srs) {
      System.err.println("sample " + sr.sample.get_sample_name() + ": is_tumor:" + sr.sample.is_tumor());  // debug

    }

    if (srs.size() == 0) {
      System.err.println("ERROR: specify -bam or -markup");  // debug
    } else {
      try {
	sf.set_resources(srs);
	sf.find_snps();
      } catch (Exception e) {
	System.err.println("SAMStreamingSNPFinder error: " + e);  // debug
	e.printStackTrace();
      }
    }

  }

  public String get_flanking_sequence(int ci, boolean before) {
    String result;
    if (before) {
      int blen = SNP_FLANK_LEN;
      int bi = (ci - start_offset) - blen;
      if (bi < 0) {
	blen += bi;
	bi = 0;
      }
      result = new String(reference_sequence, bi, blen);
    } else {
      int len = SNP_FLANK_LEN;
      int start = (ci - start_offset) + 1;
      int end = start + SNP_FLANK_LEN;
      if (end >= reference_sequence.length) len -= (end - reference_sequence.length);
      result = new String(reference_sequence, start, len);
    }
    return result;
  }

  public int get_minimum_flanking_sequence (SAMRecord sr, int read_i) {
    int l_distance = read_i;
    int r_distance = sr.getReadLength() - (read_i + 1);
    System.err.println("ri="+read_i + " l="+l_distance+ " r="+r_distance + " using=" + (l_distance < r_distance ? l_distance : r_distance));
    return l_distance < r_distance ? l_distance : r_distance;
  }

  private void end_row() throws IOException {
    if (config.REPORT_RESULTS) {
      rpt.end_row();
    } else {
      results.add(new SNP2(rpt));
      rpt.reset_row();
    }
  }

  public ArrayList<SNP2> get_results() {
    return results;
  }

  public void set_reference_sequence (ReferenceSequence refseq) {
    this.refseq = refseq;
  }

  private void track_secondary_bases (SAMRecord sr, byte tumor_normal, TumorNormal tumor_normal2) {
    int len, read_i, ref_i, ref_i2, end;
    byte[] read = sr.getReadBases();
    byte[] quals = sr.getBaseQualities();
    BaseCounter3 bc;

    //    System.err.println("ref len="+reference_sequence.length + " start_offset=" + start_offset);  // debug
    Base base;
    Strand strand = Strand.valueOfSAMRecord(sr);
    EnumMap<Base,EnumMap<Strand,EnumMap<TumorNormal,Integer>>> base2strand;
    EnumMap<Strand,EnumMap<TumorNormal,Integer>> strand2tn;
    EnumMap<TumorNormal,Integer> tn2count;
    Integer count;

    for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
      len = ab.getLength();
      read_i = ab.getReadStart() - 1;
      ref_i = ab.getReferenceStart() - 1;
      ref_i2 = ref_i - start_offset;

      if (read_i < 0 || ref_i < 0) continue;

      for (end = read_i + len; read_i < end; read_i++, ref_i++, ref_i2++) {
	if (read_i >= quals.length) {
	  // no quality info / out of range
	  //		System.err.println("SAMStrSNP: no qual info available!");  // debug
	  //	  qual_mapping_problem = true;
	} else if (quals[read_i] >= config.BROAD_BASE_TRACKING_MIN_QUALITY) {
	  //
	  // read quality at this position is good enough to track
	  //
	  if (ref_i2 >= reference_sequence.length) {
	    System.err.println("ERROR: read past end of reference seq");  // debug
	  } else if (ref_i2 < 0) {
	    System.err.println("error: index < 0");  // debug
	    // can happen if we're using only a subset of the reference sequence,
	    // and read has a portion mapped before
	  } else {
	    //
	    //  base is trackable:
	    //
	    // something that irritates me about java --
	    // here's ONE LINE of perl which represents ALL the below nonsense:
	    //
	    // $tracker_broad{$consensus_position}{$base}{$strand}{$tumor_normal} = $count;
	    //

	    base = Base.valueOf((char) read[read_i]);

	    base2strand = tracker_broad.get(ref_i);
	    // level 1: map of bases for consensus reference index
	    if (base2strand == null) {
	      base2strand = new EnumMap<Base,EnumMap<Strand,EnumMap<TumorNormal,Integer>>>(Base.class);
	      tracker_broad.put(ref_i, base2strand);
	    }

	    strand2tn = base2strand.get(base);
	    // level 2: map of strands for consensus->base
	    if (strand2tn == null) {
	      strand2tn = new EnumMap<Strand,EnumMap<TumorNormal,Integer>>(Strand.class);
	      base2strand.put(base, strand2tn);
	    }

	    tn2count = strand2tn.get(strand);
	    // level 3: map of tumor/normal -> count for consensus->base->strand
	    if (tn2count == null) {
	      tn2count = new EnumMap<TumorNormal,Integer>(TumorNormal.class);
	      strand2tn.put(strand, tn2count);
	    }

	    count = tn2count.get(tumor_normal2);
	    // level 4 (whew): consensus->base->strand->tumor_normal -> base count
	    if (count == null) count = Integer.valueOf(0);
	    tn2count.put(tumor_normal2, count + 1);
	    

	    
	  }
	} // MIN_QUALITY
      }
    }
  }

  private void track_broad_indels (SAMIndelFilter sif, SAMRecord sr, byte tumor_normal, TumorNormal tumor_normal2) {
    BaseCounter3 bc;
    
    ArrayList<IndelInfo> broad = sif.get_broad_indels();
    if (broad == null) return;

    //    System.err.println("ref len="+reference_sequence.length + " start_offset=" + start_offset);  // debug
    Strand strand = Strand.valueOfSAMRecord(sr);
    HashMap<String,EnumMap<Strand,EnumMap<TumorNormal,Integer>>> base2strand;
    EnumMap<Strand,EnumMap<TumorNormal,Integer>> strand2tn;
    EnumMap<TumorNormal,Integer> tn2count;
    Integer count;

    for (IndelInfo ii : broad) {
      //
      // $tracker{$consensus_position}{$indel_type}{$strand}{$tumor_normal} = $count;
      //
      
      String indel_type = ii.getTypeHashString();

      base2strand = tracker_broad_indels.get(ii.reference_i);
      // level 1: map consensus reference index -> indel type
      if (base2strand == null) {
	base2strand = new HashMap<String,EnumMap<Strand,EnumMap<TumorNormal,Integer>>>();
	tracker_broad_indels.put(ii.reference_i, base2strand);
      }

      strand2tn = base2strand.get(indel_type);
      // level 2: map of strands for consensus->indel_type
      if (strand2tn == null) {
	strand2tn = new EnumMap<Strand,EnumMap<TumorNormal,Integer>>(Strand.class);
	base2strand.put(indel_type, strand2tn);
      }

      tn2count = strand2tn.get(strand);
      // level 3: map of tumor/normal -> count for consensus->indel_type->strand
      if (tn2count == null) {
	tn2count = new EnumMap<TumorNormal,Integer>(TumorNormal.class);
	strand2tn.put(strand, tn2count);
      }

      count = tn2count.get(tumor_normal2);
      // level 4 (whew): consensus->indel_type->strand->tumor_normal -> base count
      if (count == null) count = Integer.valueOf(0);
      tn2count.put(tumor_normal2, count + 1);
    }
  }


  private void init_trackers() {
    // SNP site tracker:
    if (config.USE_TREEMAP) {
      tracker = new TreeMap<Integer,BaseCounter3>();
      indel_tracker = new TreeMap<Integer,HashMap<String,ArrayList<IndelInfo>>>();
    } else {
      tracker = new HashMap<Integer,BaseCounter3>();
      indel_tracker = new HashMap<Integer,HashMap<String,ArrayList<IndelInfo>>>();
    }

    if (config.ENABLE_BROAD_BASE_TRACKING) {
      tracker_broad = new HashMap<Integer,EnumMap<Base,EnumMap<Strand,EnumMap<TumorNormal,Integer>>>>();
      tracker_broad_indels = new HashMap<Integer,HashMap<String,EnumMap<Strand,EnumMap<TumorNormal,Integer>>>>();
    }
  }

  private void tnrt_report_populate(TumorNormalReferenceTracker tnrt) {
    rpt.set_value(HEADER_REF_NORMAL_COUNT, Integer.toString(tnrt.get_count(ReferenceOrVariant.REFERENCE, TumorNormal.NORMAL)));
    rpt.set_value(HEADER_REF_TUMOR_COUNT, Integer.toString(tnrt.get_count(ReferenceOrVariant.REFERENCE, TumorNormal.TUMOR)));
    rpt.set_value(HEADER_ALT_NORMAL_COUNT, Integer.toString(tnrt.get_count(ReferenceOrVariant.VARIANT, TumorNormal.NORMAL)));
    rpt.set_value(HEADER_ALT_TUMOR_COUNT, Integer.toString(tnrt.get_count(ReferenceOrVariant.VARIANT, TumorNormal.TUMOR)));

    rpt.set_value(HEADER_ALT_FWD_COUNT, Integer.toString(tnrt.get_variant_fwd_count()));
    rpt.set_value(HEADER_ALT_REV_COUNT, Integer.toString(tnrt.get_variant_rev_count()));
    rpt.set_value(HEADER_ALT_HAS_RC, tnrt.get_variant_fwd_reverse_confirmation() ? "1" : "0");

    for (TumorNormal tn : TumorNormalReferenceTracker.all_tn) {
      if (!tn.equals(TumorNormal.UNKNOWN)) {
	for (ReferenceOrVariant rov : TumorNormalReferenceTracker.all_rov) {
	  for (Strand str : TumorNormalReferenceTracker.all_strand) {
	    ArrayList<String> stuff = new ArrayList<String>();
	    stuff.add("count");
	    stuff.add(rov.equals(ReferenceOrVariant.REFERENCE) ? "ref" : "var");
	    stuff.add(tn.equals(TumorNormal.TUMOR) ? "tumor" : "normal");
	    stuff.add(str.equals(Strand.STRAND_POSITIVE) ? "fwd" : "rev");
	    String key = Funk.Str.join("_", stuff);
	    //	    System.err.println("at " + tn + " " + rov + " " + str+  " " + key);  // debug
	    rpt.set_value(key, Integer.toString(tnrt.get_count(rov, tn, str)));
	  }
	}
      }      
    }

    if (config.ENABLE_READ_REPORT) {
      //
      // read-level details
      //
      //      System.err.println("chr="+rpt.get_value(HEADER_CHR));  // debug
      tnrt_read_report(tnrt, ReferenceOrVariant.REFERENCE);
      tnrt_read_report(tnrt, ReferenceOrVariant.VARIANT);
    }
  }

  private void tnrt_read_report (TumorNormalReferenceTracker tnrt, ReferenceOrVariant rov) {
    ArrayList<SNPTrackInfo> stis = tnrt.get_sti(rov);
    if (stis != null) {
      for (SNPTrackInfo sti : stis) {
	rpt_reads.set_value(HEADER_CHR, rpt.get_value(HEADER_CHR));
	rpt_reads.set_value(HEADER_POS, rpt.get_value(HEADER_POS));
	rpt_reads.set_value(HEADER_TYPE, rpt.get_value(HEADER_TYPE));
	rpt_reads.set_value(HEADER_SIZE, rpt.get_value(HEADER_SIZE));
	// clone from main report

	rpt_reads.set_value(HEADER_READ_NAME, sti.sr.getReadName());
	rpt_reads.set_value(HEADER_STRAND, sti.sr.getReadNegativeStrandFlag() ? "-" : "+");
	if (sti.tumor_normal == TumorNormal.NORMAL_BYTE ||
	    sti.tumor_normal == TumorNormal.TUMOR_BYTE
	    ) {
	  rpt_reads.set_value(HEADER_TN, Character.toString((char) sti.tumor_normal));
	} else {
	  rpt_reads.set_value(HEADER_TN, "?");
	}
	rpt_reads.set_value(HEADER_REFERENCE_OR_VARIANT, rov == ReferenceOrVariant.REFERENCE ? "R" : "V");
	rpt_reads.set_value(HEADER_SAM_FLAGS, Integer.toString(sti.sr.getFlags()));

	if (config.READ_REPORT_TAGS != null) {
	  for (String tag : config.READ_REPORT_TAGS) {
	    // set blank default value for each tracked tag
	    rpt_reads.set_value("tag_" + tag, "");
	  }
	  for (SAMTagAndValue tav : sti.sr.getAttributes()) {
	    //	    System.err.println("saw tag " + tav.tag + " " + tav.value);  // debug
	    if (config.READ_REPORT_TAGS.contains(tav.tag)) {
	      // read contains desired tag
	      rpt_reads.set_value("tag_" + tav.tag, tav.value.toString());
	    }
	  }
	}

	try {
	  rpt_reads.end_row();
	} catch (Exception e) {
	  System.err.println("ERROR writing read report: " +e);  // debug
	  e.printStackTrace();
	}
      }
    }
  }



  private void get_some() throws IOException {
    BaseCounter3 bc;
    byte tumor_normal;
    TumorNormal tumor_normal2;
    SAMIndelFilter sif = new SAMIndelFilter(config);
    int i, len, read_i, ref_i, ref_i2, end, as;
    String rn;
    int reads_with_quality_mapping_problem = 0;

    int end_mismatch_max_window = 0;

    Counter ctr = new Counter();

    if (config.ENABLE_END_MISMATCH_FILTER) {
      for (i=0; i < config.END_MISMATCH_FILTER_WINDOWS.length; i++) {
	if (config.END_MISMATCH_FILTER_WINDOWS[i][0] > end_mismatch_max_window)
	  end_mismatch_max_window = config.END_MISMATCH_FILTER_WINDOWS[i][0];
      }
    }

    SAMRecord last_usable_sr = null;

    for (SAMRecord sr : sam) {
      if (VERBOSE) System.err.println("read: " + sr.getReadName() + " at " + sr.getAlignmentStart());  // debug

      if (PROCESSED > 0 && PROCESSED % config.READ_PING_INTERVAL == 0) {
	System.err.print("processed " + PROCESSED + ", skipped " + counter.get_total() + " (" + counter.get_summary() + ")");
	if (last_usable_sr != null) {
	  System.err.print(" last map pos=" + sr.getReferenceName() + ":" + sr.getAlignmentStart());

	}
	System.err.println("");  // debug
      }

      PROCESSED++;

      if (sr == null) {
	System.err.println("ERROR: null SAMRecord, quitting");  // debug
	break;
      }

      if (sr.getDuplicateReadFlag()) {
	// skip optical/pcr duplicates
	counter.increment("optical_pcr_duplicate");
	continue;
      }

      if (sr.getReadUnmappedFlag()) {
	// unmapped reads aren't usable
	counter.increment("unmapped");
	continue;
      }
      
      if (config.SKIP_NONPRIMARY_ALIGNMENTS &&
	  sr.getNotPrimaryAlignmentFlag()) {
	//	System.err.println("skipping non-primary read");
	// debug
	counter.increment("non_primary_alignment");
	continue;
      }

      if (sr.getMappingQuality() < config.MIN_MAPPING_QUALITY) {
	//	System.err.println("skipping read w/insufficient mapq of " + sr.getMappingQuality());  // debug
	counter.increment("low_mapq");
	continue;
      }

      if (config.sam_tag_filter != null &&
	  config.sam_tag_filter.check(sr, counter) == false) continue;

      if (config.AUTODETECT_ILLUMINA_QUALITY_2_RUNS) {
	if (PROCESSED > config.ILLUMINA_QUALITY_2_READ_SCAN_COUNT) 
	  config.AUTODETECT_ILLUMINA_QUALITY_2_RUNS = false;
	// only scan so many reads

	if (!config.ILLUMINA_QUALITY_2_HEADER_CHECKED) {
	  boolean is_illumina = false;
	  if (sfrs != null) {
	    for (SAMFileReader sfr : sfrs) {
	      SAMFileHeader sfh = sfr.getFileHeader();
	      for (SAMReadGroupRecord srgr : sfh.getReadGroups()) {
		String platform = srgr.getPlatform();
		if (platform != null) {
		  platform = platform.toLowerCase();
		  if (platform.indexOf("illumina") > -1) {
		    is_illumina = true;
		    break;
		  }
		}
	      }
	    }
	  }
	  if (is_illumina) {
	    config.ILLUMINA_QUALITY_2_HEADER_PASSED = true;
	  } else {
	    config.AUTODETECT_ILLUMINA_QUALITY_2_RUNS = false;
	  }
	  config.ILLUMINA_QUALITY_2_HEADER_CHECKED = true;
	}

	byte[] quals = sr.getBaseQualities();
	int run_start = -1;
	int run_end = -1;
	boolean qualified_run = false;
	for (int ri=0; ri < quals.length; ri++) {
	  if (quals[ri] == 2) {
	    if (run_start == -1) {
	      run_start = run_end = ri;
	    } else {
	      run_end++;
	    }
	  } else if (run_start != -1 && run_end != -1) {
	    int run_len = (run_end - run_start) + 1;
	    if (run_len >= config.ILLUMINA_QUALITY_2_MIN_RUN_LENGTH) qualified_run = true;
	    //	    System.err.println(sr.getReadName() + " run start="+run_start + " end=" + run_end + " run len = " + run_len);  // debug
	    run_start = run_end = -1;
	  }
	}
	if (run_start != -1 && run_end != -1) {
	  int run_len = (run_end - run_start) + 1;
	  if (run_len >= config.ILLUMINA_QUALITY_2_MIN_RUN_LENGTH) qualified_run = true;
	  //	  System.err.println(sr.getReadName() + " END run start="+run_start + " end=" + run_end + " run len = " + run_len);  // debug
	}

	if (qualified_run && config.ILLUMINA_QUALITY_2_HEADER_PASSED) {
	  config.AUTODETECT_ILLUMINA_QUALITY_2_RUNS = false;
	  // stop scanning
	  config.ILLUMINA_QUALITY_2_RUN_MODE = true;
	  // enable special handling of quality 2 bases in flanking sequence check
	  System.err.println("Note: Illumina quality-2 runs detected, using kinder/gentler flanking sequence quality check");  // debug
	}
      }

      last_usable_sr = sr;

      //      System.err.println("mapq="+ );  // debug

      as = sr.getAlignmentStart();
      if (as > 0) {
	current_ref_index = as - 1;
	if (PROCESSED % config.READ_FLUSH_INTERVAL == 0) {
	  call_snps(false);
	}
      }

      if (false && PROCESSED % 50000 == 0) {
	System.err.println("DEBUG, stopping after 50k reads");  // debug
	break;
      }

      rn = sr.getReferenceName();
	
      if (limited_refseq) {
	// prebuilt
      } else if (current_reference_name == null ||
		 ((config.STREAMING_MODE || single_sam) ? !current_reference_name.equals(rn) : false)) {
	// in streaming or single-sam mode, check to see if name has changed.
	// This requires all files to use EXACTLY the same reference sequence names
	// and be laid out EXACTLY the same way.
	//
	// in query mode, we've already joined queries by chromosome, correcting
	// for variations in reference sequence names, so we only need to check
	// whether current reference sequence has been initialized.
	// 

	if (current_reference_name != null)  {
	  //	  System.err.println("flushing results at refseq change, TEST ME");  // debug
	  call_snps(true);
	}
	  
	Runtime rt = Runtime.getRuntime();
	rt.gc();
	System.err.println("free memory: " + rt.freeMemory());  // debug

	Chromosome chr = Chromosome.valueOfString(rn);

	//	if (chr == null) {
	//	  System.err.println("encountered unusable reference sequence " + rn + ", stopping");  // debug
	//	  break;
	//	}
	//
	// DISABLED: this prevents users with non-chromosome reference sequence names
	// from finding variants.  However the side effect is detection will 
	// continue in streaming mode for ALL reference sequences (i.e. "random"
	// sequences as well).  Use "query mode" to avoid this.

	if (false) {
	  try {
	    System.err.println("pausing before refseq parse...");
	    Thread.sleep(1000 * 120);
	  } catch (InterruptedException e) {}
	  System.err.println("continuing");  // debug
	}

	System.err.print("loading refseq for " + rn + "...");  // debug
	String standardized_name = Chromosome.standardize_name(rn);
	long before = System.currentTimeMillis();

	if (config.ENABLE_FAST_TARGET_REFSEQ &&
	    config.SNP_SEARCH_RANGE != null &&
	    config.SNP_SEARCH_RANGE.isValid()) {
	  int rlen = refseq.get_length(standardized_name);
	  reference_sequence = new byte[rlen];
	  int rstart = config.SNP_SEARCH_RANGE.start - config.FAST_TARGET_REFSEQ_FLANK;
	  int rend = config.SNP_SEARCH_RANGE.end + config.FAST_TARGET_REFSEQ_FLANK;
	  if (rstart < 1) rstart = 1;
	  if (rend > rlen) rend = rlen;

	  int chunk_len = (rend - rstart) + 1;
	  Arrays.fill(reference_sequence, (byte) 'N');
	  byte[] chunk = refseq.get_region(standardized_name, rstart, chunk_len);
	  System.arraycopy(chunk, 0, reference_sequence, rstart - 1, chunk_len);
	  //	  System.err.println("huzzah! " + rstart + " " + rend + " " + chunk_len);  // debug
	  //	  System.exit(1);
	} else {
	  reference_sequence = refseq.get_all(standardized_name);
	}
	long elapsed = System.currentTimeMillis() - before;
	System.err.println("took " + elapsed + " ms");  // debug

	if (false) {
	  try {
	    System.err.println("pausing after refseq parse...");
	    Thread.sleep(1000 * 120);
	  } catch (InterruptedException e) {}
	  System.err.println("continuing");  // debug
	}

	init_trackers();
	mmf = new SAMMismatchFilter(config, reference_sequence, start_offset);
	current_reference_name = rn;
	//	current_reference_label = rn.indexOf("chr") == 0 ? rn : "chr" + rn;
	current_reference_label = standardized_name;
	if (use_dbsnp) dbsnp.set_current_chromosome(chr);
      }

      //
      //  get Sample record associated with this read
      //
      if (has_samples) {
	i = single_sam ? 0 : spi.current_buf_index;
	Sample current_sample = samples[i];
	if (current_sample.is_tumor()) {
	  tumor_normal = TUMOR_BYTE;
	  tumor_normal2 = TumorNormal.TUMOR;
	} else if (current_sample.is_normal()) {
	  tumor_normal = NORMAL_BYTE;
	  tumor_normal2 = TumorNormal.NORMAL;
	} else {
	  tumor_normal = '?';
	  tumor_normal2 = TumorNormal.UNKNOWN;
	}
      } else {
	tumor_normal = '?';
	tumor_normal2 = TumorNormal.UNKNOWN;
      }

      if (config.ENABLE_BROAD_BASE_TRACKING) {
	track_secondary_bases(sr, tumor_normal, tumor_normal2);
      }

      boolean mmf_ok = mmf.filter(sr);
      if (VERBOSE) System.err.println("mismatch filter passed for " + SAMUtils.get_printable_read_name(sr) + " => " + mmf_ok);  // debug

      if (!mmf_ok) continue;
      // room for improvement: examine SNP distribution at the sample level?

      byte[] read = sr.getReadBases();
      byte[] quals = sr.getBaseQualities();

      int ignore_read_i_first, ignore_read_i_last;
      if (sr.getReadNegativeStrandFlag()) {
	// read mapping is 3' -> 5'
	ignore_read_i_first = read.length - 1;
	ignore_read_i_last = 0;
      } else {
	// read mapping is 5' -> 3'
	ignore_read_i_first = 0;
	ignore_read_i_last = read.length - 1;
      }

      //
      //  gather indel info
      //
      if (sif.filter(sr)) {
	if (VERBOSE) System.err.println("SIF hit for " + sr.getReadName());  // debug

	for (IndelInfo ii : sif.get_indels()) {
	  //	    System.err.println("indel hit at " + (ii.reference_i + 1) + " len=" + ii.length);  // debug
	  HashMap<String,ArrayList<IndelInfo>> cons_bucket = indel_tracker.get(ii.reference_i);
	  if (cons_bucket == null) {
	    indel_tracker.put(ii.reference_i, cons_bucket = new HashMap<String,ArrayList<IndelInfo>>());
	  }

	  String key = ii.getTypeHashString();
	  ArrayList<IndelInfo> list = cons_bucket.get(key);
	  if (list == null) cons_bucket.put(key, list = new ArrayList<IndelInfo>());
	  ii.sr = sr;
	  ii.tumor_normal = tumor_normal;
	  list.add(ii);
	  //	    System.err.println("added, count now " + list.size());  // debug

	}
      } else {
	if (VERBOSE) System.err.println("SIF miss for " + sr.getReadName());
      }

      if (config.ENABLE_BROAD_BASE_TRACKING) 
	track_broad_indels(sif, sr, tumor_normal, tumor_normal2);

      
      boolean[] blacklist = null;
      if (config.ENABLE_END_MISMATCH_FILTER) {
	//
	//  ignore mismatches which cluster near read ends.
	//  These may be due to mismapped deletions
	//  (i.e. not enough sequence is left after the deletion
	//  to detect it and map the remainder of the read properly,
	//  so it's left aligned with mismatches)
	//
	int window_end = read.length - end_mismatch_max_window;
	Base read_base;
	boolean[] mismatches = new boolean[read.length];
	Arrays.fill(mismatches, false);
	for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	  //
	  // 1. gather positions of high-quality mismatches within read ends
	  //
	  len = ab.getLength();
	  read_i = ab.getReadStart() - 1;
	  ref_i = ab.getReferenceStart() - 1;
	  ref_i2 = ref_i - start_offset;
	  if (read_i < 0 || ref_i < 0) continue;
	  // hack
	  for (end = read_i + len; read_i < end; read_i++, ref_i++, ref_i2++) {
	    if (read_i >= quals.length ||
		quals[read_i] < config.MIN_QUALITY ||
		// FIX ME: use lower threshold???
		ref_i2 < 0 ||
		ref_i2 >= reference_sequence.length
		) continue;
	    if (read_i < end_mismatch_max_window ||
		read_i >= window_end) {
	      // check bases within max window size of read end for mismatches
	      //	      System.err.println("read_len=" + read.length + " check " + read_i);  // debug

	      read_base = Base.valueOf((char) read[read_i]);
	      if (!read_base.equals((char) reference_sequence[ref_i2])) {
		//		System.err.println("mismatch " + read_base + " " + (char) reference_sequence[ref_i2] + " at " + (ref_i2 + 1));  // debug
		mismatches[read_i] = true;
	      }
	    }
	  }
	}

	//
	//  2. see if detected mismatches exceed thresholds set by config
	//
	for (int wi=0; wi < config.END_MISMATCH_FILTER_WINDOWS.length; wi++) {
	  int window_size = config.END_MISMATCH_FILTER_WINDOWS[wi][0];
	  int total_mismatches = config.END_MISMATCH_FILTER_WINDOWS[wi][1];
	  for (int dir = 0; dir <= 1; dir++) {
	    int si,ei;
	    if (dir == 0) {
	      si = 0;
	      ei = window_size;
	      if (ei > read.length) ei = read.length;
	    } else {
	      si = read.length - window_size;
	      if (si < 0) si = 0;
	      ei = read.length;
	    }

	    int count = 0;
	    //	    System.err.println("si="+si + " ei="+ei + " rlen="+ read.length);  // debug

	    for (i = si; i < ei; i++) {
	      if (mismatches[i]) count++;
	    }

	    if (count >= total_mismatches) {
	      if (blacklist == null) {
		blacklist = new boolean[read.length];
		Arrays.fill(blacklist, false);
	      }
	      for (i = si; i < ei; i++) {
		if (mismatches[i]) {
		  //		  System.err.println("blacklist index " + i + " for " + SAMUtils.get_printable_read_name(sr) + " win_size:" + window_size + " mm:" + total_mismatches);  // debug
		  blacklist[i] = true;
		}
	      }
	    }

	    //	    System.err.println("len="+ read.length + " si " + si + " ei " + ei + " mismatches " + count);  // debug
	  }
	  //	  System.err.println(window_size + " " + total_mismatches);  // debug
	}
      }

      // 
      //  gather SNP-calling information from aligned regions
      //
      boolean qual_mapping_problem = false;
      Base current_base;
      for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	len = ab.getLength();
	read_i = ab.getReadStart() - 1;
	ref_i = ab.getReferenceStart() - 1;
	ref_i2 = ref_i - start_offset;

	if (read_i < 0) {
	  System.err.println("ERROR: can't include " + sr.getReadName() + ", read_i < 0");
	} else if (ref_i < 0) {
	  System.err.println("ERROR: can't include " + sr.getReadName() + ", ref_i < 0");
	} else {
	  //      System.err.println("read: " + new String(read, read_i, len));  // debug

	  //	    System.err.println("read=" + SAMUtils.get_printable_read_name(sr) + " ref_i=" + (ref_i - start_offset) + " len="+ len);
	  //	    System.err.println("read=" + SAMUtils.get_printable_read_name(sr) + " reference=" + (ref_i + 1));

	  for (end = read_i + len; read_i < end; read_i++, ref_i++, ref_i2++) {

	    if (read_i >= quals.length) {
	      // no quality info / out of range
	      //		System.err.println("SAMStrSNP: no qual info available!");  // debug
	      qual_mapping_problem = true;
	    } else if (quals[read_i] >= config.MIN_QUALITY) {
	      //
	      // read quality at this position is good enough to track
	      //
	      if (ref_i2 >= reference_sequence.length) {
		System.err.println("ERROR: read past end of reference seq");  // debug
	      } else if (ref_i2 < 0) {
		System.err.println("error: index < 0");  // debug
		// can happen if we're using only a subset of the reference sequence,
		// and read has a portion mapped before
	      } else {
		// site is mapped within the reference sequence

		if (config.MIN_FLANKING_QUALITY_WINDOW > 0 ?
		    SAMUtils.flanking_quality_check(quals, read_i, 1, config.MIN_FLANKING_QUALITY, config.MIN_FLANKING_QUALITY_WINDOW, config.ILLUMINA_QUALITY_2_RUN_MODE) : true) {
		  //
		  // FIX ME: create masked version of sequence instead, FASTER!!!
		  //

		  //		  System.err.println("  base at " + (ref_i + 1) + ": " + (char) read[read_i] + " qual:" + quals[read_i]);  // debug

		  if (config.ENABLE_END_MISMATCH_FILTER && 
		      blacklist != null &&
		      blacklist[read_i]
		      ) {
		    //		    System.err.println("blacklist hit!");  // debug
		    continue;
		  }

		  bc = tracker.get(ref_i);
		  if (bc == null) {
		    bc = new BaseCounter3();
		    if (config.MINOR_ALLELE_MODE) {
		      // in this mode disregard the formal reference:
		      // "reference" will be the most common allele (consensus)
		    } else {
		      bc.add_base((char) reference_sequence[ref_i2]);
		      // in standard mode, always consider the 
		      // reference sequence an observation
		    }
		    tracker.put(ref_i, bc);
		  }

		  current_base = Base.valueOf((char) read[read_i]);
		  if ((config.IGNORE_MISMATCHES_IN_FIRST_READ_BASE &&
		       read_i == ignore_read_i_first) ||
		      (config.IGNORE_MISMATCHES_IN_LAST_READ_BASE &&
		       read_i == ignore_read_i_last)) {
		    // ignoring mismatches vs. reference in first and/or last base
		    if (!Base.valueOf((char) reference_sequence[ref_i2]).equals(current_base)) {
		      System.err.println("ignoring " +
					 (read_i == ignore_read_i_first ? "first" : "last") +
					 "-base mismatch on " +
					 (sr.getReadNegativeStrandFlag() ? "-" : "+") +
					 " read=" + sr.getReadName() + " pos=" + (ref_i + 1) + " as=" + sr.getAlignmentStart() + " ae=" + sr.getAlignmentEnd()); 
		      continue;
		    }

		  }

		  bc.add_base(current_base,
			      new SNPTrackInfo(sr, read_i, tumor_normal),
			      true);
		} else {
		  //		  System.err.println("flanking quality window fail for " + SAMUtils.get_printable_read_name(sr) + " at " + (ref_i + 1) + " mfq=" + config.MIN_FLANKING_QUALITY + " win=" + config.MIN_FLANKING_QUALITY_WINDOW);  // debug
		}
	      }
		
	    } // >= MIN_QUALITY
	  }
	}
      }

      if (qual_mapping_problem) reads_with_quality_mapping_problem++;
    }  // for SAMRecord...


    if (reads_with_quality_mapping_problem > 0) {
      System.err.println("reads with quality mapping problem: " + reads_with_quality_mapping_problem);  // debug
    }

  }


  private void clean_intmap_through (AbstractMap map, int max_value) {
    //    System.err.println("IMPLEMENT ME");  // debug
    ArrayList<Object> keys = new ArrayList<Object>(map.keySet());
    for (Object o : keys) {
      if (((Integer) o) <= max_value) {
	//	System.err.println("removing " + o);  // debug
	map.remove(o);
      }
    }
  }

  private int gather_counts (EnumMap<Base,EnumMap<Strand,EnumMap<TumorNormal,Integer>>> base2strand, Base b, ReferenceOrVariant rov, TumorNormalReferenceTracker tnrt) {
    EnumMap<Strand,EnumMap<TumorNormal,Integer>> strand2tn;
    EnumMap<TumorNormal,Integer> tn2count;
    int total = 0;

    //    System.err.println("b2s="+base2strand + " b="+ b);  // debug

    strand2tn = base2strand.get(b);
    if (strand2tn != null) {
      for (Strand strand : strand2tn.keySet()) {
	tn2count = strand2tn.get(strand);
	for (TumorNormal tn : tn2count.keySet()) {
	  Integer count = tn2count.get(tn);
	  //	  System.err.println("hey now " + b + " " + strand + " " + tn + " " + count);  // debug
	  total += count;
	  tnrt.add_set(rov, strand, tn, count);
	}
      }
    }

    return total;
  }

  private int gather_counts2 (HashMap<String,EnumMap<Strand,EnumMap<TumorNormal,Integer>>> base2strand, String key, ReferenceOrVariant rov, TumorNormalReferenceTracker tnrt) {
    // bleh; generify?
    EnumMap<Strand,EnumMap<TumorNormal,Integer>> strand2tn;
    EnumMap<TumorNormal,Integer> tn2count;
    int total = 0;

    //    System.err.println("key="+key);  // debug

    if (base2strand == null) {
      //      System.err.println("note: no base2strand for " + key);  // debug
    } else {
      strand2tn = base2strand.get(key);
      if (strand2tn != null) {
	for (Strand strand : strand2tn.keySet()) {
	  tn2count = strand2tn.get(strand);
	  for (TumorNormal tn : tn2count.keySet()) {
	    Integer count = tn2count.get(tn);
	    //	  System.err.println("hey now " + b + " " + strand + " " + tn + " " + count);  // debug
	    total += count;
	    tnrt.add_set(rov, strand, tn, count);
	  }
	}
      }
    }

    return total;
  }

  private void prune_tracker() {
    if (tracker == null) return;

    BaseCounter3 bc3;
    EnumMap<Base,BaseCountInfo> base_map;
    BaseCountInfo bci;
    for (Integer ci : tracker.keySet()) {
      base_map = tracker.get(ci).get_saw_bases();
      for (Base b : base_map.keySet()) {
	bci = base_map.get(b);
	if (bci.sequences.size() >= config.READ_TRACKING_LIMIT_TRIGGER) {
	  System.err.println("pruning " + b + " counts at " + (ci + 1) + ": " + bci.sequences.size() + " reads");  // debug
	  prune_snp_track_info(bci.sequences);
	}
      }
    }
  }

  private void prune_snp_track_info (ArrayList<Object> list_to_prune) {
    HashMap<String,TreeMap<Integer,ArrayList<SNPTrackInfo>>> buckets = new HashMap<String,TreeMap<Integer,ArrayList<SNPTrackInfo>>>();
    TreeMap<Integer,ArrayList<SNPTrackInfo>> bucket2score;
    ArrayList<SNPTrackInfo> track_list;

    boolean verbose_prune = false;

    StringCounter sc = new StringCounter();

    float scale_factor = (float) config.READ_TRACKING_LIMIT / list_to_prune.size();
    if (verbose_prune) System.err.println("scaling needed: " + scale_factor + " " + list_to_prune.size());  // debug

    for (Object o : list_to_prune) {
      // broadly categorize reads based on:
      //   - tumor/normal status
      //   - strand
      //   - unclipped start position (mod 5)
      // this ensures we'll preserve a diversity of properties in trimmed reads.

      SNPTrackInfo sti = (SNPTrackInfo) o;

      String bucket_key = (char) sti.tumor_normal + (sti.sr.getReadNegativeStrandFlag() ? "-" : "+") + (sti.sr.getUnclippedStart() % 5);

      int score = sti.get_quality() * (sti.get_minimum_flanking_sequence() >= config.MIN_ALT_READS_WITH_FLANKING_SEQUENCE_WINDOW ? 100 : 1);

      // within each category,
      // sort by quality score, preferring entries meeting flanking sequence check

      bucket2score = buckets.get(bucket_key);
      if (bucket2score == null) {
	bucket2score = new TreeMap<Integer,ArrayList<SNPTrackInfo>>();
	buckets.put(bucket_key, bucket2score);
      }

      track_list = bucket2score.get(score);
      if (track_list == null) {
	track_list = new ArrayList<SNPTrackInfo>();
	bucket2score.put(score, track_list);
      }
      track_list.add(sti);
      sc.increment(bucket_key);
      if (verbose_prune) System.err.println("key="+bucket_key + " q=" + sti.get_quality() + " score="+score + " spos=" + sti.sr.getUnclippedStart());  // debug
    }

    //
    // trim contents of each bucket by universal scaling factor.
    // 
    // 
    if (verbose_prune) System.err.println("bucket count: " +buckets.keySet().size());  // debug

    ArrayList<SNPTrackInfo> trimmed = new ArrayList<SNPTrackInfo>();

    if (verbose_prune) System.err.println("begin trimming");  // debug

    for (String bucket_key : buckets.keySet()) {
      int bucket_size = sc.get_count_for(bucket_key);
      int after_size = (int) (bucket_size * scale_factor);
      if (after_size < config.READ_TRACKING_LIMIT_BUCKET_EXEMPTION) {
	if (verbose_prune) System.err.println("not trimming " + bucket_key);  // debug
      } else {
	int remove_count = bucket_size - after_size;
	if (verbose_prune) System.err.println("bucket " + bucket_key + "=" + bucket_size + " after=" + after_size + " remove=" + remove_count);
	bucket2score = buckets.get(bucket_key);
	for (Integer score : bucket2score.keySet()) {
	  ArrayList<SNPTrackInfo> set = bucket2score.get(score);
	  for (SNPTrackInfo sti : bucket2score.get(score)) {
	    if (verbose_prune) System.err.print(bucket_key + " " + score);
	    if (remove_count-- > 0) {
	      if (verbose_prune) System.err.println(" pruned");  // debug
	    } else {
	      if (verbose_prune) System.err.println(" kept");  // debug
	      trimmed.add(sti);
	    }
	  }
	}
      }
    }

    if (verbose_prune) System.err.println("final: left=" + trimmed.size());  // debug

    list_to_prune.clear();
    list_to_prune.addAll(trimmed);
  }

  private void populate_sample() {
    if (has_samples) {
      HashSet<String> ns = new HashSet<String>();
      HashSet<String> ts = new HashSet<String>();
      // FIX ME: singleton
      for (int i = 0; i < samples.length; i++) {
	if (samples[i] != null) {
	  String sname = samples[i].get_sample_name();
	  TumorNormal tn = samples[i].get_tumornormal();
	  if (sname != null && tn != null) {
	    if (tn.equals(TumorNormal.NORMAL)) {
	      ns.add(sname);
	    } else if (tn.equals(TumorNormal.TUMOR)) {
	      ts.add(sname);
	    }
	  }
	}
      }
      if (ns.size() > 0) rpt.set_value(HEADER_NORMAL_SAMPLE, Funk.Str.join(",", ns));
      if (ts.size() > 0) rpt.set_value(HEADER_TUMOR_SAMPLE, Funk.Str.join(",", ts));
    }
  }

  private boolean unique_alt_reads_check (TumorNormalReferenceTracker tnrt) {
    boolean ok = true;
    int unique_alt_reads = tnrt.get_unique_read_name_count(ReferenceOrVariant.VARIANT);
    rpt.set_value(HEADER_UNIQUE_ALT_READS, Integer.toString(unique_alt_reads));
    if (unique_alt_reads < config.MIN_UNIQUE_READ_NAMES_FOR_ALT_ALLELE) {
      ok = false;
    }
    return ok;
  }

  private boolean mate_pair_qa (BaseCountInfo bci1, BaseCountInfo bci2) {
    boolean result = true;
    BaseCountInfo set1, set2;
    if (bci1.sequences.size() < bci2.sequences.size()) {
      set1 = bci1;
      set2 = bci2;
    } else {
      set1 = bci2;
      set2 = bci1;
    }

    HashSet<String> set1_ids = new HashSet<String>();
    for (Object o : set1.sequences) {
      set1_ids.add(((SNPTrackInfo) o).sr.getReadName());
      //      System.err.println("hashing " + SAMUtils.get_printable_read_name(((SNPTrackInfo) o).sr));
    }

    ArrayList<Object> set1_delete = new ArrayList<Object>();
    ArrayList<Object> set2_delete = new ArrayList<Object>();

    HashSet<String> bad_ids = new HashSet<String>();

    String rn;
    for (Object o : set2.sequences) {
      rn = ((SNPTrackInfo) o).sr.getReadName();
      if (set1_ids.contains(rn)) {
	//	System.err.println("hey now, disagreement for " + ((SNPTrackInfo) o).sr.getReadName());  // debug
	//	System.err.println("hey now, disagreement for " + SAMUtils.get_printable_read_name(((SNPTrackInfo) o).sr));
	bad_ids.add(rn);
	set2_delete.add(o);
      }
    }

    if (bad_ids.size() > 1) {
      result = false;
      set2.sequences.removeAll(set2_delete);
      set2.count -= set2_delete.size();

      for (Object o : set1.sequences) {
	rn = ((SNPTrackInfo) o).sr.getReadName();
	if (bad_ids.contains(rn)) set1_delete.add(o);
      }
      //      System.err.println("before:" + set1.sequences.size());  // debug
      set1.sequences.removeAll(set1_delete);
      set1.count -= set1_delete.size();
      //      System.err.println("after:" + set1.sequences.size());  // debug

    }
    
    return result;
  }

  private void starts_populate (Reporter rpt, UniqueStartTracker ust) {
    rpt.set_value(HEADER_UNIQUE_ALT_READ_START, Integer.toString(ust.get_unique_read_start_positions()));
    rpt.set_value(HEADER_UNIQUE_ALT_READ_START_F, Integer.toString(ust.get_unique_read_start_positions_fwd()));
    rpt.set_value(HEADER_UNIQUE_ALT_READ_START_R, Integer.toString(ust.get_unique_read_start_positions_rev()));
  }

  private void mapq_populate (Reporter rpt, MapQTracker mapq) {
    rpt.set_value(HEADER_AVG_MAPQ_ALTERNATIVE, Integer.toString(mapq.get_average_mapping_quality()));
  }

  private void sam_tag_populate (Reporter rpt, SAMTagTracker stt) {
    rpt.set_value(HEADER_SAM_TAGS_ALTERNATIVE, Funk.Str.join(" ", stt.get_results()));
  }

  private void somatic_germline_populate() {
    int ref_n_count, ref_t_count, alt_n_count, alt_t_count;
    
    if (config.ENABLE_BROAD_BASE_TRACKING) {
      ref_n_count = Integer.parseInt(rpt.get_value(HEADER_BROAD_REF_NORMAL_COUNT));
      ref_t_count = Integer.parseInt(rpt.get_value(HEADER_BROAD_REF_TUMOR_COUNT));
      alt_n_count = Integer.parseInt(rpt.get_value(HEADER_BROAD_ALT_NORMAL_COUNT));
      alt_t_count = Integer.parseInt(rpt.get_value(HEADER_BROAD_ALT_TUMOR_COUNT));
    } else {
      ref_n_count = Integer.parseInt(rpt.get_value(HEADER_REF_NORMAL_COUNT));
      ref_t_count = Integer.parseInt(rpt.get_value(HEADER_REF_TUMOR_COUNT));
      alt_n_count = Integer.parseInt(rpt.get_value(HEADER_ALT_NORMAL_COUNT));
      alt_t_count = Integer.parseInt(rpt.get_value(HEADER_ALT_TUMOR_COUNT));
    }

    GermlineSomaticLOH gsl = new GermlineSomaticLOH();
    //    System.err.println("DEBUG: gsl for " + rpt.get_value(HEADER_NAME));  // debug

    if (gsl.call(ref_n_count, ref_t_count, alt_n_count, alt_t_count)) {
      String value = "";
      if (gsl.is_somatic()) {
	value = "S";
      } else if (gsl.is_germline()) {
	value = "G";
      }
      rpt.set_value(HEADER_SOMATIC_OR_GERMLINE, value);
      //      rpt.set_value(HEADER_LOH_FLAG, gsl.is_loh() ? "LOH" : "");
      String loh_desc = "";
      if (gsl.is_loh()) {
	int code = gsl.get_loh_type();
	if (code == GermlineSomaticLOH.LOH_LOSS_OF_REFERENCE_ALLELE) {
	  loh_desc = "LOH_reference";
	} else if (code == GermlineSomaticLOH.LOH_LOSS_OF_VARIANT_ALLELE) {
	  loh_desc = "LOH_variant";
	} else {
	  System.err.println("ERROR: unhandled LOH code " + code);  // debug
	  loh_desc = "LOH_error_fix_me";
	}
      }
      rpt.set_value(HEADER_LOH_FLAG, loh_desc);
    } else {
      // insufficient data
      rpt.set_value(HEADER_SOMATIC_OR_GERMLINE, "0");
      rpt.set_value(HEADER_LOH_FLAG, "0");
    }

    rpt.set_value(HEADER_ALT_RATIO_NORMAL, Funk.Str.float_decimal_format(gsl.get_alt_freq_normal(), config.REPORT_DECIMAL_ROUNDING));
    rpt.set_value(HEADER_ALT_RATIO_TUMOR, Funk.Str.float_decimal_format(gsl.get_alt_freq_tumor(), config.REPORT_DECIMAL_ROUNDING));
    rpt.set_value(HEADER_ALT_RATIO_NORMAL_TUMOR_DIFF, Funk.Str.float_decimal_format(gsl.get_alt_tn_freq_diff(), config.REPORT_DECIMAL_ROUNDING));

  }

  private boolean skew_populate (TumorNormalReferenceTracker tnrt) {
    StrandSkewFilter ssf = new StrandSkewFilter(config, tnrt);
    boolean result = ssf.skew_test();
    String value = "";
    if (ssf.is_evaluatable()) {
      value = Funk.Str.float_decimal_format(ssf.get_normalized_variant_plus_strand_frequency(), config.REPORT_DECIMAL_ROUNDING);
    } else {
      //      System.err.println("can't evaluate, reason is " + ssf.explain_unevaluatable());  // debug
    }
    rpt.set_value(HEADER_STRAND_SKEW, value);
    return result;
  }

  private boolean fishers_exact_strand_bias_check (TumorNormalReferenceTracker tnrt) {
    boolean result = true;
    int reference_fwd = 0;
    int reference_rev = 0;
    int variant_fwd = 0;
    int variant_rev = 0;

   if (config.FISHERS_EXACT_DATA_TYPE.equals(TumorNormal.UNKNOWN)) {
      //
      // include data for both normal and tumor
      //
      ArrayList<TumorNormal> tns = new ArrayList<TumorNormal>();
      tns.add(TumorNormal.NORMAL);
      tns.add(TumorNormal.TUMOR);

      for (TumorNormal tn : tns) {
	reference_fwd += tnrt.get_count(ReferenceOrVariant.REFERENCE, tn, Strand.STRAND_POSITIVE);
	reference_rev += tnrt.get_count(ReferenceOrVariant.REFERENCE, tn, Strand.STRAND_NEGATIVE);
	variant_fwd += tnrt.get_count(ReferenceOrVariant.VARIANT, tn, Strand.STRAND_POSITIVE);
	variant_rev += tnrt.get_count(ReferenceOrVariant.VARIANT, tn, Strand.STRAND_NEGATIVE);
      }
    } else {
     //
     //  use counts only from the specified sample type
     //
     reference_fwd = tnrt.get_count(ReferenceOrVariant.REFERENCE,
				    config.FISHERS_EXACT_DATA_TYPE,
				    Strand.STRAND_POSITIVE);
     reference_rev = tnrt.get_count(ReferenceOrVariant.REFERENCE,
				    config.FISHERS_EXACT_DATA_TYPE,
				    Strand.STRAND_NEGATIVE);
     variant_fwd = tnrt.get_count(ReferenceOrVariant.VARIANT,
				    config.FISHERS_EXACT_DATA_TYPE,
				    Strand.STRAND_POSITIVE);
     variant_rev = tnrt.get_count(ReferenceOrVariant.VARIANT,
				    config.FISHERS_EXACT_DATA_TYPE,
				    Strand.STRAND_NEGATIVE);
   }

   if (
       // ONLY run test if:
       (variant_fwd == 0 || variant_rev == 0) &&
       // variant is not already observed on both strands, AND
       ((reference_fwd + variant_fwd) > 0 && (reference_rev + variant_rev) > 0)
       // we have read coverage on both strands
       ) {
     FisherExact fe = new FisherExact(reference_fwd + reference_rev +
				      variant_fwd + variant_rev);
     double p = fe.getTwoTailedP(reference_fwd, reference_rev,
				 variant_fwd, variant_rev);
     if (VERBOSE) System.err.println("Fisher's exact: " + reference_fwd + " " + reference_rev + " " + variant_fwd + " " + variant_rev + " => " + p);  // debug

     if (p < config.FISHERS_EXACT_STRAND_BIAS_CUTOFF) result = false;
   }

    return result;
  }


}
