package Ace2;

import java.util.*;

public class KnownGenomeBuild {
  private ArrayList<GenomeBuildInfo> known_genomes;

  public KnownGenomeBuild () {
    setup();
  }

  private void setup() {
    known_genomes = new ArrayList<GenomeBuildInfo>();

    GenomeBuildInfo gbi = new GenomeBuildInfo("hg18");
    known_genomes.add(gbi);
    gbi.set("chr1","247249719");
    gbi.set("chr10","135374737");
    gbi.set("chr11","134452384");
    gbi.set("chr12","132349534");
    gbi.set("chr13","114142980");
    gbi.set("chr14","106368585");
    gbi.set("chr15","100338915");
    gbi.set("chr16","88827254");
    gbi.set("chr17","78774742");
    gbi.set("chr18","76117153");
    gbi.set("chr19","63811651");
    gbi.set("chr2","242951149");
    gbi.set("chr20","62435964");
    gbi.set("chr21","46944323");
    gbi.set("chr22","49691432");
    gbi.set("chr3","199501827");
    gbi.set("chr4","191273063");
    gbi.set("chr5","180857866");
    gbi.set("chr6","170899992");
    gbi.set("chr7","158821424");
    gbi.set("chr8","146274826");
    gbi.set("chr9","140273252");
    gbi.set("chrM","16571");
    gbi.set("chrX","154913754");
    gbi.set("chrY","57772954");

    gbi = new GenomeBuildInfo("hg19");
    known_genomes.add(gbi);
    gbi.set("chr1","249250621");
    gbi.set("chr10","135534747");
    gbi.set("chr11","135006516");
    gbi.set("chr12","133851895");
    gbi.set("chr13","115169878");
    gbi.set("chr14","107349540");
    gbi.set("chr15","102531392");
    gbi.set("chr16","90354753");
    gbi.set("chr17","81195210");
    gbi.set("chr18","78077248");
    gbi.set("chr19","59128983");
    gbi.set("chr2","243199373");
    gbi.set("chr20","63025520");
    gbi.set("chr21","48129895");
    gbi.set("chr22","51304566");
    gbi.set("chr3","198022430");
    gbi.set("chr4","191154276");
    gbi.set("chr5","180915260");
    gbi.set("chr6","171115067");
    gbi.set("chr7","159138663");
    gbi.set("chr8","146364022");
    gbi.set("chr9","141213431");
    gbi.set("chrM","16571");
    gbi.set("chrX","155270560");
    gbi.set("chrY","59373566");

    gbi = new GenomeBuildInfo("hg19_updated_MT");
    known_genomes.add(gbi);
    gbi.set("chr1","249250621");
    gbi.set("chr10","135534747");
    gbi.set("chr11","135006516");
    gbi.set("chr12","133851895");
    gbi.set("chr13","115169878");
    gbi.set("chr14","107349540");
    gbi.set("chr15","102531392");
    gbi.set("chr16","90354753");
    gbi.set("chr17","81195210");
    gbi.set("chr18","78077248");
    gbi.set("chr19","59128983");
    gbi.set("chr2","243199373");
    gbi.set("chr20","63025520");
    gbi.set("chr21","48129895");
    gbi.set("chr22","51304566");
    gbi.set("chr3","198022430");
    gbi.set("chr4","191154276");
    gbi.set("chr5","180915260");
    gbi.set("chr6","171115067");
    gbi.set("chr7","159138663");
    gbi.set("chr8","146364022");
    gbi.set("chr9","141213431");
    gbi.set("chrM","16569");
    gbi.set("chrX","155270560");
    gbi.set("chrY","59373566");
  }

  public String identify_build(HashMap<Chromosome,Integer> lengths) {
    String result = null;
    boolean VERBOSE = false;

    HashMap<String,Integer> hit_counts = new HashMap<String,Integer>();

    HashSet<String> hits = new HashSet<String>();
    HashSet<String> misses = new HashSet<String>();

    for (GenomeBuildInfo gbi : known_genomes) {
      for (Chromosome c : lengths.keySet()) {
	// each chr in the query file
	int query_len = lengths.get(c);
	Integer known_len = gbi.chr_lengths.get(c);
	if (known_len != null) {
	  if (query_len == known_len) {
	    if (VERBOSE) System.err.println("hit for " + c + " on " + gbi.build_name);  // debug
	    hits.add(gbi.build_name);
	  } else {
	    if (VERBOSE) System.err.println("miss for " + c + " on " + gbi.build_name + " expected:" + known_len + " this:" + query_len);  // debug
	    misses.add(gbi.build_name);
	  }
	}
      }
    }
    
    ArrayList<String> passed = new ArrayList<String>();
    
    for (String name : hits) {
      if (!misses.contains(name)) passed.add(name);
    }

    if (passed.size() == 1) result = passed.get(0);

    return result;
  }

  public GenomeBuildInfo find_build(String name) {
    GenomeBuildInfo result = null;
    for (GenomeBuildInfo gbi : known_genomes) {
      if (gbi.get_name().equals(name)) {
	result = gbi;
	break;
      }
    }
    return result;
  }

}
