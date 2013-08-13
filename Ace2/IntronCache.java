package Ace2;
// TO DO:
// - track exact genes hit

import java.io.*;
import java.util.*;
import java.util.zip.*;

import IsoView.*;

public class IntronCache {
  //
  // load refGene-style records and cache intron positions by reference sequence.
  // should be more efficient since we are loading a very large number of these.
  //
  //  HashMap<String,HashSet<String>> cache;
  // reference sequence -> junctions
  HashMap<String,HashMap<String,HashSet<UCSCRefGene>>> cache;
  // reference sequence -> junction -> matching UCSCRefGene objects
  
  public IntronCache (UCSCRefGeneReader r) throws IOException {
    setup(r);
  }

  private void setup (UCSCRefGeneReader r) throws IOException {
    cache = new HashMap<String,HashMap<String,HashSet<UCSCRefGene>>>();

    long start = System.currentTimeMillis();

    for (UCSCRefGene rg : r) {
      Transcript t = new Transcript(rg, true);
      for (TranscriptIntron ti : t.introns) {
	String ref_name = Chromosome.standardize_name(ti.get_reference());
	if (ref_name.indexOf("chr") != 0)
	  System.err.println("WARNING: non-chr* reference sequence " + ref_name);

	HashMap<String,HashSet<UCSCRefGene>> junction_bucket = cache.get(ref_name);
	if (junction_bucket == null) cache.put(ref_name, junction_bucket = new HashMap<String,HashSet<UCSCRefGene>>());
	String range = ti.get_range_digest().toString();

	HashSet<UCSCRefGene> rg_bucket = junction_bucket.get(range);
	if (rg_bucket == null) junction_bucket.put(range, rg_bucket = new HashSet<UCSCRefGene>());

	rg_bucket.add(rg);
      }
    }

    System.err.println("intron cache:");  // debug
    int total = 0;
    for (String key : cache.keySet()) {
      int count = cache.get(key).size();
      System.err.println("  " + key + ": " + count);
      total += count;
    }
    System.err.println("  total="+total);  // debug
    System.err.println("flatfile db load: " + (System.currentTimeMillis() - start) + " ms");  // debug
  }

  public HashSet<UCSCRefGene> find_exon_junction (SplicedReadInfo sri) {
    HashSet<UCSCRefGene> result = null;
    String ref_name = Chromosome.standardize_name(sri.reference_name);
    HashMap<String,HashSet<UCSCRefGene>> junction_bucket = cache.get(ref_name);
    if (junction_bucket == null) {
      if (Chromosome.valueOfString(sri.reference_name) != null) 
	System.err.println("WTF: no bucket for " + sri.reference_name);
      // happens in mm9 for chrM (no gene annotations).
      // "mostly harmless"
    } else {
      String key = (sri.segment_1_end + 1) +  "-" +
      (sri.segment_2_start - 1);
      // convert from exon bases to intron bases
      //      System.err.println("key="+key + " bucket="+ bucket.size());  // debug
      result = junction_bucket.get(key);
    }
    return result;
  }

}
