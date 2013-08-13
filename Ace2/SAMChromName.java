package Ace2;
// get name used for a chrom in a .bam file
// MNE 11/2009

import net.sf.samtools.*;
import java.io.*;
import java.util.*;

public class SAMChromName {
  SAMFileReader sfr;
  String[] chr_labels;

  public SAMChromName (File f) {
    sfr = new SAMFileReader(f);
    setup();
  }

  public SAMChromName (SAMFileReader sfr) {
    this.sfr = sfr;
    setup();
  }

  private void setup() {
    SAMFileHeader h = sfr.getFileHeader();
    SAMSequenceDictionary dict = h.getSequenceDictionary();
    chr_labels = new String[25];
    Arrays.fill(chr_labels, null);
    for (SAMSequenceRecord ssr : dict.getSequences()) {
      String ref_name = ssr.getSequenceName();
      Chromosome c = Chromosome.valueOfString(ref_name);
      if (c != null) {
	//
	// .bam reference sequence name matches a known chromosome
	//
	int c_index = c.toInt() - 1;
	chr_labels[c_index] = ref_name;
	// reference name the .bam file uses for this chromosome
      }
    }
  }

  public String get_bam_reference_name (String chr) {
    Chromosome c = Chromosome.valueOfString(chr);
    return c == null ? null : chr_labels[c.toInt() - 1];
  }

  public String[] get_labels() {
    return chr_labels;
  }

  public static void main (String[] argv) {
    File bam_file = null;
    String chr = null;
    boolean show_list = false;
    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	bam_file = new File(argv[++i]);
      } else if (argv[i].equals("-chr")) {
	chr = argv[++i];
      } else if (argv[i].equals("-list")) {
	show_list = true;
      } else {
	System.err.println("ERROR: unknown parameter " + argv[i]);  // debug
	System.exit(1);
      }
    }

    if (bam_file != null) {
      SAMChromName scn = new SAMChromName(bam_file);
      if (chr != null) {
	String ref_name = scn.get_bam_reference_name(chr);  // debug
	if (ref_name == null) ref_name = "";
	//      System.err.println(chr + " => " + ref_name);
	System.out.println(ref_name);  // debug
      } else if (show_list) {
	String[] labels = scn.get_labels();
	ArrayList<String> ls = new ArrayList<String>();
	for (int i=0; i < labels.length; i++) {
	  String l = labels[i];
	  if (l == null) l = "";
	  ls.add(l);
	}
	System.out.println(Funk.Str.join(",", ls));
      } else {
	System.err.println("specify -chr or -list");  // debug
      }
    } else {
      System.err.println("specify -bam and [-chr|-list]");  // debug
    }

  }
  
}