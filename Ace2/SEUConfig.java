// config for SAMExtractUnmapped2

package Ace2;

import java.io.*;

public class SEUConfig {
  File bam_file;

  String reference_name = null;
  int ref_start = 0;
  int ref_end = 0;

  public static String SAM_INTERESTING_TAG = "XU";

  ReferenceSequence reference_sequence = null;
  // library of reference sequences

  String report_basename;
  String output_directory = null;

  int HQ_MISMATCH_EXTRACT_THRESHOLD = 4;
  int HQ_MISMATCH_WINDOW_SIZE = HQ_MISMATCH_EXTRACT_THRESHOLD * 3;

  public static final int FLAG_HAS_HQMM = 0x01;
  public static final int FLAG_HAS_HQMM_SC = 0x02;
  public static final int FLAG_HAS_HQMM_EC = 0x04;
  public static final int FLAG_HAS_HQ_INSERTION = 0x08;
  public static final int FLAG_HAS_HQ_DELETION = 0x10;

  boolean VERBOSE = false;
  boolean EXTRACT_INDELS = true;
  boolean EXTRACT_DUPLICATES = true;

  boolean QUERY_UNMAPPED = false;

  boolean ENABLE_READ_LIMIT = false;
  int READ_LIMIT = 0;
  boolean EXTRACT_INTERESTING_MATES = true;
  
  //  String CURRENT_REFERENCE_NAME = null;
  //  SAMMismatchFilter MMF = null;
  File temp_dir = null;

  // set only one of these booleans to true:
  // option A: (DOG SLOW)
  boolean MATE_QUERY_DIRECT = false;

  // option B: (faster!)
  boolean MATE_QUERY_GROUP = true;
  int MATE_QUERY_GROUP_QUEUE_LIMIT = 50000;
  // max records to queue before flushing

  //  int MATE_QUERY_GROUP_MAX_ALIGN_DISTANCE = 5;
  // int MATE_QUERY_GROUP_MAX_ALIGN_DISTANCE = 100;
  int MATE_QUERY_GROUP_MAX_ALIGN_DISTANCE = 10000;
  // max alignStart distance to group queries
  // group end

  int READ_CHUNK_COUNT = 200000;
  // how many reads at a time to preload 
  // used as a cache for mate sequence lookups

  //  boolean EXTRACT_MATES_ON_STANDARD_REFERENCES_ONLY = true;
  boolean EXTRACT_MATES_ON_STANDARD_REFERENCES_ONLY = false;
  // more "raw" default

  boolean WRITE_BAM = true;
  boolean WRITE_FASTQ = false;

  boolean ITERATE_REFERENCES = false;
  // rather than processing a single reference sequence,
  // iterate through all reference sequences.

  boolean EXTRACT_UNMAPPED_READS_ONLY = false;

}
