package Ace2;
// fixed set of dbSNP records, i.e. from UCSC

import java.util.*;

public class dbSNPSet implements dbSNPQuery {
  private HashMap<Integer,ArrayList<dbSNP>> snp_set;

  public dbSNPSet (ArrayList<dbSNP> snp_list) {
    snp_set = new HashMap<Integer,ArrayList<dbSNP>>();

    for (dbSNP snp : snp_list) {
      ArrayList<dbSNP> bucket = snp_set.get(snp.start);
      if (bucket == null) snp_set.put(snp.start, bucket = new ArrayList<dbSNP>());
      bucket.add(snp);
    }
  }

  public boolean snp_matches (int base_number, Base b1, Base b2) {
    // is there a SNP at the given postion showing the specified bases?
    // this implementation is probably "fast enough" and may not need a caching layer.

    boolean result = false;
    ArrayList<dbSNP> hits = snp_set.get(base_number);
    // FIX ME: index? base number??
    if (hits != null) {
      for (dbSNP snp : hits) {
	if (snp.matches(b1, b2)) {
	  result = true;
	  break;
	}
      }
    }

    return result;
  }

}
