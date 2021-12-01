package net.peacefulcraft.borough.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
		ds.close();
	}

	public BoroughClaim createClaim(String name, UUID owner) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("INSERT INTO `claim`(name, creator_uuid) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, name);
			stmt.setString(2, owner.toString());
			stmt.executeUpdate();

			ResultSet keys = stmt.getGeneratedKeys();
			keys.next();
			int claimId = keys.getInt(1);
			stmt.close();

			return new BoroughClaim(claimId, name);

		} catch (SQLException ex) {
			Borough._this().logSevere("Error creating claim " + name + " for " + owner + ". ");
			throw new RuntimeException("Query error.", ex);
		}
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

	public BoroughChunk getBoroughChunk(String world, int x, int y, int z) {
		return null;

	}

	public BoroughChunk getBoroughChunk(String name) {
		return null;
		// user:claim_name
	}
}
