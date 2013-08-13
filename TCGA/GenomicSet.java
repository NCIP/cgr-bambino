package TCGA;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.Dimension;
import java.awt.Rectangle;

public class GenomicSet extends Observable implements Observer {
  
  private ArrayList<GenomicBin> bins;
  private int bin_size = -1;
  private int bin_count = -1;

  private GenomicMeasurement gm;

  private boolean is_loaded = false;
  private boolean is_genomic = false;
  private boolean is_marker_bin = false;

  private static String CHR_PREFIX = "chr";

  public static final int STYLE_GENOMIC = 1;
  public static final int STYLE_MARKER_LABEL = 2;

  private int synthetic_bin_style = 0;

  public GenomicSet () {
    // debug
  }

  public GenomicSet (String fn) throws IOException,FileNotFoundException {
    if (fn.lastIndexOf(".gz") == fn.length() - 3) {
      setup(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fn)))));
    } else {
      setup(new BufferedReader(new FileReader(fn)));
    }
  }

  public GenomicSet (InputStream ins) throws IOException {
    setup(new BufferedReader(new InputStreamReader(ins)));
  }

  public GenomicSet (GenomicMeasurement gm, int style, Observer o) {
    if (o != null) addObserver(o);
    this.gm = gm;
    this.synthetic_bin_style = style;
    is_loaded = false;
    bins = new ArrayList<GenomicBin>();
    gm.addObserver(this);
    update(null,null);
  }

  public void update (Observable o, Object arg) {
    if (gm.is_loaded() && bins.isEmpty()) {
      if (synthetic_bin_style == STYLE_GENOMIC) {
	// bins are intended to tile the genome equally: 
	// generate approximate bin coverage
	generate_genomic_bins(gm.get_column_count());
      } else if (synthetic_bin_style == STYLE_MARKER_LABEL) {
	// 1:1 bins from marker labels
	generate_marker_bins();
      } else {
	System.err.println("ERROR: unknown bin style");  // debug
      }
    }
  }

  public boolean is_loaded() {
    return is_loaded;
  }

  private void setup(BufferedReader br) throws IOException {
    is_loaded = false;
    bins = new ArrayList<GenomicBin>();
    //
    // parse bin info
    //
    String delimiter = "\t";
    String header_line = br.readLine();
    String[] h = header_line.split(delimiter);
    String line;

    if ((h[0].equals("Chromosome") || h[0].equals("BinName"))
	 && h[1].equals("c1") && h[2].equals("c2")) {
      // format OK
      boolean first = true;
      while (true) {
	line = br.readLine();
	if (line == null) break;
	String[] f = line.split(delimiter);
	if (f.length == 3) {
	  GenomicBin gb = new GenomicBin(f[0],
					 Integer.parseInt(f[1]),
					 Integer.parseInt(f[2])
					 );
	  bins.add(gb);
	  if (first) {
	    is_genomic = gb.bin_name.indexOf("chr") == 0;
	    first = false;
	  }
	} else {
	  throw new IOException("format error: line");
	}
      }
    } else {
      throw new IOException("format error: header line " + header_line);
    }

    load_finished();
  }

  private void load_finished() {
    calculate_summary_info();
    is_loaded = true;
    setChanged();
    notifyObservers();
  }


  public ArrayList<GenomicBin> get_bins() {
    return bins;
  }

  private GenomeScaler get_genome_scaler() {
    // hack: make a class instance?
    if (gm == null) {
      System.err.println("hmm: no GenomicMeasurement ref, assuming hg18");  // debug
      return new GenomeScaler(GenomeScaler.GENOME_VERSION_HG18);
    } else {
      return new GenomeScaler(gm.get_genome_version());
    }
  }

  public void generate_genomic_bins (int size) {
    
    //    System.err.println("GENERATING GenomicSet genomic bins of size: "+size);  // debug

    BinIndex bi = new BinIndex(gm);
    boolean genome_to_scale = bi.is_multi_bin();

    if (true || genome_to_scale == true) {
      // 
      //  assume bins represent equal-sized samples of the genome
      //
      GenomeScaler gs = get_genome_scaler();
      gs.set_dimension(new Dimension(size,1));
      bins = new ArrayList<GenomicBin>();
      for (int chr = 1; chr <= 24; chr++) {
	int start = gs.get_chr_position(chr, GenomeScaler.POSITION_START) + 1;
	int end = gs.get_chr_position(chr, GenomeScaler.POSITION_END);
	bins.add(new GenomicBin(
				(CHR_PREFIX + get_chr_label(chr)),
				start,
				end
				));
	//      System.err.println("chr " + chr + ": start=" + start + " end=" + end);
      }
    } else {
      //
      //  bins represent single genes: not to scale, though in genomic order
      //  set bin boundaries at known chromosome marker start points
      //
      System.err.println("FIX ME!");  // debug

      String[] chr_start_markers = {
	"OR4F5",
	"FAM110C",
	"CHL1",
	"ZNF595",
	"PLEKHG4B",
	"DUSP22",
	"FAM20C",
	"OR4F21",
	"WASH1",
	"ZMYND11",
	"SCGB1C1",
	"IQSEC3",
	"TUBA3C",
	"OR11H12",
	"LOC283755",
	"POLR3K",
	"RPH3AL",
	"USP14",
	"OR4F17",
	"DEFB125",
	"TPTE",
	"A26C3",
	"XG",
	"SRY",
      };

      for (int chr = 1; chr <= 24; chr++) {
	int this_i = bi.find(chr_start_markers[chr - 1]);
	int next_i;
	int next = chr + 1;
	if (next > 24) {
	  next_i = size + 1;
	} else {
	  next_i = bi.find(chr_start_markers[next - 1]);
	}
	if (this_i != -1 && next_i != -1) {
	  //	  int start = this_i + 1;
	  //	  int end = next_i;
	  int start = this_i;
	  int end = next_i - 1;
	  bins.add(new GenomicBin(
				  (CHR_PREFIX + get_chr_label(chr)),
				  start,
				  end
				  ));
	}
      }
    }

    is_genomic = true;

    load_finished();
  }

  public String get_chr_label (int chr) {
    String label = Integer.toString(chr);
    if (chr == 23) label = "X";
    if (chr == 24) label = "Y";
    return label;
  }

  public GenomicLocation get_genomic_location_for_bin (int bin) {
    GenomicLocation result = new GenomicLocation();
    GenomeScaler gs = get_genome_scaler();
    for (GenomicBin gb : bins) {
      if (bin >= gb.start && bin <= gb.end) {
	result.chromosome = gb.bin_name;
	int bins_in_chr = (gb.end - gb.start) + 1;
	int bs = bin - gb.start;
	float start = (float) bs / bins_in_chr;
	float end = (float) (bs + 1) / bins_in_chr;
	int chr = chr2int(gb.bin_name);
	if (chr >= 1 && chr <= 24) {
	  int clen = gs.get_chromosome_length_bp(chr);
	  result.start = (int) (clen * start);
	  result.end = (int) (clen * end);
	} else {
	  System.err.println("ERROR in chr format for " + gb.bin_name);  // debug
	}
      }
    }
    return result;
  }

  public static void main (String [] argv) {
    try {
      //      GenomicSet gs = new GenomicSet("broad_snp6_genomicset.txt");
	    //GenomicSet gs = new GenomicSet("Gene_Expression_AffyU133_Bin.txt");
      if (false) {
	GenomicMeasurement gm = new GenomicMeasurement("carl.txt", false);
	GenomicSet gs = new GenomicSet(gm, STYLE_GENOMIC, null);
	Rectangle sel = gs.get_selection_for_chr_location(7, 10000, 1000000);
	System.err.println("sel="+sel);  // debug
      } else if (true) {
	GenomicMeasurement gm = new GenomicMeasurement("Genome_CopyNumber_TCGA_BRCA_Level3.txt", false);
	GenomicSet gs = new GenomicSet(gm, STYLE_GENOMIC, null);
	//	GenomicLocation gl = new GenomicLocation("chr1:16633790-17125961");
	GenomicLocation gl = new GenomicLocation("chr10:200000-2000000");

	gs.find_bins_for(gl);
      } else if (false) {
	GenomicSet gs = new GenomicSet();
	gs.generate_genomic_bins(15105);
	GenomicLocation gl = gs.get_genomic_location_for_bin(1000);
      } else {
	GenomicMeasurement gm = new GenomicMeasurement("test.txt", false);
	GenomicSet gs = new GenomicSet(gm, STYLE_MARKER_LABEL, null);
      }

      // 127,471,196-127,495,720
      
      //      System.err.println(gs.get_bin_for(7, 127471196));
      //      System.err.println(gs.get_bin_for(7, 127495720));
      //      System.err.println(gs.get_bin_for(2,0));

    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  private void calculate_summary_info() {
    bin_count = 0;
    for (GenomicBin gb : bins) {
      if (gb.end > bin_count) bin_count = gb.end;
    }

    if (is_genomic) {
      GenomeScaler gs = get_genome_scaler();
      long genome_length = gs.get_total_genome_length_bp();
      bin_size = (int) (genome_length / bin_count);
    } else {
      bin_size = 1;
    }
    //    System.err.println("bin count:" + bin_count + " bin size:"+bin_size);
    
  }

  public int get_bin_count() {
    return bin_count;
  }

  public int get_bin_for(int chrom, int position) {
    //
    //  return INDEX of bin in set for given chromosome and nucleotide position
    //
    String l = CHR_PREFIX + get_chr_label(chrom);

    GenomicBin start_bin = null;
    int result = -1;
    for (GenomicBin gb : bins) {
      if (gb.bin_name.equals(l)) {
	start_bin = gb;
	break;
      }
    }
    if (start_bin == null) {
      System.err.println("ERROR translating bin coord for " + chrom + " pos " +position);  // debug
    } else {
      result = (start_bin.start - 1) + (position / bin_size);
    }

    return result;
  }

  public int chr2int (String chr) {
    int result = -1;
    if (chr.length() > 3 && chr.substring(0,3).equals("chr")) {
      String rest = chr.substring(3);
      try {
	result = Integer.parseInt(rest);
      } catch (NumberFormatException e) {
	if (rest.equals("X")) result = 23;
	if (rest.equals("Y")) result = 24;
      }
    }
    return result;
  }

  public boolean is_marker_bin() {
    return is_marker_bin;
  }

  public void generate_marker_bins() {
    String[] headers = gm.get_headers();
    bins = new ArrayList<GenomicBin>();
    for (int i=0; i < headers.length; i++) {
      bins.add(new GenomicBin(headers[i], i + 1, i + 1));
    }
    is_genomic = false;
    is_marker_bin = true;

    load_finished();
  }

  private void dump_bins() {
    System.err.println("bin dump:");  // debug
    for (GenomicBin bin : bins) {
      System.err.println("  " + bin.bin_name + ": " + bin.start + "-" + bin.end);  // debug
    }
  }

  public Rectangle get_selection_for_chr_location (int chrom, int start, int end) {
    // given a chromosome name and start/end location within it (in nt),
    // return selection in bin space
    return gm.generate_selection_start_end(
					   get_bin_for(chrom, start),
					   get_bin_for(chrom, end)
					   );
  }

  public Rectangle get_selection_for_chr_location (String chrom, int start, int end) {
    // given a chromosome name and start/end location within it (in nt),
    // return selection in bin space
    int chr = chr2int(chrom);
    if (chr > 0) {
      return get_selection_for_chr_location(chr, start, end);
    } else {
      System.err.println("ERROR parsing chromosome: " + chrom);  // debug
      return null;
    }
  }

  public void collapse_to (ArrayList<Integer> bin_list) {
    // Do The Collapse
    // reformat layout to match subset of bins
    // input: set of 0-based bin indexes

    ArrayList<GenomicBin> new_bins = new ArrayList<GenomicBin>();

    int next_start = 1;
    for (GenomicBin bin : bins) {
      // collapse bins to include only ranges in given list
      GenomicBin remapped = bin.map(bin_list, next_start);
      if (remapped != null) {
	new_bins.add(remapped);
	next_start = remapped.end + 1;
      }
    }

    bins = new_bins;
    //    System.err.println("size="+bins.size() + " genomic:" + is_genomic);  // debug

    calculate_summary_info();
    //    System.err.println("bs="+bin_size);  // debug

  }

  public GenomicBinRange find_bins_for (GenomicLocation gl) {
    //
    //  return 0-based range of bins overlapping a given location
    //
    GenomicBinRange result = null;

    //
    //  find bin:
    //
    GenomicBin hit_bin = null;
    for (GenomicBin bin : bins) {
      if (bin.bin_name.equals(gl.chromosome)) {
	hit_bin = bin;
	break;
      }
    }

    if (hit_bin == null) {
      System.err.println("ERROR: can't find chr in bin set: " + gl.chromosome);  // debug
    } else {
      //
      // determine which bins overlap target location:
      //
      result = new GenomicBinRange(hit_bin, gl, get_genome_scaler());
    }

    return result;
  }


  
}
