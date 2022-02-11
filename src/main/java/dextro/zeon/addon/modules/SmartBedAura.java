package dextro.zeon.addon.modules;

import com.google.common.util.concurrent.AtomicDouble;

import dextro.zeon.addon.Zeon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.atomic.AtomicReference;

public class SmartBedAura extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgRender = settings.createGroup("Render");
    
    public final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(10).sliderRange(0, 20).build());
    public final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder().name("place-range").defaultValue(5).sliderRange(0, 10).build());
    public final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder().name("min-damage").description("The minimum damage to inflict on your target.").defaultValue(7).range(0, 36).sliderMax(36).build());
    public final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder().name("max-self-damage").description("The maximum damage to inflict on yourself.").defaultValue(7).range(0, 36).sliderMax(36).build());

    
    private final Setting<Integer> autoMoveSlot = sgGeneral.add(new IntSetting.Builder()
            .name("move-slot")
            .description("The slot Auto Move.")
            .defaultValue(9)
            .min(1)
            .sliderMin(1)
            .max(9)
            .sliderMax(9)
            .build()
    );
    
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The slot Auto Move.")
            .defaultValue(10)
            .min(1)
            .sliderMin(1)
            .max(20)
            .sliderMax(20)
            .build()
    );
    
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders the block where it is placing a bed.")
            .defaultValue(true)
            .build()
        );

        private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
        );

        private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color for positions to be placed.")
            .defaultValue(new SettingColor(15, 255, 211,75))
            .build()
        );

        private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color for positions to be placed.")
            .defaultValue(new SettingColor(15, 255, 211))
            .build()
        );
    
    //public static Executor cached = Executors.newCachedThreadPool();
    private Thread thread;
    private BlockPos breakPos;
    private int ticks;
    private CardinalDirection direct;
    private final AtomicReference<BlockPos.Mutable> posPlaceBlock = new AtomicReference<>(new BlockPos.Mutable());
    public PlayerEntity target;
    private final BlockPos.Mutable offsetPos = new BlockPos.Mutable();

    public SmartBedAura() {
        super(Zeon.Combat, "smart-bed-aura", "BedAura with dmg calc");
    }

    @Override
    public void onActivate() {
        ticks = delay.get();
        bestDamage.set(0);
        direct = CardinalDirection.North;
    }

    @Override
    public void onDeactivate() {
        ticks = delay.get();
        bestDamage.set(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPreTick(TickEvent.Pre event) {
        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
        if (target == null) {
            breakPos = null;
            return;
        }
        doAutoMove();
        if (ticks <= 0 && target != null)
        {
            try
            {
                if (thread == null || thread.getState() == Thread.State.TERMINATED)
                {
                    // тут происходит плейс, если найден хороший блок. в потоке расчёта дамага плейсить нельзя
                    if (bestDamage.get() != 0) {
                        FindItemResult bedItem = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
                        BlockUtils.place(posPlaceBlock.get(), bedItem, 0);
                        error("Best damage: " + bestDamage.get());                        
                        breakBedPos(breakPos);
                        bestDamage.set(0);
                    }

                    thread = new Thread(this::doPlace, "Bed Aura");
                    thread.start();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return;
            }

            ticks = delay.get();
        } else ticks--;
    }

    AtomicDouble bestDamage = new AtomicDouble(0);

    private void doPlace()
    {
    	error("started placing");

        //переменные говна
        AtomicReference<Double> offsetSelfDamage = new AtomicReference<>((double) 0);
        AtomicReference<Double> offsetTargetDamage = new AtomicReference<>((double) 0);

        // блок итератор по факту возвращает расчёт урона в Render Thread, а нам это не нужно
        BlockPos.Mutable bp = new BlockPos.Mutable();
        BlockPos mc_player_pos = mc.player.getBlockPos();

        final int i_place_range = (int) Math.ceil(placeRange.get());
        for (int x = -i_place_range; x <= i_place_range; ++x)
            for (int y = -i_place_range; y <= i_place_range; ++y)
                for (int z = -i_place_range; z <= i_place_range; ++z)
                {
                    bp.set(mc_player_pos).move(x, y, z);
if(BlockUtilsWorld.distanceBetween(mc_player_pos, bp) < placeRange.get()) continue;
                    //провверка основной хуйни для основного блока
                    try
                    {
                        if (!BlockUtils.canPlace(bp, true)) continue;
                    }
                    catch (Exception e) {continue;}

                    double targetDamage = DamageUtils.bedDamage(target, new Vec3d(bp.getX() + 0.5, bp.getY() + 0.6, bp.getZ() + 0.5));
                    double selfDamage = DamageUtils.bedDamage(mc.player, new Vec3d(bp.getX() + 0.5, bp.getY() + 0.6, bp.getZ() + 0.5));


                    //оффсет блок и вся залупа для него
                    for (int i = 0; i < 3; i++) {
                        for (CardinalDirection direction : CardinalDirection.values()) {
                            if (mc.world.getBlockState(bp.offset(direction.toDirection())).getMaterial().isReplaceable()){
                                offsetSelfDamage.set(DamageUtils.bedDamage(mc.player, new Vec3d(bp.offset(direction.toDirection()).getX() + 0.5, bp.offset(direction.toDirection()).getY() + 0.6, bp.offset(direction.toDirection()).getZ() + 0.5)));
                                offsetTargetDamage.set(DamageUtils.bedDamage(target, new Vec3d(bp.offset(direction.toDirection()).getX() + 0.5, bp.offset(direction.toDirection()).getY() + 0.6, bp.offset(direction.toDirection()).getZ() + 0.5)));
                                offsetPos.set(bp.offset(direction.toDirection()));
                                direct = direction;
                            }
                        }
                    }

                    // селф демейдж
                    if (selfDamage > maxSelfDamage.get() || offsetSelfDamage.get() > maxSelfDamage.get()) continue;

                    //поставка лучшего урона и лучшего места для кровати
                    if (targetDamage > minDamage.get() || offsetTargetDamage.get() > minDamage.get()) {
                        if (offsetTargetDamage.get() > bestDamage.get()) {
                            bestDamage.set(offsetTargetDamage.get());
                            posPlaceBlock.get().set(bp.toImmutable());  // .toImmutable() - важно, потому что bp - Mutable
                            breakPos = bp.toImmutable();
                        } else if (targetDamage > bestDamage.get()) {
                            bestDamage.set(targetDamage);
                            posPlaceBlock.get().set(bp.toImmutable());
                            breakPos = bp.toImmutable();
                        }
                    }
                }

        error("ended placing");
    }
    
    private void doAutoMove() {
        if (InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem).getSlot() == -1) {
            int slot = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).getSlot();
            InvUtils.move().from(slot).toHotbar(autoMoveSlot.get() - 1);
        }
    }
    
    private void breakBedPos(BlockPos pos) {
        if (pos == null) return;

        boolean wasSneaking = mc.player.isSneaking();
        if (wasSneaking) mc.player.input.sneaking = false;
       error("Breaking...");
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, false));
        if (wasSneaking) mc.player.input.sneaking = true;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (posPlaceBlock.get() == null || !render.get()) return;
    	

            int x = posPlaceBlock.get().getX();
            int y = posPlaceBlock.get().getY();
            int z = posPlaceBlock.get().getZ();

            switch (direct) {
                case North: {
                	event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                	 break;
                }
                case South: {
                	event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                	 break;
                }
                case East: {
                	event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                	 break;
                }
                case West: {
                	event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                	 break;
                }
            }
        }
    
}