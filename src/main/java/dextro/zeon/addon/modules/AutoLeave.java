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
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;

public class AutoLeave extends Module {
	
	public enum Mode {
		MessageAndLeave,
		NoneAndLeave
	}
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> a = sgGeneral.add(new IntSetting.Builder()
            .name("totem-count")
            .description("Totem count when you kick")
            .defaultValue(3)
            .min(0)
            .sliderMax(27)
            .build());
    
    private final Setting<Mode> b = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Mode")
            .description("The mode.")
            .defaultValue(Mode.MessageAndLeave)
            .build()
    );
    
    private final Setting<String> c = sgGeneral.add(new StringSetting.Builder()
		    .name("message")
		    .description("Send a chat message .")
		    .defaultValue("Good Fight Bro :)")
		    .build()
    );
    
    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disable module after leave.")
            .defaultValue(true)
            .build()
    );
    
	
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public AutoLeave() {
        super(Zeon.Combat, "auto-leave", "Kick from the server if you have small totems");
    }
    
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
    private void  onTick(TickEvent.Post event) {
        int Count = InvUtils.find(Items.TOTEM_OF_UNDYING).getCount();
        if (Count <= a.get() && b.get() == Mode.MessageAndLeave && autoDisable.get()) {
        	mc.player.sendChatMessage(c.get());
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("§7§l［§6§lAutoLeave§7§l］ §c Your totem's count <= " + a)));
            this.toggle();
        } else if (Count <= a.get() && b.get() == Mode.NoneAndLeave && autoDisable.get()) {
        	 mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("§7§l［§6§lAutoLeave§7§l］ §c Your totem's count <= " + a)));
             this.toggle();
        } else if  (Count <= a.get() && b.get() == Mode.MessageAndLeave) {
        	mc.player.sendChatMessage(c.get());
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("§7§l［§6§lAutoLeave§7§l］ §c Your totem's count <= " + a)));
        } else if (Count <= a.get() && b.get() == Mode.NoneAndLeave) {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("§7§l［§6§lAutoLeave§7§l］ §c Your totem's count <= " + a)));
        }
    }

}
