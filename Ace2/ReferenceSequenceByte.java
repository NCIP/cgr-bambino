package Ace2;

import java.io.*;
import java.util.*;

public class ReferenceSequenceByte implements ReferenceSequence {
  //
  //
  //
  private static final int BUFFER_LEN = 1000;
  private ChromosomeDisambiguator cd;
  private String genome,reference_name;
  private GenomeBuildInfo gbi;
  private int base_num;
  private char base;

  public ReferenceSequenceByte (String params) {
    String[] stuff = params.split(",");
    if (stuff.length == 4) {
      int i = 0;
      genome = stuff[i++];

      KnownGenomeBuild kgb = new KnownGenomeBuild();
      gbi = kgb.find_build(genome);
      if (gbi == null) {
	System.err.println("ERROR: dunno genome " + genome);  // debug
	System.exit(1);
      } else {
	reference_name = stuff[i++];
	base_num = Integer.parseInt(stuff[i++]);
	base = stuff[i++].charAt(0);
	System.err.println("base is " + base);  // debug

	HashSet<String> refs = new HashSet<String>();
	refs.add(reference_name);
	cd = new ChromosomeDisambiguator(refs);
      }
    } else {
      System.err.println("error: needs 3 fields");  // debug
      System.exit(1);
    }
    
  }

  public byte[] get_region (String sequence_name, int start_base, int length) throws IOException {
    throw new IOException("get_region() unsupported");
  };
  // fetch a region of a reference sequence
  // start_base is base NUMBER (i.e. starts with 1), NOT 0-based index

  public byte[] get_all (String sequence_name) throws IOException {
    // fetch entire reference sequence
    int len = get_length(sequence_name);
    byte[] reference_sequence = new byte[len];
    Arrays.fill(reference_sequence, (byte) 'N');
    String local_name = cd.find(sequence_name);
    if (local_name != null && local_name.equals(reference_name)) {
      reference_sequence[base_num - 1] = (byte) base;
    }
    return reference_sequence;
  }

  public int get_length (String sequence_name) throws IOException {
    // sequence length
    return gbi.get_length_for(sequence_name);
  }

  public boolean supports_sequence_list() {
    return false;
  }

  public ArrayList<String> get_sequence_names() {
    return null;
  }


}

