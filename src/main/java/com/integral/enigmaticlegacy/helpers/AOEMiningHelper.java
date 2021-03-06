package com.integral.enigmaticlegacy.helpers;

import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AOEMiningHelper {

    public static final Random random = new Random();

    /** Attempt to break blocks around the given pos in a 3x3x1 square relative to the targeted face.*/
    public static void attemptBreakNeighbors(World world,
                                             BlockPos pos,
                                             PlayerEntity player,
                                             Set<Block> effectiveOn,
                                             Set<Material> effectiveMaterials,
                                             boolean checkHarvestLevel) {

        RayTraceResult trace = calcRayTrace(world, player, RayTraceContext.FluidMode.ANY);

        if (trace.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult blockTrace = (BlockRayTraceResult) trace;
            Direction face = blockTrace.getFace();

            int fortuneLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, player.getHeldItemMainhand());
            int silkLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, player.getHeldItemMainhand());

            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;

                    BlockPos target = null;

                    if (face == Direction.UP    || face == Direction.DOWN)  target = pos.add(a, 0, b);
                    if (face == Direction.NORTH || face == Direction.SOUTH) target = pos.add(a, b, 0);
                    if (face == Direction.EAST  || face == Direction.WEST)  target = pos.add(0, a, b);

                    attemptBreak(world, target, player, effectiveOn, effectiveMaterials, fortuneLevel, silkLevel, checkHarvestLevel, null, (objPos, objState) -> {});
                }
            }
        }
    }

    /** To break, the given block must be contained in effectiveOn, or its material contained in effectiveMaterials, and
     * the block cannot have the wither-immune tag. Wither-immune seems to be the only comprehensive list of "blocks you
     * shouldn't be able to break" in the game.
     * @param toolDamageConsumer Consumer that is called for each block destroyed. It is expected to either damage
     * or not damage the tool used for mining the block. */
    public static void attemptBreak(World world,
                                    BlockPos pos,
                                    PlayerEntity player,
                                    Set<Block> effectiveOn,
                                    Set<Material> effectiveMaterials,
                                    int fortuneLevel,
                                    int silkLevel,
                                    boolean checkHarvestLevel, ItemStack tool, 
                                    BiConsumer<BlockPos, BlockState> toolDamageConsumer) {
        BlockState state = world.getBlockState(pos);

        boolean validHarvest = !checkHarvestLevel || player.getHeldItemMainhand().canHarvestBlock(state);
        boolean isEffective = effectiveOn.contains(state.getBlock()) || effectiveMaterials.contains(state.getMaterial());
        boolean witherImmune = BlockTags.WITHER_IMMUNE.contains(state.getBlock());

        if (validHarvest && isEffective && !witherImmune) {
            world.destroyBlock(pos, false);
            Block.spawnDrops(state, world, pos, null, player, player.getHeldItemMainhand());
            
            
            toolDamageConsumer.accept(pos, state);
            
            int exp = state.getExpDrop(world, pos, fortuneLevel, silkLevel);
            if (exp > 0) {
                state.getBlock().dropXpOnBlockBreak(world, pos, exp);
            }
        }
    }

    /** Copy-pasted from "Item.rayTrace" which is protected static, making it unusable in my own static helper methods.*/
    public static RayTraceResult calcRayTrace(World worldIn, PlayerEntity player, RayTraceContext.FluidMode fluidMode) {
        float f = player.rotationPitch;
        float f1 = player.rotationYaw;
        Vec3d vec3d = player.getEyePosition(1.0F);
        float f2 = MathHelper.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = MathHelper.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = -MathHelper.cos(-f * ((float)Math.PI / 180F));
        float f5 = MathHelper.sin(-f * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue();;
        Vec3d vec3d1 = vec3d.add((double)f6 * d0, (double)f5 * d0, (double)f7 * d0);
        return worldIn.rayTraceBlocks(new RayTraceContext(vec3d, vec3d1, RayTraceContext.BlockMode.OUTLINE, fluidMode, player));
    }
    
    /** Copy-pasted from "Item.rayTrace" which is protected static, making it unusable in my own static helper methods.*/
    public static Vector3 calcRayTrace(World worldIn, PlayerEntity player, RayTraceContext.FluidMode fluidMode, double distance) {
        float f = player.rotationPitch;
        float f1 = player.rotationYaw;
        Vec3d vec3d = player.getEyePosition(1.0F);
        float f2 = MathHelper.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = MathHelper.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = -MathHelper.cos(-f * ((float)Math.PI / 180F));
        float f5 = MathHelper.sin(-f * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = distance;
        Vec3d vec3d1 = vec3d.add((double)f6 * d0, (double)f5 * d0, (double)f7 * d0);
        RayTraceResult result = worldIn.rayTraceBlocks(new RayTraceContext(vec3d, vec3d1, RayTraceContext.BlockMode.OUTLINE, fluidMode, player));
        
        if (result.getType() == RayTraceResult.Type.BLOCK)
        	return new Vector3(result.getHitVec());
        else {
        	Vector3 vec = new Vector3(player.getLookVec()).multiply(64F).add(new Vector3(player.getPositionVector()));
        	return vec;
        }
    }
    
    
    public static void harvestPlane(World world, PlayerEntity player, Direction dir, BlockPos pos, Set<Material> effectiveMaterials, int radius, boolean harvestLevelCheck, @Nullable BlockPos excludedBlock, ItemStack tool, BiConsumer<BlockPos, BlockState> toolDamageConsumer) {
    	int fortuneLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, player.getHeldItemMainhand());
        int silkLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, player.getHeldItemMainhand());
        int supRad = (radius-1)/2;
        
        for (int a = -supRad; a <= supRad; a++) {
            for (int b = -supRad; b <= supRad; b++) {
                BlockPos target = null;

                if (dir == Direction.UP    || dir == Direction.DOWN)  target = pos.add(a, 0, b);
                if (dir == Direction.NORTH || dir == Direction.SOUTH) target = pos.add(a, b, 0);
                if (dir == Direction.EAST  || dir == Direction.WEST)  target = pos.add(0, a, b);
                
                if (excludedBlock != null)
                	if (target.equals(excludedBlock))
                		continue;
                
                attemptBreak(world, target, player, Sets.newHashSet(), effectiveMaterials, fortuneLevel, silkLevel, harvestLevelCheck, tool, toolDamageConsumer);
            }
        }
    }
    
    public static void harvestCube(World world, PlayerEntity player, Direction dir, BlockPos centralPos, Set<Material> effectiveMaterials, int planeRadius, int depth, boolean harvestLevelCheck, @Nullable BlockPos excludedBlock, ItemStack tool, BiConsumer<BlockPos, BlockState> toolDamageConsumer) {
    	
    	for (int a = 0; a < depth; a++) {
    		int x = 0;
    		int y = 0;
    		int z = 0;
    		
    		if (dir == Direction.UP) y -= a;
    		if (dir == Direction.DOWN) y += a;
            if (dir == Direction.SOUTH) z -= a;
            if (dir == Direction.NORTH) z += a;
            if (dir == Direction.EAST) x -= a;
            if (dir == Direction.WEST) x += a;
    		
    		harvestPlane(world, player, dir, new BlockPos(centralPos).add(x, y, z), effectiveMaterials, planeRadius, harvestLevelCheck, excludedBlock, tool, toolDamageConsumer);
    	}
    }
    
}
