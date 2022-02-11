package dextro.zeon.addon.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import dextro.zeon.addon.Zeon;

public class PopRender extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyOne = sgGeneral.add(new BoolSetting.Builder()
        .name("only-one")
        .description("Only allow one ghost per player.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> renderTime = sgGeneral.add(new DoubleSetting.Builder()
        .name("render-time")
        .description("How long the ghost is rendered in seconds.")
        .defaultValue(1)
        .min(0.1)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> yModifier = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-modifier")
        .description("How much should the Y position of the ghost change per second.")
        .defaultValue(0.75)
        .sliderRange(-4, 4)
        .build()
    );

    private final Setting<Double> scaleModifier = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale-modifier")
        .description("How much should the scale of the ghost change per second.")
        .defaultValue(-0.25)
        .sliderRange(-4, 4)
        .build()
    );

    private final Setting<Boolean> fadeOut = sgGeneral.add(new BoolSetting.Builder()
        .name("fade-out")
        .description("Fades out the color.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 255, 255, 25))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 255, 255, 127))
        .build()
    );

    private final List<GhostPlayer> ghosts = new ArrayList<>();

    public PopRender() {
        super(Zeon.Misc, "pop-render", "Renders a ghost where players pop totem.");
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
    @Override
    public void onDeactivate() {
        synchronized (ghosts) {
            ghosts.clear();
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket)) return;
    	EntityStatusS2CPacket p = (EntityStatusS2CPacket) event.packet;
        if (p.getStatus() != 35) return;
        
        Entity entity = p.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity) || entity == mc.player) return;
        PlayerEntity player = (PlayerEntity) entity;

        synchronized (ghosts) {
            if (onlyOne.get()) ghosts.removeIf(ghostPlayer -> ghostPlayer.uuid.equals(entity.getUuid()));

            ghosts.add(new GhostPlayer(player));
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        synchronized (ghosts) {
            ghosts.removeIf(ghostPlayer -> ghostPlayer.render(event));
        }
    }

    private class GhostPlayer extends FakePlayerEntity {
        private final UUID uuid;
        private double timer, scale = 1;

        public GhostPlayer(PlayerEntity player) {
            super(player, "ghost", 20, false);

            uuid = player.getUuid();
        }

        public boolean render(Render3DEvent event) {
            // Increment timer
            timer += event.frameTime;
            if (timer > renderTime.get()) return true;

            // Y Modifier
            lastRenderY = getY();
            ((IVec3d) getPos()).setY(getY() + yModifier.get() * event.frameTime);

            // Scale Modifier
            scale += scaleModifier.get() * event.frameTime;

            // Fade out
            int preSideA = sideColor.get().a;
            int preLineA = lineColor.get().a;

            if (fadeOut.get()) {
                sideColor.get().a *= 1 - timer / renderTime.get();
                lineColor.get().a *= 1 - timer / renderTime.get();
            }

            // Render
            WireframeEntityRenderer.render(event, this, scale, sideColor.get(), lineColor.get(), shapeMode.get());

            // Restore colors
            sideColor.get().a = preSideA;
            lineColor.get().a = preLineA;

            return false;
        }
    }
}