package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class ImageScalePanel2 extends ScalePanel2 {
  private BufferedImage offscreen;
  private JScrollPane jsp;

  public ImageScalePanel2 (BufferedImage offscreen, JScrollBar jsb_h, JScrollBar jsb_v) {
    super(new Dimension(offscreen.getWidth(), offscreen.getHeight()),
	  jsb_h, jsb_v);
    this.offscreen = offscreen;
  }

  public void set_image (BufferedImage offscreen) {
    this.offscreen = offscreen;
    set_raw_size(new Dimension(offscreen.getWidth(), offscreen.getHeight()));
    set_extents_horizontal();
    set_extents_vertical();
  }

  public void set_image_size (Dimension d) {
    set_raw_size(d);
    set_extents_horizontal();
    set_extents_vertical();
  }
  
  protected void paintComponent(Graphics g) {
    Dimension d_component = getSize();
    Dimension d_scaled = get_scaled_size();
    //    System.err.println("scaled:"+d_scaled + " x_scale:"+get_horizontal_scale_level());  // debug

    //    System.err.println("img size:"+offscreen.getWidth() + "x"+offscreen.getHeight());

    g.setColor(Color.black);
    g.fillRect(0,0,d_component.width,d_component.height);

    int start_x = get_unscaled_x_start();
    int start_y = get_unscaled_y_start();

    // FIX ME:
    // write some kind of rectangle translation code in ScalePanel2
    // which takes Graphics as an arg.

    float x_scale = get_horizontal_scale_level();
    float y_scale = get_vertical_scale_level();

    float x_fit = d_component.width / x_scale;
    float y_fit = d_component.height / y_scale;

    //    int end_x = get_unscaled_x_end();
    //    int end_y = get_unscaled_y_end();

    double x_ceil = Math.ceil(x_fit);
    double y_ceil = Math.ceil(y_fit);

    int end_x = start_x + (int) x_ceil;
    int end_y = start_y + (int) y_ceil;
    // round up the number of pixels from source image to paint.
    // The last pixel may be only partially painted to the extent it is offscreen.

    int end_x_comp = (int) (x_ceil * x_scale);
    int end_y_comp = (int) (y_ceil * y_scale);
    // target image coordinates: last pixel may be offscreen

    //    System.err.println("start:"+start_x + " end:" + end_x + " x_scale:"+x_scale +" x_fit:"+x_fit + " end_x:" + end_x + " end_x_comp:"+end_x_comp);  // debug

    //    System.err.println("image paint, scaled="+get_scaled_size() + " size:" + getSize() + " preferred:" + getPreferredSize());  // debug

    g.drawImage(offscreen,
		0, 0, end_x_comp, end_y_comp,
		// coordinates of the Component being painted on

		start_x, start_y, end_x, end_y,
		// coordinates in offscreen image being painted
		// (zoom will be applied here)

		null);
  }

}
