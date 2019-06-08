/**
 * 
 */
package com.github.distanteye.pdf_book.spe_wrappers;

import com.github.distanteye.pdf_book.swing_panel_extensions.Utils;

/**
 * @author Vigilant
 *
 */
public class IntWrapper extends AccessWrapper<String> {
	int value;
	
	public IntWrapper(int val)
	{
		this.value = val;
	}
	
	@Override
	public String getValue() {
		return ""+value;
	}

	@Override
	public void setValue(String item) {
		if (!Utils.isInteger(item))
		{
			throw new IllegalArgumentException("Must pass an integer to IntWrapper!");
		}
		else
		{
			this.value = Integer.parseInt(item);
		}
	}

	@Override
	public boolean isInt() {
		return true;
	}

	
}
