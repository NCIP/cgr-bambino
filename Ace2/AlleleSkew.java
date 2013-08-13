package Ace2;

import java.util.*;

public class AlleleSkew {
  //
  // detect skewed distribution of alleles between normal and tumor reads
  //

  private Assembly assembly;
  private EnumMap<TumorNormal,EnumMap<Base,Integer>> counts;
  //  private HashSet<Base> saw_bases;
  private TreeSet<Base> saw_bases;
  private HashMap<TumorNormal,AlleleSkewInfo> summaries;

  private static float HOMOZYGOUS_CUTOFF = 0.95f;
  // fraction of observations required to consider a SNP homozygous.

  public AlleleSkew (Assembly assembly) {
    this.assembly = assembly;
    counts = new EnumMap<TumorNormal,EnumMap<Base,Integer>>(TumorNormal.class);
    counts.put(TumorNormal.NORMAL, new EnumMap<Base,Integer>(Base.class));
    counts.put(TumorNormal.TUMOR, new EnumMap<Base,Integer>(Base.class));
    //    saw_bases = new HashSet<Base>();
    saw_bases = new TreeSet<Base>();
  }

  public AlleleSkewInfo get_info_for (TumorNormal tn) {
    return summaries.get(tn);
  }

  public ArrayList<Base> get_bases() {
    return new ArrayList<Base>(saw_bases);
  }

  public void add (BaseCountInfo bi) {
    saw_bases.add(bi.base);
    // set of all observed bases

    //    for (AssemblySequence as : bi.sequences) {
    for (Object o : bi.sequences) {
      // build index of TumorNormal -> Base -> counts:
      AssemblySequence as = (AssemblySequence) o;
      Sample s = assembly.get_sample_for(as.get_name());
      TumorNormal tn = s.get_tumornormal();

      if (tn.isValid()) {
	// tumor or normal only
	EnumMap<Base,Integer> counter = counts.get(tn);
	Integer count = counter.get(bi.base);
	if (count == null) count = Integer.valueOf(0);
	counter.put(bi.base, count + 1);
      }
    }
  }

  public void analyze() {
    summaries = new HashMap<TumorNormal,AlleleSkewInfo>();

    float inverse_cutoff = 1 - HOMOZYGOUS_CUTOFF;

    for (TumorNormal tn : counts.keySet()) {
      // find total # of reads for this type:

      int total_reads = 0;
      EnumMap<Base,Integer> counter = counts.get(tn);

      for (Integer i : counter.values()) {
	total_reads += i;
      }

      if (total_reads == 0) continue;

      AlleleSkewInfo info = new AlleleSkewInfo(tn);
      summaries.put(tn, info);

      for (Base b : saw_bases) {
	// what fraction of (tumor/normal) reads use this Base?
	Integer count = counts.get(tn).get(b);
	int c = (count == null) ? 0 : count;
	float ratio = (float) c / total_reads;

	if (ratio >= HOMOZYGOUS_CUTOFF) {
	  // SNP homozygous for this allele
	  info.add_base(b);
	} else if (ratio <= inverse_cutoff) {
	  // SNP homozygous for other allele
	} else {
	  // heterozygous for both bases
	  info.add_base(b);
	}

	//	System.err.println(tn + " " + b + " " + ratio);  // debug
      }
      //      System.err.println(tn + ": " + total_reads);  // debug
      
      //      System.err.println(info);  // debug
    }

    //    System.err.println("somatic change?: " + is_somatic_variation());
    //    System.err.println("summary: " + toString());  // debug

    //    System.err.println("");  // debug

  }

  public boolean is_somatic_variation() {
    // is there a difference in SNP profile (homozygous vs heterozygous)
    // in tumor vs normal?
    ArrayList<TumorNormal> keys = new ArrayList<TumorNormal>(summaries.keySet());
    if (keys.size() > 1) {
      return !summaries.get(TumorNormal.TUMOR).equals(summaries.get(TumorNormal.NORMAL));
    } else {
      return false;
    }
  }

  public int get_somatic_variation_int() {
    ArrayList<TumorNormal> keys = new ArrayList<TumorNormal>(summaries.keySet());
    if (keys.size() > 1) {
      return summaries.get(TumorNormal.TUMOR).equals(summaries.get(TumorNormal.NORMAL)) ? 0 : 1;
    } else {
      return -1;
    }
  }

  public String toString() {
    String label;
    ArrayList<TumorNormal> keys = new ArrayList<TumorNormal>(summaries.keySet());
    if (keys.size() > 1) {
      if (is_somatic_variation()) {
	label = "somatic: " +
	  TumorNormal.NORMAL + " " +
	  summaries.get(TumorNormal.NORMAL) + ", " +
	  TumorNormal.TUMOR + " " +
	  summaries.get(TumorNormal.TUMOR);
      } else {
	label = "germline: " + summaries.get(keys.get(0));
      }
    } else if (keys.size() == 0) {
      label = "can't compute (no sample tumor/normal info)";
    } else {
      TumorNormal tn = keys.get(0);
      label = "can't compute (need both N and T): " + tn + summaries.get(tn);
    }
    return label;
  }

    
}
