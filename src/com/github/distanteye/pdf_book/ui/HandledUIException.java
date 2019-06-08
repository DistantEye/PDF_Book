/**
 * 
 */
package com.github.distanteye.pdf_book.ui;

/**
 * Stubclass for abstracting exceptions to be caught and turned into a UI prompt alert
 * @author Vigilant
 *
 */
public class HandledUIException extends RuntimeException {
	private static final long serialVersionUID = 2694929139090021969L;


	public HandledUIException() {
	}

	/**
	 * @param arg0
	 */
	public HandledUIException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public HandledUIException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public HandledUIException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public HandledUIException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
