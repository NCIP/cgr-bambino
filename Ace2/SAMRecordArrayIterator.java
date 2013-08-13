package Ace2;

import java.util.*;
import net.sf.samtools.SAMRecord;

public class SAMRecordArrayIterator implements Iterator {
  private SAMConsensusMapping[] maps;
  int index = 0;

  public SAMRecordArrayIterator (SAMConsensusMapping[] maps) {
    this.maps = maps;
    this.index = 0;
  }

  // begin Iterator stub
  public boolean hasNext() {
    return index < maps.length;
  }

  public SAMRecord next() {
    SAMRecord sr = maps[index++].sr;
    return sr;
  }

  public void remove() {
    System.err.println("remove() not supported");  // debug
  }
  // end Iterator stub


}