package Ace2;
// dopey binary dump of dbSNP SNPs (SNPs only, no indels)
// HORRIBLE: use JDBC instead

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class dbSNPDB implements dbSNPQuery {
  // flatfile dbSNP blob

  static final int BUCKET_SIZE = 5000;
  private String blob_filename;
  private HashMap<Chromosome,IntBucketIndex> index;
  private HashMap<String,dbSNPHeader> headers;

  private Chromosome current_chr = null;
  private IntBucketIndex current_bucket = null;

  private static final int SUPPORTED_VERSION = 2;
  private static final String MAGIC = "CdDF";
  private boolean CACHE = true;

  public dbSNPDB (String blob_filename) {
    this.blob_filename = blob_filename;
    index = new HashMap<Chromosome,IntBucketIndex>();
    headers = new HashMap<String,dbSNPHeader>();
    parse_header();
  }

  private void parse_header() {
    try {
      InputStream is = new FileInputStream(new File(blob_filename));
      DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
      byte[] buf = new byte[4];
      dis.read(buf);
      int version = dis.readInt();
      if (new String(buf).equals(MAGIC) && version == SUPPORTED_VERSION) {
	int total_snp_count = dis.readInt();
	int total_chrs = dis.readInt();
	for (int ci = 0; ci < total_chrs; ci++) {
	  int len = dis.readInt();
	  buf = new byte[len];
	  dis.read(buf);
	  dbSNPHeader header = new dbSNPHeader();
	  header.name = Chromosome.standardize_name(new String(buf));
	  header.count = dis.readInt();
	  header.position = dis.readInt();

	  headers.put(header.name, header);

	  //	  System.err.println("header: " + header.name + " count:" + header.count + " pos:" + header.position);  // debug
	  
	}
      } else {
	System.err.println("ERROR: incorrect magic/version for " + blob_filename);  // debug
      }
    } catch (Exception e) {
      System.err.println("ERROR parsing " + blob_filename + ": " + e);  // debug
      e.printStackTrace();
      System.exit(1);
    }
    
  }

  public void set_caching (boolean v) {
    CACHE = v;
  }

  public void set_current_chromosome (Chromosome chr) {
    if (!CACHE) index.clear();
    current_chr = chr;
    if (chr == null) {
      // non-standard chromosome name parses to null; no data
      current_bucket = null;
    } else {
      current_bucket = index.get(chr);
      if (current_bucket == null) {
	try {
	  current_bucket = parse_chr(chr.toString());
	  if (CACHE) index.put(chr, current_bucket);
	} catch (Exception e) {
	  System.err.println("parse error: " + e.toString());  // debug
	  e.printStackTrace();
	}
      }
    }
  }

  public IntBucketIndex parse_chr(String key) throws IOException,FileNotFoundException {
    //    index = new HashMap<Chromosome,IntBucketIndex>();

    Funk.Timer t = new Funk.Timer("dbsnp parse of " + key);

    RandomAccessFile raf = new RandomAccessFile(new File(blob_filename), "r");

    dbSNPHeader header = headers.get(key);
    if (header == null) {
      System.err.println("ERROR, no dbSNP header for  + key");  // debug
      return null;
    }

    raf.seek(header.position);

    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(raf.getFD()));
    // buffer for (much) better read performance
    DataInputStream dis = new DataInputStream(bis);

    IntBucketIndex bucket = new IntBucketIndex(BUCKET_SIZE);
    dbSNPEntry entry;

    for (int i = 0; i < header.count; i++) {
      entry = new dbSNPEntry(
			     dis.readInt(),
			     dis.readInt() + 1,
			     // convert snp129.chromStart to base number
			     dis.readByte()
			     );
      
      bucket.add(entry.chrom_start, entry);

      //      System.err.println(entry.describe());  // debug

    }
    t.finish();
    return bucket;
  }

  public dbSNPEntry find (int ref_base_num, Base b1, Base b2) {
    dbSNPEntry result = null;
    if (current_chr == null || current_bucket == null) {
      //      System.err.println("ERROR: call set_current_chromosome() first");  // debug
      // hide: might legitimately be null if using a BAM w/non-standard chr names
    } else {
      dbSNPEntry e;
      for (Object o : current_bucket.find(ref_base_num)) {
	e = (dbSNPEntry) o;
	if (e.matches(ref_base_num, b1, b2)) {
	  //	  System.err.println("hey to the now now! chr=" + current_chr + " rs=" + e.rs_num);  // debug
	  result = e;
	  break;
	}
      }
    }
    return result;
  }
  
  public static void main (String[] argv) {
    try {
      SNPConfig config = new SNPConfig();
      config.DBSNP_BLOB_FILE = "c:/generatable/dbsnp/dbsnp_binary_v2.blob";
      // debug
      dbSNPDB dbsnp = new dbSNPDB(config.DBSNP_BLOB_FILE);
      dbsnp.set_caching(true);

      for (int i = 1; i < 10; i++) {
	String key = "chr" + i;
	System.err.println("loading " + key);  // debug
	dbsnp.set_current_chromosome(Chromosome.valueOfString(key));
	try {
	  System.out.println("sleeping...");
	  Thread.sleep(5 * 1000);
	} catch (InterruptedException e) {}
      }	

      //      dbsnp.parse();
    } catch (Exception e) {
      System.err.println("ERROR: " + e);
      e.printStackTrace();
    }
  }

  //
  // dbSNPquery interface start:
  //
  public boolean snp_matches (int base_number, Base b1, Base b2) {
    // note: this may be a little pokey; use w/dbSNPQueryCacher
    boolean result = false;
    //    System.err.println("snp_matches");  // debug
    //    System.err.println("cc="+current_chr + " cb="+current_bucket);  // debug
    
    dbSNPEntry hit = find(base_number, b1, b2);
    return hit != null;
  }
  //
  // dbSNPquery interface end
  //


  
}