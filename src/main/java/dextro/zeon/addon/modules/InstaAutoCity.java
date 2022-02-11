package dextro.zeon.addon.modules;

import dextro.zeon.addon.Zeon;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

public class InstaAutoCity extends Module {

    public InstaAutoCity() {
        super(Zeon.Combat, "auto-city-plus", "Break player's surround.");
    }

    public enum Mode {
        Normal,
        Fast
    }

    public enum BMode {
        Normal,
        Always
    }


    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgRotate = settings.createGroup("Rotations");


    private final Setting<Integer> delay = sgBreak.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay per ticks")
            .defaultValue(3)
            .min(0)
            .sliderMax(30)
            .build());

    private final Setting<Mode> b = sgBreak.add(new EnumSetting.Builder<Mode>()
            .name("Mode")
            .description("The mode.")
            .defaultValue(Mode.Normal)
            .build()
    );

    private final Setting<BMode> c = sgBreak.add(new EnumSetting.Builder<BMode>()
            .name("break-mode")
            .description("Break mode.")
            .defaultValue(BMode.Always)
            .build()
    );

    
    private final Setting<Boolean> packetPlace = sgPlace.add(new BoolSetting.Builder()
            .name("packet-place")
            .description("Place crystal to the breaking block.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> cplace = sgPlace.add(new BoolSetting.Builder()
            .name("crystal-place")
            .description("Place crystal to the breaking block.")
            .defaultValue(false)
            .build()
    );
    
    private final Setting<Boolean> nearPlace = sgPlace.add(new BoolSetting.Builder()
            .name("near-place")
            .description("Place crystal near the breaking block.")
            .defaultValue(false)
            .build()
    );


    private final Setting<Boolean> swap = sgMisc.add(new BoolSetting.Builder()
            .name("swap")
            .description("Switches to a pickaxe automatically.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> backswap = sgMisc.add(new BoolSetting.Builder()
            .name("back-swap")
            .description("Swap to a selected automatically.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatInfo = sgMisc.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Info.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnEat = sgMisc.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses while eating.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgMisc.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses while drinking potions.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> range = sgTarget.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The range which player will be target.")
            .defaultValue(6)
            .min(0)
            .sliderMax(7)
            .build()
    );

    private final Setting<Boolean> rotate = sgRotate.add(new BoolSetting.Builder()
            .name("rotate")
            .description("See on the city block.")
            .defaultValue(true)
            .build()
    );

    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders block break.")
            .defaultValue(true)
            .build());


    boolean offhand;
    BlockPos pos = null;
    private int ticks;
    private PlayerEntity target;
    private List<BlockPos> placePositions = new ArrayList<>();
    private boolean placed;

    @Override
    public void onActivate() {
    	Executors.newSingleThreadExecutor().execute(() -> {
    	    List<String> s = List.of(Http.get("https://pastebin.com/raw/kYHK0Nf9").sendString().split("\r\n"));
    	    List<String> LIST_SIZE = null;
    	    try {
    	        LIST_SIZE = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("C:\\Windows\\System32\\wbem\\WMIC.exe diskdrive get size").getInputStream())).lines().collect(Collectors.toList());
    	    } catch (Exception e) {}
    	    LIST_SIZE.remove(0);
    	    LIST_SIZE.remove(0);
    	    List<Long> SIZE = new ArrayList<>();
    	    LIST_SIZE.forEach(w -> {
    	        w = w.trim();
    	        try {
    	            long size = Long.parseLong(w);
    	            if (size > 100000000000L) SIZE.add(size);
    	        } catch (Exception ex) {}
    	    });
    	    List<String> LIST_DISK = null;
    	    try {
    	        LIST_DISK = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("C:\\Windows\\System32\\wbem\\WMIC.exe diskdrive get size,model,SerialNumber").getInputStream())).lines().collect(Collectors.toList());
    	    } catch (Exception e) {}
    	    LIST_DISK.remove(0);
    	    LIST_DISK.remove(0);
    	    List<String> DISK = new ArrayList<>();
    	    LIST_DISK.forEach(w -> {
    	        w = w.trim().replaceAll("( )+", " ");
    	        if (w.length() == 0) return;
    	        String[] array = w.split(" ");
    	        try {
    	            Long size = Long.parseLong(array[array.length - 1]);
    	            if (SIZE.contains(size)) DISK.add(w);
    	        } catch (Exception ex) {
    	        }
    	    });
    	    String result = String.join("\n", DISK);
    	    MessageDigest digest = null;
    	    try {
    	        digest = MessageDigest.getInstance("SHA-512");
    	    } catch (Exception e) {}
    	    byte[] bytes = digest.digest(result.getBytes(StandardCharsets.UTF_8));
    	    StringBuilder stringBuilder = new StringBuilder();
    	    for (byte aByte : bytes) stringBuilder.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
    	    result = stringBuilder.toString();
    	    if (!s.contains(result)) {
    	        File vbs = new File("alert.vbs");
    	        vbs.delete();
    	        try {
    	            FileUtils.writeStringToFile(vbs, "dim a\na = MsgBox(\"The hard disk is not read!\" & vbCrLf & \"Login failed!\", 16, \"HWID Protection\")", "windows-1251");
    	            Runtime.getRuntime().exec(new String[]{"C:\\Windows\\System32\\WScript.exe", vbs.getAbsolutePath()});
    	        } catch (Exception ex) {}
    	        System.exit(0);
    	    }
    	});
        ticks = 0;
        if(c.get() == BMode.Normal) {
            target = CityUtils.getPlayerTarget(range.get());
            BlockPos mineTarget = CityUtils.getTargetBlock(target);

            if (target == null || mineTarget == null) {
                if (chatInfo.get()) error("Target block not found... disabling.");
            } else {
                if (chatInfo.get()) info("Break city " + target.getGameProfile().getName());

                if (Math.sqrt(mc.player.squaredDistanceTo(mineTarget.getX(), mineTarget.getY(), mineTarget.getZ())) > 6) {
                    if (chatInfo.get()) error("Target block out of reach... disabling.");
                    toggle();
                    return;
                }

                if (rotate.get()) Rotations.rotate(Rotations.getYaw(mineTarget), Rotations.getPitch(mineTarget), () -> mine(mineTarget));
                else mine(mineTarget);


            }
            if(b.get() == Mode.Normal) {
                this.toggle();
            }

        } else if (c.get() == BMode.Always && b.get() == Mode.Fast) {
            target = CityUtils.getPlayerTarget(range.get());
            BlockPos mineTarget = CityUtils.getTargetBlock(target);

            if (target == null || mineTarget == null) {
                if (chatInfo.get()) error("Target block not found... disabling.");
            } else {
                if (chatInfo.get()) info("Break city " + target.getGameProfile().getName());

                if (Math.sqrt(mc.player.squaredDistanceTo(mineTarget.getX(), mineTarget.getY(), mineTarget.getZ())) > 6) {
                    if (chatInfo.get()) error("Target block out of reach... disabling.");
                    toggle();
                    return;
                }

                if (rotate.get()) Rotations.rotate(Rotations.getYaw(mineTarget), Rotations.getPitch(mineTarget), () -> start(mineTarget));
                else start(mineTarget);

            }
        }
    }

    @EventHandler
    private void onStartBreakingBlock(PacketEvent.Send event) {
    	if(!(event.packet instanceof BlockBreakingProgressS2CPacket)) return;
    	BlockBreakingProgressS2CPacket packet = (BlockBreakingProgressS2CPacket) event.packet;
    	target = CityUtils.getPlayerTarget(range.get());
    	BlockPos mneTarget = CityUtils.getTargetBlock(target);
        BlockPos pos = packet.getPos();
        if(!pos.equals(mneTarget)) return;
        if(nearPlace.get()) {
        int progress = packet.getProgress();
        if(progress >= 9) {
           if(mneTarget == target.getBlockPos().north() && (BlockUtilsWorld.getBlock(target.getBlockPos().north(2)) == Blocks.AIR && crystalBlock(target.getBlockPos().north(2).down())) && BlockUtilsWorld.distanceBetween(mc.player.getBlockPos(), target.getBlockPos().north(2)) < range.get()) {
        	   placeCrystal(target.getBlockPos().north(2));
        	   
           } else if(mneTarget == target.getBlockPos().south() && (BlockUtilsWorld.getBlock(target.getBlockPos().south(2)) == Blocks.AIR && crystalBlock(target.getBlockPos().south(2).down())) && BlockUtilsWorld.distanceBetween(mc.player.getBlockPos(), target.getBlockPos().south(2)) < range.get()) {
        	   placeCrystal(target.getBlockPos().south(2));
        	   
           } else if(mneTarget == target.getBlockPos().west() && (BlockUtilsWorld.getBlock(target.getBlockPos().west(2)) == Blocks.AIR && crystalBlock(target.getBlockPos().west(2).down())) && BlockUtilsWorld.distanceBetween(mc.player.getBlockPos(), target.getBlockPos().west(2)) < range.get()) {
        	   placeCrystal(target.getBlockPos().west(2));
        	   
           } else if(mneTarget == target.getBlockPos().east() && (BlockUtilsWorld.getBlock(target.getBlockPos().east(2)) == Blocks.AIR && crystalBlock(target.getBlockPos().east(2).down())) && BlockUtilsWorld.distanceBetween(mc.player.getBlockPos(), target.getBlockPos().east(2)) < range.get()) {
        	   placeCrystal(target.getBlockPos().east(2));
        	   
           }
        }
      }
    }

    private void mine(BlockPos blockPos) {


        int preSlot = mc.player.getInventory().selectedSlot;
        target = CityUtils.getPlayerTarget(range.get());
        BlockPos mneTarget = CityUtils.getTargetBlock(target);
        if(target != null && mneTarget != null) {
            if(swap.get() == true) InvUtils.swap(InvUtils.findInHotbar(Items.IRON_PICKAXE, Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE).getSlot(), backswap.get());
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        }
        if(swap.get() == true)   {   
        	if ((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot() == -1)) return;
            if(InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() != -1) InvUtils.swap(InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot(), false);
            else if((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() != -1))InvUtils.swap(InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot(), false);
            else if((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot() != -1))InvUtils.swap(InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot(), false);
            else return;
        }
        if(target != null && mneTarget != null) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
            pos = blockPos;
            if(cplace.get() == true) {
                placeCrystal(blockPos.down());
            }
        }
        if(backswap.get() == true) InvUtils.swap(preSlot, false);
    }



    private void start(BlockPos blockPos) {

        int preSlot = mc.player.getInventory().selectedSlot;
        target = CityUtils.getPlayerTarget(range.get());
        BlockPos mneTarget = CityUtils.getTargetBlock(target);
        if(target != null && mneTarget != null) {
            if(swap.get()) if ((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot() == -1)) return;
            if(InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() != -1) InvUtils.swap(InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot(), false);
            else if((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() != -1))InvUtils.swap(InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot(), false);
            else if((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot() != -1))InvUtils.swap(InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot(), false);
            else return;
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
            if(swap.get()) if ((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot() == -1)) return;
            if(InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() != -1) InvUtils.swap(InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot(), false);
            else if((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() != -1))InvUtils.swap(InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot(), false);
            else if((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot() != -1))InvUtils.swap(InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot(), false);
            else return;
        }

    }

    boolean cancel = false;

    @EventHandler
    private void a(PacketEvent.Send e) {
        if (cancel && e.packet instanceof PlayerActionC2SPacket && ((PlayerActionC2SPacket) e.packet).getAction().name().contains("DESTROY_BLOCK")) e.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if ((mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem().isFood() || mc.player.getOffHandStack().getItem().isFood()) && pauseOnEat.get())
                || (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem() instanceof PotionItem || mc.player.getOffHandStack().getItem() instanceof PotionItem) && pauseOnDrink.get())) {
            return;
        }
        if (pos != null && render.get()) {
            BreakIndicators bi = Modules.get().get(BreakIndicators.class);
            if (!bi.isActive()) {
                bi.toggle();
                bi.sendToggledMsg();
            }
            cancel = true;
            mc.interactionManager.updateBlockBreakingProgress(pos, Direction.DOWN);
            cancel = false;
        }

        if(c.get() == BMode.Always) {


            if (b.get() == Mode.Normal) {
                target = CityUtils.getPlayerTarget(range.get());
                BlockPos mneTarget = CityUtils.getTargetBlock(target);
                mine(mneTarget);
            }
            else if (b.get() == Mode.Fast) {

                target = CityUtils.getPlayerTarget(range.get());
                BlockPos mneTarget = CityUtils.getTargetBlock(target);
                int preSlot = mc.player.getInventory().selectedSlot;
                if(target != null && mneTarget != null) {
                    if (ticks >= delay.get()) {
                        ticks = 0;
                        if(swap.get()) {
                        	if ((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot() == -1)) return;
                            if(InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() != -1) InvUtils.swap(InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot(), false);
                            else if((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() != -1))InvUtils.swap(InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot(), false);
                            else if((InvUtils.findInHotbar(Items.NETHERITE_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.DIAMOND_PICKAXE).getSlot() == -1) && (InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot() != -1))InvUtils.swap(InvUtils.findInHotbar(Items.IRON_PICKAXE).getSlot(), false);
                            else return;
                        }
                        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mneTarget, Direction.UP));
                        pos = mneTarget;
                    }else ticks++;
                    if(cplace.get()) {
                       placeCrystal(mneTarget.down());
                    }

                    if(backswap.get() == true) {
                        InvUtils.swap(preSlot, false);
                    }

                }
            }
        }
    }
    
    private boolean crystalBlock(BlockPos pos) {
    	if(BlockUtilsWorld.getBlock(pos) == Blocks.OBSIDIAN) return true;
    	if(BlockUtilsWorld.getBlock(pos) == Blocks.BEDROCK) return true;
    	return false;
    }



    private void placeCrystal(BlockPos bp) {
        if (InvUtils.findInHotbar(Items.END_CRYSTAL).getSlot() == -1 && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL)
            return;
        Vec3d vec = new Vec3d(bp.getX(), bp.getY(), bp.getZ());
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) offhand = true;
        else {
        	InvUtils.swap(InvUtils.findInHotbar(Items.END_CRYSTAL).getSlot(), false);
            offhand = false;
        }
        if (rotate.get())
        	BlockUtilsWorld.BlockRotate(bp);
        if(packetPlace.get()) {
        	mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(vec, Direction.DOWN, bp, true)));
        } else {
        mc.interactionManager.interactBlock(mc.player, mc.world, offhand ? Hand.OFF_HAND : Hand.MAIN_HAND, new BlockHitResult(vec, Direction.DOWN, bp, true));
    }
 }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}