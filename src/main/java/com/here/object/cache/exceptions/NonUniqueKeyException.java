package com.here.object.cache.exceptions;

/**
 * 
 * @author amajha
 *
 */
public class NonUniqueKeyException extends RuntimeException {

	/**
	 * 
	 */
	public NonUniqueKeyException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public NonUniqueKeyException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public NonUniqueKeyException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public NonUniqueKeyException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public NonUniqueKeyException(Throwable cause) {
		super(cause);
	}
	
}
