package Ace2;

import java.util.*;
import net.sf.samtools.*;

public class SAMRecordArrayIterable<SAMRecord> implements Iterable {
  private SAMConsensusMapping[] maps;

  public SAMRecordArrayIterable (SAMConsensusMapping[] maps) {
    this.maps = maps;
  }

  // begin Iteratable stub
  public Iterator<SAMRecord> iterator() {
    return new SAMRecordArrayIterator(maps);
  }
  // end Iteratable stub

}