package TCGA;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

public class TableTools {
  public static void calcColumnWidths(JTable table) {
    // from http://www.chka.de/swing/table/cell-sizes.html
    JTableHeader header = table.getTableHeader();

    TableCellRenderer defaultHeaderRenderer = null;

    if (header != null)
      defaultHeaderRenderer = header.getDefaultRenderer();

    TableColumnModel columns = table.getColumnModel();
    TableModel data = table.getModel();

    int margin = columns.getColumnMargin(); // only JDK1.3

    int rowCount = data.getRowCount();

    int totalWidth = 0;

    for (int i = columns.getColumnCount() - 1; i >= 0; --i)
      {
        TableColumn column = columns.getColumn(i);
            
        int columnIndex = column.getModelIndex();
            
        int width = -1; 

        TableCellRenderer h = column.getHeaderRenderer();
          
        if (h == null)
	  h = defaultHeaderRenderer;
            
        if (h != null) // Not explicitly impossible
	  {
            Component c = h.getTableCellRendererComponent
	      (table, column.getHeaderValue(),
	       false, false, -1, i);
                    
            width = c.getPreferredSize().width;
	  }
       
        for (int row = rowCount - 1; row >= 0; --row)
	  {
            TableCellRenderer r = table.getCellRenderer(row, i);
                 
            Component c = r.getTableCellRendererComponent
	      (table,
	       data.getValueAt(row, columnIndex),
	       false, false, row, i);
        
	    width = Math.max(width, c.getPreferredSize().width);
	  }

        if (width >= 0)
	  column.setPreferredWidth(width + margin); // <1.3: without margin
        else
	  ; // ???
            
        totalWidth += column.getPreferredWidth();
      }

    // only <1.3:   totalWidth += columns.getColumnCount() * columns.getColumnMargin();


    Dimension size = table.getPreferredScrollableViewportSize();
    size.width = totalWidth;
    //    table.setPreferredScrollableViewportSize(size);

    // table.sizeColumnsToFit(-1); <1.3; possibly even table.revalidate()

    // if (header != null)
    //     header.repaint(); only makes sense when the header is visible (only <1.3)
  }
  
}