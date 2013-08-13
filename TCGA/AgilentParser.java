package TCGA;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class AgilentParser {
  private BufferedReader br;
  private ArrayList<AgilentBlock> blocks;
  private ArrayList<String> genome_markers;
  private HashMap<String,Float> marker2value;

  public AgilentParser (String fn) throws FileNotFoundException,IOException {
    if (fn.lastIndexOf(".gz") == fn.length() - 3) {
      br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fn))));
    } else {
      br = new BufferedReader(new FileReader(fn));
    }
    setup();
  }

  public void parse (BufferedReader br) throws IOException {
    String line;
    
    blocks = new ArrayList<AgilentBlock>();
    String[] f,g;
    AgilentBlock ab_data_type = null;
    AgilentBlock ab_headers = null;

    HashSet[] chr2marker = new HashSet[25];
    for (int i = 0; i <= 24; i++) {
      chr2marker[i] = new HashSet<String>();
    }
    marker2value = new HashMap<String,Float>();

    while ((line = br.readLine()) != null) {
      f = line.split("\t");
      AgilentBlock ab = new AgilentBlock(f);
      String name = ab.get_name();
      if (name.equals("TYPE")) {
	// data type of data to follow
	ab_data_type = ab;
      } else if (name.equals("DATA")) {
	// data for previously specified columns and format
	if (ab_headers.get_name().equals("FEATURES")) {
	  //	  ab.report(ab_headers);
	  String gene = ab.get(ab_headers.get_index_of("GeneName"));
	  if (gene.substring(0,3).equals("chr")) {
	    g = gene.split(":");
	    String chr_s = g[0].substring(3);
	    int chr_i;
	    if (chr_s.equals("X")) {
	      chr_i = 23;
	    } else if (chr_s.equals("Y")) {
	      chr_i = 24;
	    } else {
	      chr_i = Integer.parseInt(chr_s);
	    }

	    //	    System.err.println("chr="+chr_s + " int=" + chr_i);  // debug
	    //	    markers.add(gene);
	    Float fv = Float.parseFloat(ab.get(ab_headers.get_index_of("LogRatio")));
	    chr2marker[chr_i].add(gene);
	    marker2value.put(gene,fv);
	    //	    System.err.println("gene="+gene +" log="+fv.floatValue() * 100);  // debug
	  } else {
	    //	    System.err.println("skipping non-chr marker " + gene);  // debug
	  }
	} else {
	  System.err.println("skipping data for " + ab_headers.get_name());
	}
      } else {
	// headers for data to follow
	ab_headers = ab;
	ab_headers.build_index();
      }
    }

    //
    //  sort markers genomically
    //
    genome_markers = new ArrayList<String>();
    for (int ci = 1; ci <= 24; ci++) {
      //      System.err.println(ci + ":" + chr2marker[ci].size());  // debug
      if (chr2marker[ci].size() > 0) {
	ArrayList markers = new ArrayList(chr2marker[ci]);
	Collections.sort(markers);
	genome_markers.addAll(markers);
      }
    }

  }

  private void setup() throws IOException {
    parse(br);
  }

  public static void main (String [] argv) {
    try {
      String fn = "07ER05488.txt.gz";
      //      String fn = "agil_test.txt";
      AgilentParser ap = new AgilentParser(fn);
    } catch (Exception e) {
      System.err.println("ERROR:"+e);  // debug
    }
  }

  public ArrayList<String> get_genome_markers() {
    return genome_markers;
  }

  public HashMap<String,Float> get_marker2value() {
    return marker2value;
  }

}
