package Ace2;

import java.util.*;

import static Ace2.Constants.ALIGNMENT_GAP_CHAR;
import net.sf.samtools.SAMRecord;

class BaseCounter3 {
  public static boolean ALLOW_GAP_SNPS = false;
  // FIX ME: make an instance variable
  public static boolean ALLOW_UNKNOWN_BASES = false;
  public static boolean INCLUDE_ZERO_COUNTS = true;

  private Assembly assembly;
  private int min_quality = 0;

  private EnumMap<Base, BaseCountInfo> saw_bases = new EnumMap<Base,BaseCountInfo>(Base.class);
  // keys: unique bases observed
  // values: read count for each base type
  //
  // NOTE: some keys may have 0 counts! (i.e. calling add_base(char))

  public BaseCounter3 () {
  }

  public BaseCounter3 (Assembly assembly) {
    this.assembly=assembly;
  }

  public void add_base (char base) {
    add_base(Base.valueOf(base), null, false);
  }

  public void add_base (Base base) {
    add_base(base, null, false);
  }

  public void add_base (Base key, Object sequence, boolean include_count) {
    BaseCountInfo bci = saw_bases.get(key);
    if (bci == null) saw_bases.put(key, bci = new BaseCountInfo(key));
    if (include_count) bci.count++;
    // might not want to include if we want to say track consensus base
    // but not include that observation in the read count
    if (sequence != null) bci.sequences.add(sequence);

    if (!ALLOW_GAP_SNPS) saw_bases.remove(Base.BASE_GAP);
    if (!ALLOW_UNKNOWN_BASES) saw_bases.remove(Base.BASE_UNKNOWN);
    // FIX ME/VILE: move these to some summary routine for better performance
    // problem with that is that would then have to enforce calling
    // summary/finish function before EVERY operation (bleh)
  }

  public void set_min_quality (int q) {
    // minimum base quality to include in aggregation
    min_quality = q;
  }

  public void add_sequences(int cpos) {
    int q;
    for (AssemblySequence as : assembly.get_aligned_at(cpos)) {
      q = as.get_quality(cpos);
      if (q < min_quality) continue;
      SAMRecord sr = as.get_samrecord();
      if (sr != null && sr.getDuplicateReadFlag()) {
	// ignore optical duplicates
	continue;
      }
      
      //      add_base(Base.valueOf(as.get_base(cpos)), q, as, true);
      add_base(Base.valueOf(as.get_base(cpos)), null, true);
    }
  }

  public EnumMap<Base,BaseCountInfo> get_saw_bases() {
    return saw_bases;
  }

  public BaseCountInfo get_info (Base base) {
    return saw_bases.get(base);
  }

  public BaseCountInfo get_info (char base) {
    return saw_bases.get(Base.valueOf(base));
  }

  public int count_bases() {
    //
    // how many different bases have we observed?
    //
    return saw_bases.size();
  }

  public int count_sequences() {
    // this includes coverage for ALL alleles (not just reference / best SNP)
    int total=0;
    for (BaseCountInfo bci : saw_bases.values()) {
      total += bci.count;
    }
    return total;
  }

  public int count_sequences_for(char base) {
    BaseCountInfo bci = saw_bases.get(Base.valueOf(base));
    return bci == null ? 0 : bci.count;
  }

  public Base[] get_bases_by_frequency() {
    HashSet<Integer> sizes = new HashSet<Integer>();
    int valid=0;
    for (BaseCountInfo bci : saw_bases.values()) {
      if (INCLUDE_ZERO_COUNTS ? true : bci.count > 0) {
	// count 0 is possible for "placeholder" counts, e.g. add_base(c) when
	// including consensus nucleotide
	sizes.add(bci.count);
	valid++;
      }
    }

    ArrayList<Integer> s2 = new ArrayList<Integer>(sizes);
    Collections.sort(s2);
    Collections.reverse(s2);
    //    System.err.println(s2);  // debug

    Base[] results = new Base[valid];
    int ri=0;
    
    for (int sz : s2) {
      for (Base b : saw_bases.keySet()) {
	if (saw_bases.get(b).count == sz) {
	  results[ri++] = b;
	}
      }
    }

    return results;
  }

  public HashSet<Character> get_bases() {
    HashSet<Character> results = new HashSet<Character>();
    for (Base b : saw_bases.keySet()) {
      if (INCLUDE_ZERO_COUNTS ? true : saw_bases.get(b).count > 0) {
	results.add(b.charValue());
      }
    }
    return results;
  }

  public static void main (String[] argv) {
    Base x = Base.valueOf('a');
    System.err.println(x);  // debug

  }

  

}
