package Ace2;

import java.util.*;
import net.sf.samtools.*;

public class AnnotationLoader extends Observable implements Runnable {
  AceViewerConfig config;
  private boolean is_loaded;

  public AnnotationLoader (AceViewerConfig config, boolean async) {
    this.config = config;
    is_loaded = false;
    if (async) {
      new Thread(this).start();
    } else {
      run();
    }
  }

  public void run() {
    
    String chr_name = null;

    boolean broken = false;

    if (config.region.tname == null) {
      broken = true;
      //    } || config.region.tname.indexOf("chr") != 0) {
    } else {
      chr_name = Chromosome.standardize_name(config.region.tname);
      //      System.err.println("chr: before="+config.region.tname + " after=" + chr_name);  // debug
    }

    if (broken) {
      is_loaded = true;
      config.refgenes = null;
      config.set_dbsnp(null);
      return;
    }

    Funk.Timer tm = new Funk.Timer("db annotation setup");

    JDBCCache ucsc = config.get_ucsc_genome_client();
    Exception exception = null;

    //
    //  refGene setup:
    //
    String cmd;
    if (config.region.gene_name != null) {
      cmd = "select * from refGene where name2=\"" + config.region.gene_name + "\"";
    } else {
      cmd = "select * from refGene where chrom=\"" + config.region.get_ucsc_chromosome() + "\"" +
	" and not(txEnd < " + config.region.range.start + 
	" or txStart > " + config.region.range.end + ") order by name";
    }
    System.err.println("refGene query: " + cmd);  // debug

    ArrayList<HashMap<String,String>> results = null;
    try {
      results = ucsc.query(cmd);
      if (results.size() > 0) {
	ArrayList<RefGene> refgenes = new ArrayList<RefGene>();
	PadMap pm = config.intron_compressor == null ?
	  config.assembly.get_padmap() : config.intron_compressor.get_raw_padmap();
	// if introns have been trimmed, we need to do setup with
	// UNTRIMMED reference sequence as this is required to ensure
	// all canonical exons are parsed properly

	for (HashMap<String,String> row : results) {
	  RefGene rg = new RefGene(row);
	  rg.consensus_adjust(config.ruler_start);
	  rg.consensus_setup(pm);
	  rg.intron_splice_adjust(config);
	  refgenes.add(rg);
	}
	config.refgenes = refgenes;
	// set only when completely finished
      }
    } catch (Exception e) {
      System.err.println("database error: " + e);  // debug
      e.printStackTrace();
      exception = e;
    }

    //
    //  dbSNP setup:
    //

    String snp_table = JDBCCache.UCSC_SNP_TABLE;
    System.err.println("default SNP table: " + snp_table);  // debug

    if (config.DETECT_SNP_TABLE_NAME && !config.DETECTED_SNP_TABLE_NAME) {
      //
      // detect name of latest available dbSNP build table (once)
      //
      String schema_table = "Tables_in_" + JDBCCache.UCSC_DB_DATABASE;
      //      System.err.println("table=" + schema_table);  // debug
      cmd = "show tables where " + schema_table + " like 'snp1%' and length(" + schema_table + ") = 6";
      try {
	int highest = 0;
	for (HashMap<String,String> row : ucsc.query(cmd)) {
	  String tname = row.get("TABLE_NAME");
	  int build_num = Integer.parseInt(tname.substring(3));
	  //	  System.err.println("bn="+build_num);  // debug
	  if (build_num > highest) highest = build_num;
	}
	if (highest > 0) {
	  snp_table = "snp" + highest;
	  System.err.println("autodetected SNP table " + snp_table);  // debug
	  JDBCCache.UCSC_SNP_TABLE = snp_table;
	}
	config.DETECTED_SNP_TABLE_NAME = true;
      } catch (Exception e) {
	System.err.println("ERROR detecting SNP table");  // debug
	e.printStackTrace();
      }
    }

    System.err.println("SNP query: refname=" + chr_name);

    cmd = "select * from " + snp_table + " where chrom=\"" + chr_name + "\" and chromStart >= " + config.region.range.start + " and chromEnd <= " + config.region.range.end + " and class=\"single\"";
    System.err.println("SNP query: " + cmd);  // debug

    try {
      //      results = ucsc.query(cmd);
      ArrayList<dbSNP> dbsnp = new ArrayList<dbSNP>();
      for (HashMap<String,String> row : ucsc.query(cmd)) {
	dbSNP snp = new dbSNP(row);
	snp.consensus_adjust(config.ruler_start);
	boolean usable = true;
	if (config.intron_compressor != null) {
	  if (config.intron_compressor.is_completely_trimmed(snp.start, snp.end, false)) {
	    usable = false;
	  } else {
	    int offset = config.intron_compressor.get_start_shift(snp.start, false);
	    snp.start -= offset;
	    snp.end -= offset;
	  }
	}
	//	System.err.println("adding snp at " + snp.start + ", reference sequence pos=" + (snp.start + config.ruler_start));  // debug
	if (usable) dbsnp.add(snp);
      }
      config.set_dbsnp(dbsnp);
      // set only when finished
    } catch (Exception e) {
      System.err.println("database error: " + e);  // debug
      e.printStackTrace();
      exception = e;
    }

    tm.finish();

    is_loaded = true;
    setChanged();
    notifyObservers();
  }


}