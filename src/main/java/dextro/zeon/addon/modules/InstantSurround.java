package dextro.zeon.addon.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import dextro.zeon.addon.Zeon;
import dextro.zeon.addon.modules.CustomCrystalAuraV4.SupportMode;
import dextro.zeon.addon.modules.ExtraSurround.Version;

import org.apache.commons.io.FileUtils;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class InstantSurround extends Module {
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgSupport = settings.createGroup("Support");
    private final SettingGroup sgProtect = settings.createGroup("Protect");
    private final SettingGroup sgAntiGhost = settings.createGroup("Anti Ghost");  
    private final SettingGroup sgToggle = settings.createGroup("Toggle");
    private final SettingGroup sgMisc = settings.createGroup("Misc");  
    private final SettingGroup sgRender = settings.createGroup("Render");  
    
    private final Setting<Double> speed = sgPlace.add(new DoubleSetting.Builder()
    		.name("place-speed")
    		.description("The maximum amount of blocks placed in a tick.")
    		.defaultValue(1)
    		.min(0)
    		.build()
    		);
    
    private final Setting<Boolean> doubleHeight = sgPlace.add(new BoolSetting.Builder()
            .name("double-height")
            .description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.")
            .defaultValue(false)
            .build()
        );
    
    private final Setting<Boolean> hit = sgPlace.add(new BoolSetting.Builder()
            .name("block-hit-result")
            .description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.")
            .defaultValue(false)
            .build()
        );
    
    private final Setting<Boolean> check = sgPlace.add(new BoolSetting.Builder()
            .name("block-pos-check")
            .description("Checks material of surround blocks to replaceability.")
            .defaultValue(false)
            .build()
        );
    
    private final Setting<Center> center = sgPlace.add(new EnumSetting.Builder<Center>()
            .name("center")
            .description("Teleports you to the center of the block.")
            .defaultValue(Center.OnlyOnActivate)
            .build()
        );
    
    private final Setting<Boolean> support = sgSupport.add(new BoolSetting.Builder()
            .name("support")
            .description("Places block under surround block.")
            .defaultValue(false)
            .build()
        );
    
    private final Setting<Integer> supportDelay = sgSupport.add(new IntSetting.Builder()
            .name("support-delay")
            .description("Delay after placing support block in ticks.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 5)
            .visible(support::get)
            .build()
        );
    
    private final Setting<Boolean> protect = sgProtect.add(new BoolSetting.Builder()
            .name("protect")
            .description("Attempts to break crystals around surround positions to prevent surround break.")
            .defaultValue(true)
            .build()
        );
    
    private final Setting<Double> distance = sgProtect.add(new DoubleSetting.Builder()
    		.name("distance-between")
    		.description("Maximum distance between crystal and surround to attack.")
    		.defaultValue(1)
    		.min(0)
    		.sliderMax(3)
    		.visible(protect::get)
    		.build()
    		);
    
    private final Setting<Boolean> antighost = sgAntiGhost.add(new BoolSetting.Builder()
            .name("anti-ghosts")
            .description("Clicks on blocks to prevents spawn ghost blocks.")
            .defaultValue(false)
            .build()
        );
    
    private final Setting<Integer> clickDelay = sgAntiGhost.add(new IntSetting.Builder()
    		.name("click-delay")
    		.description("The maximum amount of clicks in a tick.")
    		.defaultValue(5)
    		.min(0)
    		.visible(antighost::get)
    		.build()
    		);
    
    private final Setting<Boolean> toggleComplete = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-complete")
            .description("Disables module when surround is complete.")
            .defaultValue(false)
            .build()
        );
    
    private final Setting<Boolean> toggleY = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-y-change")
            .description("Disables module when players jumps/falls.")
            .defaultValue(true)
            .build()
        );
    
    private final Setting<Boolean> toggleTeleport = sgToggle.add(new BoolSetting.Builder()
            .name("toggle-on-teleport")
            .description("Disables module when you used choruses or perls.")
            .defaultValue(true)
            .build()
        );
    
    private final Setting<Boolean> antiBreak = sgMisc.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Places blocks near surround to prevents surround break.")
            .defaultValue(true)
            .build()
        );
    
    private final Setting<Boolean> velocity = sgMisc.add(new BoolSetting.Builder()
            .name("air-stop")
            .description("Stops you when you falling and places block under player.")
            .defaultValue(false)
            .build()
        );
    
    
    private final Setting<Boolean> rotate = sgMisc.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically faces towards the obsidian being placed.")
            .defaultValue(true)
            .build()
        );
	
	private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
			.name("render")
			.description("Render surround blocks.")
			.defaultValue(true)
			.build());

	   private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
	            .name("shape-mode")
	            .description("How the shapes are rendered.")
	            .defaultValue(ShapeMode.Lines)
	            .build()
	    );

	    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
	            .name("side-color")
	            .description("The side color.")
	            .defaultValue(new SettingColor(255, 255, 255, 75))
	            .build()
	    );

	    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
	            .name("line-color")
	            .description("The line color.")
	            .defaultValue(new SettingColor(255, 255, 255, 255))
	            .build()
	    );
	    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<BlockPos> supportPositions = new ArrayList<>();
    private int delay;
    private int sendDelay;
    
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
        if (!supportPositions.isEmpty()) supportPositions.clear();
        if(center.get() == Center.OnlyOnActivate) PlayerUtils.centerPlayer();
	}
    
    @EventHandler
    private void onTickPre(TickEvent.Pre e) {
    	  if (toggleY.get() && mc.options.keyJump.isPressed()) {
              toggle();
              return;
          }
    	if(center.get() == Center.Always) PlayerUtils.centerPlayer();
    	findSupportPos();
    	if(support.get() && supportPositions.size() != 0) {
    		if (delay >= supportDelay.get()) {
                delay = 0;
                doSupport();
            } else delay++;
    	}
    	if(checkSupport() || !support.get()) doInstantPlace();
    	 if(protect.get() == true) {
    	        for(Entity entity : mc.world.getEntities()) {
    	        	if (entity instanceof EndCrystalEntity) {
    	        	    FindItemResult slot = InvUtils.findInHotbar(Items.OBSIDIAN);
    	                BlockPos crystalPos = entity.getBlockPos();
    	                if (BlockUtilsWorld.distanceBetween(mc.player.getBlockPos(), crystalPos) <= distance.get()) {
    	                	mc.interactionManager.attackEntity(mc.player, entity);
    	                    entity.remove(RemovalReason.KILLED);;
    	                    BlockUtils.place(crystalPos, slot, rotate.get(), 50);
    	                 return;
    	                }
    	            }
    	        };
    	    }
    }
    
    @EventHandler
    private void onTickPost(TickEvent.Post e) {
    	if (mc.getNetworkHandler() == null) return;
        BlockPos pos = mc.player.getBlockPos();
                    if (!mc.world.getBlockState(pos).isAir() && !mc.world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
                        PlayerActionC2SPacket south = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(pos.south()), Direction.UP);
                        PlayerActionC2SPacket north = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(pos.north()), Direction.UP);
                        PlayerActionC2SPacket east = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(pos.east()), Direction.UP);
                        PlayerActionC2SPacket west = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(pos.west()), Direction.UP);
                        
                        PlayerActionC2SPacket southup = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(pos.south().up()), Direction.UP);
                        PlayerActionC2SPacket northup = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(pos.north().up()), Direction.UP);
                        PlayerActionC2SPacket eastup = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(pos.east().up()), Direction.UP);
                        PlayerActionC2SPacket westup = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, new BlockPos(pos.west().up()), Direction.UP);
                        if (sendDelay >= supportDelay.get()) {
                        	sendDelay = 0;
                        if(doubleHeight.get()) {
                       mc.getNetworkHandler().sendPacket(south);
                       mc.getNetworkHandler().sendPacket(north);
                       mc.getNetworkHandler().sendPacket(east);
                       mc.getNetworkHandler().sendPacket(west);
                       
                       mc.getNetworkHandler().sendPacket(southup);
                       mc.getNetworkHandler().sendPacket(northup);
                       mc.getNetworkHandler().sendPacket(eastup);
                       mc.getNetworkHandler().sendPacket(westup);
                       } else {
                    	   mc.getNetworkHandler().sendPacket(south);
                           mc.getNetworkHandler().sendPacket(north);
                           mc.getNetworkHandler().sendPacket(east);
                           mc.getNetworkHandler().sendPacket(west);
                       }
                   } else sendDelay++;
        }             
    }

    @EventHandler
    private void onRender(Render3DEvent e) {
        if (!render.get()) return;
        if(mc.player.getBlockPos().south() != null) e.renderer.box( mc.player.getBlockPos().south(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if(mc.player.getBlockPos().west() != null) e.renderer.box( mc.player.getBlockPos().west(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if(mc.player.getBlockPos().north() != null) e.renderer.box( mc.player.getBlockPos().north(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if(mc.player.getBlockPos().east() != null) e.renderer.box( mc.player.getBlockPos().east(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if(doubleHeight.get()) {
        	if(mc.player.getBlockPos().south() != null) e.renderer.box( mc.player.getBlockPos().south().up(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if(mc.player.getBlockPos().west() != null) e.renderer.box( mc.player.getBlockPos().west().up(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if(mc.player.getBlockPos().north() != null) e.renderer.box( mc.player.getBlockPos().north().up(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if(mc.player.getBlockPos().east() != null) e.renderer.box( mc.player.getBlockPos().east().up(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
    
    @EventHandler
    private void onStartBreakingBlock(PacketEvent.Receive event) {
    	if(!(event.packet instanceof BlockBreakingProgressS2CPacket)) return;
    	BlockBreakingProgressS2CPacket packet = (BlockBreakingProgressS2CPacket) event.packet;
        BlockPos pos = packet.getPos();
    	FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if(!obsidian.found()) {
        	toggle();
        	return;
        }
        if (antiBreak.get() && BlockUtilsWorld.isSurrounded(mc.player)) {
            if(pos.equals(mc.player.getBlockPos().south())) {
                
                BlockUtils.place(pos.south(), obsidian, rotate.get(), 50);
                BlockUtils.place(pos.east(), obsidian, rotate.get(), 50);
                BlockUtils.place(pos.west(), obsidian, rotate.get(), 50);
                
            } else if(pos.equals(mc.player.getBlockPos().east())) {
            	
            	BlockUtils.place(pos.east(), obsidian, rotate.get(), 50);
            	BlockUtils.place(pos.north(), obsidian, rotate.get(), 50);
            	BlockUtils.place(pos.south(), obsidian, rotate.get(), 50);
            	
            } else if(pos.equals(mc.player.getBlockPos().west())) {
            	
            	BlockUtils.place(pos.west(), obsidian, rotate.get(), 50);
            	BlockUtils.place(pos.south(), obsidian, rotate.get(), 50);
            	BlockUtils.place(pos.north(), obsidian, rotate.get(), 50);
            	
            } else if(pos.equals(mc.player.getBlockPos().north())) {
            	
            	BlockUtils.place(pos.north(), obsidian, rotate.get(), 50);
            	BlockUtils.place(pos.west(), obsidian, rotate.get(), 50);
            	BlockUtils.place(pos.east(), obsidian, rotate.get(), 50);
            	
            }
        }
    }
    
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket && toggleTeleport.get()) {toggle();return;}
    }
    
    private void doInstantPlace() {
    	if(doubleHeight.get()) {
      		int speedDelay = 0;
      		for (BlockPos bp : new BlockPos[]{
                    new BlockPos(mc.player.getBlockPos().down().getX(), mc.player.getBlockPos().down().getY(), mc.player.getBlockPos().down().getZ()),
                    new BlockPos(mc.player.getBlockPos().north().getX(), mc.player.getBlockPos().north().getY(), mc.player.getBlockPos().north().getZ()),
                    new BlockPos(mc.player.getBlockPos().east().getX(), mc.player.getBlockPos().east().getY(), mc.player.getBlockPos().east().getZ()),
                    new BlockPos(mc.player.getBlockPos().south().getX(), mc.player.getBlockPos().south().getY(), mc.player.getBlockPos().south().getZ()),
                    new BlockPos(mc.player.getBlockPos().west().getX(), mc.player.getBlockPos().west().getY(), mc.player.getBlockPos().west().getZ()),
                    new BlockPos(mc.player.getBlockPos().north().getX(), mc.player.getBlockPos().north().getY() + 1, mc.player.getBlockPos().north().getZ()),
                    new BlockPos(mc.player.getBlockPos().east().getX(), mc.player.getBlockPos().east().getY() + 1, mc.player.getBlockPos().east().getZ()),
                    new BlockPos(mc.player.getBlockPos().south().getX(), mc.player.getBlockPos().south().getY() + 1, mc.player.getBlockPos().south().getZ()),
                    new BlockPos(mc.player.getBlockPos().west().getX(), mc.player.getBlockPos().west().getY() + 1, mc.player.getBlockPos().west().getZ())
                    }) {

            	 if (speedDelay >= speed.get()) {
                     return;
                 }
                //checks material
                if (mc.world.getBlockState(bp).getMaterial().isReplaceable() || !check.get()) {

                    FindItemResult slot = InvUtils.findInHotbar(Items.OBSIDIAN);
                    if(!slot.found()) {
                    	toggle();
                    	return;
                    }
              
                    if (!mc.player.isOnGround() && velocity.get()) {
                        mc.player.setVelocity(0, 0, 0);
                    }
                    if(hit.get()) {
                    InvUtils.swap(InvUtils.findInHotbar(Items.OBSIDIAN).getSlot(), false);
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(bp.getX(), bp.getY(), bp.getZ()), Direction.DOWN, bp, true));
                    }
                    else {
                    BlockUtils.place(bp, slot, rotate.get(), 50);
                    }
                    if(toggleComplete.get()) {
                    	toggle();
                    	return;
                    }
                    speedDelay++;
                }
              }
            } else {
            	int speedDelay = 0;
                for (BlockPos bp : new BlockPos[]{
                        new BlockPos(mc.player.getBlockPos().down().getX(), mc.player.getBlockPos().down().getY(), mc.player.getBlockPos().down().getZ()),
                        new BlockPos(mc.player.getBlockPos().north().getX(), mc.player.getBlockPos().north().getY(), mc.player.getBlockPos().north().getZ()),
                        new BlockPos(mc.player.getBlockPos().east().getX(), mc.player.getBlockPos().east().getY(), mc.player.getBlockPos().east().getZ()),
                        new BlockPos(mc.player.getBlockPos().south().getX(), mc.player.getBlockPos().south().getY(), mc.player.getBlockPos().south().getZ()),
                        new BlockPos(mc.player.getBlockPos().west().getX(), mc.player.getBlockPos().west().getY(), mc.player.getBlockPos().west().getZ())
                        }) {

                	 if (speedDelay >= speed.get()) {
                         return;
                     }
                    //checks material
                    if (mc.world.getBlockState(bp).getMaterial().isReplaceable() || !check.get()) {

                        FindItemResult slot = InvUtils.findInHotbar(Items.OBSIDIAN);
                        if(!slot.found()) {
                        	toggle();
                        	return;
                        }
                  
                        if (!mc.player.isOnGround() && velocity.get()) {
                            mc.player.setVelocity(0, 0, 0);
                        }

                        if(hit.get()) {
                            InvUtils.swap(InvUtils.findInHotbar(Items.OBSIDIAN).getSlot(), false);
                            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(bp.getX(), bp.getY(), bp.getZ()), Direction.DOWN, bp, true));
                            }
                            else {
                            BlockUtils.place(bp, slot, rotate.get(), 50);
                            }
                        if(toggleComplete.get()) {
                        	toggle();
                        	return;
                        }
                        speedDelay++;
                    }
                  }
            }
    }
    
    private void doSupport() {
    	int speedDelay = 0;
    	 for (BlockPos bp : supportPositions) {

        	 if ((speedDelay >= speed.get()) || supportPositions.size() <= 0) {
                 return;
             }

                FindItemResult slot = InvUtils.findInHotbar(Items.OBSIDIAN);
                if(!slot.found()) {
                	toggle();
                	return;
                }

                BlockUtils.place(bp, slot, rotate.get(), 50);
                speedDelay++;
            }
          
    }
    
    private void findSupportPos() {
        supportPositions.clear();
        BlockPos pos = mc.player.getBlockPos();
                addSupport(pos.add(1, -1, 0));
                addSupport(pos.add(-1, -1, 0));
                addSupport(pos.add(0, -1, 1));
                addSupport(pos.add(0, -1, -1));
    }

    private boolean checkSupport() {
    	BlockPos playerPos = mc.player.getBlockPos();
    	if(mc.world.getBlockState(playerPos.east().down()).getMaterial().isReplaceable()) return false;
    	if(mc.world.getBlockState(playerPos.west().down()).getMaterial().isReplaceable()) return false;
    	if(mc.world.getBlockState(playerPos.south().down()).getMaterial().isReplaceable()) return false;
    	if(mc.world.getBlockState(playerPos.north().down()).getMaterial().isReplaceable()) return false;
    	return true;
    }
    
    private void addSupport(BlockPos blockPos) {
        if (!supportPositions.contains(blockPos) && mc.world.getBlockState(blockPos).getMaterial().isReplaceable() && mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent())) supportPositions.add(blockPos);
    }
    
	public InstantSurround() {
		super(Zeon.Combat, "moment-surround", "One tick surround.");
	}
	
	public enum Center {
		Never("Never"),
		OnlyOnActivate("Only On Activate"),
		Always("Always");
		
		private final String title;
		
		Center(String title) {
            this.title = title;
        }
		
        @Override
        public String toString() {
            return title;
        }
	}
}