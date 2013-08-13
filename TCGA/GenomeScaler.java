package TCGA;

import java.awt.Dimension;

public class GenomeScaler {
  // 
  //  TO DO: subset of chromosomes visible??
  //

  public static final int POSITION_CENTER = 1;
  public static final int POSITION_START = 2;
  public static final int POSITION_END = 3;

  public static final String GENOME_VERSION_HG18 = "hg18";
  public static final String GENOME_VERSION_HG19 = "hg19";

  private String genome_version;

  private int[] chromosome_lengths_bp_hg18 = {
    247249719,
    242951149,
    199501827,
    191273063,
    180857866,
    170899992,
    158821424,
    146274826,
    140273252,
    135374737,
    134452384,
    132349534,
    114142980,
    106368585,
    100338915,
    88827254,
    78774742,
    76117153,
    63811651,
    62435964,
    46944323,
    49691432,
    154913754, // X
    57772954,  // Y
  };
  // lengths in bp from NC_ sequences as of 2/2008
  // e.g. index 0 is chr 1 => NC_000001 => 247249719 bp

  private int[] chromosome_lengths_bp_hg19 = {
    249250621,
    243199373,
    198022430,
    191154276,
    180915260,
    171115067,
    159138663,
    146364022,
    141213431,
    135534747,
    135006516,
    133851895,
    115169878,
    107349540,
    102531392,
    90354753,
    81195210,
    78077248,
    59128983,
    63025520,
    48129895,
    51304566,
    155270560,
    59373566,
  };

  private int[] chromosome_lengths_bp;

  private long[] chromosome_start_bp = new long[24];
  private long[] chromosome_end_bp = new long[24];
  private long[] chromosome_center_bp = new long[24];

  private Dimension d;

  private long total_genome_length_bp = 0;
  private int total_genome_length_kb;
  private int total_genome_length_mb;

  public GenomeScaler (String genome_version) {
    this.genome_version = genome_version;
    setup();
  }

//   public GenomeScaler (Dimension d) {
//     this.d = d;
//     static_setup();
//   }

  public long get_total_genome_length_bp () {
    return total_genome_length_bp;
  }
  
  public int get_chromosome_length_bp (int chr) {
    int result = -1;
    if (chr >= 1 && chr <= 24) {
      return chromosome_lengths_bp[chr - 1];
    } else {
      System.err.println("chr error!");  // debug
    }
    return result;
  }

  public int get_chromosome_length_bp (String chr) {
    int result = -1;
    if (chr.indexOf("chr") == 0) {
      result = get_chromosome_length_bp(Integer.parseInt(chr.substring(3)));
    }
    return result;
  }

  public int get_chr_position (int chr, int pos_code) {
    // get scaled position of a chromosome in current coordinates
    int result = -1;
    long pos_bp = 0;
    if (chr < 1 || chr > 24) {
      System.err.println("ERROR: chr out of range (1-24)");  // debug
    } else {
      chr--;  // 0-based
      if (pos_code == POSITION_CENTER) {
	pos_bp = chromosome_center_bp[chr];
      } else if (pos_code == POSITION_START) {
	pos_bp = chromosome_start_bp[chr];
      } else if (pos_code == POSITION_END) {
	pos_bp = chromosome_end_bp[chr];
      } else {
	System.err.println("ERROR: invalid pos_code");  // debug
      }

      float fraction = (float) ((double) pos_bp / (double) total_genome_length_bp);
      result = (int) ((float) d.width * fraction);
      //      System.err.println(result);  // debug
    }
    
    return result;
  }

  public void set_dimension (Dimension d) {
    this.d = d;
  }

  private void setup() {
    
    if (genome_version.equals(GENOME_VERSION_HG18)) {
      System.err.println("GenomeScaler: using hg18");  // debug
      chromosome_lengths_bp = chromosome_lengths_bp_hg18;
    } else if (genome_version.equals(GENOME_VERSION_HG19)) {
      System.err.println("GenomeScaler: using hg19");  // debug
      chromosome_lengths_bp = chromosome_lengths_bp_hg19;
    } else {
      System.err.println("ERROR: unknown genome version");  // debug
    }

    total_genome_length_bp = 0;
    total_genome_length_mb = 0;
    total_genome_length_kb = 0;
    for (int i=0; i < 24; i++) {
      chromosome_start_bp[i] = total_genome_length_bp;
      total_genome_length_bp += chromosome_lengths_bp[i];
      chromosome_end_bp[i] = total_genome_length_bp;
      chromosome_center_bp[i] = chromosome_start_bp[i] + (chromosome_lengths_bp[i] / 2);
    }
    total_genome_length_mb = (int) (total_genome_length_bp / 1000000);
    total_genome_length_kb = (int) (total_genome_length_bp / 1000);
    //      System.err.println("genome bp:"+total_genome_length_bp);  // debug
    //      System.err.println(total_genome_length_kb);  // debug
    //System.err.println(total_genome_length_mb);  // debug
  }

  public static void main (String [] argv) {
    GenomeScaler gs = new GenomeScaler(GENOME_VERSION_HG18);
    //    gs.set_dimension(new Dimension(800,600));
    gs.set_dimension(new Dimension(1024,768));

    for (int chr = 1; chr <= 24; chr++) {
      System.err.println("chr " + chr + ": " +
			 gs.get_chr_position(chr, POSITION_CENTER));
    }
  }
  

}
