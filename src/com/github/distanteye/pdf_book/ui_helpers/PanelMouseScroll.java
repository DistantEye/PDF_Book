/**
	* 
	*/
package com.github.distanteye.pdf_book.ui_helpers;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

//adapted from https://stackoverflow.com/a/31173371/2934891
public class PanelMouseScroll extends MouseAdapter {
	private Point origin;
	private JPanel panel;
	
	public PanelMouseScroll(JPanel panel)
	{
		this.panel = panel;
	}

	@Override
	public void mousePressed(MouseEvent e) {
	    origin = new Point(e.getPoint());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	    if (origin != null) {
	        JViewport viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, panel);
	        if (viewPort != null) {
	            int deltaX = origin.x - e.getX();
	            int deltaY = origin.y - e.getY();

	            Rectangle view = viewPort.getViewRect();
	            view.x += deltaX;
	            view.y += deltaY;

	            panel.scrollRectToVisible(view);
	        }
	    }
	}
}