package com.here.object.cache.exceptions;

public class InvalidConfigException extends RuntimeException {

	/**
	 * 
	 */
	public InvalidConfigException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public InvalidConfigException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InvalidConfigException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public InvalidConfigException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public InvalidConfigException(Throwable cause) {
		super(cause);
	}
	
}
