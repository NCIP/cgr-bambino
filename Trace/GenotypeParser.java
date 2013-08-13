package Trace;
import java.io.*;
import java.net.*;
import java.util.*;

import Funk.DelimitedFile;

public class GenotypeParser {
  private Hashtable<String,GenotypeData> snps;
  private Vector<String> sorted_snps;
  private HashSet<String> all_traces;
  // unique list of traces
  private HashMap<String,String> t2o;
  private HashMap<String,GenotypeTrace> all_gt;
  private URL url;

  public static boolean OLD_MODE = false;

  public GenotypeParser (String filename) throws Exception {
    FileInputStream fis = new FileInputStream(filename);
    setup(fis);
  }

  public GenotypeParser (URL url) throws Exception {
    this.url = url;
    URLConnection uc = url.openConnection();
    //    uc.setUseCaches(true);
    // ?
    setup(uc.getInputStream());
  }

  private void setup (InputStream is) throws Exception {
    if (OLD_MODE) {
      // original format: no column labels
      parse_old(is);
    } else {
      // standard delimited file with column labels
      parse_input(is);
    }
  }
  
  public URL get_url () {
    return url;
  }

  private void parse_input (InputStream is) throws Exception {
    //
    // parse version 2 of info format
    //
    DelimitedFile df = new DelimitedFile();
    df.uppercase_labels(true);
    df.parse(is);

    Enumeration rows = df.get_rows_enumeration();

    snps = new Hashtable<String,GenotypeData>();
    all_traces = new HashSet<String>();

    t2o = new HashMap<String,String>();
    // sanity check: are references to a given trace all in the same orientation?
    all_gt = new HashMap<String,GenotypeTrace>();

    while (rows.hasMoreElements()) {
      Hashtable row = (Hashtable) rows.nextElement();

      //      System.err.println("labels: " + df.get_labels());

      String snp_name = (String) row.get("SNPNAME");

      GenotypeData gd;
      if (snps.containsKey(snp_name)) {
        // already have a record for this SNP
        gd = snps.get(snp_name);
      } else {
        // new SNP
        gd = new GenotypeData();
        gd.snp_name = snp_name;
        snps.put(snp_name, gd);
      }
      
      gd.sample_id = (String) row.get("SAMPLEID");
      gd.genotype_quality = hash_int(row, "SNPGENOTYPEQUALITY");

      GenotypeTrace gt = new GenotypeTrace();
      gt.allele_1 = (String) row.get("ALLELE1");
      gt.allele_2 = (String) row.get("ALLELE2");

      if (false) {
        // disabled: indel alleles may be 1 or 2 characters
        if (gt.allele_1.length() > 1) {
          throw new Exception("error: allele1 longer than 1 character: " + gt.allele_1);
        }

        if (gt.allele_2.length() > 1) {
          throw new Exception("error: allele2 longer than 1 character: " + gt.allele_2);
        }
      }

      gt.trace_id = (String) row.get("TRACEID");
      gt.trace_label = (String) row.get("TRACELABEL");
      //      System.err.println("label=" + gt.trace_label);  // debug

      gt.trace_offset = hash_int(row, "TRACEOFFSET");
      gt.sequence_position = hash_int(row, "SNPPOS");
      gt.orientation = (String) row.get("ORIENTATION");
      gt.genotype_quality = hash_int(row, "TRACEGENOTYPEQUALITY");

      all_traces.add(gt.trace_id);
      all_gt.put(gt.trace_id, gt);

      if (t2o.containsKey(gt.trace_id)) {
        if (!t2o.get(gt.trace_id).equals(gt.orientation)) {
          // sanity check: we need references to traces 
          throw new Exception("trace orientation mismatch!");
        }
      } else {
        t2o.put(gt.trace_id, gt.orientation);
      }
        
      gd.add_trace(gt);
      // each GenotypeData can have multiple GenotypeTrace records
    }

    sorted_snps = new Vector<String>(snps.keySet());
    Collections.sort(sorted_snps);
  }

  private void parse_old (InputStream is) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    StringTokenizer st;
    // HACK: could break if null entries!

    snps = new Hashtable<String,GenotypeData>();
    all_traces = new HashSet<String>();
    all_gt = new HashMap<String,GenotypeTrace>();

    t2o = new HashMap<String,String>();
    // sanity check: are references to a given trace all in the same orientation?

    String allele_1,allele_2;

    while ((line = br.readLine()) != null) {
      st = new StringTokenizer(line);

      String snp_name = st.nextToken();
      GenotypeData gd;
      if (snps.containsKey(snp_name)) {
        // already have a record for this SNP
        gd = snps.get(snp_name);
      } else {
        // new SNP
        gd = new GenotypeData();
        gd.snp_name = snp_name;
        snps.put(snp_name, gd);
      }
      
      gd.sample_id = st.nextToken();
      allele_1 = st.nextToken();
      allele_2 = st.nextToken();

      if (false) {
        if (allele_1.length() > 1) {
          throw new Exception("error: allele longer than 1 character: " + allele_1);
        }

        if (allele_2.length() > 1) {
          throw new Exception("error: allele longer than 1 character: " + allele_2);
        }
      }

      gd.genotype_quality = Integer.parseInt(st.nextToken());

      while (st.hasMoreTokens()) {
        GenotypeTrace gt = new GenotypeTrace();
        gt.allele_1 = allele_1;
        gt.allele_2 = allele_2;
        // set alleles to values declared at start of row.
        // presumably these apply to all this row's traces,
        // and only this row's traces.

        //        gt.trace_id = st.nextToken() + ".ab1";
	// FIX ME: HORRIBLE HACK
        gt.trace_id = st.nextToken();
        // trace server

        gt.trace_offset = Integer.parseInt(st.nextToken());
        gt.sequence_position = Integer.parseInt(st.nextToken());
        gt.orientation = st.nextToken();
        gt.genotype_quality = Integer.parseInt(st.nextToken());

	all_traces.add(gt.trace_id);
        all_gt.put(gt.trace_id, gt);

	if (t2o.containsKey(gt.trace_id)) {
	  if (!t2o.get(gt.trace_id).equals(gt.orientation)) {
	    // sanity check: we need references to traces 
	    throw new Exception("trace orientation mismatch!");
	  }
	} else {
	  t2o.put(gt.trace_id, gt.orientation);
	}
        
        gd.add_trace(gt);
        // each GenotypeData can have multiple GenotypeTrace records
      }
    }

    sorted_snps = new Vector<String>(snps.keySet());
    Collections.sort(sorted_snps);

    //
    //
    
  }

  public static void main (String [] argv) {
    try {
      
      GenotypeParser gp;
      if (true) {
        //
        // load from file
        //
        //        String filename = "genotype_small_ti_onecol_labeled.dat";
        //        String filename = "genotype_tiny.dat";
        //        String filename = "genotype_rich_error.dat";
        //        String filename = "genotype_doubleallele.dat";
        String filename = argv.length > 0 ? argv[0] : 
          "genotype_doubleallele.dat";

        System.err.println("loading from local file " + filename);  // debug
        gp = new GenotypeParser(filename);
      } else {
        //String addr = "http://lpgws.nci.nih.gov/mne/gv/genotype.dat";
        //        String addr = "http://lpgws.nci.nih.gov/mne/gv/genotype_small_ti.dat";
        String addr = "http://lpgws.nci.nih.gov/mne/gv/genotype_small_ti_onecol.dat";
        System.err.println("loading from " + addr);
        gp = new GenotypeParser(new URL(addr));
      }

      if (true) {
        new GenotypeViewer(gp);
      } else {
        //      GenotypeData gd = gp.get_genotypes_for_snp("geneC17_217");
        GenotypeData gd = gp.get_genotypes_for_snp("geneC17_71");
        System.err.println("gd=" + gd);  // debug
        HashSet<String> alleles = gd.get_alleles();
        System.err.println(alleles);  // debug
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      new Exception().printStackTrace();
    }
  }

  public Vector<String> get_snp_ids () {
    // return (sorted) list of SNP IDs
    return sorted_snps;
  }

  public GenotypeData get_genotypes_for_snp (String snp_id) {
    return snps.get(snp_id);
  }
  
  public HashSet<String> get_unique_traces () {
    return all_traces;
  }

  public GenotypeTrace get_genotypetrace_for (String trace) {
    return all_gt.get(trace);
  }

  public String get_orientation_for (String trace) {
    return t2o.get(trace);
  }

  private void die () {
    die("dying");
  }
  private void die (String msg) {
    System.err.println(msg);  // debug
    System.exit(1);
  }

  private int hash_int (Hashtable ht, String key) {
    int result = 0;
    if (ht.containsKey(key)) result = Integer.parseInt((String) ht.get(key));
    return(result);
  }

  
}
