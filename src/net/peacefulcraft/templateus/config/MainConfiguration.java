package net.peacefulcraft.templateus.config;

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
	
}
