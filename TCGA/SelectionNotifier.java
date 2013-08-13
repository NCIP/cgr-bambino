package TCGA;

import java.awt.Rectangle;
import java.awt.event.*;
import java.util.*;

public class SelectionNotifier extends Observable implements Runnable {
  private Rectangle pending_selection;

  public void set_selection (Rectangle sel) {
    setChanged();
    notifyObservers(sel);
  }

  public void set_pending_selection(Rectangle sel) {
    pending_selection = sel;
  }

  public void run() {
    set_selection(pending_selection);
  }

}
