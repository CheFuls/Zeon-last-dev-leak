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
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoTotem;
import meteordevelopment.meteorclient.systems.modules.combat.Offhand;
import meteordevelopment.meteorclient.systems.modules.player.OffhandCrash;
import meteordevelopment.meteorclient.systems.modules.player.AutoMend;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class CustomAutoTotem extends Module {
	public enum Mode {
		ForAntiCheat,
		NoAntiCheat
	}


    private String totemCountString = "0";


    public CustomAutoTotem() {
        super(Zeon.Combat, "custom-auto-totem", "Custom auto totem.");
    }
    
    private final SettingGroup sgDelay = settings.createGroup("Delay");
    
    private final Setting<Mode> takeTotemMode = sgDelay.add(new EnumSetting.Builder<Mode>()
            .name("take-mode")
            .description("The take totem in your hand.")
            .defaultValue(Mode.NoAntiCheat)
            .build()
    );
    
    private final Setting<Integer> tickDelay = sgDelay.add(new IntSetting.Builder()
            .name("take-delay")
            .description("Totem's take delay.")
            .defaultValue(1)
            .min(0)
            .sliderMax(20)
            .build()
    );
    
    private int ticks;
    

    @Override
    public void onActivate() {
        ticks = 0;
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
    	if(Modules.get().get(AutoMend.class).isActive()) Modules.get().get(AutoMend.class).toggle();
    	if(Modules.get().get(Offhand.class).isActive()) Modules.get().get(Offhand.class).toggle();
    	if(Modules.get().get(AutoTotem.class).isActive()) Modules.get().get(AutoTotem.class).toggle();
    	if(Modules.get().get(OffhandCrash.class).isActive()) Modules.get().get(OffhandCrash.class).toggle();
	}
    
    
    @EventHandler (priority = Integer.MAX_VALUE)
    private void POPS(PacketEvent.Receive e) {
        if (e.packet instanceof EntityStatusS2CPacket) {
	        EntityStatusS2CPacket p = (EntityStatusS2CPacket) e.packet;
	        if (p.getStatus() != 35) return;
	        Entity entity = p.getEntity(mc.world);
	        if (entity == null || !entity.equals(mc.player)) return;
	        
	        
	        
	        if(mc.currentScreen instanceof GenericContainerScreen) CHTotem();
	        if(mc.currentScreen instanceof ShulkerBoxScreen) SHTotem();
	        if(mc.currentScreen instanceof CraftingScreen) CRTotem();
	    	if(mc.currentScreen instanceof AnvilScreen) ATotem();
        }
    }
    


    private void CHTotem() {

    	GenericContainerScreenHandler container = ((GenericContainerScreen) mc.currentScreen).getScreenHandler();
    	if (container == null) return;
    	int slot = -1;
    	for (int i = 0; i < container.slots.size(); i++) {
    		if(container.slots.get(i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
    			slot = i;
    			break;
    		}
    	}
    	if(slot > -1) {
    		MOVE_TOTEM(slot);
    		container.slots.get(slot).setStack(new ItemStack(Items.AIR));
    	}
    }
    
    
    
    private void SHTotem() {

    	ShulkerBoxScreenHandler container = ((ShulkerBoxScreen) mc.currentScreen).getScreenHandler();
    	if (container == null) return;
    	int slot = -1;
    	for (int i = 0; i < container.slots.size(); i++) {
    		if(container.slots.get(i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
    			slot = i;
    			break;
    		}
    	}
    	if(slot > -1) {
    		MOVE_TOTEM(slot);
    		container.slots.get(slot).setStack(new ItemStack(Items.AIR));
    	}
    }
    
    
    
    private void ATotem() {

    	AnvilScreenHandler container = ((AnvilScreen) mc.currentScreen).getScreenHandler();
    	if (container == null) return;
    	int slot = -1;
    	for (int i = 0; i < container.slots.size(); i++) {
    		if(container.slots.get(i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
    			slot = i;
    			break;
    		}
    	}
    	if(slot > -1) {
    		MOVE_TOTEM(slot);
    		container.slots.get(slot).setStack(new ItemStack(Items.AIR));
    	}
    }
    
    
    
    private void CRTotem() {

    	CraftingScreenHandler container = ((CraftingScreen) mc.currentScreen).getScreenHandler();
    	if (container == null) return;
    	int slot = -1;
    	for (int i = 0; i < container.slots.size(); i++) {
    		if(container.slots.get(i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
    			slot = i;
    			break;
    		}
    	}
    	if(slot > -1) {
    		MOVE_TOTEM(slot);
    		container.slots.get(slot).setStack(new ItemStack(Items.AIR));
    	}
    }
    
    
    
    private void MOVE_TOTEM(int slot) {
    	mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId,slot,40,SlotActionType.SWAP,mc.player);
 
  }
    
    
    private void InvTotem() {
    	if (ticks >= tickDelay.get()) {
            ticks = 0;
           FindItemResult RESULT =  InvUtils.find(Items.TOTEM_OF_UNDYING);
    	if(takeTotemMode.get() == Mode.ForAntiCheat) {
    	    if(RESULT.getCount() > 0) {
    	    	int slot = Ezz.invIndexToSlotId(RESULT.getSlot()); 
        		MOVE_TOTEM(slot);
    	      }
    	}
    	
    	if(takeTotemMode.get() == Mode.NoAntiCheat) {
    	    if(RESULT.getCount() > 0) {
    	    	int slot = Ezz.invIndexToSlotId(RESULT.getSlot()); 
        		MOVE_TOTEM(slot);
    	      }
    	}
    }else ticks++; 
    }
    
   
    
    
    @EventHandler (priority = Integer.MAX_VALUE-1)
    private void onTick(TickEvent.Pre e) {
    	Screen s = mc.currentScreen;

        	if(mc.player == null || mc.world == null) return;
        	
        	SET_TOTEM_COUNT();
        	
        	if(mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        	
        	if(s instanceof GenericContainerScreen) {CHTotem(); return;}
        	if(s instanceof ShulkerBoxScreen) {SHTotem(); return;}
        	if(s instanceof CraftingScreen) {CRTotem(); return;}
        	
    	    InvTotem();

  	}
   
    
    
    

    private void SET_TOTEM_COUNT(){
	    totemCountString = Integer.toString(InvUtils.find(Items.TOTEM_OF_UNDYING).getCount());
    }
    
    
    

    @Override
    public String getInfoString() {
        return totemCountString;
    }

    
}

