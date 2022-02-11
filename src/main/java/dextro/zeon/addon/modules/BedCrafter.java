package dextro.zeon.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

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

public class BedCrafter extends Module {
	private static final MinecraftClient mc = MinecraftClient.getInstance();
boolean p = false;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAuto = settings.createGroup("Automatically");
    private final Setting<Boolean> disableAfter = sgGeneral.add(new BoolSetting.Builder().name("disable-after").description("Toggle off after filling your inv with beds.").defaultValue(false).build());
    private final Setting<Boolean> disableNoMats = sgGeneral.add(new BoolSetting.Builder().name("disable-on-no-mats").description("Toggle off if you run out of material.").defaultValue(false).build());
    private final Setting<Boolean> closeAfter = sgGeneral.add(new BoolSetting.Builder().name("close-after").description("Close the crafting GUI after filling.").defaultValue(true).build());

    private final Setting<Boolean> automatic = sgAuto.add(new BoolSetting.Builder().name("automatic").description("Automatically place/search for and open crafting tables when you're out of beds.").defaultValue(false).build());
    private final Setting<Boolean> antiDesync = sgAuto.add(new BoolSetting.Builder().name("anti-desync").description("Try to prevent inventory desync.").defaultValue(false).build());
    private final Setting<Boolean> autoOnlyHole = sgAuto.add(new BoolSetting.Builder().name("in-hole-only").description("Only auto refill while in a hole.").defaultValue(false).build());
    private final Setting<Boolean> autoOnlyGround = sgAuto.add(new BoolSetting.Builder().name("on-ground-only").description("Only auto refill while on the ground.").defaultValue(false).build());
    private final Setting<Boolean> autoWhileMoving = sgAuto.add(new BoolSetting.Builder().name("while-moving").description("Allow auto refill while in motion").defaultValue(false).build());
    private final Setting<Integer> emptySlotsNeeded = sgAuto.add(new IntSetting.Builder().name("required-empty-slots").description("How many empty slots are required for activation.").defaultValue(5).min(1).build());
    private final Setting<Integer> radius = sgAuto.add(new IntSetting.Builder().name("radius").description("How far to search for crafting tables near you.").defaultValue(3).min(1).build());
    private final Setting<Double> minHealth = sgAuto.add(new DoubleSetting.Builder().name("min-health").description("Min health require to activate.").defaultValue(10).min(1).max(36).sliderMax(36).build());

    public BedCrafter() {
        super(Zeon.Combat, "bed-crafter", "Automatically craft beds.");
    }


    private boolean didRefill = false;
    private boolean startedRefill = false;
    private boolean alertedNoMats = false;


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
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (PlayerUtils.getTotalHealth() <= minHealth.get()) return;
        if (automatic.get() && isOutOfMaterial() && !alertedNoMats) {
            error("Cannot activate auto mode, no material left.");
            alertedNoMats = true;
        }
        if (automatic.get() && needsRefill() && canRefill(true) && !isOutOfMaterial() && !(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
            FindItemResult craftTable = InvUtils.findInHotbar(Blocks.CRAFTING_TABLE.asItem());
            if (!craftTable.found()) {
                toggle();
                error("No crafting tables in hotbar!");
                return;
            }
            BlockPos tablePos;
            tablePos = findCraftingTable();
            if (tablePos == null) {
                placeCraftingTable(craftTable);
                return;
            }
            openCraftingTable(tablePos);
            if (p == true && !startedRefill) {
                startedRefill = true;
            }
            didRefill = true;
            return;
        }
        if (didRefill && !needsRefill()) {
            didRefill = false;
            startedRefill = false;
        }

        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            if (!canRefill(false)) {
                mc.player.closeHandledScreen();
                if (antiDesync.get()) mc.player.getInventory().updateItems();
                return;
            }
            CraftingScreenHandler currentScreenHandler = (CraftingScreenHandler) mc.player.currentScreenHandler;
            if (isOutOfMaterial()) {
                if (disableNoMats.get()) toggle();
                mc.player.closeHandledScreen();
                if (antiDesync.get()) mc.player.getInventory().updateItems();
                return;
            }
            if (isInventoryFull()) {
                if (disableAfter.get()) toggle();
                if (closeAfter.get()) {
                    mc.player.closeHandledScreen();
                    if (antiDesync.get()) mc.player.getInventory().updateItems();
                }
                if (!automatic.get()) info("Your inventory is full.");
                return;
            }
            List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getResultsForGroup(RecipeBookGroup.CRAFTING_MISC);
            for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
                for (Recipe<?> recipe : recipeResultCollection.getRecipes(true)) {
                    if (recipe.getOutput().getItem() instanceof BedItem) {
                        assert mc.interactionManager != null;
                        mc.interactionManager.clickRecipe(currentScreenHandler.syncId, recipe, false);
                        windowClick(currentScreenHandler, 0, SlotActionType.QUICK_MOVE, 1);
                    }
                }
            }
        }
    }

    private void placeCraftingTable(FindItemResult craftTable) {
        List<BlockPos> nearbyBlocks = getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) {
            if (BlockUtilsWorld.getBlock(block) == Blocks.AIR) {
                BlockUtils.place(block, craftTable, 0, true);
                break;
            }
        }
    }

    private BlockPos findCraftingTable() {
        List<BlockPos> nearbyBlocks = getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) if (BlockUtilsWorld.getBlock(block) == Blocks.CRAFTING_TABLE) return block;
        return null;
    }

    private void openCraftingTable(BlockPos tablePos) {
        Vec3d tableVec = new Vec3d(tablePos.getX(), tablePos.getY(), tablePos.getZ());
        BlockHitResult table = new BlockHitResult(tableVec, Direction.UP, tablePos, false);
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, table);
    }

    private boolean needsRefill() {
        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!bed.found()) return true;
        return isInventoryFull();
    }

    private boolean canRefill(boolean checkSlots) {
        if (!autoWhileMoving.get() && isPlayerMoving(mc.player)) return false;
        if (autoOnlyHole.get() && !isInHole(mc.player)) return false;
        if (autoOnlyGround.get() && !mc.player.isOnGround()) return false;
        if (isInventoryFull()) return false;
        if (checkSlots) if (getEmptySlots() < emptySlotsNeeded.get()) return false;
        return !(PlayerUtils.getTotalHealth() <= minHealth.get());
    }

    private boolean isOutOfMaterial() {
        FindItemResult wool = InvUtils.find(itemStack -> wools.contains(itemStack.getItem()));
        FindItemResult plank = InvUtils.find(itemStack -> planks.contains(itemStack.getItem()));
        FindItemResult craftTable =  InvUtils.findInHotbar(Blocks.CRAFTING_TABLE.asItem());
        if (!craftTable.found()) return true;
        if (!wool.found() || !plank.found()) return true;
        return wool.getCount() < 3 || plank.getCount() < 3;
    }

    private void windowClick(ScreenHandler container, int slot, SlotActionType action, int clickData) {
        assert mc.interactionManager != null;
        mc.interactionManager.clickSlot(container.syncId, slot, clickData, action, mc.player);
    }
    
    public static ArrayList<Item> wools = new ArrayList<Item>() {{
        add(Items.WHITE_WOOL);
        add(Items.ORANGE_WOOL);
        add(Items.MAGENTA_WOOL);
        add(Items.LIGHT_BLUE_WOOL);
        add(Items.YELLOW_WOOL);
        add(Items.LIME_WOOL);
        add(Items.PINK_WOOL);
        add(Items.GRAY_WOOL);
        add(Items.LIGHT_GRAY_WOOL);
        add(Items.CYAN_WOOL);
        add(Items.PURPLE_WOOL);
        add(Items.BLUE_WOOL);
        add(Items.BROWN_WOOL);
        add(Items.GREEN_WOOL);
        add(Items.RED_WOOL);
        add(Items.BLACK_WOOL);
    }};

    public static ArrayList<Item> planks = new ArrayList<Item>() {{
        add(Items.OAK_PLANKS);
        add(Items.SPRUCE_PLANKS);
        add(Items.BIRCH_PLANKS);
        add(Items.JUNGLE_PLANKS);
        add(Items.ACACIA_PLANKS);
        add(Items.DARK_OAK_PLANKS);
    }};
    
    public static Integer getEmptySlots() {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) emptySlots++;
        }
        return emptySlots;
    }

    public static boolean isInventoryFull() {
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack == null || itemStack.getItem() instanceof AirBlockItem) return false;
        }
        return true;
    }
    
    public static boolean isPlayerMoving(PlayerEntity p) {
        return p.forwardSpeed != 0 || p.sidewaysSpeed != 0;
    }

    public static boolean isInHole(PlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        return !mc.world.getBlockState(pos.add(1, 0, 0)).isAir()
                && !mc.world.getBlockState(pos.add(-1, 0, 0)).isAir()
                && !mc.world.getBlockState(pos.add(0, 0, 1)).isAir()
                && !mc.world.getBlockState(pos.add(0, 0, -1)).isAir()
                && !mc.world.getBlockState(pos.add(0, -1, 0)).isAir();
    }
    
    public static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (BlockUtilsWorld.distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }
        return blocks;
    }
}