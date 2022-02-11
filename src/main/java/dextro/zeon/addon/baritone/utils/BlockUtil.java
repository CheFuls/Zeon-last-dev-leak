package dextro.zeon.addon.baritone.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BlockUtil {
	
    private static final MinecraftClient mc = MinecraftClient.getInstance();
	/**
	 * Searches around the player to find the given block.
	 * @radius the radius to search around the player
	 */
	public static BlockPos findBlock(Block block, int radius) {
        for (int x = (int) (mc.player.getX() - radius); x < mc.player.getX() + radius; x++) {
            for (int z = (int) (mc.player.getZ() - radius); z < mc.player.getZ() + radius; z++) {
                for (int y = (int) (mc.player.getY() + radius); y > mc.player.getY() - radius; y--) {
                	BlockPos pos = new BlockPos(x, y, z);
                	if (mc.world.getBlockState(pos).getBlock().equals(block)) {
                		return pos;
                	}
                }
            }
        }
		
		return null;
	}
	

	
	/**
	 * Gets all the BlockPositions in the given radius around the pos
	 */
	public static ArrayList<BlockPos> getAll(Vec3d pos, int radius) {
		ArrayList<BlockPos> list = new ArrayList<BlockPos>();
		
        for (int x = (int) (pos.x - radius); x < pos.x + radius; x++) {
            for (int z = (int) (pos.z - radius); z < pos.z + radius; z++) {
                for (int y = (int) (pos.y + radius); y > pos.y - radius; y--) {
            		list.add(new BlockPos(x, y, z));
                }
            }
        }
        
        return list;
	}
	
	

	
	/**
	 * Distance between these 2 blockpositions
	 */
	public static int distance(BlockPos first, BlockPos second) {
		return Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY()) + Math.abs(first.getZ() - second.getZ());
	}
	
	/**
	 * Checks if the block is in render distance or known by the client.
	 */

	

}