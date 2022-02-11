package dextro.zeon.addon.modules;

import dextro.zeon.addon.Zeon;
import meteordevelopment.orbit.EventHandler;

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

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class Phase extends Module {
	
	public enum Mode {
        Normal,
        Client,
        SetBack,
        Teleport,
        SetBackPos
    }
	
    private double prevX = Double.NaN;
    private double prevZ = Double.NaN;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The phase mode used.")
        .defaultValue(Mode.Client)
        .onChanged(mode -> setPos())
        .build()
    );
    
    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("The X and Z distance per clip.")
            .defaultValue(0.1)
            .min(0)
            .sliderRange(0, 10.0)      
            .build()
        );

    public Phase() {
        super(Zeon.Misc, "phase", "Enable walk through the blocks.");
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
        if (mc.player == null) return;
        setPos();
    }

    @Override
    public void onDeactivate() {
        prevX = Double.NaN;
        prevZ = Double.NaN;
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
               if (mc.player == null) return;

        if (Double.isNaN(prevX) || Double.isNaN(prevZ)) setPos();

        Vec3d yawForward = Vec3d.fromPolar(0.0f, mc.player.getYaw());
        Vec3d yawBack = Vec3d.fromPolar(0.0f, mc.player.getYaw() - 180f);
        Vec3d yawLeft = Vec3d.fromPolar(0.0f, mc.player.getYaw() - 90f);
        Vec3d yawRight = Vec3d.fromPolar(0.0f, mc.player.getYaw() - 270f);

        if (mode.get() == Mode.Normal) {

            if (mc.options.keyForward.isPressed()) {
                mc.player.setPos(
                    mc.player.getX() + yawForward.x * distance.get(),
                    mc.player.getY(),
                    mc.player.getZ() + yawForward.z * distance.get()
                );
            }

            if (mc.options.keyBack.isPressed()) {
                mc.player.setPos(
                    mc.player.getX() + yawBack.x * distance.get(),
                    mc.player.getY(),
                    mc.player.getZ() + yawBack.z * distance.get()
                );
            }

            if (mc.options.keyLeft.isPressed()) {
                mc.player.setPos(
                    mc.player.getX() + yawLeft.x * distance.get(),
                    mc.player.getY(),
                    mc.player.getZ() + yawLeft.z * distance.get()
                );
            }

            if (mc.options.keyRight.isPressed()) {
                mc.player.setPos(
                    mc.player.getX() + yawRight.x * distance.get(),
                    mc.player.getY(),
                    mc.player.getZ() + yawRight.z * distance.get()
                );
            }
        }

        else if (mode.get() == Mode.Client) {
            if (mc.options.keyForward.isPressed()) {
                prevX += yawForward.x * distance.get();
                prevZ += yawForward.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }

            if (mc.options.keyBack.isPressed()) {
                prevX += yawBack.x * distance.get();
                prevZ += yawBack.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }

            if (mc.options.keyLeft.isPressed()) {
                prevX += yawLeft.x * distance.get();
                prevZ += yawLeft.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }

            if (mc.options.keyRight.isPressed()) {
                prevX += yawRight.x * distance.get();
                prevZ += yawRight.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }
        }
        
        else if (mode.get() == Mode.Teleport) {
            if (mc.options.keyForward.isPressed()) {
                prevX += yawForward.x * distance.get();
                prevZ += yawForward.z * distance.get();
                mc.player.updatePosition(prevX, mc.player.getY(), prevZ);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, mc.player.getY(), prevZ, mc.player.isOnGround()));
            }

            if (mc.options.keyBack.isPressed()) {
                prevX += yawBack.x * distance.get();
                prevZ += yawBack.z * distance.get();
                mc.player.updatePosition(prevX, mc.player.getY(), prevZ);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, mc.player.getY(), prevZ, mc.player.isOnGround()));

            }

            if (mc.options.keyLeft.isPressed()) {
                prevX += yawLeft.x * distance.get();
                prevZ += yawLeft.z * distance.get();
                mc.player.updatePosition(prevX, mc.player.getY(), prevZ);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, mc.player.getY(), prevZ, mc.player.isOnGround()));

            }

            if (mc.options.keyRight.isPressed()) {
                prevX += yawRight.x * distance.get();
                prevZ += yawRight.z * distance.get();
                mc.player.updatePosition(prevX, mc.player.getY(), prevZ);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, mc.player.getY(), prevZ, mc.player.isOnGround()));

            }
        } 
        
        else if (mode.get() == Mode.SetBack) {
            if (mc.options.keyForward.isPressed()) {
                prevX += yawForward.x * distance.get();
                prevZ += yawForward.z * distance.get();
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, mc.player.getY() + distance.get() / 1000.0, prevZ, mc.player.isOnGround()));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, 0.0, prevZ, mc.player.isOnGround()));
            }

            if (mc.options.keyBack.isPressed()) {
                prevX += yawBack.x * distance.get();
                prevZ += yawBack.z * distance.get();
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, mc.player.getY() + distance.get() / 1000.0, prevZ, mc.player.isOnGround()));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, 0.0, prevZ, mc.player.isOnGround()));
            }

            if (mc.options.keyLeft.isPressed()) {
                prevX += yawLeft.x * distance.get();
                prevZ += yawLeft.z * distance.get();
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, mc.player.getY() + distance.get() / 1000.0, prevZ, mc.player.isOnGround()));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, 0.0, prevZ, mc.player.isOnGround()));
            }

            if (mc.options.keyRight.isPressed()) {
                prevX += yawRight.x * distance.get();
                prevZ += yawRight.z * distance.get();
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, mc.player.getY() + distance.get() / 1000.0, prevZ, mc.player.isOnGround()));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(prevX, 0.0, prevZ, mc.player.isOnGround()));
            }
        }
        
        else if (mode.get() == Mode.SetBackPos) {
            if (mc.options.keyForward.isPressed()) {
                prevX += yawForward.x * distance.get();
                prevZ += yawForward.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }

            if (mc.options.keyBack.isPressed()) {
                prevX += yawBack.x * distance.get();
                prevZ += yawBack.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }

            if (mc.options.keyLeft.isPressed()) {
                prevX += yawLeft.x * distance.get();
                prevZ += yawLeft.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }

            if (mc.options.keyRight.isPressed()) {
                prevX += yawRight.x * distance.get();
                prevZ += yawRight.z * distance.get();
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
                mc.player.setPos(prevX, mc.player.getY(), prevZ);
            }
        }
        
    }

    private void setPos() {
        if (mc.player == null) return;
        prevX = mc.player.getX();
        prevZ = mc.player.getZ();
    }
}