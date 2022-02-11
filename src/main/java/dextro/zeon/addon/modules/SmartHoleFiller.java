package dextro.zeon.addon.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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

public class SmartHoleFiller extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBlocks = settings.createGroup("Blocks");
    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("horizontal-radius")
        .description("Horizontal radius in which to search for holes.")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("vertical-radius")
        .description("Vertical radius in which to search for holes.")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
        .name("doubles")
        .description("Fills double holes.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The ticks delay between placement.")
        .defaultValue(1)
        .min(0)
        .build()
    );
    
    private final Setting<Double> targetRange = sgTarget.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The minimum distance between the hole and the player for the hole to be filled.")
            .defaultValue(10)
            .min(0)
            .sliderMax(15)
            .max(15)
            .build()
    );
    
    private final Setting<Double> distance = sgTarget.add(new DoubleSetting.Builder()
            .name("min-distance")
            .description("The minimum distance between the hole and the player for the hole to be filled.")
            .defaultValue(2)
            .min(1)
            .sliderMax(5)
            .max(5)
            .build()
    );

    private final Setting<Boolean> rotate = sgBlocks.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates towards the holes being filled.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders an overlay where blocks will be placed.")
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
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232))
        .build()
    );

    private final Setting<SettingColor> nextSideColor = sgRender.add(new ColorSetting.Builder()
        .name("next-side-color")
        .description("The side color of the next block to be placed.")
        .defaultValue(new SettingColor(227, 196, 245, 10))
        .build()
    );

    private final Setting<SettingColor> nextLineColor = sgRender.add(new ColorSetting.Builder()
        .name("next-line-color")
        .description("The line color of the next block to be placed.")
        .defaultValue(new SettingColor(227, 196, 245))
        .build()
    );

    private final Pool<Hole> holePool = new Pool<>(Hole::new);
    private final List<Hole> holes = new ArrayList<>();
    private final byte NULL = 0;
    private PlayerEntity target;
    private int timer;

    public SmartHoleFiller() {
        super(Zeon.Combat, "smart-hole-filler", "Fills holes when the player wants to jump into them.");
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
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
    	target = CityUtils.getPlayerTarget(targetRange.get());
        for (Hole hole : holes) holePool.free(hole);
        holes.clear();
	 FindItemResult block = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && obsidian.contains(Block.getBlockFromItem(itemStack.getItem())));
     if (!block.found())
     {
         error("No obsidian in hotbar!");
         toggle();
     return;
    }

        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
            if (!validHole(blockPos)) return;

            int bedrock = 0, obsidian = 0;
            Direction air = null;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) continue;

                BlockState state = mc.world.getBlockState(blockPos.offset(direction));

                if (state.getBlock() == Blocks.BEDROCK) bedrock++;
                else if (state.getBlock() == Blocks.OBSIDIAN) obsidian++;
                else if (direction == Direction.DOWN) return;
                else if (validHole(blockPos.offset(direction)) && air == null) {
                    for (Direction dir : Direction.values()) {
                        if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                        BlockState blockState1 = mc.world.getBlockState(blockPos.offset(direction).offset(dir));

                        if (blockState1.getBlock() == Blocks.BEDROCK) bedrock++;
                        else if (blockState1.getBlock() == Blocks.OBSIDIAN) obsidian++;
                        else return;
                    }

                    air = direction;
                }
            }
 if(target != null)
 {
            if (obsidian + bedrock == 5 && air == null && BlockUtilsWorld.distanceBetween(target.getBlockPos(), blockPos) <= distance.get()) holes.add(holePool.get().set(blockPos, NULL));
            else if (obsidian + bedrock == 8 && doubles.get() && air != null && BlockUtilsWorld.distanceBetween(target.getBlockPos(), blockPos) <= distance.get()) {
                holes.add(holePool.get().set(blockPos, Dir.get(air)));
            }
 }
        });
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
    	target = CityUtils.getPlayerTarget(targetRange.get());
        if (timer <= 0 && !holes.isEmpty() && target != null) {
        		 FindItemResult block = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && obsidian.contains(Block.getBlockFromItem(itemStack.getItem())));
        	     if (!block.found())
        	     {
        	     error("No obsidian in hotbar!");
        	     toggle();
        	     return;
        	     }
        	     if(BlockUtilsWorld.distanceBetween(target.getBlockPos(), holes.get(0).blockPos) <= distance.get())
     	        {
     	        BlockUtils.place(holes.get(0).blockPos, block, rotate.get(), 10, true);
     	        }

                 timer = placeDelay.get();
        	}
        

        timer--;
    }

    private boolean validHole(BlockPos pos) {
        if (mc.player.getBlockPos().equals(pos)) return false;
        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return false;
        return !((AbstractBlockAccessor) mc.world.getBlockState(pos.up()).getBlock()).isCollidable();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        for (Hole hole : holes) {
            boolean isFirst = hole == holes.get(0);

            Color side = isFirst ? nextSideColor.get() : sideColor.get();
            Color line = isFirst ? nextLineColor.get() : lineColor.get();

            event.renderer.box(hole.blockPos, side, line, shapeMode.get(), hole.exclude);
        }
    }

    private static class Hole {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public byte exclude;

        public Hole set(BlockPos blockPos, byte exclude) {
            this.blockPos.set(blockPos);
            this.exclude = exclude;

            return this;
        }
    }
    
    
    public static ArrayList<Block> obsidian = new ArrayList<Block>() {{
        add(Blocks.OBSIDIAN);
    }};
}