package com.moblaunch.plugin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * 当玩家蓄力结束准备投掷生物时触发
 * 可以取消投掷，或者修改投掷的速度向量
 */
public class MobLaunchEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Player player;
    private final Entity entity;
    private Vector velocity;

    public MobLaunchEvent(Player player, Entity entity, Vector velocity) {
        this.player = player;
        this.entity = entity;
        this.velocity = velocity;
    }

    public Player getPlayer() {
        return player;
    }

    public Entity getEntity() {
        return entity;
    }

    /**
     * 获取即将应用的投掷速度向量
     */
    public Vector getVelocity() {
        return velocity;
    }

    /**
     * 修改投掷速度向量
     */
    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}