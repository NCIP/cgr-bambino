package Ace2;

public class SNP {
  public String filename, contig;
  public int position, location_id;
  public double score;

  public SNP2 snp2;
  // extended
  
  public SNP () {
  }

  public SNP (int p, double s) {
    position = p;
    score = s;
  }
}
