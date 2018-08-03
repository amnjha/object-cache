package com.here.object.cache.exceptions;

/**
 * 
 * @author amajha
 *
 */
public class ObjectNotSerialzableException extends RuntimeException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ObjectNotSerialzableException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public ObjectNotSerialzableException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ObjectNotSerialzableException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public ObjectNotSerialzableException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public ObjectNotSerialzableException(Throwable cause) {
		super(cause);
	}

}
