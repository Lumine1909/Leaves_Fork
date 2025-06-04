package org.leavesmc.leaves.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;

public enum ForcePeacefulModeSwitchType {
    BLAZE(Blaze.class),
    CREEPER(Creeper.class),
    DROWNED(Drowned.class),
    ELDER_GUARDIAN(ElderGuardian.class),
    ENDERMAN(EnderMan.class),
    ENDERMITE(Endermite.class),
    EVOKER(Evoker.class),
    GHAST(Ghast.class),
    GIANT(Giant.class),
    GUARDIAN(Guardian.class),
    HOGLIN(Hoglin.class),
    HUSK(Husk.class),
    ILLUSIONER(Illusioner.class),
    MAGMA_CUBE(MagmaCube.class),
    PHANTOM(Phantom.class),
    PIGLIN(Piglin.class),
    PIGLIN_BRUTE(PiglinBrute.class),
    PILLAGER(Pillager.class),
    RAVAGER(Ravager.class),
    SHULKER(Shulker.class),
    SILVERFISH(Silverfish.class),
    SKELETON(Skeleton.class),
    SLIME(Slime.class),
    SPIDER(Spider.class),
    STRAY(Stray.class),
    VEX(Vex.class),
    VINDICATOR(Vindicator.class),
    WARDEN(Warden.class),
    WITCH(Witch.class),
    WITHER_SKELETON(WitherSkeleton.class),
    ZOGLIN(Zoglin.class),
    ZOMBIE(Zombie.class),
    ZOMBIE_VILLAGER(ZombieVillager.class),
    ZOMBIFIED_PIGLIN(ZombifiedPiglin.class),
    WITHER(WitherBoss.class),
    CAVE_SPIDER(CaveSpider.class),
    BREEZE(Breeze.class),
    BOGGED(Bogged.class);

    private final Class<? extends Entity> entityClass;

    ForcePeacefulModeSwitchType(Class<? extends Entity> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<? extends Entity> getEntityClass() {
        return entityClass;
    }
}
