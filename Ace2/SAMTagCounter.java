package Ace2;

import java.util.*;
import java.io.*;

import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.*;

public class SAMTagCounter {
  static int REPORT_INTERVAL = 100000;
  HashMap<String,Integer> saw;
  HashMap<String,HashSet<Object>> saw_values;
  int read_count;
  private boolean print_unique_mode = false;

  public void set_print_unique_mode (boolean b) {
    print_unique_mode = b;
  }

  public void count (SAMFileReader sfr, HashSet<String> wanted) {
    int i;
    
    saw = new HashMap<String,Integer>();
    saw_values = new HashMap<String,HashSet<Object>>();

    read_count=0;
    
    for (SAMRecord sr : sfr) {
      if (++read_count % REPORT_INTERVAL == 0) report();

      for (SAMTagAndValue tav : sr.getAttributes()) {
	//	System.err.println("tag="+tav.tag);  // debug
	if (wanted.contains(tav.tag)) {
	  Integer count = saw.get(tav.tag);
	  if (count == null) count = 0;
	  saw.put(tav.tag, count + 1);

	  if (print_unique_mode) {
	    HashSet<Object> sv = saw_values.get(tav.tag);
	    if (sv == null) saw_values.put(tav.tag, sv = new HashSet<Object>());

	    if (!sv.contains(tav.value)) {
	      System.err.println(tav.tag + " " + tav.value);  // debug
	      sv.add(tav.value);
	    }
	    

	  }
	}
      }
    }

    System.err.println("done:");  // debug
    report();
  }

  private void report() {
    System.err.println("reads:" + read_count);  // debug
    for (String key : saw.keySet()) {
      System.err.println(key + ": " + saw.get(key));  // debug
    }
  }

  
  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SAMTagCounter counter = new SAMTagCounter();
    HashSet<String> wanted = new HashSet<String>();
    SAMFileReader sfr = null;

    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	sfr = new SAMFileReader(new File(argv[++i]));
      } else if (argv[i].equals("-tag")) {
	wanted.add(argv[++i]);
      } else if (argv[i].equals("-print-unique")) {
	counter.set_print_unique_mode(true);
      } else {
	System.err.println("unknown argument " + argv[i]);  // debug
	System.exit(1);
      }
    }

    if (sfr == null) {
      System.err.println("specify -bam");
    } else if (wanted.size() == 0) {
      System.err.println("specify -tag [-tag ...]");  // debug
    } else {
      counter.count(sfr, wanted);
    }
  }

}