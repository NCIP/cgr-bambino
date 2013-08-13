package TCGA;

public class BinLabel {
  // decode various bin formats, e.g. "chr3:215.4-215.6MB"
  private String bin_label;
  private GenomicLocation genomic_location = null;

  public BinLabel (String s) {
    bin_label = s;
    parse();
  }

  public boolean is_genomic() {
    return genomic_location != null;
  }

  private void parse() {
    String lc = bin_label.toLowerCase();
    if (lc.indexOf("chr") == 0 && lc.lastIndexOf("mb") == lc.length() - 2) {
      // e.g. "chr3:215.4-215.6MB"
      String[] chr_pos = lc.split(":");
      if (chr_pos.length == 2) {
	String[] start_end = chr_pos[1].split("-");
	if (start_end.length == 2) {
	  float start = Float.parseFloat(start_end[0]);
	  float end = Float.parseFloat(start_end[1].substring(0, start_end[1].length() - 2));
	  genomic_location = new GenomicLocation(
						 chr_pos[0],
						 (int) (start * 1000000),
						 (int) (end * 1000000)
						 );
	}
      }
    }
  }
  
  
  public static void main (String [] argv) {
    BinLabel bl = new BinLabel("chr3:215.4-215.6MB");
  }

  public GenomicLocation get_genomic_location() {
    return genomic_location;
  }
  
}
