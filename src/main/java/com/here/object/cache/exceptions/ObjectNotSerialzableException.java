package com.here.object.cache.exceptions;

public class ObjectNotSerialzableException extends RuntimeException
{

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
