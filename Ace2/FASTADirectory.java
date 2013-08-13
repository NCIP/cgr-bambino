package Ace2;
import java.util.*;
import java.io.*;

public class FASTADirectory implements ReferenceSequence {
  //
  // read FASTA data from a directory (each sequence in its own file, e.g. hg18 distribution)
  //

  ArrayList<String> file_suffixes;
  private String fa_dir;

  public FASTADirectory (String directory) {
    fa_dir = directory;
    file_suffixes = new ArrayList<String>();
    file_suffixes.add("fa");
    file_suffixes.add("fasta");
  }

  public byte[] get_region (String sequence_name, int start_base, int length) throws IOException {
    // fetch a region of a reference sequence

    byte[] results = null;
    File f = find_fasta_file(sequence_name);
    if (f != null) {
      RandomAccessFile raf = new RandomAccessFile(f, "r");
      FASTAFileTools ffp = new FASTAFileTools();
      ffp.detect_record_properties(raf);
      results = ffp.get_region(raf, start_base, length);
    }

    return results;
  }

  public byte[] get_all (String sequence_name) throws IOException {
    //
    // read entire reference sequence
    //
    byte[] results = null;
    File f = find_fasta_file(sequence_name);
    if (f != null) {
      RandomAccessFile raf = new RandomAccessFile(f, "r");
      FASTAFileTools ffp = new FASTAFileTools();
      ffp.detect_record_properties(raf);
      results = ffp.get_all(raf);
    }
    //    System.err.println("results="+new String(results));  // debug

    return results;
  }


  public int get_length (String sequence_name) throws IOException {
    // sequence length
    File f = find_fasta_file(sequence_name);
    int result = ReferenceSequence.NULL_LENGTH;
    if (f != null) {
      RandomAccessFile raf = new RandomAccessFile(f, "r");
      FASTAFileTools ffp = new FASTAFileTools();
      FAIIndexRecord index = ffp.detect_record_properties(raf);
      result = index.sequence_length;
      //      result = (int) ffp.get_sequence_length();
      raf.close();
    }
    return result;
  }

  public File find_fasta_file (String sequence_name) {
    File result = null;
    for (String key : SAMUtils.get_refname_alternates(sequence_name)) {
      for (String suffix : file_suffixes) {
	//	System.err.println("trying " + key + " " + suffix);  // debug
	String fq = fa_dir + File.separator + key + "." + suffix;
	File f = new File(fq);
	if (f.exists()) {
	  result = f;
	  break;
	}
      }
      if (result != null) break;
    }

    if (result == null) {
      System.err.println("WARNING: can't find FASTA file for " + sequence_name);  // debug
    }

    return result;
  }

  public static void main (String[] argv) {
    try {
      FASTADirectory fad = new FASTADirectory("c:\\generatable\\hg18");
      String id = "chr17_mini";
      System.err.println("len=" + fad.get_length(id));
      
      byte[] sequence_all = fad.get_all(id);
      System.err.println("read len=" + sequence_all.length);  // debug
      System.err.println("full="+new String(sequence_all));  // debug

      byte[] sequence_excerpt = fad.get_region(id, 40, 40);
      System.err.println("got="+new String(sequence_excerpt));  // debug

    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }

  public boolean supports_sequence_list() {
    return true;
  }

  public ArrayList<String> get_sequence_names() {
   ArrayList<String> results = null;
    System.err.println("FIX ME: FASTADirectory.get_sequence_list() not implemented");  // debug
    return results;
   }


}
