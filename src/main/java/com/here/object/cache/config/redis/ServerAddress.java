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
	private int database;
	
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
	 * @param host host-name of the server to connect
	 * @param port port on which the server expects a connection
	 * @param isSSL whether the connection is secured over SSL
	 * @param database the database index to use for storage of data
	 */
	public ServerAddress(String host, int port, boolean isSSL, int database) {
		super();

		if(database < 0 || database >=16){
			throw new IllegalArgumentException("Database Index can be between 0 and 15");
		}

		this.host = host;
		this.port = port;
		this.connectionString="redis"+(isSSL?"s":"")+"://"+host+":"+port+"/"+database;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return the connectionString
	 */
	public String getConnectionString() {
		return connectionString;
	}

	public int getDatabase() {
		return database;
	}
}