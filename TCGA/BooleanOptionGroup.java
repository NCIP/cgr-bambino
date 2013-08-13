package TCGA;

import java.util.*;

public class BooleanOptionGroup {
  private ArrayList<BooleanOption> bos;

  public BooleanOptionGroup () {
    bos = new ArrayList<BooleanOption>();
  }

  public void add (BooleanOption bo) {
    bos.add(bo);
    bo.set_bog(this);
  }

  public void setSelected(BooleanOption bo_selected) {
    for (BooleanOption bo : bos) {
      bo.setValueOnly(bo.equals(bo_selected));
    }
  }

}