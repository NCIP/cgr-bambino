//
//  queue up SAMRecord mate queries and attempt to find results
//  with as few query calls as possible.
//  MNE 6/2012
//

package Ace2;

import java.util.*;
import java.io.*;

import net.sf.samtools.*;

public class SEUMateQuery {
  private SEUConfig config;
  private SEUInteresting interesting;
  private SEUWriter seuw;
  private SortedJobQueue<SAMReadQuery> queue;
  private SAMFileReader sfr;
  private String reference_name;
  private int reference_index;
  private long processing_time = 0;
  public static boolean VERBOSE = false;
  
  public SEUMateQuery (SEUConfig config,
		       SEUInteresting interesting,
		       SEUWriter seuw) {
    // this constructor uses a persistent SEUInteresting object.
    this.config = config;
    this.interesting = interesting;
    this.seuw = seuw;
    setup();
  } 

  public SEUMateQuery (SEUConfig config,
		       SEUWriter seuw) {
    // this constructor creates a temporary SEUInteresting object
    // every time queue is flushed.  This is slower but saves a lot
    // of RAM as we don't have to keep entire genome in memory.
    // Should be used with large queue sizes!
    this.config = config;
    this.seuw = seuw;
    interesting = null;
    setup();
  } 

  public String get_reference_name() {
    return reference_name;
  }

  private void setup() {
    reference_name = null;
    sfr = new SAMFileReader(config.bam_file);
    // FIX ME: SHARE?
    queue = new SortedJobQueue<SAMReadQuery>();
  }

  public void add_mate (SAMRecord sr) throws IOException {
    queue.add(new SAMReadQuery(sr, true), sr.getMateAlignmentStart());
    if (reference_name == null) {
      reference_name = sr.getMateReferenceName();
      reference_index = sr.getMateReferenceIndex();
    }
    if (queue.get_job_count() >= config.MATE_QUERY_GROUP_QUEUE_LIMIT) flush();
  }

  public int get_job_count() {
    return queue.get_job_count();
  }

  public void flush() throws IOException {
    long start_time = System.currentTimeMillis();
    
    TreeMap<Integer,ArrayList<SAMReadQuery>> jobs = queue.get_queue();

    ArrayList<Range> queries = new ArrayList<Range>();

    //
    //  reduce targets:
    //
    Range r = null;
    for (Integer pos : jobs.keySet()) {
      if (r != null &&
	  (pos - r.start) > config.MATE_QUERY_GROUP_MAX_ALIGN_DISTANCE) 
	r = null;
      // create new entry if distance is too great
      if (r == null) queries.add(r = new Range(pos, pos));
      r.end = pos;
    }

    SAMRecord sr;
    SAMReadQuery hit;
    int scan_count = 0;
    int query_count = 0;
    for (Range query : queries) {
      if (VERBOSE) System.err.println("query: " + query.start +"-" + query.end);

      query_count++;
      ArrayList<SAMReadQuery> targets = null;
      int last_start = -1;

      SAMRecordIterator iterator;
      if (query.start == query.end) {
	iterator = sfr.queryAlignmentStart(reference_name, query.start);
      } else {
	iterator = sfr.queryOverlapping(reference_name, query.start, query.end);
      }
      int as;
      while (iterator.hasNext()) {
	sr = iterator.next();
	scan_count++;
    	if (sr.getReadUnmappedFlag()) continue;
	as = sr.getAlignmentStart();
	if (as < query.start) continue;
	// since queryOverlapping() is used, the same records
	// may appear in different queries.  To prevent false
	// duplicate matches, only process reads whose alignments 
	// start in the query window.
	if (as > query.end) break;
	// past target region in query, so safe to quit

	if (as != last_start) {
	  //
	  // get target reads at this alignment start position
	  //
	  last_start = as;
	  targets = jobs.get(last_start);
	  if (VERBOSE && targets != null) {
	    System.err.println("targets at " + last_start + ":");  // debug
	    for (SAMReadQuery target : targets) {
	      System.err.println("  " + target.readName + " " + target.alignmentStart + " " + (target.negativeStrandFlag ? "-" : "+"));  // debug
	    }
	  }
	}

	if (targets != null) {
	  //
	  // we want some reads aligned to this start position
	  //
	  //	  if (VERBOSE) System.err.println("checking: " + sr.getReadName() + " " + sr.getAlignmentStart() + " " + (sr.getReadNegativeStrandFlag() ? "-" : "+"));  // debug

	  for (SAMReadQuery target : targets) {
	    if (target.matches(sr)) {
	      if (target.hit == null) {
		target.hit = sr;
	      } else {
		System.err.println("WTF: multiple hits for target!");  // debug
	      }
	      //	      System.err.println("FOUND MATCH: " + sr.getReadName());  // debug
	      break;
	    }
	  }
	}
      }
      iterator.close();
    }


    //
    //  check overall success:
    //
    int total = 0;
    int found = 0;
    for (Integer pos : jobs.keySet()) {
      for (SAMReadQuery q : jobs.get(pos)) {
	total++;
	if (q.hit != null) found++;
      }
    }
    processing_time += System.currentTimeMillis() - start_time;
    // don't include processing (below) in this time,
    // to better compare w/direct query style

    SEUInteresting seui = interesting;
    if (seui == null) {
      System.err.println("creating temporary SEUI for RI " + reference_index + " name=" + reference_name);  // debug
      seui = new SEUInteresting(config, reference_index);
    }

    for (Integer pos : jobs.keySet()) {
      for (SAMReadQuery q : jobs.get(pos)) {
	if (q.hit != null) {
	  seui.interesting_check(q.hit);
	  // mark up "interesting" status
	  //	  bfw.addAlignment(q.hit);
	  seuw.addAlignment(q.hit, seui);
	  // save
	}
      }
    }

    queue.reset();
    // empty queue

    System.err.println("SEUMateQuery summary: scan_count " + scan_count + " in " + query_count + " queries, found "+ found + "/" + total + " in " + processing_time + " ms");

  }
  
}
