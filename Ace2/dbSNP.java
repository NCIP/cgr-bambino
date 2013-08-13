package Ace2;
// UCSC dbSNP database row (e.g. snp129)

import java.util.*;

public class dbSNP {
  String name,chrom,strand,observed_bases;
  int start,end;

  public dbSNP (HashMap<String,String> row) {
    name = row.get("name");
    chrom = row.get("chrom");
    start = Integer.parseInt(row.get("chromStart"));
    end = Integer.parseInt(row.get("chromEnd"));
    strand = row.get("strand");
    observed_bases = row.get("observed");
  }

  public dbSNP (String[] fields) {
    if (fields.length == 7 && fields[0].equals("dbsnp")) {
      // 0. dbsnp
      // 1. SNP name
      // 2. chrom
      // 3. start
      // 4. end
      // 5. strand
      // 6. observed_bases
      int fi = 1;
      name = new String(fields[fi++]);
      chrom = new String(fields[fi++]);
      start = Integer.parseInt(fields[fi++]);
      end = Integer.parseInt(fields[fi++]);
      strand = new String(fields[fi++]);
      observed_bases = new String(fields[fi++]);
    } else {
      System.err.println("dbSNP init error, wrong field length");  // debug
    }
  }

  public String get_normalized_observed_bases () {
    String result;
    if (strand.equals("+")) {
      result = observed_bases;
    } else {
      char[] res = observed_bases.toCharArray();
      for (int i = 0; i < res.length; i++) {
	if (res[i] != '/') res[i] = RefGene.complement(res[i]);
      }
      result = new String(res);
      //      System.err.println("before:"+observed_bases + " after:" +result);  // debug
    }
    return result;
  }

  public boolean matches (Base b1, Base b2) {
    HashSet<Base> dbsnp_bases = new HashSet<Base>();
    char[] norm = get_normalized_observed_bases().toCharArray();
    for (int i = 0; i < norm.length; i++) {
      if (norm[i] != '/') dbsnp_bases.add(Base.valueOf(norm[i]));
    }

    HashSet<Base> query = new HashSet<Base>();
    query.add(b1);
    query.add(b2);

    return dbsnp_bases.equals(query);
  }

  public void consensus_adjust (int i) {
    // convert SNP position to raw consensus offset
    start -= (i - 1);
    end -= (i - 1);
  }

}
