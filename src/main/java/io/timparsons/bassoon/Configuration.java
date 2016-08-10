package io.timparsons.bassoon;

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration {
	@JsonProperty("host")
	private String host;
	
	@NotNull
	@JsonProperty("port")
	private int port;
	
	@JsonProperty("idleTimeout")
	private int idleTimeout = 5000;
	
	@NotNull
	@JsonProperty("path")
	private String path;
	
	@JsonProperty("projectStage")
	private String projectStage = "Production";
	
	@JsonProperty("initParams")
	private Map<String, String> initParams;
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public int getIdleTimeout() {
		return idleTimeout;
	}
	
	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}

	public String getProjectStage() {
		return projectStage;
	}
	
	public void setProjectStage(String projectStage) {
		this.projectStage = projectStage;
	}
	
	public Map<String, String> getInitParams() {
		return initParams;
	}
	
	public void setInitParams(Map<String, String> initParams) {
		this.initParams = initParams;
	}
}
