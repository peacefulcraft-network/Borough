package net.peacefulcraft.borough.utilities;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

public class ItemClassifier {
	
	private static List<Material> armor = new ArrayList<Material>();

	public static boolean isArmor(Material item) { return armor.contains(item); }
	public static boolean isEquipable(Material item) { 
		return item == Material.ELYTRA || armor.contains(item);
	}

	static {
		armor.add(Material.LEATHER_BOOTS);
		armor.add(Material.LEATHER_LEGGINGS);
		armor.add(Material.LEATHER_CHESTPLATE);
		armor.add(Material.LEATHER_HELMET);

		armor.add(Material.IRON_BOOTS);
		armor.add(Material.IRON_LEGGINGS);
		armor.add(Material.IRON_CHESTPLATE);
		armor.add(Material.IRON_HELMET);

		armor.add(Material.CHAINMAIL_BOOTS);
		armor.add(Material.CHAINMAIL_LEGGINGS);
		armor.add(Material.CHAINMAIL_CHESTPLATE);
		armor.add(Material.CHAINMAIL_HELMET);

		armor.add(Material.GOLDEN_BOOTS);
		armor.add(Material.GOLDEN_LEGGINGS);
		armor.add(Material.GOLDEN_CHESTPLATE);
		armor.add(Material.GOLDEN_HELMET);

		armor.add(Material.DIAMOND_BOOTS);
		armor.add(Material.DIAMOND_LEGGINGS);
		armor.add(Material.DIAMOND_CHESTPLATE);
		armor.add(Material.DIAMOND_HELMET);

		armor.add(Material.NETHERITE_BOOTS);
		armor.add(Material.NETHERITE_LEGGINGS);
		armor.add(Material.NETHERITE_CHESTPLATE);
		armor.add(Material.NETHERITE_HELMET);
	}

}
