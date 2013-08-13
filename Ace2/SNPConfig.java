package Ace2;

import java.util.*;

public class SNPConfig {
  //
  // configuration for SNP-finding
  //
  //  private int READ_FLUSH_INTERVAL = 500;
  int READ_FLUSH_INTERVAL = 1000;

  float MIN_SCORE = 0;
  
  int MIN_QUALITY = 10;
  //  float MIN_MINOR_ALLELE_FREQUENCY = 0.15f;
  // one gotcha with this: what if SNPs are heterozygous and there are multiple samples??
  float MIN_MINOR_ALLELE_FREQUENCY = 0.05f;
  // 7/2011: probably too high, casual users seem to use defaults 

  int MIN_ALT_ALLELE_COUNT = 3;
  int MIN_COVERAGE = 4;

  int MIN_ALT_ALLELE_COUNT_FOR_FILTER_ENABLE = MIN_ALT_ALLELE_COUNT * 2;

  int MIN_ALT_READS_WITH_FLANKING_SEQUENCE = 1;
  int MIN_ALT_READS_WITH_FLANKING_SEQUENCE_WINDOW = 10;
  // require at least 1 read covering variant to have flanking sequence of 10

  //  int MIN_UNIQUE_READ_NAMES_FOR_ALT_ALLELE = 1;
  int MIN_UNIQUE_READ_NAMES_FOR_ALT_ALLELE = 1;
  // sometimes both reads of a mate pair are mapped to the same region.
  // in this case the read might provide multiple observations of an
  // alternative allele.  Use to specify the required minimum number of
  // unique read names showing the alternative allele.

  boolean REPORT_ALT_ALLELE_SAM_TAGS = false;
  // enable reporting of summary SAM tag statistics for reads showing alternate allele

  boolean ENABLE_MISMAPPED_DELETION_FILTER = true;
  // detects reads with possible mismapped deletions near ends of reads
  // (putative SNPs detected within deletion sites and near ends of reads)
  int MISMAPPED_DELETION_MIN_DISTANCE_FROM_END = 10;
  // if a putative SNP occurs within a called deletion, disqualify reads
  // where SNP site is within this many bases from read end (i.e. deletion 
  // occurs too close to end of read for other side to be mapped properly)
  //
  // example:
  //     491cc146166f3738859aabe26f5b0bf5.markup at 100350650
  //     4-base deletion mismapped in some reads near ends (not enough seq after to map properly!)
  //
  // found,interesting,possibly_fixable,reason_not_found,gene,unknown,source,,chr,start,end,link
  // 0,2,1,deletion position reporting error?  ACGT deletion starts +1 nt ahead,ADH6,130,genome.wustl.edu,36,4,100350649,100350649,https://cgwb-test.nci.nih.gov:8443/cgi-bin/bambino?center=100350649;chr=chr4;local_file=/tcga_next_gen/NG_buckets/bucket6/bam//TCGA-04-1348-01A-01W-0494-09_capture.bam,/tcga_next_gen/NG_buckets/bucket7/bam//TCGA-04-1348-11A-01W-0494-09_capture.bam
  // 

  boolean ENABLE_FAST_TARGET_REFSEQ = false;
  // when processing a restricted region, instead of loading an entire reference
  // sequence into memory, just load a flanking region around the target.
  // For fast reference sequence implementations (i.e. FASTA and not .2bit) should be 
  // a lot faster than loading the entire sequence into RAM.
  int FAST_TARGET_REFSEQ_FLANK = 500;
  // should be longer than the maximum possible short read length

  boolean ENABLE_MATE_PAIR_DISAGREEMENT_FILTER = true;
  // if multiple reads from a mate pair are present
  // at a SNP site and disagree, discard for purposes of SNP calling
  // TO DO: implement for INDELS TOO

  boolean ENABLE_END_MISMATCH_FILTER = true;
  //  int[][] END_MISMATCH_FILTER_WINDOWS = { {10, 3}, {6, 2} };
  int[][] END_MISMATCH_FILTER_WINDOWS = {
    {10, 3},
    {6, 2},
    //    {1, 1}
    // probably too aggressive; use MISMAPPED_DELETION_FILTER instead.
    // a specific filter for SNPs inside deletion sites might be better.
    // example: 491cc146166f3738859aabe26f5b0bf5.markup @ 100350260
    // there is additional evidence for the variant, by simply ignoring 1st
    // base we might be discarding some of the best evidence.
  };
  // { window_size, excess_mismatch_count }
  // { 10, 3 } = 3+ mismatches in 1st/last 10 nt
  // { 6, 2 }  = 2+ mismatches in 1st/last 6 nt
  // { 1, 1 }  = mismatch in 1st/last nt
  //
  // defense against reads with clusters of mismatches near ends of reads.
  // can be caused by mismapped deletions.
  //

  int MIN_FLANKING_QUALITY = 15;
  // FIX ME: revise to lower value??
  // 3/2010: 15 may be too strict for low-coverage areas...

  int MIN_FLANKING_QUALITY_WINDOW = 5;
  // require bases within +/- window to be at least given quality

  int MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE = 2;
  //    int MIN_UNIQUE_START_POSITIONS_FOR_ALT_ALLELE = 4;

  boolean REPORT_RESULTS = true;
  boolean CHECK_GENOME_VERSION = true;

  String DBSNP_BLOB_FILE = "/h1/edmonsom/dbsnp_binary_v2.blob";

  boolean STREAMING_MODE = true;

  int READ_PING_INTERVAL = 1000000;
  // progress report

  boolean LIMIT_READ_TRACKING = false;
  int READ_TRACKING_LIMIT = -1;
  int READ_TRACKING_LIMIT_TRIGGER = -1;
  int READ_TRACKING_LIMIT_BUCKET_EXEMPTION = 10;

  boolean DEBUG_SNP_MEMORY_USAGE = false;

  int REPORT_DECIMAL_ROUNDING = 3;

  static int TCGA_SAMPLE_FIELD_COUNT = 4;
  // hack
  // change to 4 to report 4-field TCGA sample name rather than 3-field patient/donor name

  static int QUERY_START_BASE = 1;
  // override only for debugging
  // (static: lazy)

  boolean AUTODETECT_ILLUMINA_QUALITY_2_RUNS = true;
  // if set, attempts to guess whether data has been processed by Illumina GA pipeline
  // (see below), which uses runs of quality 2 to indicate masked regions.
  // If this is the case the behavior of flanking quality check will be modified.
  //
  //
  // 
  // http://seqanswers.com/forums/showthread.php?t=4721
  // // Originally Posted by Bio.X2Y 
  // I'm having a look at some FASTQ files generated from the Illumina GA pipeline (I think version 1.3).
  // (2) is it normal to have a non-zero lower bound for observed quality scores (in our case, 2)?
  // (3) is there an obvious reason why none of our bases has a quality of 3, even though every other quality in the range 2 to 34 is highly represented?
  // Perhaps it is related to this new 'feature' of Pipeline 1.3+? See SLIDE 17 in http://docs.google.com/fileview?id=0...NTUyNDE3&hl=en. Here is the text of the slide:
  // "The Read Segment Quality Control Indicator: At the ends of some reads, quality scores are unreliable. Illumina has an algorithm for identifying these unreliable runs of quality scores, and we use a special indicator to flag these portions of reads A quality score of 2, encoded as a "B", is used as a special indicator. A quality score of 2 does not imply a specific error rate, but rather implies that the marked region of the read should not be used for downstream analysis. Some reads will end with a run of B (or Q2) basecalls, but there will never be an isolated Q2 basecall."
  int ILLUMINA_QUALITY_2_READ_SCAN_COUNT = 10000;
  // set this pretty high as many reads will likely be tossed before reaching scan point
  int ILLUMINA_QUALITY_2_MIN_RUN_LENGTH = 15;
  // count of consecutive nt of quality 2 to consider this processing has been performed
  boolean ILLUMINA_QUALITY_2_HEADER_CHECKED = false;
  boolean ILLUMINA_QUALITY_2_HEADER_PASSED = false;
  // header shows evidence of Illumina platform
  boolean ILLUMINA_QUALITY_2_RUN_MODE = false;
  // header and data show evidence of Illumina quality-2 runs, 
  // modify flanking sequence quality filter

  boolean AVERAGE_INSERTION_QUALITY = false;
  // for purposes of the sequence quality check, use average of inserted bases
  // rather than checking each nucleotide.  Some Illumina assemblies seem
  // to have low-quality bases in insertions for reasons unknown.

  boolean SKIP_NT_QUALITY_CHECKS_FOR_HIGH_MAPQ_INDELS = false;
  int HIGH_MAPQ_INDEL_MIN_LENGTH = -1;
  // length of indel in nt
  int HIGH_MAPQ_INDEL_MIN_MAPQ = -1;
  // minimum mapq to force acceptance of indel observation

  ArrayList<String> SNP_SEARCH_CHR_LIST = null;
  boolean SKIP_RANDOM_REFERENCE_SEQUENCES = true;

  Range SNP_SEARCH_RANGE = null;

  //
  //  mismap detection filter:
  //
  boolean ENABLE_MISMAP_FILTER = true;
  float MISMAP_BASE_FREQUENCY_THRESHOLD = 0.50f;
  // ratio of count of suspicious bases identified by mismap filter
  // to usable bases in putative SNP.  i.e. using a value of 0.5,
  // a putative SNP with 10 usable reads for a base would be failed
  // if 5 or more suspicious reads with the same base were identified.
  
  boolean ENABLE_READ_REPORT = false;
  // separate output file showing read-level details for each variant
  String READ_REPORT_FILENAME = null;

  HashSet<String> READ_REPORT_TAGS = null;

  SAMTagFilter sam_tag_filter = null;
  // whether reads used in SNP detection are required to have
  // specified tags and values

  boolean SKIP_NONPRIMARY_ALIGNMENTS = true;

  //  int MIN_MAPPING_QUALITY = 0;
  int MIN_MAPPING_QUALITY = 1;
  // 9/26/11
  // see http://www.ncbi.nlm.nih.gov/pubmed/21856737

  //
  // parameters affecting SAMMismatchFilter:
  //
  boolean ENABLE_MISMATCH_FILTER = true;
  //  int MISMATCH_FILTER_MAX_HQ_MISMATCH_COUNT = 2;
  int MISMATCH_FILTER_MAX_HQ_MISMATCH_COUNT = 3;
  int MISMATCH_FILTER_MIN_HIGH_QUALITY = 15;

  //  int MISMATCH_FILTER_MAX_LQ_MISMATCH_COUNT = 4;
  int MISMATCH_FILTER_MAX_LQ_MISMATCH_COUNT = 6;
  //  int MISMATCH_FILTER_MIN_LOW_QUALITY = 0;
  int MISMATCH_FILTER_MIN_LOW_QUALITY = 3;
  // quality <= 2 is useless in Solexa (JZ 6/2010)

  boolean MISMATCH_FILTER_FORGIVE_DBSNP_MATCHES = true;
  // if set, don't include mismatches to the reference sequence
  // which match dbSNP entries

  dbSNPQuery snp_query;
  // hack: duplicated here and in AceViewerConfig.

  boolean ENABLE_BROAD_BASE_TRACKING = true;
  // if set, adds additional "broad" columns to SNP flatfile output
  // which may be useful for genotype calling in low-coverage areas.
  //
  // This works by tracking all bases having quality MIN_QUALITY or
  // better at each consensus position.  This adds additional overhead.
  int BROAD_BASE_TRACKING_MIN_QUALITY = 15;
  // minimum nucleotide quality level for broad base tracking

  boolean USE_TREEMAP = false;
  // which is faster for our purposes:
  //   - HashMap: faster access, but need to sort keys?
  //   - TreeMap: slower access, but keys already sorted?

  boolean STRAND_SKEW_FILTER_ENABLE = true;
  //
  //  minimums required to make a skew call:
  //
  int STRAND_SKEW_CALL_MIN_STRAND_COVERAGE = 10;
  // minimum coverage for each of + and - strand
  int STRAND_SKEW_CALL_MIN_VARIANT_COUNT = 3;
  // minimum observations of variant; if variant at very low frequency,
  // might cluster on +/- just by chance.  So this is nearly useless
  // with only a few observations.  On the other hand, if the assembly 
  // is fundamentally low coverage...

  //
  //  minimums required to disqualify a call (high confidence):
  //
  int STRAND_SKEW_FILTER_MIN_STRAND_COVERAGE = 20;
  int STRAND_SKEW_FILTER_MIN_VARIANT_COUNT = 6;
  double STRAND_SKEW_FILTER_MIN_STRAND_PERCENT_TO_CONSIDER_SKEWED = 0.95f;
  // fraction of reads exclusively on one strand

  boolean ENABLE_POLY_X_RUN_MASK_INDEL = true;
  // disqualify size-1 indels occurring within poly-X runs
  int POLY_X_RUN_INDEL_MAX_LENGTH = 1;

  boolean ENABLE_POLY_X_RUN_MASK_SNP = true;
  // If a mismatch occurs within a poly-X region, consider it 
  // a low-quality mismatch for purposes of high-quality/low-quality 
  // counts regardless of base quality score.

  //  int POLY_X_MIN_RUN_LENGTH = 5;
  // original value; some false negatives (very bad)
  int POLY_X_MIN_RUN_LENGTH = 8;
  // run length affects between both POLY_X options above

  boolean MINOR_ALLELE_MODE = false;
  // 8/2012: in this mode, override the formal/given reference sequence
  // with the most frequently-observed allele.  Useful for the
  // explicit study of minor alleles, esp. when populations
  // studied may not perfectly match the formal/database reference
  // sequence (e.g. influenza)

  boolean FISHERS_EXACT_STRAND_BIAS_ENABLE = false;
  // if enabled, *REPLACES* strand skew filter for purposes of rejecting variants
  // (skew info will still be populated in report though.)
  // double FISHERS_EXACT_STRAND_BIAS_CUTOFF = 0.05f;
  double FISHERS_EXACT_STRAND_BIAS_CUTOFF = 0.10f;
  TumorNormal FISHERS_EXACT_DATA_TYPE = TumorNormal.UNKNOWN;
  // which reads to include in the Fisher's exact test:
  // tumor only, normal only, or both (UNKNOWN)?

  boolean IGNORE_MISMATCHES_IN_FIRST_READ_BASE = false;
  // for variant detection purposes, ignore mismatches vs. the
  // reference in the FIRST (5') base of the sequence as these
  // are unreliable (JZ)

  boolean IGNORE_MISMATCHES_IN_LAST_READ_BASE = false;
  // for variant detection purposes, ignore mismatches vs. the reference
  // in the LAST (3') base of the sequence: highest risk of untrimmed 
  // adapter sequence (which may actually be several bases)

}
