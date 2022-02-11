package dextro.zeon.addon.modules;

import net.minecraft.entity.Entity;

public class EntitySpawnEvent {

    public Entity entity;

    public EntitySpawnEvent(Entity entity) {
        this.entity = entity;
    }

}