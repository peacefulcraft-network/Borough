package net.peacefulcraft.borough.storage;

import java.util.UUID;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import net.peacefulcraft.borough.Borough;

public class SQLQueries {
	
	private static HikariDataSource ds;

	public static void setup() {
		HikariConfig hc = new HikariConfig();
		// hc.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
		hc.setDriverClassName("org.mariadb.jdbc.Driver");
		hc.setJdbcUrl("jdbc:mariadb://" + Borough.getConfiguration().getMysqlIp() + ":" + Borough.getConfiguration().getMysqlPort() + "/" + Borough.getConfiguration().getMysqlDatabase());
		hc.setUsername(Borough.getConfiguration().getMysqlUsername());
		hc.setPassword(Borough.getConfiguration().getMysqlPassword());
		hc.setPoolName("BoroughClaims");
		/*
		 * TODO: Recomended optimizations
		 * https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
		 */
		
		ds = new HikariDataSource(hc);
	}

	public static void teardown() {

	}

	public BoroughChunk createClaim(String name, BoroughChunk chunk) {
		return null;

	}

	public BoroughChunk deleteClaim(BoroughChunk chunk) {
		return null;

	}

	public BoroughChunk claimChunk(BoroughChunk claimSource, BoroughChunk claimTarget) {
		return null;

	}

	public BoroughChunk unclaimChunk(BoroughChunk chunk) {
		return null;

	}

	public BoroughChunk setPermissionsOnClaim(BoroughChunk chunk, UUID user, BoroughChunkPermissionLevel level) {
		return null;

	}

	public BoroughChunk unsetPermissionsOnClaim(BoroughChunk chunk, UUID user, BoroughChunkPermissionLevel level) {
		return null;

	}

	public BoroughChunk getBoroughChunk(String world, int x, int z) {
		return null;

	}

	public BoroughChunk getBoroughChunk(String name) {
		return null;
		// user:claim_name
	}
}
