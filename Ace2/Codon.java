package Ace2;

import static Ace2.Constants.ALIGNMENT_GAP_CHAR;
import static Ace2.Constants.ALIGNMENT_DELETION_CHAR;

public class Codon implements Cloneable {
  protected char [] bases;
  protected int [] consensus_pos;
  protected int [] unpadded_pos;

  private int load_index;
  private int id, valid_bases;
  private static ProteinTool protein = new ProteinTool();
  private boolean is_rc, is_unmappable;

  public Codon (int id) {
    is_rc = false;
    setup(id);
  }

  public Codon (int id, boolean is_rc) {
    setup(id);
    this.is_rc = is_rc;
    if (is_rc) load_index = 2;
  }

  private void setup (int id) {
    is_rc = false;
    this.id = id;
    bases = new char[3];
    bases[0] = bases[1] = bases[2] = '!';
    valid_bases = 0;

    consensus_pos = new int[3];
    unpadded_pos = new int[3];
    load_index = 0;
    is_unmappable = false;
  }

  public void set_unmappable (boolean v) {
    is_unmappable=v;
  }

  public boolean is_unmappable () {
    return is_unmappable;
  }

  public void increment_valid_bases() {
    valid_bases++;
  }

  public int get_valid_base_count() {
    return valid_bases;
  }

  public void append (char c, int upos, int cpos) {
    if (is_rc ? load_index >= 0 : load_index < 3) {
      bases[load_index] = Character.toUpperCase(c);
      if (is_rc) bases[load_index] = complement(bases[load_index]);
      //      System.err.println("codon append at pos " + load_index + " = " + bases[load_index]);  // debug

      unpadded_pos[load_index] = upos;
      consensus_pos[load_index] = cpos;

      load_index += is_rc ? -1 : 1;
    }
  }

  public boolean complete () {
    // enough bases?
    return (is_rc ? load_index < 0 : load_index > 2);
  }

  public String toString() {
    return new String(bases);
  }
  
  public int c_start_offset () {
    // at what offset in the consensus does this codon start?
    return consensus_pos[0];
  }

  public int c_center_offset () {
    // at what offset in the consensus is this codon centered?
    return consensus_pos[1];
  }

  public int c_last_offset () {
    return consensus_pos[2];
  }

  public int u_start_offset () {
    // at what offset in the unpadded sequence does this codon start?
    return unpadded_pos[0];
  }

  public int u_center_offset () {
    // at what offset in the unpadded sequence is this codon centered?
    return unpadded_pos[1];
  }

  public int u_last_offset () {
    // at what offset in the unpadded sequence does this codon end?
    return unpadded_pos[2];
  }

  public char to_code () {
    return protein.dna_to_code(toString());
  }

  public String to_name () {
    return protein.code_to_name(protein.dna_to_code(toString()));
  }

  public boolean is_stop () {
    // is this a stop codon?
    return to_code() == ProteinTool.STOP_CODE;
  }

  public boolean is_silent (Codon other) {
    return to_code() == other.to_code();
  }

  public int get_consensus_for (int unpadded) {
    // translate unpadded sequence offset to consensus
    for (int i = 0; i < 3; i++) {
      if (unpadded_pos[i] == unpadded) {
	return consensus_pos[i];
      }
    }
    return 0;
  }

  public boolean intersects_padded_consensus (int i) {
    return (i == consensus_pos[0] ||
	    i == consensus_pos[1] ||
	    i == consensus_pos[2]);
  }

  public Codon alt_cons_codon (int coff, char other) {
    // return the codon that would occur if the base at consensus
    // offset "coff" was changed to base "other"
    System.err.println("FIX ME: replace w/generate_altered()");  // debug
    Codon result = new Codon(id);
    for (int i = 0; i < 3; i++) {
      char base = consensus_pos[i] == coff ? other : bases[i];
      result.append(base, unpadded_pos[i], consensus_pos[i]);
    }
    return result;
  }

  public int get_id () {
    return id;
  }

  public Codon clone () {
    Codon c = null;
    try {
      c = (Codon) super.clone();
      // create shallow copy
      
      c.bases = new char[3];
      c.consensus_pos = new int[3];
      c.unpadded_pos = new int[3];
      System.arraycopy(bases, 0, c.bases, 0, bases.length);
      System.arraycopy(consensus_pos, 0, c.consensus_pos, 0, consensus_pos.length);
      System.arraycopy(unpadded_pos, 0, c.unpadded_pos, 0, unpadded_pos.length);

    } catch (Exception e) {
      System.err.println("clone error!:"+e);
    }
    return c;
  }

  public Codon generate_altered (int cpos, char variant_nt) {
    // return the codon that would occur if the base at consensus
    // offset cpos was changed to base variant_nt
    Codon altered = clone();
    for (int i=0; i < 3; i++) {
      if (consensus_pos[i] == cpos) {
	altered.bases[i] = variant_nt;
	if (is_rc) altered.bases[i] = complement(altered.bases[i]);
	break;
      }
    }
    
    return altered;
  }

  public char complement (char c) {
    // FIX ME: CENTRALIZE
    char result = 0;
    switch (c) {
    case 'a': result = 't'; break;
    case 'A': result = 'T'; break;
    case 'c': result = 'g'; break;
    case 'C': result = 'G'; break;
    case 'g': result = 'c'; break;
    case 'G': result = 'C'; break;
    case 't': result = 'a'; break;
    case 'T': result = 'A'; break;
    case 'n': result = 'n'; break;
    case 'N': result = 'N'; break;
    case ALIGNMENT_GAP_CHAR: result = ALIGNMENT_GAP_CHAR; break;
    case ALIGNMENT_DELETION_CHAR: result = ALIGNMENT_DELETION_CHAR; break;
    default:
      System.err.println("error, don't know how to complement nt " + c);  // debug
      result = c;
    }
    return result;
  }

  public void set_codon_number (int i) {
    id = i;
  }

  public int get_load_index () {
    // debug only
    return load_index;
  }

  public boolean is_spliced() {
    // codon continues in a different exon
    return Math.abs(unpadded_pos[0] - unpadded_pos[2]) != 2;
  }

  public void intron_splice_adjust (IntronCompressor ic, PadMap pm) {
    //
    // for intron splicing hack
    //
    int offset;
    for (int i = 0; i < 3; i++) {
      offset = ic.get_start_shift(consensus_pos[i], false);
      consensus_pos[i] -= offset;

      unpadded_pos[i] = pm.get_padded_to_unpadded(consensus_pos[i]);
      // untested
    }
  }    


}
