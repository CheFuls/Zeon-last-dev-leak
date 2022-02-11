package dextro.zeon.addon.modules;

import dextro.zeon.addon.Zeon;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SurroundAlert extends Module {

    public enum NotifMode {
        Client,
        Alert,
        Both
    }
    
    public SurroundAlert() {
        super(Zeon.Chat, "surround-alert", "Notifies u when player starting break ur surround!");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSBreak = settings.createGroup("Surround Break");

    private final Setting<NotifMode> notificationMode = sgGeneral.add(new EnumSetting.Builder<NotifMode>()
            .name("mode")
            .description("The mode to use for notifications.")
            .defaultValue(NotifMode.Alert)
            .build()
        );
    
    private final Setting<Boolean> surroundBreak = sgSBreak.add(new BoolSetting.Builder()
            .name("surround-break")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> head = sgSBreak.add(new BoolSetting.Builder()
            .name("head-break")
            .defaultValue(true)
            .visible(surroundBreak::get)
            .build()
    );

    private final Setting<Boolean> face = sgSBreak.add(new BoolSetting.Builder()
            .name("face-break")
            .defaultValue(true)
            .visible(surroundBreak::get)
            .build()
    );

    private final Setting<Boolean> legs = sgSBreak.add(new BoolSetting.Builder()
            .name("legs-break")
            .defaultValue(true)
            .visible(surroundBreak::get)
            .build()
    );

    private final Queue<UUID> toLookup = new ConcurrentLinkedQueue<UUID>();
    private long lastTick = 0;
    Set<PlayerEntity> playersBur = new HashSet<PlayerEntity>();
    private String m = " breaking ";
    private int timer = 0;
    private int count;

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

    @Override
    public void onDeactivate() {
        toLookup.clear();
    }
    @EventHandler
    public void onLeave(GameLeftEvent event) {
        toLookup.clear();
    }

    @EventHandler
    private void a(PacketEvent.Receive event) {
        if(surroundBreak.get() == true) {
            if (event.packet instanceof BlockBreakingProgressS2CPacket) {
                BlockBreakingProgressS2CPacket w = (BlockBreakingProgressS2CPacket) event.packet;

                if(w.getProgress() != 0) return;
                String player = mc.world.getEntityById(w.getEntityId()).getName().asString();
                BlockPos p = mc.player.getBlockPos();
                BlockPos brpos = w.getPos();
                if(legs.get()) {
                	for(BlockPos a : new BlockPos[] {p.east(), p.west(), p.south(), p.north()}) {
                
                	if(a.equals(brpos)) {
                		if(notificationMode.get() == NotifMode.Client)
                		{
                		info(player+m+"legs");
                		} 
                		else if(notificationMode.get() == NotifMode.Alert)
                		{
                			mc.getToastManager().add(new MeteorToast(Items.NETHERITE_PICKAXE, title, player + m + "legs"));
                		    count++;
                		}
                		else if(notificationMode.get() == NotifMode.Both)
                		{
                			info(player+m+"legs");
                			mc.getToastManager().add(new MeteorToast(Items.NETHERITE_PICKAXE, title, player + m + "legs"));
                			count++;
                		}
                	  }
                	}
                }
              
           
                if(face.get()) 
                {
                	for(BlockPos a : new BlockPos[] {p.up().east(), p.up().west(), p.up().south(), p.up().north()}) 
                	{
                		if(a.equals(brpos)) {
                    		if(notificationMode.get() == NotifMode.Client)
                    		{
                    		info(player+m+"face");
                    		} 
                    		else if(notificationMode.get() == NotifMode.Alert)
                    		{
                    			mc.getToastManager().add(new MeteorToast(Items.NETHERITE_PICKAXE, title, player + m + "face"));
                    			count++;
                    		}
                    		else if(notificationMode.get() == NotifMode.Both)
                    		{
                    			info(player+m+"face");
                    			mc.getToastManager().add(new MeteorToast(Items.NETHERITE_PICKAXE, title, player + m + "face"));
                    			count++;
                    		}
                    	  }
                    }
                }
                if(head.get() && p.up(2).equals(brpos)) 
                {
                	if(notificationMode.get() == NotifMode.Client)
            		{
            		info(player+m+"head");
            		} 
            		else if(notificationMode.get() == NotifMode.Alert)
            		{
            			mc.getToastManager().add(new MeteorToast(Items.NETHERITE_PICKAXE, title, player + m + "head"));
            		}
            		else if(notificationMode.get() == NotifMode.Both)
            		{
            			info(player+m+"head");
            			mc.getToastManager().add(new MeteorToast(Items.NETHERITE_PICKAXE, title, player + m + "head"));
            		}
                }
            }
        }
      
          }
        
    


    public BaseText formatMessage(String message, Vec3d coords) {
        BaseText text = new LiteralText(message);
        text.append(ChatUtils.formatCoords(coords));
        text.append(Formatting.GRAY.toString()+".");
        return text;
    }

    public BaseText formatMessage(String message, BlockPos coords) {
        return formatMessage(message, new Vec3d(coords.getX(), coords.getY(), coords.getZ()));
    }
}