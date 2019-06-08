/**
 * 
 */
package com.github.distanteye.pdf_book.spe_wrappers;

import com.github.distanteye.pdf_book.ui.Tab;

/**
 * Parent class holding common code across all the various accessors to get/set an aspect of information from a Tab object
 * The data being accessed by those subclasses should be apparent by the naming convention
 * 
 * @author Vigilant
 *
 */
public abstract class TabAccessWrapper<T> extends AccessWrapper<T>  {
	protected Tab model;
	
	public TabAccessWrapper(Tab model)
	{
		this.model = model;
	}
	
	public Tab getModel() {
		return model;
	}
	public void setModel(Tab model) {
		this.model = model;
	}
	
	abstract public T getValue();
	abstract public void setValue(T item);
	
	public void SetData(Object o)
	{
		if (!(o instanceof Tab)) { throw new IllegalArgumentException("Object miust be a Tab object"); }
		setModel((Tab)o);
	}
	
	/**
	 * Reflects whether the underlying value is an Integer or not
	 * 
	 * This provides an accessibility/performance bonus to some contexts
	 * 
	 * @return True/False as appropriate 
	 */
	abstract public boolean isInt();

}
