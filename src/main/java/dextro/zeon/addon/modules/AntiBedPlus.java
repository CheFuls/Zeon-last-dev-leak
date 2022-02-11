package dextro.zeon.addon.modules;

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

import dextro.zeon.addon.Zeon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AntiBedPlus extends Module {
	public enum placeMode
	{
		Top_Place("Top Place"),
		Feet_Place("Feet Place"),
		Double_Place("Double Place"),
		Both_Place("Both Place");
		private final String title;
		placeMode(String title) {
            this.title = title;
        }
        @Override
        public String toString() {
            return title;
        }
	}
	
	public enum Version
	{
		Mono, Multi
	}
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgMisc = settings.createGroup("Misc");


    private final Setting<Boolean> enablePlace = sgPlace.add(new BoolSetting.Builder()
        .name("enable-place")
        .description("Enable placing string.")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<placeMode> mode = sgPlace.add(new EnumSetting.Builder<placeMode>()
            .name("mode")
            .description("Place mode for AntiBed+.")
            .defaultValue(placeMode.Both_Place)
            .visible(enablePlace::get)
            .build()
    );
    
    private final Setting<Version> version = sgPlace.add(new EnumSetting.Builder<Version>()
            .name("version")
            .description("The version of minecraft.")
            .defaultValue(Version.Mono)
            .visible(enablePlace::get)
            .build()
            );

    private final Setting<Boolean> onlyInHole = sgPlace.add(new BoolSetting.Builder()
        .name("only-in-hole")
        .description("Only functions when you are standing in a hole.")
        .defaultValue(true)
        .visible(enablePlace::get)
        .build()
    );

    private final Setting<Boolean> enableBreak = sgBreak.add(new BoolSetting.Builder()
            .name("enable-break")
            .description("Enable breaking bed.")
            .defaultValue(true)
            .build()
        );
    
    private final Setting<Boolean> axeSwap = sgBreak.add(new BoolSetting.Builder()
            .name("axe-swap")
            .description("Automatically swap to axe for speed break bed.")
            .defaultValue(true)
            .build()
        );
    
    private final Setting<Boolean> onlyInHoleOne = sgPlace.add(new BoolSetting.Builder()
            .name("only-in-hole")
            .description("Only functions when you are standing in a hole.")
            .defaultValue(true)
            .visible(enableBreak::get)
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
    
    private boolean breaking;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    private final ArrayList<Vec3d> top = new ArrayList<Vec3d>() {{
    	add(new Vec3d(0, 2, 0));
    }};
    
    private final ArrayList<Vec3d> doublem = new ArrayList<Vec3d>() {{
    	add(new Vec3d(0, 0, 0));
    	add(new Vec3d(0, 1, 0));
    }};
    
    private final ArrayList<Vec3d> feet = new ArrayList<Vec3d>() {{
    	add(new Vec3d(0, 0, 0));
    }};
    
    private final ArrayList<Vec3d> both = new ArrayList<Vec3d>() {{
    	add(new Vec3d(0, 0, 0));
    	add(new Vec3d(0, 1, 0));
    	add(new Vec3d(0, 2, 0));
    }};

    public AntiBedPlus() {
        super(Zeon.Combat, "anti-bed-plus", "Places string and breaking beds around you to prevent beds being placed on you.");
    }

    @Override
    public void onActivate()
    {
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
    private void onTick(TickEvent.Pre event) {
    	if ((mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem().isFood() || mc.player.getOffHandStack().getItem().isFood()) && pauseOnEat.get())
                || (mc.player.isUsingItem() && (mc.player.getMainHandStack().getItem() instanceof PotionItem || mc.player.getOffHandStack().getItem() instanceof PotionItem) && pauseOnDrink.get())) {
            return;
        }
        if(enableBreak.get())
        {
        if (onlyInHoleOne.get() && !PlayerUtils.isInHole(true)) return;
        BlockPos head = mc.player.getBlockPos().up();

        if (mc.world.getBlockState(head).getBlock() instanceof BedBlock && !breaking) {
            Rotations.rotate(Rotations.getYaw(head), Rotations.getPitch(head), 50, () -> sendMinePackets(head));
            breaking = true;
        } else if (breaking) {
            Rotations.rotate(Rotations.getYaw(head), Rotations.getPitch(head), 50, () -> sendStopPackets(head));
            breaking = false;
        }
        }
        //Хуйня с модом
        if(enablePlace.get())
        {
        if (onlyInHole.get() && !PlayerUtils.isInHole(true)) return;
        	if(version.get() == Version.Mono)
        	{
        if (mode.get() == placeMode.Top_Place || mode.get() == placeMode.Both_Place) place(mc.player.getBlockPos().up(2));
        if (mode.get() == placeMode.Double_Place || mode.get() == placeMode.Both_Place) { place(mc.player.getBlockPos().up(1)); place(mc.player.getBlockPos());}
        if (mode.get() == placeMode.Feet_Place || mode.get() == placeMode.Both_Place) place(mc.player.getBlockPos());
        	}
        	else
        	{
        		if (isVecComplete(getSurrDesign())) {
        	      } else {
        	          BlockPos ppos = mc.player.getBlockPos();
        	          for (Vec3d b : getSurrDesign()) {
        	              BlockPos bb = ppos.add(b.x, b.y, b.z);
        	              if (BlockUtilsWorld.getBlock(bb) == Blocks.AIR) {
        	                      BlockUtils.place(bb, InvUtils.findInHotbar(Items.STRING), true, 100, false);
        	                  
        	                 
        	              }
        	          }
        	      }
        	}
        }
        }

    private void place(BlockPos blockPos) {
        if (mc.world.getBlockState(blockPos).getBlock().asItem() != Items.STRING) {
            BlockUtils.place(blockPos, InvUtils.findInHotbar(Items.STRING), 50, false);
        }
    }

    private void sendMinePackets(BlockPos blockPos) {
    	FindItemResult axe = findAxe();
    	if(axeSwap.get()) InvUtils.swap(axe.getSlot(), false);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        if(axeSwap.get()) InvUtils.swap(axe.getSlot(), false);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
    }

    private void sendStopPackets(BlockPos blockPos) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }
    
    public static boolean isVecComplete(ArrayList<Vec3d> vlist) {
        BlockPos ppos = mc.player.getBlockPos();
        for (Vec3d b: vlist) {
            BlockPos bb = ppos.add(b.x, b.y, b.z);
            if (BlockUtilsWorld.getBlock(bb) == Blocks.AIR) return false;
        }
        return true;
    }
    
    private ArrayList<Vec3d> getSurrDesign() {
        ArrayList<Vec3d> surrDesign = new ArrayList<Vec3d>();
        if (mode.get() == placeMode.Feet_Place) surrDesign.addAll(feet);
        if (mode.get() == placeMode.Top_Place) surrDesign.addAll(top);
        if (mode.get() == placeMode.Double_Place) surrDesign.addAll(doublem);
        if (mode.get() == placeMode.Both_Place) surrDesign.addAll(both);
        return surrDesign;
    }
    
    public static FindItemResult findAxe() {
        return InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof AxeItem);
    }
}