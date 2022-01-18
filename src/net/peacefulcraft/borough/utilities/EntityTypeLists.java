package net.peacefulcraft.borough.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import net.peacefulcraft.borough.Borough;

public class EntityTypeLists {
	
	private static List<EntityType> passives = new ArrayList<>();

	public static boolean isPassive(EntityType type) { return passives.contains(type); }

	private static List<EntityType> ExplosiveEntityTypes = new ArrayList<>(Arrays.asList(
			EntityType.CREEPER,
			EntityType.DRAGON_FIREBALL, 
			EntityType.FIREBALL, 
			EntityType.SMALL_FIREBALL,
			EntityType.FIREWORK, 
			EntityType.MINECART_TNT, 
			EntityType.PRIMED_TNT, 
			EntityType.WITHER, 
			EntityType.WITHER_SKULL,
			EntityType.ENDER_CRYSTAL));
	
	private static List<EntityType> ExplosivePVMEntityTypes = new ArrayList<>(Arrays.asList(
			EntityType.CREEPER,
			EntityType.DRAGON_FIREBALL, 
			EntityType.FIREBALL, 
			EntityType.SMALL_FIREBALL,
			EntityType.WITHER, 
			EntityType.WITHER_SKULL,
			EntityType.ENDER_CRYSTAL));

	private static List<EntityType> ExplosivePVPEntityTypes = new ArrayList<>(Arrays.asList(
			EntityType.FIREWORK, 
			EntityType.MINECART_TNT, 
			EntityType.PRIMED_TNT));
	
	public static boolean isInstanceOfAny(List<Class<?>> classes, Object obj) {

		for (Class<?> c : classes)
			if (c.isInstance(obj))
				return true;
		return false;
	}

	public static List<Class<?>> parseLivingEntityClassNames(List<String> mobClassNames, String errorPrefix) {

		List<Class<?>> livingEntityClasses = new ArrayList<>();
		for (String mobClassName : mobClassNames) {
			if (mobClassName.isEmpty())
				continue;

			try {
				Class<?> c = Class.forName("org.bukkit.entity." + mobClassName);
				livingEntityClasses.add(c);
			} catch (ClassNotFoundException e) {
				Borough._this().logWarning(String.format("%s%s is not an acceptable class.", errorPrefix, mobClassName));
			} catch (Exception e) {
				Borough._this().logWarning(String.format("%s%s is not an acceptable living entity.", errorPrefix, mobClassName));
			}
		}
		return livingEntityClasses;
	}
	
	/**
	 * Helper method to get a Material from an Entity.
	 * Used with protection tests in plots.
	 * 
	 * @param entityType EntityType to gain a Material value for.
	 * @return null or a suitable Material.
	 */
	@Nullable
	public static Material parseEntityToMaterial(EntityType entityType) {
		return switch (entityType) {
			case AXOLOTL -> Material.AXOLOTL_BUCKET;
			case COD -> Material.COD;
			case SALMON -> Material.SALMON;
			case PUFFERFISH -> Material.PUFFERFISH;
			case TROPICAL_FISH -> Material.TROPICAL_FISH;
			case ITEM_FRAME -> Material.ITEM_FRAME;
			case GLOW_ITEM_FRAME -> Material.GLOW_ITEM_FRAME;
			case PAINTING -> Material.PAINTING;
			case ARMOR_STAND -> Material.ARMOR_STAND;
			case LEASH_HITCH -> Material.LEAD;
			case ENDER_CRYSTAL -> Material.END_CRYSTAL;
			case MINECART, MINECART_MOB_SPAWNER -> Material.MINECART;
			case MINECART_CHEST -> Material.CHEST_MINECART;
			case MINECART_FURNACE -> Material.FURNACE_MINECART;
			case MINECART_COMMAND -> Material.COMMAND_BLOCK_MINECART;
			case MINECART_HOPPER -> Material.HOPPER_MINECART;
			case MINECART_TNT -> Material.TNT_MINECART;
			case BOAT -> Material.OAK_BOAT;
			default -> null;
		};
	}

	/**
	 * Helper method for parsing an entity to a material, or a default material if none is found.
	 * @param entityType Entity type to parse
	 * @param defaultValue Material to use if none could be found.
	 * @return The parsed material, or the fallback value.
	 */
	public static Material parseEntityToMaterial(EntityType entityType, Material defaultValue) {
		Material material = parseEntityToMaterial(entityType);
		return material == null ? defaultValue : material;
	}
	
	/**
	 * A list of explosion-causing entities.
	 * 
	 * @param entityType EntityType to test.
	 * @return true if the EntityType will explode.
	 */
	public static boolean isExplosive(EntityType entityType) {

		return ExplosiveEntityTypes.contains(entityType);	
	}
	
	/**
	 * A list of PVP explosion-causing entities.
	 * 
	 * @param entityType EntityType to test.
	 * @return true if the EntityType is PVP and will explode.
	 */
	public static boolean isPVPExplosive(EntityType entityType) {

		return ExplosivePVPEntityTypes.contains(entityType);	
	}
	
	/**
	 * A list of PVM explosion-causing entities.
	 * 
	 * @param entityType EntityType to test.
	 * @return true if the EntityType is PVM and will explode.
	 */
	public static boolean isPVMExplosive(EntityType entityType) {

		return ExplosivePVMEntityTypes.contains(entityType);	
	}

	static {
		passives.add(EntityType.ARMOR_STAND);
		passives.add(EntityType.BAT);
		passives.add(EntityType.COD);
		passives.add(EntityType.COW);
		passives.add(EntityType.CHICKEN);
		passives.add(EntityType.DOLPHIN);
		passives.add(EntityType.DONKEY);
		passives.add(EntityType.FOX);
		passives.add(EntityType.HORSE);
		passives.add(EntityType.GLOW_ITEM_FRAME);
		passives.add(EntityType.GLOW_SQUID);
		passives.add(EntityType.IRON_GOLEM);
		passives.add(EntityType.ITEM_FRAME);
		passives.add(EntityType.LLAMA);
		passives.add(EntityType.MULE);
		passives.add(EntityType.MUSHROOM_COW);
		passives.add(EntityType.OCELOT);
		passives.add(EntityType.PANDA);
		passives.add(EntityType.PARROT);
		passives.add(EntityType.PIG);
		passives.add(EntityType.POLAR_BEAR);
		passives.add(EntityType.PUFFERFISH);
		passives.add(EntityType.RABBIT);
		passives.add(EntityType.SHEEP);
		passives.add(EntityType.SNOWMAN);
		passives.add(EntityType.SQUID);
		passives.add(EntityType.TROPICAL_FISH);
	}

}
