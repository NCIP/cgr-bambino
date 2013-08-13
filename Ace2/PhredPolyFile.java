package Ace2;

import java.io.*;
import java.util.*;

public class PhredPolyFile {
  private String[] header_line;
  private ArrayList<PhredPolyFileEntry> poly;

  public static void main (String[] argv) {
    try {
      PhredPolyFile pf = new PhredPolyFile();
      //      pf.parse(new File("c:/generatable/max_poly/sg071410_07029_nuc_cadm1_rs4445669_seq_r_2010-07-14.ab1.poly"));

      System.err.println("before");  // debug
      pf.parse(new File("c:\\generatable\\max_poly\\SG052110_07020_gDNA_FRMD4A_rs4748047_seq_r_2010-05-21.ab1.poly"));
      System.err.println("after");  // debug
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }

  public void parse (File f) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
    String line;

    line = br.readLine();
    header_line = line.split("\\s+");

    poly = new ArrayList<PhredPolyFileEntry>();

    while ((line = br.readLine()) != null) {
      poly.add(new PhredPolyFileEntry(line));
    }

    //    System.err.println("len="+poly.size());  // debug
  }

  public PhredPolyFileEntry get_entry_for_index (int i) {
    return poly.get(i);
  }

  public void reverse_complement() {
    Collections.reverse(poly);
    for (PhredPolyFileEntry pfe : poly) {
      pfe.complement();
    }
  }

}

