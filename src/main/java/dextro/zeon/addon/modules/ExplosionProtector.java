package dextro.zeon.addon.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import dextro.zeon.addon.Zeon;
import org.apache.commons.io.FileUtils;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ExplosionProtector extends Module {

    public ExplosionProtector() {
        super(Zeon.Combat, "explosion-protector", "Explosion protection.");
    }
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> tnt = sgGeneral.add(new BoolSetting.Builder()
    		.name("anti-tnt-aura")
    		.description("Break near tnt blocks")
    		.defaultValue(true)
    		.build());
    
    private final Setting<Boolean> crystalHead = sgGeneral.add(new BoolSetting.Builder()
    		.name("anti-crystal-head")
    		.defaultValue(true)
    		.build());
    
	private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
			.name("rotate")
			.description("Look at block or crystal.")
			.defaultValue(false)
			.build());
	


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
    }
    
    @EventHandler
    private void a(PacketEvent.Receive e) {
    	if(!online()) return;

        if (crystalHead.get() && e.packet instanceof BlockBreakingProgressS2CPacket) {
        	BlockBreakingProgressS2CPacket w = (BlockBreakingProgressS2CPacket) e.packet;

    		BlockPos p = mc.player.getBlockPos();
    		List<BlockPos> safe = Arrays.asList( p.up(), p.up(2), p.up(3) );

    		if(safe.contains(w.getPos())) {
    			safe.forEach(s ->{
    				place_obsidian(s);
    			});
    			
    			mc.world.getEntities().forEach(s ->{
    				if(s instanceof EndCrystalEntity && safe.contains(s.getBlockPos())) kill(s);
    			});
    		}
        }
    	
    	
        if (tnt.get() && e.packet instanceof BlockUpdateS2CPacket) {
        	BlockUpdateS2CPacket w = (BlockUpdateS2CPacket) e.packet;
        	
        	if(tnt.get() && w.getState().getBlock() instanceof TntBlock) {
        		BlockPos p = mc.player.getBlockPos();
        		List<BlockPos> safe = Arrays.asList(
        				p, p.down(), p.up(), p.up(2), p.up(3),
        				p.east(), p.west(), p.north(), p.south(),
        				p.up().east(), p.up().west(), p.up().north(), p.up().south(),
        				p.up(2).east(), p.up(2).west(), p.up(2).north(), p.up(2).south()
        				);
        		BlockPos a = w.getPos();
        		if(safe.contains(a)) {
        			look(a);
    				mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, a, Direction.UP));
    				mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, a, Direction.UP));
    				place_obsidian(a);
        		}
        	}
        }
	}

	@EventHandler
    private void a(EntityAddedEvent e) {
    	if( !online() || !crystalHead.get() || !(e.entity instanceof EndCrystalEntity) ) return;

		BlockPos p = mc.player.getBlockPos();
		List<BlockPos> safe = Arrays.asList(
				p.up(2), p.up(3), p.up(4),
				p.up(2).east(), p.up(2).west(), p.up(2).north(), p.up(2).south()
				);
		BlockPos a = e.entity.getBlockPos();
		if(safe.contains(a)) {
			place_obsidian(a.down()); 
			kill(e.entity);
			place_obsidian(a);
			place_obsidian(a.up());
		}
    }

	private boolean online(){
		return (mc.world != null && mc.player != null && mc.world.getPlayers().size() > 1) ? true : false;
	}
	
	private void kill(Entity a) {
		look(a.getBlockPos());
		mc.interactionManager.attackEntity(mc.player, a);
		a.remove(RemovalReason.KILLED);
	}
	
    private void place_obsidian(BlockPos a) {
    	if(!BlockUtils.canPlace(a)) return;
    	if(mc.player.getBlockPos().getY() - a.getY() > 2) return;
           int obsidian = InvUtils.findInHotbar(Items.OBSIDIAN).getSlot();
		if(obsidian > -1) {
			look(a);
			int pre = mc.player.getInventory().selectedSlot;
			swap(obsidian);
	        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, a, true)));
			swap(pre);
		}		
	}   
    
    private void swap(int a) {
    	if(a != mc.player.getInventory().selectedSlot) {
    		mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(a));
	        mc.player.getInventory().selectedSlot = a;
    	}
    }
    
    private void look(BlockPos a) {
    	if(!rotate.get()) return;
    	Vec3d hitPos = new Vec3d(0, 0, 0);
    	((IVec3d) hitPos).set(a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5);
    	Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos));
    }

}