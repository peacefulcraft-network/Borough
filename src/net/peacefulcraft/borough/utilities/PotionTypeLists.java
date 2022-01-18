package net.peacefulcraft.borough.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.potion.PotionEffectType;

public class PotionTypeLists {
	
	private static List<PotionEffectType> negativeEffects = new ArrayList<>(Arrays.asList(
		PotionEffectType.BAD_OMEN,
		PotionEffectType.BLINDNESS,
		PotionEffectType.CONFUSION,
		PotionEffectType.HARM,
		PotionEffectType.HUNGER,
		PotionEffectType.POISON,
		PotionEffectType.SLOW,
		PotionEffectType.SLOW_DIGGING,
		PotionEffectType.SLOW_FALLING,
		PotionEffectType.UNLUCK,
		PotionEffectType.WEAKNESS,
		PotionEffectType.WITHER
	));

	/**
	 * Determines if a potion effect type is negative
	 * @param type Given potion effect type
	 * @return True if negative, false otherwise
	 */
	public static boolean isNegativeEffect(PotionEffectType type) {
		return negativeEffects.contains(type);
	}

}
