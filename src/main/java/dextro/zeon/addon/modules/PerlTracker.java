package dextro.zeon.addon.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.*;

import dextro.zeon.addon.Zeon;

public class PerlTracker extends Module {

    public PerlTracker() {
        super(Zeon.Misc, "perl-tracker", "Render the way of the arrow and perls");
    }

    public enum colorizeMode{
        Random, Selected
    }

    SettingGroup g = settings.getDefaultGroup();

    Setting<Integer> points = g.add(new IntSetting.Builder()
        .name("max-points")
        .defaultValue(150)
        .min(1)
        .sliderMin(1).sliderMax(500)
        .build());

    Setting<colorizeMode> colorMode = g.add(new EnumSetting.Builder<colorizeMode>()
        .name("color-mode")
        .defaultValue(colorizeMode.Random)
        .build());

    Setting<SettingColor> colorSetting = g.add(new ColorSetting.Builder()
        .name("trail-color")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .visible(() -> colorMode.get() == colorizeMode.Selected)
        .build());

    Map<Integer, TRAIL> arrows = new HashMap<>();
    Map<Integer, TRAIL> perls = new HashMap<>();

    class TRAIL {
        public List<Vec3d> vecs;
        public Color color;
        public TRAIL() {
            this.vecs = new ArrayList<>();
            this.color = Color.fromHsv(Utils.random(0.0D, 360.0D), 0.35D, 1.0D);
        }

        public void render(Render3DEvent e, Integer id) {
            if (vecs.isEmpty()) return;

            for (int i = 0; i < vecs.size(); i++) {
                Color rendercolor = (colorMode.get() == colorizeMode.Random ? color.copy() : colorSetting.get());
                rendercolor.a = 255 - Math.round(((vecs.size() - i) * 255) / vecs.size());
                Vec3d next;
                try {
                     next = vecs.get(i + 1);
                } catch (Exception ex) {
                    next = mc.world.getEntityById(id).getPos();
                }
                e.renderer.line(vecs.get(i).x, vecs.get(i).y, vecs.get(i).z, next.x, next.y, next.z, rendercolor);
            }

        }
    }

    @Override
    public void onActivate() {
        arrows.clear();
        perls.clear();
    }


    @EventHandler
    void a(Render3DEvent e) {
        arrows.entrySet().removeIf(o -> (mc.world.getEntityById(o.getKey()) == null || o.getValue().vecs.isEmpty()));
        perls.entrySet().removeIf(o -> (mc.world.getEntityById(o.getKey()) == null || o.getValue().vecs.isEmpty()));
        mc.world.getEntities().forEach(entity -> {
            if (entity instanceof PersistentProjectileEntity) {
            	PersistentProjectileEntity arrow = (PersistentProjectileEntity) entity;
            		if(!inGround(arrow)) {
                arrows.computeIfAbsent(arrow.getId(), v -> new TRAIL());
                if (!arrows.get(arrow.getId()).vecs.isEmpty() && arrows.get(arrow.getId()).vecs.get(arrows.get(arrow.getId()).vecs.size() - 1).equals(arrow.getPos())) return;
                arrows.get(arrow.getId()).vecs.add(arrow.getPos());
                if (arrows.get(arrow.getId()).vecs.size() > points.get()) arrows.get(arrow.getId()).vecs.remove(0);
            }
            if (entity instanceof EnderPearlEntity) {
            	EnderPearlEntity perl = (EnderPearlEntity) entity;
            	if(!inGround(perl)) {
                perls.computeIfAbsent(perl.getId(), v -> new TRAIL());
                if (!perls.get(perl.getId()).vecs.isEmpty() && perls.get(perl.getId()).vecs.get(perls.get(perl.getId()).vecs.size() - 1).equals(perl.getPos())) return;
                perls.get(perl.getId()).vecs.add(perl.getPos());
                if (perls.get(perl.getId()).vecs.size() > points.get()) perls.get(perl.getId()).vecs.remove(0);
            }
            }
        }
        });

        arrows.forEach((id, trail) -> {
            if (inGround(mc.world.getEntityById(id))) trail.vecs.remove(0);
            trail.render(e, id);
        });
        
        perls.forEach((id, trail) -> {
            if (inGround(mc.world.getEntityById(id))) trail.vecs.remove(0);
            trail.render(e, id);
        });

    }

    boolean inGround(Object entity) {
        Field inGround = entity.getClass().getSuperclass().getDeclaredFields()[7];
        inGround.setAccessible(true);
        try {
            return inGround.getBoolean(entity);
        } catch (IllegalAccessException ignored) {}
        return false;
    }

}