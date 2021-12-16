package net.peacefulcraft.borough.utilities;

import java.util.List;

import org.bukkit.entity.EntityType;

public class EntityHandler {
	
	private static List<EntityType> passives;

	public static boolean isPassive(EntityType type) { return passives.contains(type); }

	static {
		passives.add(EntityType.BAT);
		passives.add(EntityType.COD);
		passives.add(EntityType.COW);
		passives.add(EntityType.CHICKEN);
		passives.add(EntityType.DOLPHIN);
		passives.add(EntityType.DONKEY);
		passives.add(EntityType.FOX);
		passives.add(EntityType.HORSE);
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
		passives.add(EntityType.GLOW_SQUID);
	}

}
