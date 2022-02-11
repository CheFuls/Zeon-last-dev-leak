package dextro.zeon.addon.modules;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import dextro.zeon.addon.Zeon;

public class AntiGhost extends Module {
	  private static long interval = 50;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> predictState = sgGeneral.add(new BoolSetting.Builder()
            .name("predict-block-state")
            .description("predict block state")
            .defaultValue(true)
            .build()
    );
    

    AtomicBoolean manual = new AtomicBoolean(false);
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<BlockPos, BlockState> sblocks   = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> dblocks = new ConcurrentHashMap<>();
    private final Map<BlockPos, BlockState> bblocks   = new ConcurrentHashMap<>();
    
    public AntiGhost() {
        super(Zeon.Combat, "anti-ghost-block", "Only for pvp!");
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
        ClearInfo();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        // sync desynced blocks
    	dblocks.entrySet().stream()
                .filter(entry -> entry.getValue() > GetCurTime())
                .map(Map.Entry::getKey)
                .forEach(this::SyncBlockState);
    }

    //@EventHandler(priority = EventPriority.HIGHEST)
    private void onSetBlockStateOld(SetBlockStateEvent event) {
        if (manual.get()) return;

        BlockPos block = event.pos;
        BlockState synced_state = sblocks.get(block);
        for(Entity e : mc.world.getEntities())
        {
        	if(e instanceof EndCrystalEntity)
        	{
        		if(BlockPosCheck(e, block) == true) RemoveBlock(block, false);
        	} else break;
        }
        if (synced_state != null && synced_state == event.newState) {
        	dblocks.remove(block);

            if (event.oldState != null && event.oldState.getBlock() instanceof BedBlock &&
                    event.newState.getMaterial().isReplaceable())
            {
                BlockState state = event.oldState;
                Direction dir = BedBlock.getOppositePartDirection(state);
                SyncBlockState(block.offset(dir));
            }
        }
        else if (predictState.get())             dblocks.put(block, GetResponseTime());
        else                                        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSetBlockState(SetBlockStateEvent event) {
        if (manual.get()) return;
       
        
        BlockPos block = event.pos;
        BlockState synced_state = sblocks.get(block);
for(Entity e : mc.world.getEntities())
{
	if(e instanceof EndCrystalEntity)
	{
		if(BlockPosCheck(e, block) == true) RemoveBlock(block, false);
	} else break;
}
        
        if      (synced_state == null && event.oldState != null) sblocks.put(block, event.oldState);
        else if (synced_state == event.newState) {
        	dblocks.remove(block);

            // fix beds
            if (event.oldState != null && event.oldState.getBlock() instanceof BedBlock && event.newState.isAir())
                RemoveBlock(block.offset(BedBlock.getOppositePartDirection(event.oldState)), false);
        }
        else if (predictState.get())     dblocks.put(block, GetResponseTime());
        else                                        event.setCancelled(true);
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    private void onPacketReceive(PacketEvent.Receive event) {
        assert mc.world != null;
        assert mc.player != null;

        if (event.packet instanceof BlockUpdateS2CPacket) {   // fill map
            BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet;
            sblocks.put(packet.getPos(), packet.getState());
        } else if (event.packet instanceof ChunkDeltaUpdateS2CPacket) {   // fill map
            ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.packet;
            packet.visitUpdates(sblocks::put);
        }
            /*else if (event.packet instanceof ChunkDataS2CPacket) {   // fill map
                ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet;
                ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());

                WorldChunk chunk = new WorldChunk(mc.world, pos, null);
                chunk.loadFromPacket
                        (null, packet.getReadBuffer(), new CompoundTag(), packet.getVerticalStripBitmask());

                for (int x = 0; x < 16; ++x) {
                    for (int y = 0; y < mc.world.getHeight(); ++y) {
                        for (int z = 0; z < 16; ++z) {
                            BlockPos bpos = new BlockPos(x, y, z);
                            synced_blocks.put(bpos, chunk.getBlockState(bpos));
                        }
                    }
                }
            }*/
        else if (event.packet instanceof PlayerActionResponseS2CPacket) {   // fill map
            PlayerActionResponseS2CPacket packet = (PlayerActionResponseS2CPacket) event.packet;
            sblocks.put(packet.getBlockPos(), packet.getBlockState());

            //if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK && packet.isApproved())
            //bypass_blocks.put(packet.getBlockPos(), packet.getBlockState());
        } else if (event.packet instanceof BlockEventS2CPacket) {
            BlockEventS2CPacket packet = (BlockEventS2CPacket) event.packet;
            BlockPos pos = packet.getPos();
            Block block = packet.getBlock();

            BlockState synced_state = sblocks.get(pos);

            if (synced_state == null) return;
            if (synced_state.isOf(block)) return;

            mc.world.setBlockStateWithoutNeighborUpdates(pos, block.getDefaultState());
        } else if (event.packet instanceof UnloadChunkS2CPacket) {   // clean map
            UnloadChunkS2CPacket packet = (UnloadChunkS2CPacket) event.packet;
            ChunkPos unload_chunk_pos = new ChunkPos(packet.getX(), packet.getZ());

            sblocks.keySet().removeIf(b -> new ChunkPos(b).equals(unload_chunk_pos));
            dblocks.keySet().removeIf(b -> new ChunkPos(b).equals(unload_chunk_pos));
            bblocks.keySet().removeIf(b -> new ChunkPos(b).equals(unload_chunk_pos));
        } else if (event.packet instanceof GameJoinS2CPacket) {   // clear all info
            ClearInfo();
        } /*else if (event.packet instanceof BlockBreakingProgressS2CPacket) {
            if (!cfg_predict_block_state.get()) return;

            BlockBreakingProgressS2CPacket packet = (BlockBreakingProgressS2CPacket) event.packet;
            if (packet.getEntityId() == mc.player.getEntityId()) return;

            BlockState state = mc.world.getBlockState(packet.getPos());
            float breaking_delta = state.calcBlockBreakingDelta(mc.player, mc.world, packet.getPos());

            float k = packet.getProgress() / 10.0F;
            int j = (int) ((int) (k / breaking_delta - 1) +
                    Math.floorDiv(U.GetLatency(), tick.GetIntervalPerTick()));
            float future_k = breaking_delta * (float) (j + 1);
            if ((k < 0.7F && future_k >= 0.7F) || (k < 1.0F && future_k >= 1.0F))
                mc.world.removeBlock(packet.getPos(), false);
        }*/
    }

    private boolean CustomEquals(BlockState state1, BlockState state2) {
        if (state1.equals(state2)) return true;
        //if (state1.getMaterial().isReplaceable() && state2.getMaterial().isReplaceable()) return true;

        return false;
    }

    private void ClearInfo() {
    	sblocks   .clear();
    	dblocks .clear();
    	bblocks   .clear();
    }

    // /U/

    public void SyncBlockState(BlockPos block) {
        assert mc.world != null;

        BlockState client_state = mc.world.getBlockState(block);
        BlockState server_state = sblocks.get(block);

        if      (server_state == null)          RemoveBlock(block, false);
        else if (server_state != client_state)  mc.world.setBlockStateWithoutNeighborUpdates(block, server_state);
    }

    public void RemoveBlock(BlockPos block, boolean move) {
        assert mc.world != null;
        manual.set(true);
        mc.world.removeBlock(block, move);
        manual.set(false);
    }
    
    public boolean BlockPosCheck(Entity entity, BlockPos blockPos)
    {
    	if(entity.getBlockPos() == blockPos) return true;
    	return false;
    }

    public void SetBlockState(BlockPos block, BlockState state) {
        assert mc.world != null;
        manual.set(true);
        mc.world.setBlockStateWithoutNeighborUpdates(block, state);
        manual.set(false);
    }

    public boolean IsBlockSynced(BlockPos block) {
        return !dblocks.containsKey(block);
    }
    
    public static long GetCurTime()
    {
        return Util.getMeasuringTimeMs();
    }
    
    public static long GetLatency3()
    {
    	PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        
        return Math.max(playerListEntry.getLatency(), GetIntervalPerTick()) * 2 +
                GetIntervalPerTick() * 10;
    }
    public static long GetIntervalPerTick()
    {
        return Modules.get().get(ExtraSurround.class).isActive() ? interval : 50L;
    }
    public static long GetResponseTime()
    {
        return Util.getMeasuringTimeMs() + GetLatency3();
    }
}