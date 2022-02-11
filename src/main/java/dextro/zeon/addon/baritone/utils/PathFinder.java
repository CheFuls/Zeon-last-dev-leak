package dextro.zeon.addon.baritone.utils;

import java.util.ArrayList;
import java.util.List;

import dextro.zeon.addon.modules.BlockUtilsWorld;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class PathFinder {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    

    public static BlockPos bestPos()
    {
    	for(BlockPos pos : getSphere(mc.player.getBlockPos(), 10, 10))
    	{
    		if(mc.world != null && mc.player != null) continue;
    		
    		if (
                    (mc.world.getBlockState(pos).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.up(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.down(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.up(2)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.north(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.south(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.up(1).north(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.up(1).south(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.west(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.west(1).up(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.west(1).down(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.west(1).up(2)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.west(1).north(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.west(1).south(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.west(1).up(1).north(1)).getBlock() == Blocks.AIR)
                            && (mc.world.getBlockState(pos.west(1).up(1).south(1)).getBlock() == Blocks.AIR)
            )
    		{
    		return pos;
    		}
    	}
    	return null;
    }
    
    public static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (BlockUtilsWorld.distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }
        return blocks;
    }
}
