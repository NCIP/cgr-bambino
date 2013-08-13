package Funk;

import java.util.*;

public class DoWhatISayObservable extends Observable {
  // If you have a class that (a) extends some another class and (b)
  // wants to use Observable, you are SOL.  Since setChanged() is a
  // protected method of Observable, the only way to call it is to
  // subclass Observable, but you can't because Java doesn't support
  // multiple inheritance.  This is especially irritating when
  // designing new graphical widgets that extend the Canvas class:
  // extending Canvas is necessary because you have to override its
  // paint() and event-handling methods, and you also want to notify an
  // Observer of the state of the widget.  I ran into this when
  // writing Funk.Slider.  - mne 2/20/1998

  // in many/most cases just better to make a helper class
  // implementing Observable...problem w/this class is 
  // it would have to pass the "real" class as an argument,
  // and a second arg isn't possible

  public void setChanged() {
    super.setChanged();
  }
}
