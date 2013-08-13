package Ace2;
import java.util.*;
import java.security.MessageDigest;
import net.sf.samtools.SAMRecord;

public class UniqueReadName {
  //
  // generate forward/reverse suffixes to ensure unique read names
  //
  HashMap<String,Integer> read_count;
  private boolean md5_mode = false;
  private int md5_len = 0;
  private MessageDigest md5;
  private boolean inthash_mode = false;
  private int inthash_bucket_mod;
  private Integer[] inthash=null;

  public UniqueReadName() {
    setup();
  }

  private void setup() {
    read_count = new HashMap<String,Integer>();
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      System.err.println("ERROR: can't get MD5");  // debug
    }
  }

  public void reset() {
    setup();
  }

  public void set_md5_mode (boolean mode, int len) {
    md5_mode = mode;
    md5_len = len;
  }

  public void set_inthash_mode (boolean mode, int bits) {
    inthash_mode = mode;
    inthash_bucket_mod = 0;
    for (int i = 0; i < bits; i++) {
      inthash_bucket_mod = inthash_bucket_mod << 1 | 1;
    }
    System.err.println("bucket mask: " + inthash_bucket_mod);
    inthash = new Integer[inthash_bucket_mod + 1];
  }

  public String get_suffix (SAMRecord sr) {
    return get_suffix(sr.getReadName(), sr.getReadNegativeStrandFlag());
  }

  public String get_suffix (String read_name, boolean rc) {
    String suffix;
    if (rc) {
      suffix = "R";
    } else {
      suffix = "F";
    }

    String key = read_name + suffix;

    int counter, i;

    if (inthash_mode) {
      // http://www.serve.net/buz/hash.adt/java.002.html
      int h = 0;
      char[] ary = key.toCharArray();
      for (i=0; i < ary.length; i++) {
	h = (h << 2) + ary[i];
      }
      //      int ikey = Math.abs(h) % inthash_bucket_mod;
      int ikey = Math.abs(h) % inthash_bucket_mod;
      //      System.err.println(key + " hash:" + h + " => " + ikey);  // debug

      Integer cnt = inthash[ikey];
      counter = (cnt == null ? 0 : cnt) + 1;
      inthash[ikey] = Integer.valueOf(counter);
    } else {
      if (md5_mode) {
	md5.reset();
	char[] ary = key.toCharArray();
	byte[] b = new byte[ary.length];
	for(i=0; i < ary.length; i++) {
	  b[i] = (byte) ary[i];
	}
	key = new String(md5.digest(b), 0, md5_len);
      }

      Integer cnt = read_count.get(key);
      counter = (cnt == null ? 0 : cnt) + 1;
      read_count.put(key, Integer.valueOf(counter));
    }

    suffix = suffix.concat(Integer.toString(counter));

    //    if (counter > 1) System.err.println("HEY NOW " + suffix + " " + key);

    return suffix;
  }

  public static void main (String[] argv) {
    UniqueReadName urn = new UniqueReadName();
    //    urn.set_md5_mode(true, 3);
    urn.set_inthash_mode(true, 19);
    System.err.println(urn.get_suffix("someread", false));  // debug
    System.err.println(urn.get_suffix("someread", true));  // debug
    System.err.println(urn.get_suffix("otherread", false));  // debug
    System.err.println(urn.get_suffix("someread", false));  // debug

  }

  
}