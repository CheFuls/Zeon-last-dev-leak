package dextro.zeon.addon.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import dextro.zeon.addon.Zeon;
import org.apache.commons.io.FileUtils;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ExtraSurround extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public enum antcry{
        Yes,
        No
    }
    
    public enum SurrMode{
    	Normal,
    	Double,
    	Full
    }
    
    public enum ecenter {
        fast,
        legit,
        NoTP
    }
    
    public enum Version {
    	Old,
    	New,
    	Interact
    	
    }
    
	private final Setting<Version> version = sgGeneral.add(new EnumSetting.Builder<Version>()
			.name("place-mode")
			.description("Version of server where u will be pvp.")
			.defaultValue(Version.Old)
			.build()
		);
  
    private final Setting<Integer> tickDelay = sgGeneral
    		.add(new IntSetting.Builder()
    		.name("Delay")
    		.description("Delay per ticks.")
    		.defaultValue(1)
    		.min(0)
    		.max(20)
    		.sliderMin(0)
    		.sliderMax(20)
    		.visible(() -> version.get() == Version.New || version.get() == Version.Old)
    		.build()
    		);
    
    private final Setting<Double> bpt = sgGeneral
    		.add(new DoubleSetting.Builder()
    		.name("BPT")
    		.description("Blocks per tick.")
    		.defaultValue(1)
    		.min(0)
    		.max(8)
    		.sliderMin(0)
    		.sliderMax(8)
    		.visible(() -> version.get() == Version.Interact)
    		.build()
    		);
    
    private final Setting<Boolean> airVelocity = sgGeneral.add(new BoolSetting.Builder()
			.name("air-velocity")
			.description("...")
			.defaultValue(true)
			.visible(() -> version.get() == Version.Interact)
			.build()
		);
    
	private final Setting<ecenter> center = sgGeneral.add(new EnumSetting.Builder<ecenter>()
		.name("centerTP")
		.description("Teleport to center block.")
		.defaultValue(ecenter.legit)
		.visible(() -> version.get() == Version.New || version.get() == Version.Old)
		.build()
	);
	
    
	private final Setting<SurrMode> mode = sgGeneral.add(new EnumSetting.Builder<SurrMode>()
			.name("Mode")
			.description("Mode of the surround.")
			.defaultValue(SurrMode.Normal)
			.visible(() -> version.get() == Version.New || version.get() == Version.Old)
			.build()
	);
	
	
	private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());
    
	private final Setting<antcry> anti = sgGeneral.add(new EnumSetting.Builder<antcry>()
			.name("anti-crystal-aura")
			.description("Anti Break your surround(place ender-chests).")
			.defaultValue(antcry.Yes)
			.visible(() -> version.get() == Version.New || version.get() == Version.Old)
			.build()
		);
	
	private final Setting<Boolean> selfProtector = sgGeneral.add(new BoolSetting.Builder()
			.name("self-protector")
			.description("Automatically breaks crystal near ur surround.")
			.defaultValue(true)
			.build()
		);
	
	private final Setting<Boolean> anticev = sgGeneral.add(new BoolSetting.Builder()
			.name("anti-cev-breaker")
			.description("Placing block 2 blocks up from your head.")
			.defaultValue(false)
			.visible(() -> version.get() == Version.New || version.get() == Version.Old)
			.build()
		);

	private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
		.name("only-on-ground")
		.description("Works only when you standing on blocks.")
		.defaultValue(false)
		.visible(() -> version.get() == Version.New || version.get() == Version.Old)
		.build()
	);
	
	private final Setting<Boolean> disableOnJump = sgGeneral.add(new BoolSetting.Builder()
		.name("disable-on-jump")
		.description("Automatically disables when you jump.")
		.defaultValue(true)
		.visible(() -> version.get() == Version.New || version.get() == Version.Old)
		.build()
	);
	
	private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
		.name("rotate")
		.description("Automatically faces towards the obsidian being placed.")
		.defaultValue(false)
		.build());

	private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
			.name("render")
			.description("Render surround blocks.")
			.defaultValue(true)
			.build());

	   private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
	            .name("shape-mode")
	            .description("How the shapes are rendered.")
	            .defaultValue(ShapeMode.Lines)
	            .build()
	    );

	    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
	            .name("side-color")
	            .description("The side color.")
	            .defaultValue(new SettingColor(255, 255, 255, 75))
	            .build()
	    );

	    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
	            .name("line-color")
	            .description("The line color.")
	            .defaultValue(new SettingColor(255, 255, 255, 255))
	            .build()
	    );
	
	public ExtraSurround() {
		super(Zeon.Combat, "surround-plus", "Surround+");
	}
    private static final MinecraftClient mc = MinecraftClient.getInstance();

	
    private int ticks;
    private PlayerEntity target;
	BlockPos pos = null;
	private int oldSlot;
    private double startPos;
    private boolean onEchest;
    
    private final ArrayList<Vec3d> norm = new ArrayList<Vec3d>() {{
    	add(new Vec3d(0, -1, 0));
    	add(new Vec3d(1, 0, 0));
        add(new Vec3d(-1, 0, 0));
        add(new Vec3d(0, 0, 1));
        add(new Vec3d(0, 0, -1));
    }};
    
    private final ArrayList<Vec3d> doub = new ArrayList<Vec3d>() {{
    	add(new Vec3d(0, -1, 0));
    	add(new Vec3d(1, 0, 0));
        add(new Vec3d(-1, 0, 0));
        add(new Vec3d(0, 0, 1));
        add(new Vec3d(0, 0, -1));
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};
    
    
    private final ArrayList<Vec3d> full = new ArrayList<Vec3d>() {{
    	add(new Vec3d(0, -1, 0));
    	add(new Vec3d(1, 0, 0));
        add(new Vec3d(-1, 0, 0));
        add(new Vec3d(0, 0, 1));
        add(new Vec3d(0, 0, -1));
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
        add(new Vec3d(1, 0, 1));
    	add(new Vec3d(-1, 0, 1));
        add(new Vec3d(-1, 0, -1));
        add(new Vec3d(1, 0, -1));
        add(new Vec3d(2, 0, 0));
        add(new Vec3d(0, 0, 2));
        add(new Vec3d(-2, 0, 0));
        add(new Vec3d(0, 0, -2));
        add(new Vec3d(0, 2, 0));
        add(new Vec3d(1, 2, 0));
        add(new Vec3d(0, 2, 1));
        add(new Vec3d(-1, 2, 0));
        add(new Vec3d(0, 2, -1));
        add(new Vec3d(0, 3, 0));
    }};
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
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
    	if(version.get() != Version.Interact)
    	{
        ticks = 0;
        if(center.get() == ecenter.fast){
	    	double tx=0,tz=0;
	
	    	Vec3d p = mc.player.getPos(); 
	    	
		   	 if (p.x>0 && gp(p.x)<3) tx=0.3;
			 if (p.x>0 && gp(p.x)>6) tx=-0.3;
			 if (p.x<0 && gp(p.x)<3) tx=-0.3;
			 if (p.x<0 && gp(p.x)>6) tx=0.3;
		
			 if (p.z>0 && gp(p.z)<3) tz=0.3;
			 if (p.z>0 && gp(p.z)>6) tz=-0.3;
			 if (p.z<0 && gp(p.z)<3) tz=-0.3;
			 if (p.z<0 && gp(p.z)>6) tz=0.3;
		
			 if(tx!=0 || tz!=0){
		    	 double posx = mc.player.getX() + tx;
		         double posz = mc.player.getZ() + tz;
		         mc.player.updatePosition(posx, mc.player.getY(), posz);
		         mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
		    }
        }
    	}
    	else
    	{
    		oldSlot = mc.player.getInventory().selectedSlot;
            startPos = mc.player.getY();

            if (center.get() == ecenter.legit) {
                double playerX = Math.floor(mc.player.getX());
                double playerZ = Math.floor(mc.player.getZ());
                mc.player.updatePosition(playerX + 0.5, mc.player.getY(), playerZ + 0.5);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(playerX + 0.5, mc.player.getY(), playerZ + 0.5, mc.player.isOnGround()));
            }
    	}
    }


    private long gp(double v) {
    	   BigDecimal v1 = BigDecimal.valueOf(v);
	       BigDecimal v2 = v1.remainder(BigDecimal.ONE);
	       return Byte.valueOf(String.valueOf(String.valueOf(v2).replace("0.", "").replace("-", "").charAt(0)));
    }
    
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (ticks >= tickDelay.get()) {
            ticks = 0;
            
            if(center.get() == ecenter.legit){
        	
	    	double tx=0,tz=0;
	    	Vec3d p = mc.player.getPos(); 
		   	 if (p.x>0 && gp(p.x)<3) tx=0.185;
			 if (p.x>0 && gp(p.x)>6) tx=-0.185;
			 if (p.x<0 && gp(p.x)<3) tx=-0.185;
			 if (p.x<0 && gp(p.x)>6) tx=0.185;
		
			 if (p.z>0 && gp(p.z)<3) tz=0.185;
			 if (p.z>0 && gp(p.z)>6) tz=-0.185;
			 if (p.z<0 && gp(p.z)<3) tz=-0.185;
			 if (p.z<0 && gp(p.z)>6) tz=0.185;	

		
			 if(tx!=0 || tz!=0){
		    	 double posx = mc.player.getX() + tx;
		         double posz = mc.player.getZ() + tz;
		         mc.player.updatePosition(posx, mc.player.getY(), posz);
		         mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
		         return;
		    }
        }
    	
    	
        if (disableOnJump.get() && mc.options.keyJump.isPressed()) {
            toggle();
            return;
        }

        if (onlyOnGround.get() && !mc.player.isOnGround()) return;


  if(version.get() == Version.New) {
    if(mode.get() == SurrMode.Normal) {
	 if(p(0, -1, 0)) return;
	if(p(1, 0, 0)) return;
        if(p(-1, 0, 0)) return;
        if(p(0, 0, 1)) return;
        if(p(0, 0, -1)) return;
        if(anticev.get() == true) {
       	 if(p(1, 1, 0)) return;
            if(p(0, 1, 1)) return;
            if(p(-1, 1, 0)) return;
            if(p(0, 1, -1)) return;
            if(p(0, 2, 0)) return;
            if(p(0, 3, 0)) return;
            
       }
        if(anti.get() == antcry.Yes) {
            if(e(1, -1, 0)) return;
            if(e(-1, -1, 0)) return;
            if(e(0, -1, 1)) return;
            if(e(0, -1, -1)) return;
    }
       else if(anti.get() == antcry.No) {
    	   if(p(1, -1, 0)) return;
           if(p(-1, -1, 0)) return;
           if(p(0, -1, 1)) return;
           if(p(0, -1, -1)) return;
       }
}
   
   
    if(mode.get() == SurrMode.Full) {
    	    if(p(0, -1, 0)) return;
    	    if(p(1, 0, 0)) return;
            if(p(-1, 0, 0)) return;
            if(p(0, 0, 1)) return;
            if(p(0, 0, -1)) return;
        	if(p(0, -1, 0)) return; 	
        	if(p(0, -2, 0)) return;
            if(p(1, 0, 1)) return;
            if(p(-1, 0, -1)) return;
            if(p(-1, 0, 1)) return;
            if(p(1, 0, -1)) return;
            if(p(2, 0, 0)) return;
            if(p(-2, 0, 0)) return;
            if(p(0, 0, 2)) return;
            if(p(0, 0, -2)) return;
            if(p(1, 1, 0)) return;
            if(p(-1, 1, 0)) return;
            if(p(0, 1, 1)) return;
            if(p(0, 1, -1)) return;
            if(p(1, 2, 0)) return;
            if(p(0, 2, 1)) return;
            if(p(-1, 2, 0)) return;
            if(p(0, 2, -1)) return;
            if(p(0, 3, 0)) return;
            if(p(1, 2, 0)) return;
            if(p(0, 2, 0)) return;
        
            if(anticev.get() == true) {
            	 if(p(1, 1, 0)) return;
                 if(p(0, 1, 1)) return;
                 if(p(-1, 1, 0)) return;
                 if(p(0, 1, -1)) return;
                 if(p(0, 2, 0)) return;
                 if(p(0, 3, 0)) return;
                 
            }
            if(anti.get() == antcry.Yes) {


                if(e(1, -1, 0)) return;
                if(e(-1, -1, 0)) return;
                if(e(0, -1, 1)) return;
                if(e(0, -1, -1)) return;
        }
           else if(anti.get() == antcry.No) {
        	   if(p(1, -1, 0)) return;
               if(p(-1, -1, 0)) return;
               if(p(0, -1, 1)) return;
               if(p(0, -1, -1)) return;
           }

    }

        if (mode.get() == SurrMode.Double) {
        	if(p(0, -1, 0)) return;
        	if(p(1, 0, 0)) return;
            if(p(-1, 0, 0)) return;
            if(p(0, 0, 1)) return;
            if(p(0, 0, -1)) return;
            if(p(1, 1, 0)) return;
            if(p(-1, 1, 0)) return;
            if(p(0, 1, 1)) return;
            if(p(0, 1, -1)) return;
            
            if(anticev.get() == true) {
           	 if(p(1, 1, 0)) return;
                if(p(0, 1, 1)) return;
                if(p(-1, 1, 0)) return;
                if(p(0, 1, -1)) return;
                if(p(0, 2, 0)) return;
                if(p(0, 3, 0)) return;
                
           }
            if(anti.get() == antcry.Yes) {
                if(e(1, -1, 0)) return;
                if(e(-1, -1, 0)) return;
                if(e(0, -1, 1)) return;
                if(e(0, -1, -1)) return;
        }
           else if(anti.get() == antcry.No) {
        	   if(p(1, -1, 0)) return;
               if(p(-1, -1, 0)) return;
               if(p(0, -1, 1)) return;
               if(p(0, -1, -1)) return;
           }
        }
        
      
           

  }else if(version.get() == Version.Old) {
	  if (disableOnJump.get() && mc.options.keyJump.isPressed()) {
          toggle();
          return;
      }
      if (onlyOnGround.get() && !mc.player.isOnGround()) return;
      if (isVecComplete(getSurrDesign())) {
      } else {
          BlockPos ppos = mc.player.getBlockPos();
          for (Vec3d b : getSurrDesign()) {
              BlockPos bb = ppos.add(b.x, b.y, b.z);
              if (getBlock(bb) == Blocks.AIR) {
                  if (selfProtector.get()) {
                      BlockUtils.place(bb, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 100, false);
                  }
                 
              }
          }
      }
  } else 
  {
	  if (disableOnJump.get() && mc.options.keyJump.isPressed()) {
          toggle();
          return;
      }

      if (InvUtils.findInHotbar(Items.OBSIDIAN).getSlot() == -1) return;

      onEchest = mc.player.getY() < Math.round(mc.player.getY());

      placeBlocks();

  }
        }else ticks++;
        if(version.get() == Version.New || version.get() == Version.Old)
        {
        if(selfProtector.get() == true) {
        for(Entity entity : mc.world.getEntities()) {
        	if (entity instanceof EndCrystalEntity) {
        		int slot1;
        	    slot1 = InvUtils.findInHotbar(Items.OBSIDIAN).getSlot();
                BlockPos crystalPos = entity.getBlockPos();
                if (isDangerousCrystal(crystalPos)) {
                	mc.interactionManager.attackEntity(mc.player, entity);
                    entity.remove(RemovalReason.KILLED);;
                    Ezz.BlockPlace(crystalPos, slot1, rotate.get());
                    return;
                }
            }
        };
    }
    } else
    {
    	if(selfProtector.get() == true) {
            for(Entity entity : mc.world.getEntities()) {
            	if (entity instanceof EndCrystalEntity) {
                    BlockPos crystalPos = entity.getBlockPos();
                    if (isDangerousCrystal(crystalPos)) {
                    	mc.interactionManager.attackEntity(mc.player, entity);
                        entity.remove(RemovalReason.KILLED);;
                        Vec3d vec = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                        if (rotate.get()) BlockUtilsWorld.rotateEnt(entity);
                        Ezz.swap(InvUtils.findInHotbar(Items.OBSIDIAN).getSlot());
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(
                                vec, Direction.DOWN, entity.getBlockPos(), true
                        ));
                        Ezz.swap(oldSlot);
                        return;
                    }
                }
            };
        }
    }
    }
    
    private ArrayList<Vec3d> getSurrDesign() {
        ArrayList<Vec3d> surrDesign = new ArrayList<Vec3d>(norm);
        if (mode.get() == SurrMode.Double) surrDesign.addAll(doub);
        if (mode.get() == SurrMode.Full) surrDesign.addAll(full);
        return surrDesign;
    }

    @EventHandler
    private void onRender(Render3DEvent e) {
        if (!render.get()) return;
        if(mc.player.getBlockPos().south() != null) e.renderer.box( mc.player.getBlockPos().south(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if(mc.player.getBlockPos().west() != null) e.renderer.box( mc.player.getBlockPos().west(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if(mc.player.getBlockPos().north() != null) e.renderer.box( mc.player.getBlockPos().north(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if(mc.player.getBlockPos().east() != null) e.renderer.box( mc.player.getBlockPos().east(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
    
    private boolean isDangerousCrystal(BlockPos bp) {
        BlockPos ppos = mc.player.getBlockPos();
        for (Vec3d b : getSurrDesign()) {
            BlockPos bb = ppos.add(b.x, b.y, b.z);
            if (!bp.equals(bb) && distanceBetween(bb, bp) <= 1) return true;
        }
        return false;
    }
    
    private boolean p(int x, int y, int z) {
    	return Ezz.BlockPlace(Ezz.SetRelative(x, y, z), InvUtils.findInHotbar(Items.OBSIDIAN).getSlot(), rotate.get());
    
    }
    private boolean e(int x, int y, int z) {
    	return Ezz.BlockPlace(Ezz.SetRelative(x, y, z), InvUtils.findInHotbar(Items.ENDER_CHEST).getSlot(), rotate.get());
    }
    
    public static double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }
    
    public static boolean isVecComplete(ArrayList<Vec3d> vlist) {
        BlockPos ppos = mc.player.getBlockPos();
        for (Vec3d b: vlist) {
            BlockPos bb = ppos.add(b.x, b.y, b.z);
            if (getBlock(bb) == Blocks.AIR) return false;
        }
        return true;
    }
    
    public static Block getBlock(BlockPos p) {
        if (p == null) return null;
        return mc.world.getBlockState(p).getBlock();
    }
    
    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN;
               
    }
    
    private void placeBlocks() {
        int j = 0;
        for (BlockPos b : new BlockPos[]{
                new BlockPos(mc.player.getBlockPos().down().getX(), mc.player.getBlockPos().down().getY(), mc.player.getBlockPos().down().getZ()),
                new BlockPos(mc.player.getBlockPos().north().getX(), mc.player.getBlockPos().north().getY() + (onEchest ? 1 : 0), mc.player.getBlockPos().north().getZ()),
                new BlockPos(mc.player.getBlockPos().east().getX(), mc.player.getBlockPos().east().getY() + (onEchest ? 1 : 0), mc.player.getBlockPos().east().getZ()),
                new BlockPos(mc.player.getBlockPos().south().getX(), mc.player.getBlockPos().south().getY() + (onEchest ? 1 : 0), mc.player.getBlockPos().south().getZ()),
                new BlockPos(mc.player.getBlockPos().west().getX(), mc.player.getBlockPos().west().getY() + (onEchest ? 1 : 0), mc.player.getBlockPos().west().getZ())}) {

            if (j >= bpt.get()) {
                return;
            }

            if (mc.world.getBlockState(b).getBlock() instanceof AirBlock) {

                InvUtils.swap(InvUtils.findInHotbar(Items.OBSIDIAN).getSlot(), true);

                if (!mc.player.isOnGround() && airVelocity.get()) {
                    mc.player.setVelocity(0, 0, 0);
                }

                Vec3d vec = new Vec3d(b.getX(), b.getY(), b.getZ());

                if (rotate.get()) BlockUtilsWorld.rotateBl(b);

                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(
                        vec, Direction.DOWN, b, true
                ));
                j++;
            }
        }
    }
}