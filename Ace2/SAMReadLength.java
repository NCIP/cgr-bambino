package Ace2;

import java.util.*;

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;

public class SAMReadLength {

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SAMResource sres = null;
    int max = 0;

    try {
      
      boolean TRACK_POSITIONS = false;
      int MAX_TRACK_SIZE = 10;
    
      for (int i=0; i < argv.length; i++) {
	if (argv[i].equals("-bam")) {
	  sres = new SAMResource();
	  sres.import_data(SAMResourceTags.SAM_URL, argv[++i]);
	  sres.detect_sample_id();
	} else if (argv[i].equals("-max")) {
	  max = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-positions")) {
	  TRACK_POSITIONS = true;
	}
      }

      if (sres == null) {
	System.err.println("specify -bam [file]");
      } else {
	CloseableIterator<SAMRecord> iterator = sres.get_iterator();
	long read_count = 0;
	int len;

	long[] lengths = new long[5000];
	Arrays.fill(lengths, 0);
	HashMap<Integer,ArrayList<SAMRecord>> length2sam = new HashMap<Integer,ArrayList<SAMRecord>>();
	
	while (iterator.hasNext()) {
	  SAMRecord sr = iterator.next();
	  //	  if (sr.getDuplicateReadFlag()) continue;
	  len = sr.getReadLength();
	  if (len >= lengths.length) {
	    System.err.println("ERROR: read len too long: " + len);  // debug
	  } else {
	    lengths[len]++;
	    if (TRACK_POSITIONS) {
	      ArrayList<SAMRecord> sams = length2sam.get(len);
	      if (sams == null) length2sam.put(len, sams = new ArrayList<SAMRecord>());
	      if (sams.size() < MAX_TRACK_SIZE) sams.add(sr);
	    }
	    if (++read_count >= max && max > 0) break;
	  }
	}

	for (int i=0; i < lengths.length; i++) {
	  if (lengths[i] > 0) {
	    System.out.print(i + "," + lengths[i]);  // debug
	    if (TRACK_POSITIONS) {
	      ArrayList<SAMRecord> sams = length2sam.get(i);
	      int size = sams.size();
	      for (int j = 0; j < size; j++) {
		SAMRecord sr = sams.get(j);
		System.out.print("," + sr.getReferenceName() + ":" + sr.getAlignmentStart());  // debug
	      }
	    }
	    System.out.println();
	  }
	}
	
	System.err.println("read: " + read_count);  // debug
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }
      
}