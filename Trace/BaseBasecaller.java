package Trace;
// brain-dead peak finder.  Not a primary basecaller, more for 
// supplemental assignments of alternate peaks, etc.

import java.util.*;
import Funk.Stats;

public class BaseBasecaller {
  private TraceFile trace;

  private Vector [] all_peaks;
// (generics:)    private Vector<PolyPeak> called;
  private Vector called;
// (generics:)    private Vector<PolyPeak> alternate;
  private Vector alternate;

  public BaseBasecaller (TraceFile t) {
    trace = t;
    setup_internal();
  }

  private void setup_internal () {
    // init using internal trace basecalls.
    find_peaks();
// (generics:)      called = new Vector<PolyPeak>(trace.num_bases);
    called = new Vector(trace.num_bases);
    short ci;
    int i;
    PolyPeak cb;
    for (i=0; i < trace.num_bases; i++) {
      ci = TraceFile.base_to_index(trace.bases[i]);
      cb = new PolyPeak(ci);
      called.addElement(cb);
      cb.peak = trace.base_position[i];
      if (cb.peak < 0) cb.peak = 0;
      // corrupt internal call data?

      if (ci == -1) {
	// called base is an N.
	// HACK: fudge to highest peak at that location
	int amp;
	short called = -1;
	for (short j = 0; j < 4; j++) {
	  amp = trace.trace_data[j][cb.peak];
	  if (amp > cb.amplitude) {
	    cb.amplitude = amp;
	    cb.base = j;
	  }
	}
	//	System.out.println("eek at " + cb.peak + "=" + cb.amplitude);  // debug
      } else {
	cb.amplitude = trace.trace_data[ci][cb.peak];
      }
      //      System.out.println(ci + " " + cb.peak + " " + cb.amplitude);  // debug
    }
    assign_alternates();
  }

  private void assign_alternates () {
    // find alternate peaks for each called peak
// (generics:)      alternate = new Vector<PolyPeak>(called.size());
    alternate = new Vector(called.size());
    int [] ptr = new int[4];
    ptr[0] = ptr[1] = ptr[2] = ptr[3] = 0;
    // start peak search indices for each base type

    Hashtable peaks = get_peak_hash();

    int i,j,base;
    PolyPeak called_peak,other;
    int min_offset, max_offset;
    int wiggle = trace.average_peak_spacing / 2;
    int end;
    Vector v;
    String key;
    Enumeration e;
// (generics:)      Vector<PolyPeak> others = new Vector<PolyPeak>();
    Vector others = new Vector();
    int stop = called.size();
    for (i=0; i < stop; i++) {
      called_peak = (PolyPeak) called.elementAt(i);
      end = called_peak.peak + wiggle;

      // scan for possible alternative peaks
      others.removeAllElements();
      for (j=called_peak.peak - wiggle; j <= end; j++) {
	key = Integer.toString(j);
	if (peaks.containsKey(key)) {
	  v = (Vector) peaks.get(key);
	  e = v.elements();
	  while (e.hasMoreElements()) {
	    other = (PolyPeak) e.nextElement();
	    if (other.base != called_peak.base) {
	      others.addElement(other);
	    }
	  }
	}
      }

      // pick the "best" (hack, just the highest :/)
      if (others.size() > 0) {
	PolyPeak best = null;
	e = others.elements();
	// FIX ME: ITERATION
	while (e.hasMoreElements()) {
          other = (PolyPeak) e.nextElement();
	  if (best == null || other.amplitude > best.amplitude) best = other;
	}
        //	System.err.println("alt for " + called_peak.base + " at " + called_peak.peak + ": " + best.base + " at " + best.peak);  // debug
	alternate.addElement(best);
      } else {
	alternate.addElement(null);
      }
    }
  }

  private Hashtable get_peak_hash () {
      //    Hashtable h = new Hashtable();
// (generics:)      Hashtable<String,Vector<PolyPeak>> h = new Hashtable<String,Vector<PolyPeak>>();
    Hashtable h = new Hashtable();
    PolyPeak p;
    String key;
// (generics:)      Vector<PolyPeak> peaks;
    Vector peaks;
    for (short i=TraceFile.TRACE_A; i <= TraceFile.TRACE_T; i++) {
      Vector v = all_peaks[i];
      int size = v.size();
      for (int j=0; j < size; j++) {
	p = (PolyPeak) v.elementAt(j);
	key = Integer.toString(p.peak);
	if (h.containsKey(key)) {
	    peaks = (Vector) h.get(key);
	} else {
// (generics:)  	  peaks = new Vector<PolyPeak>();
	  peaks = new Vector();
	  h.put(key, peaks);
	}
	peaks.addElement(p);
      }
    }
    return h;
  }

  public void dump_peaks () {
    for (short i=TraceFile.TRACE_A; i <= TraceFile.TRACE_T; i++) {
      char base = TraceFile.index_to_base(i);
      Vector v = all_peaks[i];
      System.out.println(i + " " + v.size());  // debug
      
      for (int j=0; j < v.size(); j++) {
	PolyPeak p = (PolyPeak) v.elementAt(j);
	System.out.println(base + " " + p.peak);  // debug
      }
    }    
  }

  private void find_peaks () {
    //
    // find all the peak locations for the 4 base types
    //
    int this_sample = 0;
    int last_sample = 0;
    short i;
    int j;
    PolyPeak p;
    int minimum_amp = (int) (trace.max_amplitude * 0.05);
    // the minimum size for a peak to be considered valid (5% of max)

    all_peaks = new Vector[4];

    for (i=TraceFile.TRACE_A; i <= TraceFile.TRACE_T; i++) {
      p = new PolyPeak(i);
// (generics:)        //      Vector<PolyPeak> v = all_peaks[i] = new Vector<PolyPeak>();
      //      Vector v = all_peaks[i] = new Vector();
// (generics:)        Vector<PolyPeak> v = new Vector<PolyPeak>();
      Vector v = new Vector();
      all_peaks[i] = v;
      //      System.out.println(i);  // debug
      for (j=0; j < trace.num_samples; j++) {
	this_sample = trace.trace_data[i][j];
	//	System.out.println("pos:" + j + " this:" + this_sample + " last:" + last_sample);  // debug

	if (this_sample > last_sample) {
	  // new peak, or peak rising
	  if (p.peak > 0) {
	    // we've already marked the peak's top; complete this peak
	    p.end = j - 1;
	    //	    System.out.println("peak end; base:" + i + " start:" + p.start + " " + p.peak + " " + p.end + " amp:" + p.amplitude);
	    
	    if (p.amplitude > minimum_amp) v.addElement(p);
	    p = new PolyPeak(i);
	  } else if (p.clip_start > 0) {
	    // we're in have a "clipped" or flat peak,
	    // but the trace is starting to rise again.
	    // Assume two peaks; clip the old one here and start a new peak.
	    p.peak = p.clip_start + ((p.clip_end - p.clip_start) / 2);
	    p.amplitude = last_sample;
	    p.end = j - 1;
	    if (p.amplitude > minimum_amp) v.addElement(p);	    
	    p = new PolyPeak(i);
	  }
	  if (p.start == -1) {
	    // record beginning of new peak
	    p.start = j;
	  }
	} else if (this_sample == last_sample) {
	  if (p.clip_start == 0) p.clip_start = j - 1;
	  p.clip_end = j;
	} else if ((this_sample < last_sample) && p.peak == 0) {
	  // peak is descending
	  if (p.clip_start == 0) {
	    // normal, unclipped peak
	    p.peak = j - 1;
	  } else {
	    // a clipped peak; center is average position of clipped area
	    p.peak = p.clip_start + ((p.clip_end - p.clip_start) / 2);
	  }
	  p.amplitude = last_sample;
	}
	last_sample = this_sample;
      }
      // fix me -- final peak
    }
  }

  public static void main (String [] argv) {
    StreamDelegator.set_local(true);
    // TraceFile t = new TraceFile("/mike/edmonson/work/ken/virtual_northern/GMDR4F2_E03_GMDR51_pPR04.f2_019.ab1");
    //    TraceFile t = new TraceFile("ye50d01.s1");
    String fn = argv.length > 0 ? argv[0] : "46061.scf";
    System.err.println("file:" + fn);  // debug

    TraceFile t = new TraceFile(fn);
    BaseBasecaller bc = new BaseBasecaller(t);
  }

  public Vector get_called () {
    return called;
  }

  public Vector get_alternate () {
    return alternate;
  }

  public short get_called_base (int i) {
    // return called base at given index
    return ((PolyPeak) called.elementAt(i)).base;
  }

  public void assign_called() {
    //
    // HACK: attempt to assign primary basecalls from raw peaks.
    //

    Hashtable peaks = get_peak_hash();
    // get all peaks indexed by peak position
    String key,scan_key;

    called = new Vector();

    PolyPeak p;
    int j;
    for (int i=0; i < trace.num_samples; i++) {
      key = Integer.toString(i);
      if (peaks.containsKey(key)) {
        Vector hits = (Vector) peaks.get(key);
        
        PolyPeak peak_to_use = null;
        
        Vector candidates = (Vector) hits.clone();
        // clone, since we'll be adding possible neighbors

        //
        //  select which peak of possible candidates to use:
        //
        if (candidates.size() == 1) {
          // only one candidate peak, use it
          peak_to_use = (PolyPeak) candidates.elementAt(0);
        } else {
          // decide which candidate peak to use
          int len = candidates.size();
          int use_height = -1;
          for (j=0; j < len; j++) {
            p = (PolyPeak) candidates.elementAt(j);
            int height = trace.trace_data[p.base][p.peak];
            if (height > use_height) {
              peak_to_use = p;
              //              System.err.println("HEY NOW: using peak " + p.get_base() + " at " + i + " height=" + height);  // debug
              use_height = height;
            }
          }
        }

        called.addElement(peak_to_use);
      }
    }

    // second pass:
    //   - find median peak spacing
    //   - find too-close peaks based on some variant of median spacing
    //   - choose primary peak
    ArrayList spacing = new ArrayList();
    int len = called.size();
    PolyPeak here;
    PolyPeak prev = (PolyPeak) called.elementAt(0);
    int distance;

    for (int i = 1; i < len; i++) {
      here = (PolyPeak) called.elementAt(i);
      distance = here.peak - prev.peak;
      spacing.add(new Integer(distance));
      //      System.err.println("spacing " + distance);  // debug
      prev = here;
    }
    Stats st = new Stats(spacing);

    int low_spacing_cutoff = (int) st.get_95_percent_confidence_interval(false);
    // works fine for clean traces
    
    //    System.err.println("median: " + st.median());  // debug

    int floor = (int) (Math.ceil(st.median() * 0.33));
    // traces with indels throw a wrench into spacing distribution; 
    // require a minimum percentage of the median spacing (rounded
    // up to the next integer).

    if (low_spacing_cutoff < floor) low_spacing_cutoff = floor;

    //    System.err.println("floor: " + floor);  // debug
    //    System.err.println("low cutoff interval: " + low_spacing_cutoff);

    int pass_number = 0;
    while (true) {
      HashSet disqualified = new HashSet();
      prev = (PolyPeak) called.elementAt(0);
      int l = called.size();
      for (int i = 1; i < l; i++) {
	here = (PolyPeak) called.elementAt(i);
	if (disqualified.contains(here) ||
	    disqualified.contains(prev)) {
	  // keep moving if previously-disqualified base encountered
	  //	System.err.println("HEY NOW");  // debug
	} else {
	  distance = here.peak - prev.peak;
	  if (distance <= low_spacing_cutoff) {
	    //	  System.err.println("suspiciously close at base " + i);  // debug
	    disqualified.add(prev.amplitude > here.amplitude ? here : prev);
	    // horrible
	  }
	}
	prev = here;
      }

      System.err.println("pass:" + ++pass_number + " pruned:" + disqualified.size());  // debug

      if (disqualified.size() == 0) {
	// nothing pruned, finish
	break;
      } else {
	called.removeAll(disqualified);
      }
    }

  }

  public void set_tracefile_values () {
    // hack: set TraceFile values to (possibly-recalled) current 
    // called peak array.
    int base_count = called.size();
    trace.num_bases = base_count;
    trace.bases = new char[base_count];
    trace.base_position = new int[base_count];

    PolyPeak called_peak;
    for (int i=0; i < base_count; i++) {
      called_peak = (PolyPeak) called.elementAt(i);
      trace.bases[i] = called_peak.get_base();
      trace.base_position[i] = called_peak.peak;
    }

    
  }


}
