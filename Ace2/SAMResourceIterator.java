package Ace2;

import java.util.*;
import net.sf.samtools.*;

public class SAMResourceIterator implements Iterator<SAMRecord> {
  private SAMConsensusMapping[] maps;
  private int index;

  public SAMResourceIterator (SAMConsensusMapping[] maps) {
    this.maps = maps;
    index = 0;
  }

  // begin Iterator stub
  public boolean hasNext() {
    return index < maps.length;
  }

  public SAMRecord next() {
    return index < maps.length ? maps[index++].get_samrecord() : null;
  }

  public void remove() {
    System.err.println("ERROR: remove() not implemented");  // debug
  }
  // end Iterator stub

  
}
