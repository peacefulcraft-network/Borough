package net.peacefulcraft.borough.config;

public class MainConfiguration extends Configuration {

	public MainConfiguration() {
		super("config.yml", "config.yml");

		this.loadValues();
	}

	private void loadValues() {
		this.isDebugEnabled = this.config.getBoolean("debug");
	}

	private Boolean isDebugEnabled;
	public Boolean isDebugEnabled() {
		return this.isDebugEnabled;
	}
	public void setDebugEnabled(Boolean enabled) {
		this.isDebugEnabled = enabled;
	}

	public String getMysqlIp() {
		return this.config.getString("mysql.ip");
	}

	public int getMysqlPort() {
		return this.config.getInt("mysql.port");
	}

	public String getMysqlDatabase() {
		return this.config.getString("mysql.database");
	}

	public String getMysqlUsername() {
		return this.config.getString("mysql.username");
	}

	public String getMysqlPassword() {
		return this.config.getString("mysql.password");
	}
}
