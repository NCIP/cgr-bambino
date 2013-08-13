package Trace;

import java.io.*;
import java.util.*;

public class PolyFile {
  // loads phred-generated ".poly" file 
  // (trace context/polymorphism data);
  
  public Vector data;  //ugh

  public String filename;
  // name of the trace file, not the actual phdfile
  public boolean reverse_complemented = false;
  private boolean load_error = false;

  public PolyFile (String name) {
    reverse_complemented = false;
    load(name);
  }

  void load (String filename) {
    this.filename = filename;
    data = new Vector();
    DataInputStream d;
    try {
      d = StreamDelegator.getStream(filename, 2);
    } catch (IOException e) {
      load_error = true;
      return;
    }
    if (d == null) {
      load_error = true;
      return;
    }

    String line,key;
    BufferedReader d2 = new BufferedReader(new InputStreamReader(d));

    try {
      //	while (d.available() > 0) {
      PolyData p;
      line = d2.readLine();
      // initial line
      //      System.out.println("init:"+line);  // debug
      while (true) {
	// Infinite (blocking) loop until EOF or parsing is finished.
	// Ugly...
	line = d2.readLine();
	if (line == null) break;
	// EOF (shouldn't happen)
	data.addElement(new Poly(line));
      }
      load_error = false;
      // System.out.println(data.size());
    } catch (java.io.IOException e) {
      System.out.println(e);
    }
  }

  public void reverse_complement (int num_samples) {
    // reverse-complement the polyfile info.  Requires the count
    // of samples in the trace being rc'd.
    Vector newdata = new Vector();
    Poly p;
    for (int i = data.size() - 1; i >=0; i--) {
      p = (Poly) data.elementAt(i);
      p.reverse_complement(num_samples);
      newdata.addElement(p);
    }
    data = newdata;
    reverse_complemented = ! reverse_complemented;
  }

  public static void main (String [] argv) {
    // debug, testing
    Trace.StreamDelegator.set_local(true);
    PolyFile pf;
    pf = new PolyFile("/fccc/chlcfs/chlc5/edmonson/src/java2/VirtualNorthern/het/poly_dir/SHE1030-plt4_p53_4a_C07.f1.poly");
    System.out.println(pf.get_poly_at(0).called_base);  // debug
    pf.reverse_complement(10000);
    System.out.println(pf.get_poly_at(0).called_base);  // debug
    pf.reverse_complement(10000);
    System.out.println(pf.get_poly_at(0).called_base);  // debug
  }

  public Poly get_poly_at (int index) {
    return (Poly) data.elementAt(index);
  }

}

