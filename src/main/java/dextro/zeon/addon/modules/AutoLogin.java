package dextro.zeon.addon.modules;


import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

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

public class AutoLogin extends Module {

    public AutoLogin() {
        super(Zeon.Misc, "auto-login", "Auto /login on join.");
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
    private void Send(PacketEvent.Send e) {
    	if(e.packet instanceof ChatMessageC2SPacket){
    		String s = ((ChatMessageC2SPacket)e.packet).getChatMessage();

    		if(s.startsWith("/login ") || s.startsWith("/l ") || s.startsWith("/reg ") || s.startsWith("/register ")){
    			String[] text = s.split(" ");
    			if(text.length > 1) save(mc.getCurrentServerEntry().address.toLowerCase(), mc.player.getName().asString().toLowerCase(), text[1]);
    		}

    	}
    }


    File file = new File(MeteorClient.FOLDER, "Login.nbt");
    String prefix = String.format("§8§l［§b%s§8§l］ ",name);

	public NbtCompound getTag(){
		NbtCompound tag = new NbtCompound();
		try {
			if(file.exists()){
				tag = NbtIo.read(file);
			} else {
				file.createNewFile();
			}
		} catch (Exception ex) {}
		return tag;
	}

	public void login(){
		NbtCompound tag = getTag();
		String name = mc.player.getName().asString().toLowerCase();
		String server = mc.getCurrentServerEntry().address.toLowerCase();
		if(tag.contains(server) && tag.getCompound(server).contains(name)) {
			mc.player.sendChatMessage("/login " + tag.getCompound(server).getString(name));
		} else {
			info(String.format(prefix+"§cПароль для игрока §f%s§c не найден!",name));
		}
	}

	public void save(String server, String name, String pass){

		NbtCompound tag = getTag();

		if(tag.contains(server) && tag.getCompound(server).contains(name) && tag.getCompound(server).getString(name).equals(pass)) return;


		if(!tag.contains(server)) tag.put(server, new NbtCompound());
		tag.getCompound(server).putString(name, pass);

		try {
			NbtIo.write(tag, file);
			info(String.format(prefix+"§aПароль для игрока §f%s§a на сервере §f%s§a сохранён!",name,server));
		} catch (Exception e) {}
	}




}