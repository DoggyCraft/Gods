package com.dogonfire.gods;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.dogonfire.gods.managers.HolyPowerManager;


public class HolyArtifact
{
	private final ItemStack s;

	public HolyArtifact(ItemStack itemStack)
	{
		this.s = itemStack;
	}

	public String generateName(Material itemType, String godName)
	{
		String name = "Holy Artifact of " + godName;
		
		switch (itemType)
		{
		case DIAMOND_SHOVEL:
			name = godName + "'s Magic Stick";
			break;
		case TORCH:
			name = "Skull of " + godName;
			break;
		case CHAINMAIL_BOOTS:
			name = "Red Rose of " + godName;
			break;
		case BOW:
			name = godName + "'s Shower sponge";
			break;
		case CAKE:
			name = godName + "'s Weed";
			break;
		case MAP:
			name = godName + "'s Lantern";
			break;
		case CHICKEN:
			name = godName + "'s Paperbasket";
			break;
//		case REDSTONE_COMPARATOR:
//			name = godName + "'s Cinematicket";
//			break;
		case SAND:
			name = "Bone of " + godName;
			break;
		case COBBLESTONE_STAIRS:
			name = "Matchstick of " + godName;
			break;
//		case SIGN_POST:
//			name = godName + "'s nailcutter";
//			break;
		case REDSTONE_WIRE:
			name = "Bedside Clock of " + godName;
			break;
//		case REDSTONE_TORCH_ON:
//			name = "Fishing pole of " + godName;
//			break;
		case PUMPKIN_SEEDS:
			name = godName + "'s boots";
			break;
//		case WOOD_SPADE:
//		case WOOD_STAIRS:
//		case WOOD_STEP:
//		case WOOD_SWORD:
//		case WOOL:
//			name = godName + "'s favorite song";
//			break;
//		case MONSTER_EGG:
//			name = godName + "'s gardening shovel";
//			break;
//		case MOB_SPAWNER:
//			name = "Butterknife of " + godName;
//			break;
		case MELON_STEM:
			name = godName + "'s toothpick";
			break;
		default:
			break;
		}
		return name;
	}

	public int getCooldown()
	{
		//ItemStack nms = CraftItemStack.asNMSCopy(this.s);
		//return nms.getTag().getInt("cooldown");
		return 0;
	}

	public org.bukkit.inventory.ItemStack getItemStack()
	{
		return this.s;
	}

	public List<String> getPages()
	{
		return this.s.getItemMeta().getLore();
	}

	public String getTitle()
	{
		return this.s.getItemMeta().getDisplayName();
	}

	public int getXP()
	{
		//ItemStack nms = CraftItemStack.asNMSCopy(this.s);
		//return nms.getTag().getInt("xp");
		return 0;
	}

	public boolean hasPages()
	{
		return (this.s.hasItemMeta()) && (this.s.getItemMeta().hasLore());
	}

	public boolean hasTitle()
	{
		return (this.s.hasItemMeta()) && (this.s.getItemMeta().hasDisplayName());
	}

	public boolean isHolyArtifact()
	{
		if ((this.s.hasItemMeta()) && (this.s.getItemMeta().hasLore()))
		{
			return true;
		}
		return false;
	}

	public org.bukkit.inventory.ItemStack setCustomFields(int cooldown, int xp, HolyPowerManager.HolyPower holyPower)
	{
		//ItemStack nms = CraftItemStack.asNMSCopy(this.s);

		//if (nms.getTag() == null)
		//{
		//	nms.setTag(new NBTTagCompound());
		//}

		//nms.getTag().setInt("cooldown", cooldown);
		//nms.getTag().setInt("xp", xp);
		//nms.getTag().setString("holypower", holyPower.name());

		//return CraftItemStack.asCraftMirror(nms);
		
		return null;
	}

	public void setPages(List<String> pages)
	{
		ItemMeta itemMeta = this.s.getItemMeta();

		itemMeta.setLore(pages);

		this.s.setItemMeta(itemMeta);
	}

	public void setTitle(String title)
	{
		ItemMeta itemMeta = this.s.getItemMeta();

		itemMeta.setDisplayName(title);

		this.s.setItemMeta(itemMeta);
	}
}