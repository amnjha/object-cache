package com.here.object.cache.config.redis;

/**
 * 
 * @author amajha
 *
 */
public class ServerAddress{
	private String host;
	private int port;
	private String connectionString;
	
	/**
	 * @param host host-name of the server to connect
	 * @param port port on which the server expects a connection
	 * @param isSSL whether the connection is secured over SSL
	 */
	public ServerAddress(String host, int port, boolean isSSL) {
		super();
		this.host = host;
		this.port = port;
		this.connectionString="redis"+(isSSL?"s":"")+"://"+host+":"+port;
	}
	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}
	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}
	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}
	/**
	 * @return the connectionString
	 */
	public String getConnectionString() {
		return connectionString;
	}
	
	
}