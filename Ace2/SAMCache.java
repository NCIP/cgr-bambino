package Ace2;
// TO DO:
// - count of all entries
// - option to suppress PCR duplicates too?
import java.util.*;
import net.sf.samtools.*;

public class SAMCache {
  HashList<Integer,SAMRecord> plus, minus;
  // separate caches for + and - strand reads

  boolean VERBOSE = false;

  public SAMCache () {
    clear();
  }

  public void clear() {
    plus = new HashList<Integer,SAMRecord>();
    minus = new HashList<Integer,SAMRecord>();
  }

  public void add (SAMRecord sr) {
    if (sr.getReadUnmappedFlag() == false) {
      HashList<Integer,SAMRecord> hash = sr.getReadNegativeStrandFlag() ? minus : plus;
      hash.add(sr.getAlignmentStart(), sr);
      if (VERBOSE) System.err.println("save " + sr.getReadName() + " " + sr.getAlignmentStart() + " " + (sr.getReadNegativeStrandFlag() ? "-" : "+"));  // debug

    }
  }

  public SAMRecord find_mate (SAMRecord sr) {
    SAMRecord result = null;
    int as = sr.getMateAlignmentStart();
    ArrayList<SAMRecord> list = sr.getMateNegativeStrandFlag() ? minus.get(as) : plus.get(as);
    // list of all cached reads with same align start position and
    // strand as the mate sequence
    String qname = sr.getReadName();
    int match_count = 0;

    if (VERBOSE) System.err.print("want " + sr.getReadName() + " " + as + " " + (sr.getReadNegativeStrandFlag() ? "-" : "+") + "result=");  // debug

    if (list != null) {
      for (SAMRecord s2 : list) {
	if (s2.getReadName().equals(qname)) {
	  if (VERBOSE) System.err.println("HIT!");  // debug
	  if (match_count++ > 0) {
	    System.err.println("WTF: multiple matching mates for " + qname + " at " + as + ", this_dup:"+ s2.getDuplicateReadFlag() + " last_dup:" + result.getDuplicateReadFlag());  // debug
	  }
	  result = s2;
	}
      }
    }
    if (result == null && VERBOSE) {
      System.err.println("MISS! as="+as + " cache=" + get_cache_range());
    }

    return result;
  }

  public String get_cache_range() {
    ArrayList<Integer> keys = new ArrayList<Integer>(plus.keySet());
    Integer start = Collections.min(keys);
    Integer end = Collections.max(keys);
    return start + "-" + end;
  }
  
}
