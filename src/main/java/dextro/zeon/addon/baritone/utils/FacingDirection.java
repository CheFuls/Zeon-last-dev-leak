package dextro.zeon.addon.baritone.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;

/**
 * A list of directions including diagonal directions
 * P = Plus
 * M = Minus
 */
public enum FacingDirection {
	XP("X-Plus"),
	XM("X-Minus"),
	ZP("Z-Plus"),
	ZM("Z-Minus"),
	XP_ZP("X-Plus, Z-Plus"),
	XM_ZP("X-Minus, Z-Plus"),
	XM_ZM("X-Minus, Z-Minus"),
	XP_ZM("X-Plus, Z-Minus");
	
	public String name;
	FacingDirection(String name) {
		this.name = name;
	}
	
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private Direction direction;
	/**
	 * Gets the direction the player is looking at
	 */
	public static FacingDirection getDirection() {
	    Direction facing = mc.player.getHorizontalFacing();
		return facing == Direction.NORTH ? ZM : facing == Direction.WEST ? XM : facing == Direction.SOUTH ? ZP : XP;
	}
	
	/**
	 * Gets the closest diagonal direction player is looking at
	 */
	public static FacingDirection getDiagonalDirection() {
		Direction facing = mc.player.getHorizontalFacing();
		
		if (facing.equals(Direction.NORTH)) {
			double closest = getClosest(135, -135);
			return closest == -135 ? XP_ZM : XM_ZM;
		} else if (facing.equals(Direction.WEST)) {
			double closest = getClosest(135, 45);
			return closest == 135 ? XM_ZM : XM_ZP;
		} else if (facing.equals(Direction.EAST)) {
			double closest = getClosest(-45, -135);
			return closest == -135 ? XP_ZM : XP_ZP;
		} else {
			double closest = getClosest(45, -45);
			return closest == 45 ? XM_ZP : XP_ZP;
		}
	}
	
	//Returns the closer given yaw to the real yaw from a and b
	private static double getClosest(double a, double b) {
		double yaw = mc.player.getYaw();
		yaw = yaw < -180 ? yaw += 360 : yaw > 180 ? yaw -= 360 : yaw;
		
		if (Math.abs(yaw - a) < Math.abs(yaw - b)) {
			return a;
		} else {
			return b;
		}
	}
}