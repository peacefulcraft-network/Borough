package net.peacefulcraft.borough.storage;

import java.util.UUID;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("DELETE FROM `claim` WHERE `claim_id`=?");
			stmt.setInt(1, chunk.getClaimMeta().getClaimId());
			stmt.executeUpdate();
			stmt.close();

			return new BoroughChunk(null, chunk.getWorld(), chunk.getChunkX(), chunk.getChunkZ());

		} catch (SQLException ex) {
			Borough._this().logSevere("Error deleting claim " + chunk.getClaimMeta().getClaimId() + ".");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public BoroughChunk claimChunk(BoroughClaim claimSource, BoroughChunk claimTarget) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("INSERT INTO `claim_chunk` VALUES(?,?,?,?)");
			stmt.setInt(1, claimSource.getClaimId());
			stmt.setString(2, claimTarget.getWorld());
			stmt.setInt(3, claimTarget.getChunkX());
			stmt.setInt(4, claimTarget.getChunkZ());
			stmt.executeUpdate();
			stmt.close();

			return new BoroughChunk(claimSource, claimTarget.getWorld(), claimTarget.getChunkX(), claimTarget.getChunkZ());

		} catch (SQLException ex) {
			Borough._this().logSevere("Error extending claim " + claimSource.getClaimId() + " (" + claimTarget.getWorld() + "," + claimTarget.getChunkX() + "," + claimTarget.getChunkZ() + ").");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public BoroughChunk unclaimChunk(BoroughChunk chunk) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("DELETE FROM `claim_chunk` WHERE `world`=? AND `x`=? AND `z`=?");
			stmt.setString(1, chunk.getWorld());
			stmt.setInt(2, chunk.getChunkX());
			stmt.setInt(3, chunk.getChunkZ());
			stmt.executeUpdate();
			stmt.close();

			return new BoroughChunk(null, chunk.getWorld(), chunk.getChunkX(), chunk.getChunkZ());

		} catch (SQLException ex) {
			Borough._this().logSevere("Error deleting claim " + chunk.getClaimMeta().getClaimId() + ".");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public void setPermissionsOnClaim(BoroughChunk chunk, UUID user, BoroughChunkPermissionLevel level) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("REPLACE INTO `claim_chunk` VALUES(?,?,?)");
			stmt.setString(1, user.toString());
			stmt.setInt(2, chunk.getClaimMeta().getClaimId());
			stmt.setString(3, level.toString());
			stmt.executeUpdate();
			stmt.close();

		} catch (SQLException ex) {
			Borough._this().logSevere("Error updating permissions on claim " + chunk.getClaimMeta().getClaimId() + " (" + chunk.getWorld() + "," + chunk.getChunkX() + "," + chunk.getChunkZ() + "). Set " + user + " to " + level);
			throw new RuntimeException("Query error.", ex);
		}
	}

	public void unsetPermissionsOnClaim(BoroughChunk chunk, UUID user) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("DELETE FROM `claim_permission` WHERE `user_uuid`=? AND `claim_id`=?");
			stmt.setString(1, user.toString());
			stmt.setInt(2, chunk.getClaimMeta().getClaimId());
			stmt.executeUpdate();
			stmt.close();

		} catch (SQLException ex) {
			Borough._this().logSevere("Error updating permissions on claim " + chunk.getClaimMeta().getClaimId() + " (" + chunk.getWorld() + "," + chunk.getChunkX() + "," + chunk.getChunkZ() + "). Unuset " + user);
			throw new RuntimeException("Query error.", ex);
		}
	}

	public BoroughChunk getBoroughChunk(String world, int x, int z) {
		try (
			Connection mysql = ds.getConnection()
		) {
			PreparedStatement stmt = mysql.prepareStatement("SELECT * FROM `claim_chunk` LEFT JOIN `claim` ON `claim_chunk`.`claim_id`=`claim`.`claim_id` WHERE `world`=? AND `x`=? AND `z`=?");
			stmt.setString(1, world);
			stmt.setInt(2, x);
			stmt.setInt(3, z);
			ResultSet result = stmt.executeQuery();

			// TODO: Pending UUID resolution
			
			return null;

		} catch (SQLException ex) {
			Borough._this().logSevere("Error fetching claim (" + world + "," + x + "," + z + ").");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public BoroughChunk getBoroughChunk(String name) {
		// TODO: Pending UUID resolution

		return null;
		// user:claim_name
	}
}
