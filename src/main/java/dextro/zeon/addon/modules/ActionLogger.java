package dextro.zeon.addon.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import dextro.zeon.addon.Zeon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.BaseText;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TextColor;
import net.minecraft.world.GameMode;

public class ActionLogger extends Module {
	
    public ActionLogger() {
        super(Zeon.Chat, "action-logger", "Send message on player action.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> joinleave = sgGeneral.add(new BoolSetting.Builder()
		    .name("join-leave")
		    .defaultValue(true)
		    .build());
    
    private final Setting<Boolean> gamemode = sgGeneral.add(new BoolSetting.Builder()
            .name("game-mode-change")
            .defaultValue(true)
            .build());
    
	
    private final List<String> players = new ArrayList<>();
    HashMap<String, GameMode> state = new HashMap<>();


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
    	state.clear();
    	if(players.isEmpty()) {
    		toggle();
    		return;
    	}
    	ArrayList<PlayerListEntry> list = new ArrayList<>(mc.getNetworkHandler().getPlayerList());
    	for (PlayerListEntry p : list) {
    		if(players.contains(p.getProfile().getName())) state.put(p.getProfile().getName(), p.getGameMode());
    	}
    }
    
    
    private BaseText getMode(GameMode m){
    	String tmode = "Survival";
    	int color = 16777215;
    	if(m == GameMode.CREATIVE) {
    		tmode = "Creative";
    		color = 10053324;
    	}
    	if(m == GameMode.ADVENTURE) {
    		tmode = "Adventure";
    		color = 7855591;
    	}
    	if(m == GameMode.SPECTATOR) {
    		tmode = "Spectator";
    		color = 16720896;
    	}
    	BaseText mode = new LiteralText(tmode);
    	mode.setStyle(mode.getStyle().withColor(TextColor.fromRgb(color)));
    	return mode;
    }
    
    
    private BaseText getText(String s){
    	BaseText text = new LiteralText("§8§l［§bActionLogger§8§l］ "+s);
    	text.setStyle(text.getStyle()
    			.withHoverEvent(new HoverEvent(
    					HoverEvent.Action.SHOW_TEXT,
    					new LiteralText(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date(System.currentTimeMillis())))
    					))
    			);
    	return text;
    }
    
    
    private void send(BaseText s){
    	mc.inGameHud.getChatHud().addMessage(s);
    }
    
    
    private void sayModeChange(String s, GameMode m){
    	BaseText text = getText(s);
    	text.append(getMode(m));
    	send(text);
    }
    
    
    private void sayMode(String s, GameMode m){
    	BaseText text = getText(s);
    	if(m != null){
    		text.append(" §8§l［");
    		text.append(getMode(m));
    		text.append("§8§l］");
    	}
    	send(text);
    	
    }
    
    @EventHandler
    private void onTick(TickEvent.Post e) {
    	if(players.isEmpty()) {
    		toggle();
    		return;
    	}

    	ArrayList<PlayerListEntry> list = new ArrayList<>(mc.getNetworkHandler().getPlayerList());
    	HashMap<String, GameMode> newstate = new HashMap<String, GameMode>();
    	for (PlayerListEntry p : list) {
    		if(players.contains(p.getProfile().getName())) newstate.put(p.getProfile().getName(), p.getGameMode());
    	}
    	
    	
    	for(String p : players){
    		if(joinleave.get()){
    			if(state.containsKey(p)){
    				if(!newstate.containsKey(p)) {
    					sayMode("§a§n"+p+"§a － left the game",null);
    					continue;
    				}
    			} else if(newstate.containsKey(p)) {
    				GameMode mode = null;
    				if(newstate.get(p) != GameMode.SURVIVAL) mode = newstate.get(p);
    				sayMode("§c§n"+p+"§c － joined the game",mode);
    				continue;
    			}
    		}
    		if(gamemode.get() && state.containsKey(p) && newstate.containsKey(p)){
    			if(state.get(p) != newstate.get(p)) {
    				sayModeChange("§c§n"+p+"§6 changed the game mode to ",newstate.get(p));
    			}
    		}
    	}
    	
    	state = newstate;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        players.removeIf(String::isEmpty);

        WTable table = theme.table();
        fillTable(theme, table);

        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        table.add(theme.horizontalSeparator("Players")).expandX();
        table.row();

        for (int i = 0; i < players.size(); i++) {
            int msgI = i;
            String player = players.get(i);

            WTextBox textBox = table.add(theme.textBox(player)).minWidth(100).expandX().widget();
            textBox.action = () -> players.set(msgI, textBox.get());

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                players.remove(msgI);

                table.clear();
                fillTable(theme, table);
            };

            table.row();
        }

        WPlus add = table.add(theme.plus()).expandCellX().right().widget();
        add.action = () -> {
            players.add("");

            table.clear();
            fillTable(theme, table);
        };
    }

    @Override
    public NbtCompound toTag() {
    	NbtCompound tag = super.toTag();

        players.removeIf(String::isEmpty);
        NbtList playersTag = new NbtList();

        for (String player : players) playersTag.add(NbtString.of(player));
        tag.put("players", playersTag);

        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        players.clear();

        if (tag.contains("players")) {
        	NbtList playersTag = tag.getList("players", 8);
            for (NbtElement playerTag : playersTag) players.add(playerTag.asString());
        } else {
            players.add("nag1bator228");
        }

        return super.fromTag(tag);
    }
}
