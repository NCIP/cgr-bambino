package Trace;

// analyze trace data for polymorphism information

import java.util.*;
import Funk.*;

public class PolyData extends Observable implements Observer {
  public PolyPeak [] alternate_peaks;

  public boolean loaded = false;
  public TraceFile trace = null;
  public PhdFile phd = null;

  private Observer observer = null;

  public PolyData (TraceFile t, PhdFile p) {
    // TraceFile and PhdFile are already completely loaded
    this.trace = t;
    this.phd = p;
    build_polydata();
  }

  public PolyData (String name) {
    phd = new PhdFile(name, this);
    trace = new TraceFile(name, this);
  }

  public PolyData (String name, Observer o) {
    observer = o;
    addObserver(o);
    phd = new PhdFile(name, this);
    trace = new TraceFile(name, this);
  }

  public void update (Observable o, Object arg) {
    if (trace != null && phd != null && trace.loaded() && phd.loaded) {
      // both the TraceFile and the PhdFile have finished loading;
      // we can analyze the trace.
      build_polydata();
    }
  }

  void build_polydata () {
    alternate_peaks = new PolyPeak[phd.data.size()];

    LinkListVector [] peaks = find_peaks();
    call_alternates(peaks); 
    loaded = true;
    if (observer != null) {
      setChanged();
      notifyObservers();
    }
  }

  LinkListVector [] find_peaks () {
    //
    // find all the peak locations for the 4 trace types
    //
    int this_sample = 0;
    int last_sample = 0;
    short i;
    int j;
    PolyPeak p;
    LinkListVector peaks[] = new LinkListVector[4];
    int minimum_amp = (int) (trace.max_amplitude * 0.05);
    // the minimum size for a peak to be considered valid (5% of max)

    for (i=0; i < 4; i++) {
      p = new PolyPeak(i);
      LinkListVector v = peaks[i] = new LinkListVector();
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
    return peaks;
  }

  void call_alternates (LinkListVector [] peaks) {
    // for each called peak, record plausible alternate peaks (if any)
    int i,j,start,end,ignore;
    PolyPeak p;
    int pmax = phd.data.size() - 1;
    for (i=0; i <= pmax; i++) {
      PhdData called = (PhdData) phd.data.elementAt(i);

      PhdData prev = (i == 0) ? null : (PhdData) phd.data.elementAt(i - 1);
      PhdData next = (i == pmax) ? null : (PhdData) phd.data.elementAt(i + 1);

      //      System.out.println("pos:" + i + " called base:" + called.base + " pos:" + called.position);  // debug

      switch (called.base) {
      case 'a' : case 'A': ignore = TraceFile.TRACE_A; break;
      case 'c' : case 'C': ignore = TraceFile.TRACE_C; break;
      case 'g' : case 'G': ignore = TraceFile.TRACE_G; break;
      case 't' : case 'T': ignore = TraceFile.TRACE_T; break;
      default: ignore = -1; break;
      }

      //      System.out.println("");  // debug
      //      System.out.println("current:" + called + " cb:" + called.base + " ignoring:" + ignore + " at:" + called.position);

      //
      //  look for "best" alternate peak
      //
      PolyPeak best = null;
      for (j=0; j < 4; j++) {
	if (j != ignore) {  // ignore the called base in this area
	  while (true) {
	    p = (PolyPeak) peaks[j].current();
	    if (p == null) break;
	    if (prev != null && p.peak < prev.position) {
	      // alternate peak is too far before called peak;
	      if (peaks[j].next() == false) break;
	      // move to next, exit loop if we're at the end
	    } else {
	      //   System.out.println("peak ok:" + j + " " + p.peak);  // debug
	      if (peak_check(called, prev, next, p, best)) best = p;
	      if ((p = (PolyPeak) peaks[j].get_next()) != null) {
		// next peak is valid; check it too
		if (peak_check(called, prev, next, p, best)) best = p;
	      }
	      break;  //done
	    }
	  }
	}
      }

      if (best == null) {
	alternate_peaks[i] = null;
      } else {
	best.called = called;
	// keep reference to phd data for the called position
	alternate_peaks[i] = best;
      }

      //      String msg = "called:" + called.base + " pos:" + called.position;
//       if (best != null) {
// 	msg = msg + " alt peak is:" + best.base + " at:" + best.peak + " amp:" + best.amplitude;
// 	if (best.called != null) {
// 	  msg = msg + " cb:" + best.called.base + " cb2:" + best.called_base + " cp:" + best.called.position + " ref:" + alternate_peaks[i] + " pref:" + alternate_peaks[i].called;
// 	}
//       }
//       System.out.println(msg);
    }
  }

  boolean peak_check (PhdData current, PhdData prev, PhdData next, 
		      PolyPeak p, PolyPeak best) {
    // is the candidate peak, "p", an acceptable alternate peak
    // for the peak represented at "called"?  If so, is it better
    // (stronger) than the peak represented by "best"?

    boolean result = false;

    if ((prev == null || p.peak > prev.position) &&
	(next == null || p.peak < next.position)) {
      // if the candidate peak is after the previous called peak
      // and before the next called peak...
      PhdData phd = (p.peak < current.position) ? prev : next;
      if (phd == null ||
	  (Math.abs(current.position - p.peak) <
	   Math.abs(phd.position - p.peak))) {
	// if the candidate peak is closer to this peak than
	// the next peak in the appropriate direction...
	if (best == null || p.amplitude > best.amplitude) result = true;
	//	System.out.println("type:" + p.base + " amp:" + p.amplitude + " pos:" + p.peak + " start:" + p.start + " end:" + p.end);  // debug

      }
    }
    return result;
  }

}


