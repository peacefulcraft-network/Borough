package net.peacefulcraft.borough.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.peacefulcraft.borough.Borough;

public abstract class Configuration {

	private String resourceName;
	protected String configName;
		public String getConfigName() { return this.configName; }

	protected File configFile;
		public File getConfigFile() { return this.configFile; }

	protected FileConfiguration config;

	public Configuration(String configName) {
		this.configName = configName;
		this.createDefaultConfiguration(true);
		this.loadConfiguration();
	}

	public Configuration(String resourceName, String configName) {
		this.resourceName = resourceName;
		this.configName = configName;
		this.createDefaultConfiguration(false);
		this.loadConfiguration();
	}

	protected void createDefaultConfiguration(Boolean empty) {
		try {
			this.configFile = new File(Borough._this().getDataFolder(), this.configName);
			if (!this.configFile.exists()) {
				this.configFile.getParentFile().mkdirs();
				if (empty) {
					this.configFile.createNewFile();
				} else {
					InputStream in = getClass().getClassLoader().getResourceAsStream(this.resourceName);
					OutputStream out = new FileOutputStream(this.configFile);
					byte[] copyBuffer = new byte[1024];
					int read;
					while((read = in.read(copyBuffer)) > 0) {
						out.write(copyBuffer, 0, read);
					}
					out.close();
					in.close();
				}
			} else {
				Borough._this().logNotice("Found existing file at " + this.configName + " - not creating a new one");
			}
		} catch (IOException e) {
			Borough._this().logSevere("Error initializing config file " + this.configName);
			e.printStackTrace();
		}
	}

	protected void loadConfiguration() {
		this.config = YamlConfiguration.loadConfiguration(this.configFile);
	}

	public void saveConfiguration() {
		try {
			this.config.save(this.configFile);
		  } catch (IOException e) {
			e.printStackTrace();
			Borough._this().logSevere("Unable to save configuration file.");
		  }
	}

	/**@return True if creatures can trample crops */
	public boolean isCreatureTrampleEnabled() { return this.config.getBoolean("world.creature_trample"); }
	/**@return True if players can trample crops */
	public boolean isPlayerTrampleEnabled() { return this.config.getBoolean("world.player_trample"); }
	/**@return True if enderman can pick up blocks */
	public boolean isEndermanGriefEnabled() { return this.config.getBoolean("world.enderman_grief"); }
	/**@return True if creatures can trigger pressure plate */
	public boolean isCreatureTriggeringPressurPlateEnabled() { return this.config.getBoolean("world.creature_trigger_pressure_plate"); }
}
