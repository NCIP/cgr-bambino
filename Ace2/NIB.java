package Ace2;
// parse/extract from .NIB packed genome files
// http://genome.ucsc.edu/FAQ/FAQformat#format8
// 

import java.io.*;
import java.util.*;

public class NIB implements ReferenceSequence {
  RandomAccessFile raf;
  private byte[] long_buf = new byte[4];
  File nib_file;

  public static String DEFAULT_NIB_DIR = null;

  private static final byte NT_MASK = 0x07;
  // bottom 3 bits
  private static final byte MASK_MASKED = 0x08;
  private static final int HEADER_BYTES = 8;
  // 2 ints

  private static final int BUFFER_SIZE = 16384;
  //  private static final int BUFFER_SIZE = 10;

  private int BUFFER_INDEX = -1;
  // string index, not base number
  private byte[] BUFFER = null;

  int nib_len;

  public NIB (Chromosome chr) throws FileNotFoundException {
    setup(null, chr);
  }

  public NIB() {
    // for use w/ReferenceSequence interface
  }

  /*
   * MCR edit: changed all of the setup calls to use the sequence_name directly.
   * Also added a new parallel constructor
   */
  
  public NIB (String sequence_name) throws FileNotFoundException {
    setup(null, sequence_name);
  }
  
  public byte[] get_region (String sequence_name, int start_base, int length) throws IOException {
    // fetch a region of a reference sequence
    byte[] result = null;
    setup(null, sequence_name);
    result = get_sequence(start_base, length);
    return result;
  }

  public byte[] get_all (String sequence_name) throws IOException {
    // fetch entire sequence
    byte[] result = null;
    setup(null, sequence_name);
    result = read_all();
    return result;
  }

  public int get_length (String sequence_name) throws IOException {
    int length = ReferenceSequence.NULL_LENGTH;
    try {
      setup(null, sequence_name);
      length = nib_len;
    } catch(FileNotFoundException e) {}
    return length;
  }
  
  /*
   * MCR edit: made the setup method delegate to a new setup method that takes
   * a string.  It then mimics the behavior of FASTADirectory to find the file. 
   */
  
  private void setup (String directory, Chromosome chr) throws FileNotFoundException {
    setup(directory, chr.toString());
  }

  private void setup (String directory, String sequence_name) throws FileNotFoundException {
    BUFFER = new byte[BUFFER_SIZE];
    if (directory == null) directory = guess_nib_dir();
    for (String key : SAMUtils.get_refname_alternates(sequence_name)) {
      String fn = directory + File.separator + key + ".nib";
      nib_file = new File(fn);
      if(nib_file.exists()) break;
    }
    raf = new RandomAccessFile(nib_file, "r");
    nib_len = get_sequence_length();
  }

  /*
   * End MCR edit
   */
  
  private String guess_nib_dir () {
    String[] dirs = {
      DEFAULT_NIB_DIR,
      "/TCGA/nextgensupport/hg18_nib/",
      "/generatable/hg18/",
      "/tcga_next_gen/Genome_Analysis/edmonson/support/hg18_nib",
      "."
    };
    
    int i;
    for (i = 0; i < dirs.length; i++) {
      if (dirs[i] != null) {
	File f = new File(dirs[i]);
	if (f.exists()) break;
      }
    }
    return dirs[i];

  }


  private int read_vaxian_long() throws IOException {
    int result = 0;
    if (raf.read(long_buf) == 4) {
      //      result = long_buf[0] + (long_buf[1] << 8) + (long_buf[2] << 16) + (long_buf[3] << 24);

      result = (long_buf[0] & 0xff) +
	((long_buf[1] & 0xff) << 8) +
	((long_buf[2] & 0xff) << 16) +
	((long_buf[3] & 0xff) << 24);
      // masking required since java bytes are signed!

    } else {
      throw new IOException("insufficient bytes to read long");
    }
    return result;
  }
  
  public int get_sequence_length() {
    int nib_len = -1;
    try {
      raf.seek(0);
      int magic = read_vaxian_long();
      // magic; readInt() is wrong order (?)
      nib_len = read_vaxian_long();
      int expected_len = (int) (nib_file.length() - HEADER_BYTES) * 2;
      if (expected_len != nib_len) {
	// WTF?  sometimes header length doesn't agree w/implied file size length
	//      System.err.println("WTF: .nib header says len=" + nib_len + ", file size implies len=" + expected_len);
	if (expected_len > nib_len) {
	  //	System.err.println("using " + expected_len);  // debug
	  nib_len = expected_len;
	}
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }

    return nib_len;
  }

  public byte[] get_sequence(int base_num, int len) throws IOException,EOFException {
    //    System.err.println("buf size="+BUFFER_SIZE);  // debug
    long pos = HEADER_BYTES + ((base_num - 1) / 2);
    System.err.println("pos for base " + base_num + " = " + pos);  // debug

    raf.seek(pos);
    
    int wanted = len;

    byte[] result = new byte[len];
    int ptr = 0;

    if ((base_num + (wanted - 1)) > nib_len) {
      throw new IOException("attempt to read past end of sequence");
    } else {
      boolean oddity = (base_num - 1) % 2 > 0;
      // first nucleotide starts in 2nd nibble

      int bi;

      while (wanted > 0) {
	int packed_size = (wanted / 2) + 2;
	int read_size = packed_size <= BUFFER.length ? packed_size : BUFFER.length;
	int read = raf.read(BUFFER, 0, read_size);
	if (read > 0) {
	  for (bi=0; wanted > 0 && bi < read; bi++) {
	    if (oddity) {
	      // first nucleotide starts in 2nd nibble of byte
	      //	      System.err.println("ODDITY");  // debug
	      result[ptr++] = nibble_to_nt(BUFFER[bi]);
	      wanted--;
	      oddity = false;
	    } else {
	      result[ptr++] = nibble_to_nt((byte) (BUFFER[bi] >> 4));
	      wanted--;
	      if (wanted > 0) {
		result[ptr++] = nibble_to_nt(BUFFER[bi]);
		wanted--;
	      }
	    }
	  }
	} else {
	  throw new IOException("read() returned " + read);
	}
      }
    }

    return result;
  }


  public byte[] read_all() throws IOException {
    //
    // return entire .nib sequence as char array
    //

    int nib_len = get_sequence_length();

    if (false) {
      System.err.println("DEBUG: short NIB read!");  // debug
      nib_len = 100000;
    } else {
      System.err.println("reading full NIB record");  // debug
    }

    byte[] result = new byte[nib_len];

    //    System.err.println("len="+nib_len);  // debug

    int bi=0;
    byte[] buffer = new byte[1024];
    int i;

    while (true) {
      int read = raf.read(buffer);
      if (read == -1) break;
      // EOF
      //      System.err.println("read " + read + " bi="+bi);  // debug
      for (i = 0; i < read; i++) {
	if (bi >= nib_len) break;
	// only necessary in debug mode
	result[bi++] = nibble_to_nt((byte) (buffer[i] >> 4));
	if (bi >= nib_len) break;
	result[bi++] = nibble_to_nt(buffer[i]);
	if (bi >= nib_len) break;
      }
    }

    //    System.err.println(new String(result, 0, 5000));  // debug
    //    System.err.println(new String(result, result.length - 50, 50));  // debug

    return result;
  }


  private byte nibble_to_nt (byte nibble) {
    byte result;
    switch (nibble & NT_MASK) {
    case 0: result = 'T'; break;
    case 1: result = 'C'; break;
    case 2: result = 'A'; break;
    case 3: result = 'G'; break;
    default: result = 'N'; break;
    }
    if ((nibble & MASK_MASKED) > 0) result = (byte) Character.toLowerCase((char) result);
    return result;
  }

  

  public byte get_nucleotide_at (int base_num) {
    // return a single nucleotide (1-based base number, NOT string index)
    byte result = ' ';
    if (base_num < 1 || base_num > nib_len) {
      System.err.println("ERROR: base " + base_num + " out of bounds");  // debug
    } else {
      int bi = -1;
      try {
	get_buffer_for(base_num);
	bi = (base_num - 1 - BUFFER_INDEX) / 2;
	//      System.err.println("nt bi="+bi );

	if (base_num % 2 > 0) {
	  // first nibble
	  result = nibble_to_nt((byte) (BUFFER[bi] >> 4));
	} else {
	  result = nibble_to_nt(BUFFER[bi]);
	}
      } catch (ArrayIndexOutOfBoundsException e) {
	System.err.println("array out of bounds for base num " + base_num + ", bi="+bi);  // debug
      }
    }

    return result;
  }

  private boolean get_buffer_for (int base_num) {
    //    int max_seek = (nib_len - (BUFFER_SIZE * 2)) + 1;
    //    if (base_num > max_seek) base_num = max_seek;
    if (base_num > nib_len) base_num = nib_len;
    int bi = base_num - 1;
    if ((bi % 2) == 1) bi--;
    // bases are packed into 2 bytes, change start index to that of 1st nt in byte
    boolean result = true;
    //    System.err.println("bi:"+ bi + " buffer_index:"+BUFFER_INDEX);  // debug

    if (BUFFER_INDEX == -1 || bi < BUFFER_INDEX || bi >= (BUFFER_INDEX + BUFFER_SIZE)) {
      // need to refresh buffer
      //      System.err.println("refresh");  // debug
      long pos = HEADER_BYTES + (bi / 2);
      //      System.err.println("pos for bi " + bi + "=" + pos);  // debug
      BUFFER_INDEX = bi;
      //      System.err.println("BUFFER_INDEX="+bi);  // debug

      try {
	raf.seek(pos);
	raf.readFully(BUFFER);
      } catch (EOFException e) {
	// EOF, ignore (code limits reading beyond last base)
      } catch (Exception e2) {
	System.err.println("ERROR: " + e2);  // debug
	e2.printStackTrace();
	Arrays.fill(BUFFER, (byte) 0);
	result = false;
      }
    }
    return result;
  }


  public static void main(String[] argv) {
    try {
      NIB nib = new NIB(Chromosome.valueOfString("chr2"));

      System.err.println("nib len="+ nib.get_sequence_length());

      //      byte[] sequence = nib.read_all();
      
      if (true) {
	if (argv.length == 2) {
	  int start_base = Integer.parseInt(argv[0]);
	  int len = Integer.parseInt(argv[1]);
	  byte[] chunk = nib.get_sequence(start_base, len);
	  String str = new String(chunk);
	  System.err.println("chunk="+str);  // debug
	  System.err.println("slen="+str.length());  // debug

	  System.err.println(new String(chunk));  // debug
	} else {
	  System.err.println("specify start_base and length");  // debug
	}
      } else if (false) {
	System.out.println(">chr18");  // debug
	int i;
	char nt;
	int BUF_LEN = 50;
	byte[] buf = new byte[BUF_LEN];
	int bi = 0;
	for (i=1; i <= 76117153; i++) {
	  //	for (i=76100000; i <= 76117153; i++) {
	  buf[bi++] = nib.get_nucleotide_at(i);
	  if (bi >= BUF_LEN) {
	    System.out.println(new String(buf));
	    bi = 0;
	  }
	}
	if (bi > 0) System.out.println(new String(buf, 0, bi));
	System.err.println("final bi="+bi);  // debug
      } else {
	if (argv.length > 0) {
	  int pos = Integer.parseInt(argv[0]);
	  if (pos >= 1) {
	    byte nt = nib.get_nucleotide_at(1);
	    System.err.println("nt at 1 is " + (char) nt);  // debug
	    nt = nib.get_nucleotide_at(pos);
	    System.err.println("nt is " + nt);  // debug
	  } else {
	    System.err.println("pos must be >= 1 (base number)");  // debug
	  }
	} else {
	  System.err.println("specify nucleotide number");  // debug
	}
      }
      //      System.err.println(new String(sequence, 0, 50));  // debug
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public boolean supports_sequence_list() {
    return true;
  }

  public ArrayList<String> get_sequence_names() {
    ArrayList<String> results = null;
    System.err.println("FIX ME: NIB.get_sequence_list() not implemented");  // debug
    return results;
  }



}