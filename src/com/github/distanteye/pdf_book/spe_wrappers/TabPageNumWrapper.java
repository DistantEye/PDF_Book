/**
 * 
 */
package com.github.distanteye.pdf_book.spe_wrappers;

import com.github.distanteye.pdf_book.swing_panel_extensions.Utils;
import com.github.distanteye.pdf_book.ui.Tab;

/**
 * @author Vigilant
 *
 */
public class TabPageNumWrapper extends TabAccessWrapper<String> {

	public TabPageNumWrapper(Tab model) {
		super(model);
	}

	public String getValue() {
		return ""+model.getCurrentPage();
	}


	@Override
	public void setValue(String item) {
		if (!Utils.isInteger(item))
		{
			throw new IllegalArgumentException("Must pass an integer to TabPageNumWrapper!");
		}
		else
		{
			model.setCurrentPage(Integer.parseInt(item));
		}
	}

	/**
	 * Reflects whether the underlying value is an Integer or not
	 * 
	 * This provides an accessibility/performance bonus to some contexts
	 * 
	 * @return True/False as appropriate 
	 */
	public boolean isInt()
	{
		return true;
	}

}
