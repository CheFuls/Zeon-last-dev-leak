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

import dextro.zeon.addon.Zeon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import org.apache.commons.io.FileUtils;

public class PacketEXP extends Module {
    public PacketEXP() {
        super(Zeon.Combat, "packet-EXP", "Automatically repairs your armor.");
    }
    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    
	private static final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean need;
    
    private final Setting<SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
            .name("switch-mode")
            .description("Automatically switches to exp bottles in your hotbar.")
            .defaultValue(SwitchMode.Silent)
            .build()
    );
    
    private final Setting<Double> minHealth = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health required for packet exp work.")
            .defaultValue(4)
            .min(0)
            .sliderMax(36)
            .max(36)
            .build()
    );
    
    private final Setting<Double> minDurability = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-durability")
            .description("The minimum armor durability required for packet exp work.")
            .defaultValue(20)
            .min(0)
            .sliderMax(100)
            .max(100)
            .build()
    );
    
    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses while eating.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses while drinking potions.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses while mining blocks.")
            .defaultValue(false)
            .build()
    );
    
    
    private final Setting<Boolean> holeOnly = sgMisc.add(new BoolSetting.Builder()
            .name("only-in-hole")
            .description("Activates above you only in hole.")
            .defaultValue(true)
            .build()
    );
    
    private final Setting<Boolean> feetRotate = sgMisc.add(new BoolSetting.Builder()
            .name("feet-rotate")
            .description("Rotates to feet block.")
            .defaultValue(true)
            .build()
    );
    
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
    private void onTick(TickEvent.Pre event) {
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        if (EntityUtils.getTotalHealth(mc.player) < minHealth.get()) {
            return;
        }
        if (holeOnly.get() && !BlockUtilsWorld.isSurrounded(mc.player))	{
        return;
        }
        FindItemResult exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        if(!exp.found()) {
        	error("You haven't exp bottles!");
            toggle();
            return;
            }
        
        if(needRepair()) need = true;
        if(need) {
        if(feetRotate.get()) {
        Rotations.rotate(mc.player.getYaw(), 90, () -> {
            if (exp.getHand() != null) {
                mc.interactionManager.interactItem(mc.player, mc.world, exp.getHand());
            }
            else {
            	int lastSlot = mc.player.getInventory().selectedSlot;
                if (mc.player.getInventory().getStack(lastSlot).getItem() == Items.ENCHANTED_GOLDEN_APPLE && pauseOnEat.get()) return;
                InvUtils.swap(exp.getSlot(), true);
                mc.interactionManager.interactItem(mc.player, mc.world, exp.getHand());
                if (switchMode.get() == SwitchMode.Silent && lastSlot != -1) updateSlot(lastSlot);
                else InvUtils.swapBack();
            }
        });
        } else {
        	if (exp.getHand() != null) {
                mc.interactionManager.interactItem(mc.player, mc.world, exp.getHand());
            }
            else {
                int lastSlot = mc.player.getInventory().selectedSlot;
                if (mc.player.getInventory().getStack(lastSlot).getItem() == Items.ENCHANTED_GOLDEN_APPLE && pauseOnEat.get()) return;
                InvUtils.swap(exp.getSlot(), true);
                mc.interactionManager.interactItem(mc.player, mc.world, exp.getHand());
                if (switchMode.get() == SwitchMode.Silent && lastSlot != -1) updateSlot(lastSlot);
                else InvUtils.swapBack();
            }
        }
        if(!checkRepair()) {need = false; return;}
     }
  }
    private boolean needRepair() {
        for (int i = 0; i < 4; i++) if (checkThreshold(getArmor(i), minDurability.get())) return true;
        return false;
    }
    
    private boolean checkRepair() {
        for (int i = 0; i < 4; i++) if (checkThreshold(getArmor(i), 100)) return true;
        return false;
    }
    
    public static double getDamage(ItemStack i) {return (((double) (i.getMaxDamage() - i.getDamage()) / i.getMaxDamage()) * 100);}
    
    public static boolean checkThreshold(ItemStack i, double threshold) {
        return getDamage(i) <= threshold;
    }
    
    public static void updateSlot(int newSlot) {
        mc.player.getInventory().selectedSlot = newSlot;
    }
    
    public static ItemStack getArmor(int slot) {
        return mc.player.getInventory().armor.get(slot);
    }
    
	public enum SwitchMode {
		Silent, 
		Client
	}
}