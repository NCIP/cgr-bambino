package TCGA;

import java.util.*;
import javax.swing.table.*;

public class DefaultTableModelReadOnly extends DefaultTableModel {
  public DefaultTableModelReadOnly (Vector rows, Vector columns) {
    super(rows, columns);
  }
  
  public boolean isCellEditable(int row, int column) {
    return false;
  }
}

