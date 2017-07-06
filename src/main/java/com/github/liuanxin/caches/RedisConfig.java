package com.github.liuanxin.caches;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisConfig extends JedisPoolConfig {

	private String host = Protocol.DEFAULT_HOST;
	private int port = Protocol.DEFAULT_PORT;
	private String password;
	private int database = Protocol.DEFAULT_DATABASE;
	private String clientName;
	private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
	private int soTimeout = Protocol.DEFAULT_TIMEOUT;


	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		if (host == null || "".equals(host.trim())) {
			host = Protocol.DEFAULT_HOST;
		}
		this.host = host;
	}

	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		if (password == null || "".equals(password.trim())) {
			password = null;
		}
		this.password = password;
	}

	public int getDatabase() {
		return database;
	}
	public void setDatabase(int database) {
		this.database = database;
	}

	public String getClientName() {
		return clientName;
	}
	public void setClientName(String clientName) {
		if (clientName == null || "".equals(clientName.trim())) {
			clientName = null;
		}
		this.clientName = clientName;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getSoTimeout() {
		return soTimeout;
	}
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}
}
