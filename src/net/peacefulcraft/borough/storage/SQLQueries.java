package net.peacefulcraft.borough.storage;

import java.util.ArrayList;
import java.util.List;
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
		ds.close();
	}

	public static BoroughClaim createClaim(String name, UUID owner) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("INSERT INTO `claim`(claim_name, creator_uuid) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, name);
			stmt.setString(2, owner.toString());
			stmt.executeUpdate();

			ResultSet keys = stmt.getGeneratedKeys();
			keys.next();
			int claimId = keys.getInt(1);
			stmt.close();

			List<UUID> owners = new ArrayList<UUID>();
			owners.add(owner);

			List<UUID> moderators = new ArrayList<UUID>();
			List<UUID> builders = new ArrayList<UUID>();

			return new BoroughClaim(claimId, name, owners, moderators, builders);

		} catch (SQLException ex) {
			Borough._this().logSevere("Error creating claim " + name + " for " + owner + ". ");
			throw new RuntimeException("Query error.", ex);
		}
	}

	/**
	 * Fetch a BoroughChunk by it's encoded name [username:claimname]
	 * @param name
	 * @return BoroughClaim object or NULL if no such object exists.
	 */
	public static BoroughClaim getBoroughClaim(String claimName, UUID owner) {
		try (
			Connection mysql = ds.getConnection()
		) {
			PreparedStatement stmt = mysql.prepareStatement("SELECT `claim_id`,`claim_name` FROM `claim` WHERE `creator_uuid`=? AND `claim_name`=?");
			stmt.setString(1, owner.toString());
			stmt.setString(2, claimName);
			ResultSet result = stmt.executeQuery();

			if (!result.next()) { return null; }
			
			// Grab fields
			int claim_id = result.getInt("claim_id");
			String claim_name = result.getNString("claim_name");
			stmt.close();

			stmt = mysql.prepareStatement("SELECT `user_uuid`,`level` FROM `claim_permission` WHERE `claim_id`=?");
			stmt.setInt(1, claim_id);
			result = stmt.executeQuery();
			List<UUID> owners = new ArrayList<UUID>();
			owners.add(owner);
			List<UUID> moderators = new ArrayList<UUID>();
			List<UUID> builders = new ArrayList<UUID>();

			// Get permissions
			while (result.next()) {
				String level = result.getString("level");
				if (level == null) { continue; }

				BoroughChunkPermissionLevel perms = BoroughChunkPermissionLevel.valueOf(level);
				switch (perms) {
					case BUILDER:
						builders.add(UUID.fromString(result.getString("user_uuid")));
						break;
					case MODERATOR:
						moderators.add(UUID.fromString(result.getString("user_uuid")));
						break;
					case OWNER:
						owners.add(UUID.fromString(result.getString("user_uuid")));
						break;
				}
			}

			BoroughClaim claimMeta = new BoroughClaim(claim_id, claim_name, owners, moderators, builders);
			String username = Borough.getUUIDCache().uuidToUsername(owner);
			Borough.getClaimStore().claimCache.put(BoroughClaimStore.getClaimKey(username, claimName), claimMeta);
			return claimMeta;
		} catch (SQLException ex) {
			Borough._this().logSevere("Error fetching claim (" + claimName + ").");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static void deleteClaim(BoroughChunk chunk) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("DELETE FROM `claim` WHERE `claim_id`=?");
			stmt.setInt(1, chunk.getClaimMeta().getClaimId());
			stmt.executeUpdate();
			stmt.close();

		} catch (SQLException ex) {
			Borough._this().logSevere("Error deleting claim " + chunk.getClaimMeta().getClaimId() + ".");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static BoroughChunk claimChunk(BoroughClaim claimSource, BoroughChunk claimTarget) {
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

			BoroughChunk chunk = new BoroughChunk(claimSource, claimTarget.getWorld(), claimTarget.getChunkX(), claimTarget.getChunkZ());
			return chunk;

		} catch (SQLException ex) {
			Borough._this().logSevere("Error extending claim " + claimSource.getClaimId() + " (" + claimTarget.getWorld() + "," + claimTarget.getChunkX() + "," + claimTarget.getChunkZ() + ").");
			throw new RuntimeException("Query error.", ex);
		}
	}

	/**
	 * Fetch a BoroughChunk and associated metadata using the chunk location
	 * @param world
	 * @param x
	 * @param z
	 * @return
	 * 
	 * @throws RuntimeException If claim meta is corrupted.
	 * @throws SQLException If a query error occurs.
	 */
	public static BoroughChunk getBoroughChunk(String world, int x, int z) {
		try (
			Connection mysql = ds.getConnection()
		) {
			PreparedStatement stmt = mysql.prepareStatement("SELECT * FROM `claim_chunk` LEFT JOIN `claim` ON `claim_chunk`.`claim_id`=`claim`.`claim_id` LEFT JOIN `uuid_cache` ON `claim`.`creator_uuid`=`uuid_cache`.`uuid` WHERE `world`=? AND `x`=? AND `z`=?");
			stmt.setString(1, world);
			stmt.setInt(2, x);
			stmt.setInt(3, z);
			ResultSet result = stmt.executeQuery();

			if (!result.next()) { return null; }

			String ownerUsername = result.getString("username");
			if (ownerUsername == null) {
			// unclaimed
				return new BoroughChunk(null, world, x, z);

			} else {
			// claimed
				String claimName = ownerUsername + ":" + result.getString("name");
				BoroughClaim claimMeta = Borough.getClaimStore().getClaim(claimName);
				BoroughChunk chunk = new BoroughChunk(claimMeta, result.getString("world"), result.getInt("x"), result.getInt("z"));
				Borough.getClaimStore().chunkCache.put(BoroughClaimStore.getChunkKey(chunk), chunk);
	
				return chunk;
			}

		} catch (SQLException ex) {
			Borough._this().logSevere("Error fetching claim (" + world + "," + x + "," + z + ").");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static BoroughChunk unclaimChunk(BoroughChunk chunk) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("DELETE FROM `claim_chunk` WHERE `world`=? AND `x`=? AND `z`=?");
			stmt.setString(1, chunk.getWorld());
			stmt.setInt(2, chunk.getChunkX());
			stmt.setInt(3, chunk.getChunkZ());
			stmt.executeUpdate();
			stmt.close();

			chunk = new BoroughChunk(null, chunk.getWorld(), chunk.getChunkX(), chunk.getChunkZ());
			Borough.getClaimStore().chunkCache.put(BoroughClaimStore.getChunkKey(chunk), chunk);
			return chunk;

		} catch (SQLException ex) {
			Borough._this().logSevere("Error deleting claim " + chunk.getClaimMeta().getClaimId() + ".");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static void setPermissionsOnClaim(BoroughClaim claim, UUID user, BoroughChunkPermissionLevel level) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("REPLACE INTO `claim_chunk` VALUES(?,?,?)");
			stmt.setString(1, user.toString());
			stmt.setInt(2, claim.getClaimId());
			stmt.setString(3, level.toString());
			stmt.executeUpdate();
			stmt.close();

		} catch (SQLException ex) {
			Borough._this().logSevere("Error updating permissions on claim " + claim.getClaimId() + ". Tried to set " + user + " to " + level);
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static void unsetPermissionsOnClaim(BoroughClaim claim, UUID user) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("DELETE FROM `claim_permission` WHERE `user_uuid`=? AND `claim_id`=?");
			stmt.setString(1, user.toString());
			stmt.setInt(2, claim.getClaimId());
			stmt.executeUpdate();
			stmt.close();

		} catch (SQLException ex) {
			Borough._this().logSevere("Error updating permissions on claim " + claim.getClaimId() + ". Tried to unset " + user);
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static void storeUUIDUsernameMapping(UUID uuid, String username) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("REPLACE INTO `uuid_cache` VALUES(?,?)");
			stmt.setString(1, uuid.toString());
			stmt.setString(2, username);
			stmt.executeUpdate();
			stmt.close();

		} catch (SQLException ex) {
			Borough._this().logSevere("Error commiting UUID->Username mapping to cache. " + uuid.toString() + " -> " + username);
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static String getUsernameByUUID(UUID uuid) {
		try (
			Connection mysql = ds.getConnection()
		) {
			PreparedStatement stmt = mysql.prepareStatement("SELECT `username` FROM `uuid_cache` WHERE `uuid`=?");
			stmt.setString(1, uuid.toString());
			ResultSet result = stmt.executeQuery();
			
			if (result.next()) {
				return result.getString("username");
			} else {
				return null;
			}

		} catch (SQLException ex) {
			Borough._this().logSevere("Error fetching UUID " + uuid);
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static UUID getUUIDByUsername(String username) {
		try (
			Connection mysql = ds.getConnection()
		) {
			PreparedStatement stmt = mysql.prepareStatement("SELECT `uuid` FROM `uuid_cache` WHERE `username`=?");
			stmt.setString(1, username);
			ResultSet result = stmt.executeQuery();
			
			if (result.next()) {
				return UUID.fromString(result.getString("uuid"));
			} else {
				return null;
			}

		} catch (SQLException ex) {
			Borough._this().logSevere("Error fetching username " + username);
			throw new RuntimeException("Query error.", ex);
		}
	}
}
