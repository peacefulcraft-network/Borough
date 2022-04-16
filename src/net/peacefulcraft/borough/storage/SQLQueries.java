package net.peacefulcraft.borough.storage;

import java.util.ArrayList;
import java.util.Collections;
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

			List<UUID> owners = Collections.synchronizedList(new ArrayList<UUID>());
			owners.add(owner);

			List<UUID> moderators = Collections.synchronizedList(new ArrayList<UUID>());
			List<UUID> builders = Collections.synchronizedList(new ArrayList<UUID>());

			return new BoroughClaim(claimId, name, owners, moderators, builders);

		} catch (SQLException ex) {
			Borough._this().logSevere("Error creating claim " + name + " for " + owner + ". ");
			// duplicate key, try to refresh cache because it is probably wrong if this happens.
			if (ex.getErrorCode() == 1022) {
				Borough.mysqlThreadPool.execute(() -> { Borough.getClaimStore().getClaim(name, true); });
			}
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
			PreparedStatement stmt = mysql.prepareStatement("SELECT * FROM `claim` WHERE `creator_uuid`=? AND `claim_name`=?");
			stmt.setString(1, owner.toString());
			stmt.setString(2, claimName);
			ResultSet result = stmt.executeQuery();

			if (!result.next()) { return null; }
			
			// Grab fields
			int claim_id = result.getInt("claim_id");
			String claim_name = result.getNString("claim_name");
			Boolean allowBlockDamage = result.getBoolean("allow_block_damage");
			Boolean allowFluidMovement = result.getBoolean("allow_fluid_movement");
			Boolean allowPvP = result.getBoolean("allow_pvp");
			Boolean allowPistonMovement = result.getBoolean("allow_piston_movement");
			Boolean allowTeleport = result.getBoolean("allow_teleport");
			Boolean allowMobSpawn = result.getBoolean("allow_mob_spawn");
			result.close();
			stmt.close();

			stmt = mysql.prepareStatement("SELECT `user_uuid`,`level` FROM `claim_permission` WHERE `claim_id`=?");
			stmt.setInt(1, claim_id);
			result = stmt.executeQuery();
			List<UUID> owners = Collections.synchronizedList(new ArrayList<UUID>());
			owners.add(owner);
			List<UUID> moderators = Collections.synchronizedList(new ArrayList<UUID>());
			List<UUID> builders = Collections.synchronizedList(new ArrayList<UUID>());

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

			BoroughClaim claimMeta = new BoroughClaim(claim_id, claim_name, owners, moderators, builders, allowBlockDamage, allowFluidMovement, allowPvP, allowPistonMovement, allowTeleport, allowMobSpawn);
			result.close();
			stmt.close();
			return claimMeta;
		} catch (SQLException ex) {
			Borough._this().logSevere("Error fetching claim (" + claimName + ").");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static void deleteClaim(BoroughClaim claim) {
		try (
			Connection mysql = ds.getConnection();
		) {
			PreparedStatement stmt = mysql.prepareStatement("DELETE FROM `claim` WHERE `claim_id`=?");
			stmt.setInt(1, claim.getClaimId());
			stmt.executeUpdate();
			stmt.close();

		} catch (SQLException ex) {
			Borough._this().logSevere("Error deleting claim " + claim.getClaimId() + ".");
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static void claimChunk(BoroughClaim claimSource, BoroughChunk claimTarget) {
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

			claimTarget.setClaimMeta(BoroughClaimStore.getClaimKey(claimSource.getCreatorUsername(), claimSource.getClaimName()));
			claimSource.getChunks().add(claimTarget);

		} catch (SQLException ex) {
			Borough._this().logSevere("Error extending claim " + claimSource.getClaimId() + " (" + claimTarget.getWorld() + "," + claimTarget.getChunkX() + "," + claimTarget.getChunkZ() + ").");
			// duplicate key, try to refresh cache because it is probably wrong if this happens.
			if (ex.getErrorCode() == 1022) {
				Borough.mysqlThreadPool.execute(() -> { Borough.getClaimStore().getChunk(claimTarget.getWorld(), claimTarget.getChunkX(), claimTarget.getChunkZ(), true); });
			}
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
			PreparedStatement stmt = mysql.prepareStatement("SELECT `claim_name`,`username`,`world`,`x`,`z` FROM `claim_chunk` LEFT JOIN `claim` ON `claim_chunk`.`claim_id`=`claim`.`claim_id` LEFT JOIN `uuid_cache` ON `claim`.`creator_uuid`=`uuid_cache`.`uuid` WHERE `world`=? AND `x`=? AND `z`=?");
			stmt.setString(1, world);
			stmt.setInt(2, x);
			stmt.setInt(3, z);
			ResultSet result = stmt.executeQuery();

			if (!result.next()) { return null; }

			String ownerUsername = result.getString("username");
			Borough._this().logDebug("Found chunk with owner (" + world + "," + x + "," + z + ") " + ownerUsername);
			if (ownerUsername == null) {
			// unclaimed
				return new BoroughChunk("", world, x, z);

			} else {
			// claimed
				String claimKey = BoroughClaimStore.getClaimKey(ownerUsername, result.getString("claim_name"));
				BoroughClaim claimMeta = Borough.getClaimStore().getClaim(claimKey);
				BoroughChunk chunk = new BoroughChunk(claimKey, result.getString("world"), result.getInt("x"), result.getInt("z"));
				claimMeta.getChunks().add(chunk);

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

			chunk.getClaimMeta().getChunks().remove(chunk);
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
			PreparedStatement stmt = mysql.prepareStatement("REPLACE INTO `claim_permission` VALUES(?,?,?)");
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

	/**
	 * Fetches claim names and permission levels for all the claims which the provided user has permissions on.
	 * Note that the result set will be added to the provided lists. Lists will be sychrnoized such that claimPerms.get(n) will have
	 * the permission level for claimNames.get(n).
	 * @param user The user to fetch claims for
	 * @param claimNames A list to store the claim names in.
	 * @param claimPerms A list to store the claim permission levels in.
	 */
	public static void getClaimsByUser(UUID user, List<String> claimNames, List<BoroughChunkPermissionLevel> claimPerms) {
		try (
			Connection mysql = ds.getConnection()
		) {
			PreparedStatement stmt = mysql.prepareStatement("""
				SELECT CONCAT(`username`, \":\", `claim_name`) as `claim_name`, `level` FROM (
					(
					(SELECT `creator_uuid`,`claim_name`,\"OWNER\" as `level` FROM `claim` WHERE `creator_uuid`=?)
					UNION
					(SELECT `creator_uuid`,`claim_name`,`level` FROM `claim_permission` LEFT JOIN `claim` on `claim_permission`.`claim_id` = `claim`.`claim_id` WHERE `user_uuid`=?)
					) AS `claimset` LEFT JOIN `uuid_cache` ON `uuid_cache`.`UUID`=`claimset`.`creator_uuid`
				)
			""");
			stmt.setString(1, user.toString());
			stmt.setString(2, user.toString());
			ResultSet result = stmt.executeQuery();

			while (result.next()) {
				claimNames.add(result.getString("claim_name"));
				claimPerms.add(BoroughChunkPermissionLevel.valueOf(result.getString("level")));
			}
			result.close();
			stmt.close();

		} catch (SQLException ex) {
			Borough._this().logSevere("Error fetching user claim names " + user);
			throw new RuntimeException("Query error.", ex);
		}
	}

	public static void setClaimFlag(BoroughClaim claim, BoroughClaimFlag flag, boolean value) {
		try (
			Connection mysql = ds.getConnection()
		) {
			PreparedStatement stmt = mysql.prepareStatement("UPDATE `claim` SET `" + flag.toString().toLowerCase() + "`=? WHERE `claim_id`=?");
			stmt.setBoolean(1, value);
			stmt.setInt(2, claim.getClaimId());
			stmt.executeUpdate();
			stmt.close();
		} catch (SQLException ex) {

		}
	}
}