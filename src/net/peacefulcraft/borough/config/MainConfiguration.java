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

	public Integer getWorkerPoolSize() {
		return this.config.getInt("worker_pool_size", 5);
	}

	/**@return True if creatures can trample crops */
	public boolean isCreatureTrampleEnabled() { return this.config.getBoolean("world.creature_trample"); }
	/**@return True if players can trample crops */
	public boolean isPlayerTrampleEnabled() { return this.config.getBoolean("world.player_trample"); }
	/**@return True if enderman can pick up blocks */
	public boolean isEndermanGriefEnabled() { return this.config.getBoolean("world.enderman_grief"); }
	/**@return True if creatures can trigger pressure plate */
	public boolean isCreatureTriggeringPressurPlateEnabled() { return this.config.getBoolean("world.creature_trigger_pressure_plate"); }
	/**@return True if staff bypass is enabled */
	public boolean isStaffBypassEnabled() { return this.config.getBoolean("staff_bypass"); }
}
