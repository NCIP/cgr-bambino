package Ace2;
// integer-sorted queue of jobs.
//
// used by SAMExtractUnmapped2 to queue queries by alignment start position.
// Theoretically this might lead to more efficient system caching of
// BAM data during query calls (i.e. if reads are adjacent and/or 
// near recent query sites).

// MNE 6/2012

import java.util.*;

public class SortedJobQueue<T> {
  private TreeMap<Integer,ArrayList<T>> queue;
  private int job_count;
  
  public SortedJobQueue () {
    reset();
  }

  public TreeMap<Integer,ArrayList<T>> get_queue() {
    return queue;
  }

  public void reset() {
      queue = new TreeMap<Integer,ArrayList<T>>();
      job_count = 0;
    }
  
  public boolean add (T job, int site) {
    ArrayList<T> bucket = queue.get(site);
    if (bucket == null) queue.put(site, bucket = new ArrayList<T>());
    job_count++;
    return bucket.add(job);
  }

  public boolean isEmpty() {
      return queue.isEmpty();
  }

  public T next() {
      // shift the first available job from the queue
      T result = null;
      
      Integer key = queue.firstKey();
      //      System.err.println("key="+key);  // debug

      if (key != null) {
	// something pending
	ArrayList<T> bucket = queue.get(key);
	if (bucket.size() > 0) result = bucket.remove(0);
	if (bucket.size() == 0) {
	  //	  System.err.println("deleting bucket for " + key);  // debug
	  queue.remove(key);
	}
      }
      //      System.err.println("result="+result);  // debug
      return result;
  }

  public int get_job_count () {
      return job_count;
    }

  public int get_job_count_manual() {
      int count = 0;
      for (Integer key : queue.keySet()) {
	count += queue.get(key).size();
      }
      return count;
    }

  public static void main (String[] argv) {
    SortedJobQueue<SAMReadQuery> sjq = new SortedJobQueue<SAMReadQuery>();

    SAMReadQuery x = new SAMReadQuery("one_read1", 1, true);
    sjq.add(x, 1);
    sjq.add(new SAMReadQuery("one_read2", 1, true), 1);
    sjq.add(new SAMReadQuery("2_read1", 2, true), 2);

    int count = 0;
    while (!sjq.isEmpty()) {
      SAMReadQuery q = sjq.next();
      System.err.println("q="+q.readName + " at " + q.alignmentStart);  // debug

      if (++count == 1) {
	sjq.add(new SAMReadQuery("one_read3", 1, true), 1);
      }
    }



  }
}
