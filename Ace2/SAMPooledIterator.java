package Ace2;
// Picard's net.sf.picard.sam.MergingSamRecordIterator
// nearly fits the bill, but want to keep track of source
// SAMFileReader

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import net.sf.samtools.util.PeekIterator;
//import net.sf.picard.sam.SamFileHeaderMerger;

import java.io.*;
import java.util.*;

public class SAMPooledIterator implements Iterator,Iterable<SAMRecord> {
  private ArrayList<SAMFileReader> sfrs_list;
  private SAMRecord[] read_buf;
  private SAMFileReader[] sfrs;
  private PeekIterator[] iterators;
  private ArrayList<String> dictionary_ids;
  private String current_target_id;
  private boolean check_genome_version = true;
  private ArrayList<Iterable<SAMRecord>> custom_iterables = null;
  private String restrict_reference_name = null;
  private Range restrict_range = null;
  private ArrayList<CloseableIterator> cis = null;

  private boolean STANDARDIZE_REFERENCE_NAMES = true;
  // munges reference/dictionary names to strip leading "chr", if present.
  // This allows .bam files using variants of the same reference name
  // (i.e. "chr1" vs "1") to be considered identical and combined.
  // Full virtualization into Chromosome objects will fail for
  // .bam files using non-chromosome reference sequences.

  int current_buf_index;
  // publicly visible (faster than sub call)

  private static final int UNDEF_INDEX = -1;

  public SAMPooledIterator () {
    sfrs_list = new ArrayList<SAMFileReader>();
  }

  public Iterator<SAMRecord> iterator() {
    return this;
  }

  public void set_custom_iterables(ArrayList<Iterable<SAMRecord>> custom_iterables) {
    this.custom_iterables = custom_iterables;
  }

  // begin Iterator stub
  public boolean hasNext() {
    boolean result = false;
    for (int i = 0; i < iterators.length; i++) {
      if (iterators[i] != null && iterators[i].hasNext()) {
	result = true;
	break;
      }
    }
    return result;
  }

  public SAMRecord next() {
    SAMRecord result = null;
    if (current_target_id == null) {
      if (dictionary_ids.size() == 0) {
	System.err.println("dictionary EOF");  // debug
	return null;
      } else {
	current_target_id = dictionary_ids.remove(0);
	System.err.println("cid="+current_target_id);  // debug
      }
    }
    int i;
    current_buf_index = UNDEF_INDEX;
    int want_align_start = UNDEF_INDEX;
    int as;
    String ref_name;
    boolean some_records_left = false;
    for (i=0; i < iterators.length; i++) {
      // for each iterator, peek() if possible to get next record
      if (read_buf[i] == null) {
	if (iterators[i] != null && iterators[i].hasNext()) read_buf[i] = (SAMRecord) iterators[i].peek();
	// bleh: types?
      }

      //      System.err.println(read_buf[i].getReferenceName());

      if (read_buf[i] != null) {
	some_records_left = true;
	ref_name = read_buf[i].getReferenceName();
	if (STANDARDIZE_REFERENCE_NAMES) ref_name = SAMUtils.get_standardized_refname(ref_name);
	if (ref_name.equals(current_target_id)) {
	  // a record is available for the current target record
	  as = read_buf[i].getAlignmentStart();
	  if (current_buf_index == UNDEF_INDEX || as < want_align_start) {
	    // first qualified record, or earlier-starting
	    current_buf_index = i;
	    want_align_start = as;
	  }
	}
      }
    }

    if (current_buf_index == UNDEF_INDEX) {
      // no usable records found
      if (some_records_left) {
	current_target_id = null;
	result = next();
	// recurse
      }
    } else {
      // ok
      result = (SAMRecord) iterators[current_buf_index].next();
      read_buf[current_buf_index] = null;
    }

    return result;
  }

  public SAMFileReader getSAMFileReader() {
    return current_buf_index == UNDEF_INDEX ? null : sfrs[current_buf_index];
  }

  public void remove() {
    // no-op
  }
  // end Iterator stub

  
  public void add (SAMFileReader sfr) {
    sfrs_list.add(sfr);
  }

  public void addAll(ArrayList<SAMFileReader> sfrs) {
    for (SAMFileReader sfr : sfrs) {
      add(sfr);
    }
  }

  public void set_genome_version_check (boolean v) {
    check_genome_version = v;
  }

  public void set_restrict_reference (String ref_name) {
    restrict_reference_name = ref_name;
  }

  public void set_restrict_range (Range restrict_range) {
    this.restrict_range = restrict_range;
  }

  public boolean prepare() {
    //
    // hacky check to see if these readers are compatible...
    // (i.e. they reference the same target sequences in the same order)
    //
    ArrayList<ArrayList<Object>> sets = new ArrayList<ArrayList<Object>>();
    for (SAMFileReader sfr : sfrs_list) {
      SAMFileHeader h = sfr.getFileHeader();
      SAMSequenceDictionary dict = h.getSequenceDictionary();
      ArrayList<Object> list = new ArrayList<Object>();
      list.add(h.getSortOrder());
      for (SAMSequenceRecord sr : dict.getSequences()) {
	//	System.err.println(sr.getSequenceName());  // debug
	  String ref_name = new String(sr.getSequenceName());
	  if (STANDARDIZE_REFERENCE_NAMES) ref_name = SAMUtils.get_standardized_refname(ref_name);

	  list.add(ref_name);
	if (check_genome_version) list.add(Integer.valueOf(sr.getSequenceLength()));
      }
      sets.add(list);

      if (dictionary_ids == null) {
	dictionary_ids = new ArrayList<String>();
	for (SAMSequenceRecord sr : dict.getSequences()) {
	    String ref_name = new String(sr.getSequenceName());
	    if (STANDARDIZE_REFERENCE_NAMES) ref_name = SAMUtils.get_standardized_refname(ref_name);
	    //	    System.err.println("name="+sr.getSequenceName() + " std=" + ref_name);
	    dictionary_ids.add(ref_name);
	}
	current_target_id=null;
      }
    }

    boolean ok = true;
    int size = sfrs_list.size();
    int i;

    //    System.err.println("RREF="+restrict_reference_name);  // debug
    
    if (restrict_reference_name != null) {
      // don't compare as we're querying specific regions
      //      System.err.println("restrict mode");  // debug
    } else {
	for (i = size - 2; i >= 0; i--) {
	    ArrayList<Object> s1 = sets.get(i);
	    ArrayList<Object> s2 = sets.get(i + 1);
	    if (!s1.equals(s2)) {
		System.err.println("ERROR: can't pool iterators:");  // debug
		System.err.println("file1: " + s1);  // debug
		System.err.println("file2: " + s2);  // debug
		ok = false;
	    }
	}
    }

    read_buf = new SAMRecord[size];
    sfrs = new SAMFileReader[size];
    iterators = new PeekIterator[size];

    cis = new ArrayList<CloseableIterator>();

    for (i=0; i < size; i++) {
      sfrs[i] = sfrs_list.get(i);
      if (false) {
	System.err.println("DEBUG: using restricted iterator");  // debug
	//	iterators[i] = new PeekIterator(sfrs[i].queryAlignmentStart("chr1", 121120000));
	//	iterators[i] = new PeekIterator(sfrs[i].queryContained("chr1", 121186800, 121187000));
	iterators[i] = new PeekIterator(sfrs[i].queryContained("chr10", 54230000, 64230000));
      } else if (restrict_reference_name != null) {
	CloseableIterator<SAMRecord> ci;

	SAMFileHeader h = sfrs[i].getFileHeader();
	SAMSequenceDictionary dict = h.getSequenceDictionary();
	String query_ref_name = null;
	int len = 0;
	for (SAMSequenceRecord dsr : dict.getSequences()) {
	  String bam_ref_name = new String(dsr.getSequenceName());
	  if (Chromosome.standardize_name(bam_ref_name).equals(restrict_reference_name)) {
	    query_ref_name = bam_ref_name;
	    len = dsr.getSequenceLength();
	    break;
	  }
	}
	if (query_ref_name == null) {
	  // looking for a particular reference name in BAM header but can't find it
	  System.err.println("NOTE: can't find bam header entry for " + restrict_reference_name);  // debug
	  iterators[i] = null;
	} else {
	  //	  ci = sfrs[i].queryAlignmentStart(query_ref_name, 1);
	  current_target_id = query_ref_name;
	  //	  dictionary_ids = new ArrayList<String>();
	  //	  dictionary_ids.add(query_ref_name);
	  
	  //	  ci = sfrs[i].queryOverlapping(query_ref_name, 1, len);
	  int qstart = SNPConfig.QUERY_START_BASE;
	  int qend = len;
	  if (restrict_range != null) {
	    qstart = restrict_range.start;
	    qend = restrict_range.end;
	    //	    System.err.println("restricting to " + qstart + " " + qend);  // debug
	  }

	  ci = sfrs[i].queryOverlapping(query_ref_name, qstart, qend);
	  //	  System.err.println("iterator on " + query_ref_name);  // debug
	  cis.add(ci);
	  iterators[i] = new PeekIterator(ci);
	  //	  System.err.println("hasNext: " + iterators[i].hasNext());  // debug
	}
      } else if (custom_iterables != null) {
	iterators[i] = new PeekIterator(custom_iterables.get(i).iterator());
      } else {
	iterators[i] = new PeekIterator(sfrs[i].iterator());
      }
      read_buf[i] = null;
    }

    return ok; 
  }

  
  public static void main (String[] argv) {

    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SAMPooledIterator spi = new SAMPooledIterator();

    spi.add(new SAMFileReader(new File("TCGA-09-0364-01A-02W-0370-10_SOLiD.bam_mini.bam")));
    spi.add(new SAMFileReader(new File("TCGA-09-0364-10A-01W-0370-10_454.bam_mini.bam")));

    //spi.add(new SAMFileReader(new File("20_combined.bam")));
    //spi.add(new SAMFileReader(new File("55_combined.bam")));
    // start of chr1 + chr2 for 2 tumor samples
    
    if (spi.prepare()) {
	for (SAMRecord sr : spi) {
	    System.err.println("hey now " + spi.getSAMFileReader() + " => " + sr.getReferenceName() + " " + sr.getReadName() + " " + sr.getAlignmentStart());  // debug
	}
    } else {
	System.err.println("error preparing iterator");  // debug
    }
  }

  public void close() {
    if (cis != null) {
      for (CloseableIterator old : cis) {
	old.close();
      }
    }
  }

}
