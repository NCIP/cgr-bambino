package Ace2;

import java.util.*;
import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.*;

public class SAMTagTracker {
  private SNPConfig config;

  private static int MAX_STRING_LENGTH_TO_TRACK = 3;
  // FIX ME: also hardcode list of tags we KNOW won't be useful 
  // (alternative quality strings, etc.)
  private ArrayList<String> results;

  public SAMTagTracker (ArrayList list) {
    int read_count = 0;
    SAMRecord sr = null;

    HashSet<String> ignore_tags = new HashSet<String>();
    // don't track these tags
    ignore_tags.add("BQ");
    ignore_tags.add("CQ");
    ignore_tags.add("CS");
    ignore_tags.add("E2");
    ignore_tags.add("MD");
    ignore_tags.add("OQ");
    ignore_tags.add("OC");
    ignore_tags.add("PG");
    ignore_tags.add("Q2");
    ignore_tags.add("R2");
    ignore_tags.add("RG");
    ignore_tags.add("U2");

    HashMap<String,HashMap<String,Integer>> track_string = new HashMap<String,HashMap<String,Integer>>();
    // $track_string{$tag}{$value} = $count
    HashMap<String,ArrayList<Integer>> track_int = new HashMap<String,ArrayList<Integer>>();

    for (Object o : list) {
      if (o instanceof SNPTrackInfo) {
	sr = ((SNPTrackInfo) o).sr;
	//	int start = sti.sr.getAlignmentStart();
      } else if (o instanceof IndelInfo) {
	sr = ((IndelInfo) o).sr;
      } else {
	System.err.println("ERROR: unhandled object " + o);  // debug
	System.exit(1);
      }
      read_count++;

      for (SAMTagAndValue tav : sr.getAttributes()) {
	if (ignore_tags.contains(tav.tag)) continue;
	if (tav.value instanceof String || tav.value instanceof Character) {
	  String v = tav.value instanceof String ? (String) tav.value : ((Character) tav.value).toString();
	  if (v.length() <= MAX_STRING_LENGTH_TO_TRACK) {
	    HashMap<String,Integer> count_bucket = track_string.get(tav.tag);
	    if (count_bucket == null) track_string.put((String) tav.tag,
						       count_bucket = new HashMap<String,Integer>());
	    Integer count = count_bucket.get(v);
	    if (count == null) count = 0;
	    count_bucket.put(v, count + 1);
	  } else {
	    System.err.println("not tracking tag " + tav.tag);  // debug
	  }
	} else if (tav.value instanceof Integer) {
	  list = track_int.get(tav.tag);
	  if (list == null) track_int.put((String) tav.tag, list = new ArrayList<Integer>());
	  list.add((Integer) tav.value);
	} else {
	  System.err.println("tag=" + tav.tag + " value=" + tav.value + " type=" +tav.value.getClass());  // debug
	  System.err.println("unhandled SAM tag tracking type: " + tav.value.getClass() + " tag=" + tav.tag);  // debug
	}
      }
    }

    results = new ArrayList<String>();

    // 
    //  integer tags:
    //
    for (String tag : track_int.keySet()) {
      boolean first = true;
      ArrayList<Integer> values = track_int.get(tag);
      //      System.err.println("size of " + tag + "=" + values.size());  // debug

      int min = 0;
      int max = 0;
      long total = 0;
      // hack
      Collections.sort(values);
      for (Integer value : values) {
	total += value;
	if (first) {
	  min = max = value;
	  first = false;
	} else {
	  if (value < min) min = value;
	  if (value > max) max = value;
	}
      }
      int avg = (int) (total / values.size());
      int median = values.get((values.size() - 1) / 2);
      String result = tag + ":i:" + min + "," + median + "," + avg + "," + max;
      results.add(result);
    }

    //
    //  string tags:
    //
    for (String tag :  track_string.keySet()) {
      HashMap<String,Integer> value_counts = track_string.get(tag);
      ArrayList vs = new ArrayList<String>();
      for (String value : value_counts.keySet()) {
	int count = value_counts.get(value);
	float freq = ((float) count / read_count);
	//	System.err.println(tag + " => " + value + " => " + count + " => " + freq);  // debug
	vs.add(value);
	vs.add(Float.toString(freq));
      }
      String result = tag + ":A:" + Funk.Str.join(",", vs);
      results.add(result);
    }
  }

  public ArrayList<String> get_results() {
    return results;
  }
  
}

