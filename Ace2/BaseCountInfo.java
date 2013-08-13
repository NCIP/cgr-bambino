package Ace2;

import java.util.*;

class BaseCountInfo {
  Base base;
  // nucleotide being tracked
  int count=0;
  // count of sequences showing this base (hacky, see ArrayLists below)
  ArrayList<Object> sequences;
  // generic associated objects (typically SNPTrackInfo)
  
  public BaseCountInfo (Base base) { 
    this.base = base;
    count=0;
    sequences = new ArrayList<Object>();
  }
  
}
