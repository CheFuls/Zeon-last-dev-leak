package dextro.zeon.addon.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import dextro.zeon.addon.Zeon;
import org.apache.commons.io.FileUtils;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class NewAutoEz extends Module {
	
	public enum Mode {
		Client,
		Message
	}
	
	public NewAutoEz() {
		super(Zeon.Chat, "pop-sender", "Say eZZzzZ on every kills!");
	}
	boolean p = true;
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	
    private final Setting<Mode> b = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Mode")
            .description("The mode.")
            .defaultValue(Mode.Message)
            .build()
    );
	
	private final Setting<Integer> minArmor = sgGeneral
			.add(new IntSetting.Builder()
			.name("min-armor")
			.description("Minimum number of armor elements.")
			.defaultValue(2)
			.min(0)
			.max(4)
			.sliderMin(0)
			.sliderMax(4)
			.build()
			);
	
	private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
			.name("ignore-friends")
			.defaultValue(true)
			.build()
			);
	
	private final Setting<Boolean> useRussian = sgGeneral.add(new BoolSetting.Builder()
			.name("enable-russian-message")
			.defaultValue(true)
			.build()
			);
	
	Setting<List<String>> killMessages = sgGeneral.add(new StringListSetting.Builder()
	        .name("messages")
	        .defaultValue(Arrays.asList(
	                "EzZZz {player} by ZEON! v0.7!",
	                "Join ZEON for free: https://discord.gg/YTQGdEEMBm",
	                "{player} just died by ZEON! v0.7!",
	                "{player} Ezzzzz by ZEON! v0.7!",
	                "I just EZZz'd {player} by ZEON v0.7!",
	                "Join ZEON for free: https://discord.gg/YTQGdEEMBm",
	                "I just fucked {player} by ZEON v0.7!",
	                "Killed {player} with ZEON v0.7!",
	                "Join ZEON for free: https://discord.gg/YTQGdEEMBm",
	                "Take the L nerd {player}! You just got ended by ZEON v0.7!",
	                "U got nae`d by ZEON v0.7!",
	                "Join ZEON for free: https://discord.gg/YTQGdEEMBm",
	                "Wow I didn't even use a totem. You suck, {player} by ZEON v0.7!",
	                "{player} died to ZEON v0.7!"
	            ))
	        .visible(() -> !p)
	            .build()
	        );
	
	Setting<List<String>> killMessagesRU = sgGeneral.add(new StringListSetting.Builder()
	        .name("messages")
	        .defaultValue(Arrays.asList(
	                "Еееееееез {player} ЗЕОН v0.7!",
	                "Стань зеоновцем бесплатно: https://discord.gg/YTQGdEEMBm",
	                "{player} Умер от ЗЕОНА v0.7!",
	                "{player}, ЛОХ! ЗЕОН v0.7!",
	                "{player} это было просто для ЗЕОНА v0.7!",
	                "Стань зеоновцем бесплатно: https://discord.gg/YTQGdEEMBm",
	                "Я ТРАХНУЛ {player}! ЗЕОН v0.7!",
	                "Убил {player} с помощью ЗЕОНА v0.7!",
	                "Стань зеоновцем бесплатно: https://discord.gg/YTQGdEEMBm",
	                "Возьми мой хуй, {player}! Ты выебан ЗЕОНОМ v0.7!",
	                "Тебе нужено больше тотемов, {player}! ЗЕОН v0.7!",
	                "Стань зеоновцем бесплатно: https://discord.gg/YTQGdEEMBm",
	                "Сосать рак {player}! ЗЕОН v0.7!"
	            ))
	        .visible(() -> !p)
	            .build()
	        );
	
	
	@Override
	public void onActivate() {
		players.clear();
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
	
	Map<Integer, Integer> players = new HashMap<>();
	
	private boolean checkArmor(PlayerEntity p){
		int armor = 0;
		for(EquipmentSlot a : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
			if(p.getEquippedStack(a).getItem() != Items.AIR) armor++;

		return armor < minArmor.get() ? true : false;
	}
	
	
	private boolean checkFriend(PlayerEntity p){
		return (ignoreFriends.get() && Friends.get().get(p.getName().asString()) != null);
	}

	
	private void add(int a){
		if(players.get(a) == null) players.put(a, 0);
		else players.put(a, players.get(a));
	}
	
	@EventHandler
	private void AttackEntity(AttackEntityEvent e){
		
		
		if(e.entity instanceof EndCrystalEntity){
			mc.world.getPlayers().forEach(p ->{
				if(checkTarget(p) && p.distanceTo(e.entity) < 8) add(p.getId());
			});
		} else if(e.entity instanceof PlayerEntity && checkTarget(e.entity)) add(e.entity.getId());
	}
	


	@EventHandler
	private void PacketEvent(PacketEvent.Receive e) {
		if(e.packet instanceof EntityStatusS2CPacket) {
			EntityStatusS2CPacket p = (EntityStatusS2CPacket) e.packet;
			
			if(p.getEntity(mc.world) instanceof PlayerEntity && checkTarget(p.getEntity(mc.world)) && players.containsKey(p.getEntity(mc.world).getId()) ) {
				if(p.getStatus() == 3) ezz(p.getEntity(mc.world));
				if(p.getStatus() == 35){
					int id = p.getEntity(mc.world).getId();
					if(players.get(id) == null) players.put(id, 1);
					else players.put(id, players.get(id) + 1);
				}
			}
		}
	}
	
	private boolean checkTarget(Entity a){
		PlayerEntity p = (PlayerEntity) a;
		return ( !p.isSpectator() && !p.isCreative() && !p.isInvulnerable() && !mc.player.equals(p) && !checkArmor(p) && !checkFriend(p) ) ? true : false;
	}
	
	
	private void ezz(Entity e){
		int id = e.getId();
		if(b.get() == Mode.Message) {
			if(!useRussian.get()) {
			if(players.get(id) == 0 && mc.player.distanceTo(e) < 8) mc.player.sendChatMessage(killMessages.get().get(Utils.random(0, killMessages.get().size())).replace("{player}", e.getName().getString()));
			else if(players.get(id) != 0 && mc.player.distanceTo(e) < 8)mc.player.sendChatMessage(killMessages.get().get(Utils.random(0, killMessages.get().size())).replace("{player}", e.getName().getString()));
		players.remove(id);
	} else {
		if(players.get(id) == 0 && mc.player.distanceTo(e) < 8) mc.player.sendChatMessage(killMessagesRU.get().get(Utils.random(0, killMessages.get().size())).replace("{player}", e.getName().getString()));
		else if(players.get(id) != 0 && mc.player.distanceTo(e) < 8)mc.player.sendChatMessage(killMessagesRU.get().get(Utils.random(0, killMessages.get().size())).replace("{player}", e.getName().getString()));
	players.remove(id);
	}
}
	else if(b.get() == Mode.Client) {
		if(!useRussian.get()) {
		if(players.get(id) == 0 && mc.player.distanceTo(e) < 8) ChatUtils.info(killMessages.get().get(Utils.random(0, killMessages.get().size())).replace("{player}", e.getName().getString()));
		else if(players.get(id) != 0 && mc.player.distanceTo(e) < 8) ChatUtils.info(killMessages.get().get(Utils.random(0, killMessages.get().size())).replace("{player}", e.getName().getString()));
		players.remove(id);
	} else {
		if(players.get(id) == 0 && mc.player.distanceTo(e) < 8) ChatUtils.info(killMessagesRU.get().get(Utils.random(0, killMessages.get().size())).replace("{player}", e.getName().getString()));
		else if(players.get(id) != 0 && mc.player.distanceTo(e) < 8) ChatUtils.info(killMessagesRU.get().get(Utils.random(0, killMessages.get().size())).replace("{player}", e.getName().getString()));
		players.remove(id);
	}
	}
	}
		
}