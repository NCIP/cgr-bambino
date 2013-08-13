package Ace2;

import java.io.*;
import java.util.*;
import java.awt.event.MouseEvent;
import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;

public class AceViewerConfig {
  //
  // startup options and default overrides
  //

  public static String VERSION = "1.07_dev";
  //  public static String VERSION = "1.06";
  // program version
  // 1.04: released 2012-01-12
  // 1.05: quasi-released 2012-03-09 (my last day at NCI)
  // 1.06: given to Richard Finney for release 2013-02-01
  //       security access exception bug fixed 2013-02-06

  public boolean REFERENCE_SEQUENCE_PREPADDED = false;

  public boolean ENABLE_QUALITY_PAINT = true;

  SAMTagFilter sam_tag_filter = null;
  // whether displayed reads are required to have specified tags and values

  boolean SHADE_SEQUENCE_IDENTIFIERS_BY_MAPQ = false;
  // if set, color intensity of sequence identifiers is based on mapping quality

  HeteroSummary hetero_summary = null;

  public boolean COMPRESS_INTRONS = false;
  IntronCompressor intron_compressor = null;

  int DEFAULT_INITIAL_VIEW_NT = 2500;

  int MINIMUM_MAPQ_FOR_DISPLAY = 0;

  SNPConfig snp_config = new SNPConfig();

  boolean ENABLE_TRACE_VIEWER = false;

  public SAMRegion region;

  public ReferenceSequence reference_sequence = null;

  String VARIABLE_FONT_TYPEFACE = "SansSerif";
  // guaranteed
  String FIXED_FONT_TYPEFACE = "Monospaced";
  // guaranteed

  String filename;
  // .ace file: filename or stream

  String title = null;

  String CONSENSUS_TAG = "Consensus";
  // semi-constant

  HashMap<String,Sample> read2sample = null;

  ArrayList<RefGene> refgenes = null;
  ArrayList<dbSNP> dbsnp = null;

  boolean ENABLE_HETERO_SUMMARY = true;
  int HETERO_SUMMARY_LINES = 3;

  boolean DETECT_SNP_TABLE_NAME = true;
  boolean DETECTED_SNP_TABLE_NAME = false;

  boolean ENABLE_OVERVIEW = true;
  int OVERVIEW_LINES = 2;
  boolean OVERVIEW_INVERTED = false;

  int ruler_start = 0;

  public int start_padded_offset = 0;
  public int start_unpadded_offset = 0;
  // start viewer centered on these coordinates

  public HashMap<Integer,Integer> intron_trim_sites = null;
  // for intron trimming mode, padded positions of trim sites

  FASTAQualityReader fq;

  public Assembly assembly;

  public boolean GENERATE_CONSENSUS = false;

  boolean ENABLE_JDBC = true;

  public boolean EXIT_ON_CLOSE = true;
  // for local file more

  boolean enable_exon_navigation = true;
  boolean ACE_AUTOCONTIG = true;
  boolean COUNTER_UNPADDED = true;
  // unpadded mode does not include gap characters in ruler's nucleotide count

  public byte[] target_sequence = null;
  // raw/unpadded target sequence (pre-consensus)

  public ArrayList<SAMResource> sams = new ArrayList<SAMResource>();

  MarkupReader markup;

  boolean CLAMP_SNP_VIEW = false;
  // when browsing SNPs, restrict view to only reads aligned at SNP site
  boolean CLAMP_SNP_VIEW_NONREFERENCE = false;
  // when browsing SNPs, restrict view to only reads aligned at SNP site 
  // having non-reference base
  int clamp_cons_start;

  boolean CLAMP_MANUAL_NONREFERENCE = true;
  // feature enable/disable, not whether it's in effect
  int clamp_nonref_cons_pos, clamp_nonref_cons_start;

  boolean CLAMP_MANUAL_VIEW = true;
  // feature enable/disable, not whether it's in effect
  int manual_clamp_cons_pos, manual_clamp_cons_start;

  boolean LOCK_VIEW_TO_ALIGNED_SEQUENCES = false;
  // if true, restrict viewer so it can't move before first aligned sequence 
  // or after last aligned sequence

  public boolean LOAD_OPTICAL_PCR_DUPLICATES = true;
  // whether to load optical/pcr duplicates from BAM file
  
  public boolean HIDE_SKIP_ONLY_ALIGNMENTS = false;

  boolean HIDE_OPTICAL_PCR_DUPLICATES = true;
  // whether to hide optical/pcr duplicates from display by default

  public boolean LOCAL_FILE_MODE = false;

  JDBCCache ucsc = null;

  int DRAG_BUTTON_MASK = MouseEvent.BUTTON1_MASK;

  int DRAG_SCALE_FACTOR = 5;
  // scaling factor applied to mouse dragging.
  // Higher values dampen drag effect.  A value of 0 is the fastest
  // but can be somewhat disorienting.

  public boolean ENABLE_VARIABLE_WIDTH_FONTS = true;
  boolean ENABLE_VARIABLE_WIDTH_RULER = true;

  boolean REFSEQ_LABEL_GENE_SYMBOL_FIRST = true;

  String CHROMAT_DIR = null;

  boolean UPPERCASE_REFERENCE_SEQUENCE = false;

  String BLAT_GENOME = "hg19";

  boolean DEFAULT_VIEW_INDELS_ONLY = false;

  int SKIP_END_BASENUM = 0;

  public JDBCCache get_ucsc_genome_client() {
    if (ucsc == null) ucsc = JDBCCache.get_ucsc_genome_client();
    return ucsc;
  }

  public void set_dbsnp (ArrayList<dbSNP> dbsnp) {
    this.dbsnp = dbsnp;
    if (dbsnp == null) {
      snp_config.snp_query = null;
    } else {
      snp_config.snp_query = new dbSNPSet(dbsnp);
      // hacktacular
    }
  }

  public void region_default_setup() throws IOException {
    if (!region.isValid()) {
      //
      //  no valid startup region specified
      //

      //      System.err.println("region default setup!");  // debug
      boolean set_to_first_read = true;

      if (region.tname != null) {
	//
	// user has provided a target reference sequence name.
	// if valid, start at the first aligned reads for that sequence.
	// if invalid, default to first read position below.
	//
	SAMResource sres = sams.get(0);
	sres.set_region(region);
	int end = region.range.end;
	// may be altered by call below
	//	      System.err.println("end="+end);  // debug

	CloseableIterator <SAMRecord> iterator = sres.get_iterator();
	while (iterator.hasNext()) {
	  SAMRecord sr = iterator.next();
	  if (sr.getReadUnmappedFlag()) continue;
	  region.tname = sr.getReferenceName();
	  region.range.start = sr.getAlignmentStart();

	  //		System.err.println("start="+region.range.start + " spec end="+end);  // debug

	  if (end > region.range.start) {
	    //		  System.err.println("end view OK!");  // debug
	    //		  System.err.println("end="+end);  // debug
	    region.range.end = end;
	  } else {
	    region.range.end = region.range.start + DEFAULT_INITIAL_VIEW_NT;
	  }
	  set_to_first_read = false;
	  break;
	}
	sres.close();
      }
	    
      if (set_to_first_read) {
	//
	//  set start position to location of first aligned reads in 1st file.
	//  HACK: would be "nicer" to pool all files and go to first reads there.
	//
	//	System.err.println("setting to first read");  // debug
	SAMFileReader sfr = sams.get(0).getSAMFileReader();
	for (SAMRecord sr : sfr) {
	  if (sr.getReadUnmappedFlag()) continue;
	  region.tname = sr.getReferenceName();
	  //	  System.err.println("  using ref name: " + region.tname);  // debug

	  region.range.start = sr.getAlignmentStart();
	  region.range.end = region.range.start + DEFAULT_INITIAL_VIEW_NT;
	  break;
	}
      }
    }
  }



}
