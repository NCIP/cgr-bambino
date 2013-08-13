package Ace2;

import java.util.*;

public class GeneInfo {
  String name;
  Chromosome chr;
  int start, end;

  public void import_ucsc (ArrayList<HashMap<String,String>> rows) {
    HashSet<String> chrs = new HashSet<String>();
    start = -1;
    end = -1;
    System.err.println("row count: " + rows.size());  // debug
    for (HashMap<String,String> row : rows) {
      String chr_raw = row.get("chrom");
      String start_raw = row.get("txStart");
      String end_raw = row.get("txEnd");

      if (chr_raw == null || start_raw == null || end_raw == null) {
	System.err.println("WARNING: corrupt db row chr="+chr_raw + " start=" + start_raw + " end=" + end_raw);  // debug
      } else if (chr_raw.indexOf("_") == -1) {
	chrs.add(chr_raw);
	int txs = Integer.parseInt(start_raw);
	int txe = Integer.parseInt(end_raw);
	if (start == -1 || txs < start) start = txs;
	if (end == -1 || txe > end) end = txe;
	//	System.err.println("range " + txs + " " + txe);  // debug
      }
    }
    //    System.err.println("final " + start + " " + end);  // debug

    if (chrs.size() == 1) {
      ArrayList<String> list = new ArrayList<String>(chrs);
      chr = Chromosome.valueOfString(list.get(0));
    } else {
      System.err.println("gene mapping AFU! size="+chrs.size());  // debug
      name = null;
      chr = null;
      start = end = -1;
    }
  }

}