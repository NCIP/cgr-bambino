package Trace;

public class PolyPeak {
  public PhdData called;
  // the phd data for the called peak for which this is the alternate peak

  public int start = -1;
  public int end, peak, amplitude;
  public int clip_start, clip_end;
  public short base;
  // when a peak is clipped (signal maxes out), start and end points
  // of the "plateau".
  // base uses static TraceFile.TRACE_A/C/G/T

  float area;

  PolyPeak () {
  }

  PolyPeak (short base) {
    this.base = base;
  }

  public char get_base () {
    // convert base index to A, C, G, T
    return TraceFile.index_to_base(base);
  }

  public int get_width () {
    return end - start;
  }
}
