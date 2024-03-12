package com.here.object.cache.config.redis;

import io.lettuce.core.RedisURI;

/**
 * 
 * @author amajha
 *
 */
public class ServerAddress{
	private String host;
	private int port;
	private String connectionString;
	private boolean isSSL;
	private int database = -1;
	
	/**
	 * @param host host-name of the server to connect
	 * @param port port on which the server expects a connection
	 * @param isSSL whether the connection is secured over SSL
	 */
	public ServerAddress(String host, int port, boolean isSSL) {
		super();
		this.host = host;
		this.port = port;
		this.isSSL = isSSL;
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

		this.host = host;
		this.port = port;
		this.isSSL = isSSL;
		this.database = database;
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

	public RedisURI getRedisURI(){
		RedisURI.Builder redisUriBuilder = RedisURI.builder().withHost(getHost()).withPort(getPort()).withSsl(isSSL);
		if(database != -1)
			redisUriBuilder.withDatabase(database);

		return redisUriBuilder.build();
	}

	public int getDatabase() {
		return database;
	}
}