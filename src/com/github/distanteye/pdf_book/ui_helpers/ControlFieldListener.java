package com.github.distanteye.pdf_book.ui_helpers;

import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * Document listener that tracks a Text Components current text as it changes vs the original and displays/removes
 * a message to a specified label when the TextComponent contents change from the original
 * 
 * @author Vigilant
 *
 */
public class ControlFieldListener implements DocumentListener {

	private JTextComponent field;
	private JLabel label;
	private String origText;
	private String labelText;
	
	public ControlFieldListener(JTextComponent field, JLabel label, String labelText)
	{
		this.field = field;
		this.label = label;
		this.origText = field.getText();
		this.labelText = labelText;
	}
	
	public void callUpdateIfReady()
	{
		String newText = field.getText();
		if (!newText.equals(origText))
		{
			label.setText(labelText);
		}
		else
		{
			label.setText("");	
		}
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		callUpdateIfReady();
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		callUpdateIfReady();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		callUpdateIfReady();
	}

}
