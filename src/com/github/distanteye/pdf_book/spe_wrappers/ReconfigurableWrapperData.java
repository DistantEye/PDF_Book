/**
 * 
 */
package com.github.distanteye.pdf_book.spe_wrappers;

/**
 * @author Vigilant
 *
 */
public interface ReconfigurableWrapperData {
	void SetData(Object o);
	ReconfigurableWrapperData CloneWrapper();
}
