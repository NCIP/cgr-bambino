package TCGA;

import java.util.*;
import java.io.IOException;

public class AgilentGlommer {
  // aggregate Agilent reports into single GenomicMeasurement-compatible report file

  public void process_files(ArrayList<String> files) {

    try {
      AgilentParser last_ap = null;
      for (String file : files) {
	System.err.println("parsing file " + file);  // debug
	AgilentParser ap = new AgilentParser(file);
	ArrayList<String> headers = ap.get_genome_markers();
	if (last_ap == null) {
	  // first set, report headers
	  System.out.print("Reporter");
	  for (int i = 0; i < headers.size(); i++) {
	    System.out.print("\t");  // debug
	    System.out.print(headers.get(i));  // debug
	  }
	  System.out.println("");  // debug
	} else {
	  if (!ap.get_genome_markers().equals(last_ap.get_genome_markers())) {
	    throw new IOException("marker list doesn't match");
	  }
	}

	// report data
	HashMap<String,Float> marker2value = ap.get_marker2value();
	Float fv;
	System.out.print(file);
	for (int i = 0; i < headers.size(); i++) {
	  System.out.print("\t");  // debug
	  fv = marker2value.get(headers.get(i));
	  if (true) {
	    double raw = Math.pow(10, fv);
	    double log2 = Math.log(raw) / Math.log(2);
	    System.out.print(Math.round(log2));  // debug
	  } else if (false) {
	    System.out.print(fv);
	  } else {
	    float v = fv * 10;
	    System.out.print(Math.round(v));
	  }
	}
	System.out.println("");  // debug

	last_ap = ap;
      }
    } catch (Exception e) {
      System.err.println("error:"+e);  // debug
    }    
  }

  public static void main (String [] argv) {
    ArrayList<String> files = new ArrayList<String>();
    if (true) {
      files.add("07ER05488.txt.gz");
      files.add("07ES06177.txt.gz");
      files.add("07ES06294.txt.gz");
      files.add("07ES06734.txt.gz");
      files.add("07ES06798.txt.gz");
      files.add("07VA02686.txt.gz");
      files.add("07VA02818.txt.gz");
      files.add("07VA02843.txt.gz");
      files.add("07VA02864.txt.gz");
      files.add("07VA03129.txt.gz");
      files.add("07VA03140.txt.gz");
      files.add("07VA03153.txt.gz");
      files.add("07VA03202.txt.gz");
      files.add("07VA03261.txt.gz");
      files.add("07VA03279.txt.gz");
      files.add("07VA03328.txt.gz");
      files.add("07VA03360.txt.gz");
      files.add("07VA03416.txt.gz");
      files.add("07VA03483.txt.gz");
      files.add("07VA03484.txt.gz");
      files.add("07VA03498.txt.gz");
      files.add("07VA03595.txt.gz");
      files.add("07VA03596.txt.gz");
      files.add("07VA03603.txt.gz");
      files.add("07VA03611.txt.gz");
      files.add("07VA03622.txt.gz");
      files.add("07VA03623.txt.gz");
      files.add("07VA03715.txt.gz");
      files.add("07VA03725.txt.gz");
      files.add("07VA03783.txt.gz");
    } else {
      files.add("agil_test.txt");
      files.add("agil_test2.txt");
    }

    AgilentGlommer ag = new AgilentGlommer();
    ag.process_files(files);
  }

  
}
