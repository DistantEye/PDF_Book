/**
 * 
 */
package com.github.distanteye.pdf_book.ui_helpers;

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

/**
 * A simple mouse adapter that allows a component to try and defocus itself should someone click outside of it
 * 
 * @author Vigilant
 *
 */
public class ComponentDefocuser extends MouseAdapter {
	private JComponent comp;
	private String origText;
	private boolean restoreOriginal;
	
	/**
	 * Creates the defocuser tied to the particular component
	 * @param comp The JComponent to consider the boundaries of and try and defocus. This is usually a TextComponent but theoretically doesn't have to be
	 */
	public ComponentDefocuser(JComponent comp) {
		this.comp = comp;
		if (comp instanceof JTextComponent)
		{
			origText = ((JTextComponent)comp).getText();
		}
		restoreOriginal = true;
	}
 
	public boolean isRestoreOriginal() {
		return restoreOriginal;
	}



	public void setRestoreOriginal(boolean restoreOriginal) {
		this.restoreOriginal = restoreOriginal;
	}

	@Override
    public void mouseClicked(MouseEvent e){
		if (!comp.hasFocus())
		{
			return; // we don't care about non-focused components
		}
		
        Rectangle boundaries = comp.getBounds();
        boundaries.setLocation(comp.getLocationOnScreen()); // this should convert the boundaries to an absolute position (hopefully)
        Point p = e.getLocationOnScreen();
        
        Container parent = comp.getParent();
        if (!boundaries.contains(p) && parent != null)
        {
        	// a nice little fallback to replace blanked out text with the default value at start if we're leaving focus on an unfilled field
        	if (restoreOriginal && comp instanceof JTextComponent && ((JTextComponent)comp).getText().trim().length() == 0)
    		{
    			((JTextComponent)comp).setText(origText);
    		}
        	
        	parent.requestFocus();
        }
    }
}
