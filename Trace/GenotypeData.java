package Trace;
import java.util.Vector;
import java.util.HashSet;
import java.util.Iterator;

public class GenotypeData {
  //
  // top-level genotype data -- shared among a set of traces
  //
  public String snp_name,sample_id;
  public int genotype_quality;
  // global for a set 
// (generics:)    public Vector<GenotypeTrace> traces;
  public Vector traces;
  
  public GenotypeData() {
// (generics:)      traces = new Vector<GenotypeTrace>();
    traces = new Vector();
  }

  public void add_trace (GenotypeTrace gt) {
    traces.add(gt);
  }

// (generics:)    public HashSet<String> get_alleles() {
  public HashSet get_alleles() {
// (generics:)      HashSet<String> hs = new HashSet<String>();
    HashSet hs = new HashSet();
    //    for (GenotypeTrace gt : traces) {
    for (Iterator i = traces.iterator(); i.hasNext(); ) {
      GenotypeTrace gt = (GenotypeTrace) i.next();
      hs.add(gt.allele_1);
      hs.add(gt.allele_2);
    }
    return hs;
  }

}
