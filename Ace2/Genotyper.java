package Ace2;

import java.util.*;

public class Genotyper {
  private Assembly assembly;
  private BaseCounter3 bc;
  private ArrayList<Base> bases;
  private HashMap<Sample,EnumMap<Base,Integer>> counts;

  public Genotyper (Assembly assembly, BaseCounter3 bc, char b1, char b2) {
    this.assembly=assembly;
    this.bc=bc;
    bases = new ArrayList<Base>();
    bases.add(Base.valueOf(b1));
    bases.add(Base.valueOf(b2));
    setup();
  }

  public Genotyper (Assembly assembly, BaseCounter3 bc, Base b1, Base b2) {
    this.assembly=assembly;
    this.bc=bc;
    bases = new ArrayList<Base>();
    bases.add(b1);
    bases.add(b2);
    setup();
  }

  private void setup() {
    //
    // map sample->nucleotide->count
    //
    counts = new HashMap<Sample,EnumMap<Base,Integer>>();

    Sample sample;
    for (Base base : bases) {
      BaseCountInfo bci = bc.get_info(base);
      if (bci != null) {
	for (Object o : bci.sequences) {
	  AssemblySequence as = (AssemblySequence) o;
	  sample = assembly.get_sample_for(as.get_name());
	  EnumMap<Base,Integer> counter = counts.get(sample);
	  if (counter == null) counts.put(sample, counter = new EnumMap<Base,Integer>(Base.class));
	  Integer count = counter.get(base);
	  if (count == null) count = Integer.valueOf(0);
	  counter.put(base, count + 1);
	}
      }
    }
  }

  public String get_summary() {
    ArrayList<String> blocks = new ArrayList<String>();
    for (Sample sample : counts.keySet()) {
      EnumMap<Base,Integer> counter = counts.get(sample);
      ArrayList<String> block = new ArrayList();
      String sample_id = sample.get_sample_name();
      block.add(sample_id == null ? "unknown_sample" : sample_id);
      for (Base base : bases) {
	block.add(base.toString());
	Integer count = counts.get(sample).get(base);
	if (count == null) count = Integer.valueOf(0);
	block.add(count.toString());
      }
      if (block.size() > 0) blocks.add(Funk.Str.join(",", block));
    }

    return blocks.size() == 0 ? "" : Funk.Str.join(" ", blocks);
  }
  
}
