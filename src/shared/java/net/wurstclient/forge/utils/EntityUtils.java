package net.wurstclient.forge.utils;

import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.forge.compatibility.WMinecraft;

public class EntityUtils {
	private final static Minecraft mc = Minecraft.getMinecraft();
	private final static WMinecraft wmc = new WMinecraft();

	public static boolean isPassive(Entity e) {
		if (e instanceof EntityWolf && ((EntityWolf) e).isAngry())
			return false;
		if (e instanceof EntityAnimal || e instanceof EntityAgeable || e instanceof EntityTameable
				|| e instanceof EntityAmbientCreature || e instanceof EntitySquid)
			return true;
		if (e instanceof EntityIronGolem && ((EntityIronGolem) e).getRevengeTarget() == null)
			return true;
		return false;
	}

	public static boolean isLiving(Entity e) {
		return e instanceof EntityLivingBase;
	}

	public static boolean isFakeLocalPlayer(Entity entity) {

		return entity != null && entity.getEntityId() == -100 && mc.player != entity;
	}

	/**
	 * Find the entities interpolated amount
	 */
	public static Vec3d getInterpolatedAmount(Entity entity, double x, double y, double z) {
		return new Vec3d((entity.posX - entity.lastTickPosX) * x, (entity.posY - entity.lastTickPosY) * y,
				(entity.posZ - entity.lastTickPosZ) * z);
	}

	public static Vec3d getInterpolatedAmount(Entity entity, Vec3d vec) {
		return getInterpolatedAmount(entity, vec.x, vec.y, vec.z);
	}

	public static Vec3d getInterpolatedAmount(Entity entity, double ticks) {
		return getInterpolatedAmount(entity, ticks, ticks, ticks);
	}

	public static boolean isMobAggressive(Entity entity) {
		if (entity instanceof EntityPigZombie) {
			// arms raised = aggressive, angry = either game or we have set the anger
			// cooldown
			if (((EntityPigZombie) entity).isArmsRaised() || ((EntityPigZombie) entity).isAngry()) {
				return true;
			}
		} else if (entity instanceof EntityWolf) {
			return ((EntityWolf) entity).isAngry() && !mc.player.equals(((EntityWolf) entity).getOwner());
		} else if (entity instanceof EntityEnderman) {
			return ((EntityEnderman) entity).isScreaming();
		}
		return isHostileMob(entity);
	}

	/**
	 * If the mob by default wont attack the player, but will if the player attacks
	 * it
	 */
	public static boolean isNeutralMob(Entity entity) {
		return entity instanceof EntityPigZombie || entity instanceof EntityWolf || entity instanceof EntityEnderman;
	}

	/**
	 * If the mob is friendly (not aggressive)
	 */
	public static boolean isFriendlyMob(Entity entity) {
		return (entity.isCreatureType(EnumCreatureType.CREATURE, false) && !EntityUtils.isNeutralMob(entity))
				|| (entity.isCreatureType(EnumCreatureType.AMBIENT, false)) || entity instanceof EntityVillager
				|| entity instanceof EntityIronGolem || (isNeutralMob(entity) && !EntityUtils.isMobAggressive(entity));
	}

	/**
	 * If the mob is hostile
	 */
	public static boolean isHostileMob(Entity entity) {
		return (entity.isCreatureType(EnumCreatureType.MONSTER, false) && !EntityUtils.isNeutralMob(entity));
	}

	/**
	 * Find the entities interpolated position
	 */
	public static Vec3d getInterpolatedPos(Entity entity, float ticks) {
		return new Vec3d(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ)
				.add(getInterpolatedAmount(entity, ticks));
	}

	/*
	 * public static Vec3d c(Entity entity, float ticks) { return
	 * getInterpolatedPos(entity,
	 * ticks).subtract(Wrapper.getMinecraft().getResourceManager();
	 * Wrapper.getMinecraft().getRenderManager().viewerPosX,
	 * mc.getMinecraft().getRenderManager().viewerPosY); }
	 */

	public static boolean isInWater(Entity entity) {
		if (entity == null)
			return false;

		double y = entity.posY + 0.01;

		for (int x = MathHelper.floor(entity.posX); x < MathHelper.ceil(entity.posX); x++)
			for (int z = MathHelper.floor(entity.posZ); z < MathHelper.ceil(entity.posZ); z++) {
				BlockPos pos = new BlockPos(x, (int) y, z);

				if (mc.world.getBlockState(pos).getBlock() instanceof BlockLiquid)
					return true;
			}

		return false;
	}

	public static boolean isDrivenByPlayer(Entity entityIn) {
		return Wrapper.getPlayer() != null && entityIn != null
				&& entityIn.equals(Wrapper.getPlayer().getRidingEntity());
	}

	public static boolean isAboveWater(Entity entity) {
		return isAboveWater(entity, false);
	}

	public static boolean isAboveWater(Entity entity, boolean packet) {
		if (entity == null)
			return false;

		double y = entity.posY - (packet ? 0.03 : (EntityUtils.isPlayer(entity) ? 0.2 : 0.5)); // increasing this seems
																								// to flag more in NCP
																								// but needs to be
																								// increased so the
																								// player lands on solid
																								// water

		for (int x = MathHelper.floor(entity.posX); x < MathHelper.ceil(entity.posX); x++)
			for (int z = MathHelper.floor(entity.posZ); z < MathHelper.ceil(entity.posZ); z++) {
				BlockPos pos = new BlockPos(x, MathHelper.floor(y), z);

				if (Wrapper.getWorld().getBlockState(pos).getBlock() instanceof BlockLiquid)
					return true;
			}

		return false;
	}

	public static double[] calculateLookAt(double px, double py, double pz, EntityPlayer me) {
		double dirx = me.posX - px;
		double diry = me.posY - py;
		double dirz = me.posZ - pz;

		double len = Math.sqrt(dirx * dirx + diry * diry + dirz * dirz);

		dirx /= len;
		diry /= len;
		dirz /= len;

		double pitch = Math.asin(diry);
		double yaw = Math.atan2(dirz, dirx);

		// to degree
		pitch = pitch * 180.0d / Math.PI;
		yaw = yaw * 180.0d / Math.PI;

		yaw += 90f;

		return new double[] { yaw, pitch };
	}

	public static boolean isPlayer(Entity entity) {
		return entity instanceof EntityPlayer;
	}

	public static double getRelativeX(float yaw) {
		return (double) (MathHelper.sin(-yaw * 0.017453292F));
	}

	public static double getRelativeZ(float yaw) {
		return (double) (MathHelper.cos(yaw * 0.017453292F));
	}
	 public static String getEntityNameColor(EntityLivingBase entity) {
	    	String name = entity.getDisplayName().getFormattedText();
	    	if(name.contains("\u00a7")) {
	    	if(name.contains("\u00a71")) { return "\u00a71"; } else
	    	if(name.contains("\u00a72")) { return "\u00a72"; } else
	    	if(name.contains("\u00a73")) { return "\u00a73"; } else
	    	if(name.contains("\u00a74")) { return "\u00a74"; } else
	    	if(name.contains("\u00a75")) { return "\u00a75"; } else
	    	if(name.contains("\u00a76")) { return "\u00a76"; } else
	    	if(name.contains("\u00a77")) { return "\u00a77"; } else
	    	if(name.contains("\u00a78")) { return "\u00a78"; } else
	    	if(name.contains("\u00a79")) { return "\u00a79"; } else
	    	if(name.contains("\u00a70")) { return "\u00a70"; } else
	    	if(name.contains("\u00a7e")) { return "\u00a7e"; } else
	    	if(name.contains("\u00a7d")) { return "\u00a7d"; } else
	    	if(name.contains("\u00a7a")) { return "\u00a7a"; } else
	    	if(name.contains("\u00a7b")) { return "\u00a7b"; } else
	    	if(name.contains("\u00a7c")) { return "\u00a7c"; } else
	    	if(name.contains("\u00a7f")) { return "\u00a7f"; };
	    	}
	    	return "null";
	    }
	 public static int getArmorColor(EntityPlayer player, ItemStack stack) {
	    	if(player == null || stack == null || stack.getItem() == null || !(stack.getItem() instanceof ItemArmor))
	    		return -1;
	    	ItemArmor itemArmor = (ItemArmor) stack.getItem();
			if(itemArmor == null || itemArmor.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER)
				return -1;
	    	return itemArmor.getColor(stack);
	    }
	 public static boolean checkTargetColor(EntityPlayer enemy) {
	    	int colorEnemy0 = getArmorColor(enemy, enemy.inventory.armorItemInSlot(0));
	    	int colorEnemy1 = getArmorColor(enemy, enemy.inventory.armorItemInSlot(1));
	    	int colorEnemy2 = getArmorColor(enemy, enemy.inventory.armorItemInSlot(2));
	    	int colorEnemy3 = getArmorColor(enemy, enemy.inventory.armorItemInSlot(3));
	    			
	    	int colorPlayer0 = getArmorColor(mc.player, mc.player.inventory.armorItemInSlot(0));
	    	int colorPlayer1 = getArmorColor(mc.player,mc.player.inventory.armorItemInSlot(1));
	    	int colorPlayer2 = getArmorColor(mc.player, mc.player.inventory.armorItemInSlot(2));
	    	int colorPlayer3 = getArmorColor(mc.player, mc.player.inventory.armorItemInSlot(3));
	    			
	    	if(colorEnemy0 == colorPlayer0 && colorPlayer0 != -1 && colorEnemy0 != 1
	    			|| colorEnemy1 == colorPlayer1 && colorPlayer1 != -1 && colorEnemy1 != 1
	    					|| colorEnemy2 == colorPlayer2 && colorPlayer2 != -1 && colorEnemy2 != 1
	    							|| colorEnemy3 == colorPlayer3 && colorPlayer3 != -1 && colorEnemy3 != 1) {
	    		return false;
	    	}
	    	return true;
	    }
	 
}
