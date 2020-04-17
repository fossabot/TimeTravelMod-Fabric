package com.rdvdev2.TimeTravelMod.common.timemachine;

import com.rdvdev2.TimeTravelMod.api.timemachine.exception.IncompatibleTimeMachineHooksException;
import com.rdvdev2.TimeTravelMod.api.timemachine.upgrade.TimeMachineHook;
import com.rdvdev2.TimeTravelMod.api.timemachine.upgrade.TimeMachineUpgrade;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TimeMachineHookRunner implements com.rdvdev2.TimeTravelMod.api.timemachine.TimeMachine {

    com.rdvdev2.TimeTravelMod.api.timemachine.TimeMachine tm;
    private final Map<TimeMachineUpgrade, HashSet<BlockPos>> upgrades;

    public TimeMachineHookRunner(com.rdvdev2.TimeTravelMod.api.timemachine.TimeMachine tm, Map<TimeMachineUpgrade, HashSet<BlockPos>> upgrades) {
        this.tm = tm;
        this.upgrades = upgrades;
    }

    public com.rdvdev2.TimeTravelMod.api.timemachine.TimeMachine removeHooks() {
        return this.tm;
    }

    public HashSet<TimeMachineUpgrade> checkIncompatibilities() {

        HashSet<TimeMachineUpgrade> incompatibilities = new HashSet<>(0);
        for (Class<? extends TimeMachineHook<?>> hook : TimeMachineHook.HOOK_TYPES) {
            Set<TimeMachineUpgrade> found = this.upgrades.keySet().stream()
                    .map(u -> (com.rdvdev2.TimeTravelMod.common.timemachine.upgrade.TimeMachineUpgrade) u)
                    .filter(u -> u.isExclusiveHook(hook))
                    .collect(Collectors.toSet());
            if (found.size() > 1) incompatibilities.addAll(found);
        }
        return incompatibilities;
    }

    @Override
    public int getCooldownTime() {
        return runHooks(tm::getCooldownTime, TimeMachineHook.CooldownHook.class);
    }

    @Override
    public int getTier() {
        return runHooks(tm::getTier, TimeMachineHook.TierHook.class);
    }

    @Override
    public List<BlockPos> coreBlocksPos() {
        return tm.coreBlocksPos(); // Directly delegate, this isn't in the Upgrades scope
    }

    @Override
    public List<BlockPos> basicBlocksPos() {
        return tm.basicBlocksPos(); // Directly delegate, this isn't in the Upgrades scope
    }

    @Override
    public List<BlockPos> airBlocksPos() {
        return tm.airBlocksPos(); // Directly delegate, this isn't in the Upgrades scope
    }

    @Override
    public BlockState[] getControllerBlocks() {
        return tm.getControllerBlocks(); // Directly delegate, this isn't in the Upgrades scope
    }

    @Override
    public BlockState[] getCoreBlocks() {
        return tm.getCoreBlocks(); // Directly delegate, this isn't in the Upgrades scope
    }

    @Override
    public BlockState[] getBasicBlocks() {
        return tm.getBasicBlocks(); // Directly delegate, this isn't in the Upgrades scope
    }

    @Override
    public int getEntityMaxLoad() {
        return runHooks(tm::getEntityMaxLoad, TimeMachineHook.EntityMaxLoadHook.class);
    }

    @Override
    public void run(World world, PlayerEntity playerIn, BlockPos controllerPos, Direction side) {
        runVoidHooks(() -> tm.run(world, playerIn, controllerPos, side), TimeMachineHook.RunHook.class, world, playerIn, controllerPos, side);
    }

    @Override
    public boolean triggerTemporalExplosion(World world, BlockPos controllerPos, Direction side) {
        return runHooks(() -> tm.triggerTemporalExplosion(world, controllerPos, side), TimeMachineHook.TriggerTemporalExplosionHook.class, world, controllerPos, side);
    }

    @Override
    public void teleporterTasks(@Nullable Entity entity, World worldIn, World worldOut, BlockPos controllerPos, Direction side, boolean shouldBuild) {
        runVoidHooks(() -> tm.teleporterTasks(entity, worldIn, worldOut, controllerPos, side, shouldBuild), TimeMachineHook.TeleporterTasks.class, entity, worldIn, worldOut, controllerPos, side, shouldBuild);
    }

    @Override
    public List<BlockPos> getCoreBlocksPos(Direction side) {
        return tm.getCoreBlocksPos(side);
    }

    @Override
    public List<BlockPos> getBasicBlocksPos(Direction side) {
        return tm.getBasicBlocksPos(side);
    }

    @Override
    public List<BlockPos> getAirBlocksPos(Direction side) {
        return tm.getAirBlocksPos(side);
    }

    @Override
    public BlockState[] getBlocks() {
        return tm.getBlocks();
    }

    @Override
    public boolean isBuilt(World world, BlockPos controllerPos, Direction side) {
        return tm.isBuilt(world, controllerPos, side);
    }

    @Override
    public boolean isCooledDown(World world, BlockPos controllerPos, Direction side) {
        return tm.isCooledDown(world, controllerPos, side);
    }

    @Override
    public boolean isOverloaded(World world, BlockPos controllerPos, Direction side) {
        return tm.isOverloaded(world, controllerPos, side);
    }

    @Override
    public boolean isPlayerInside(World world, BlockPos controllerPos, Direction side, PlayerEntity player) {
        return tm.isPlayerInside(world, controllerPos, side, player);
    }

    @Override
    public Box getAirSpace(BlockPos controllerPos, Direction side) {
        return tm.getAirSpace(controllerPos, side);
    }

    @Override
    public int getCorruptionMultiplier() {
        return tm.getCorruptionMultiplier();
    }

    @Override
    public String toString() {
        return tm.toString();
    }

    private <T> T runHooks(Supplier<T> original, Class<? extends TimeMachineHook<T>> clazz, Object... args) {
        for(TimeMachineUpgrade upgrade:upgrades.keySet()) {
            if (((com.rdvdev2.TimeTravelMod.common.timemachine.upgrade.TimeMachineUpgrade) upgrade).isExclusiveHook(clazz)) {
                return ((com.rdvdev2.TimeTravelMod.common.timemachine.upgrade.TimeMachineUpgrade) upgrade).runHook(Optional.empty(), clazz, this, args);
            }
        }
        T result = original.get();
        for (TimeMachineUpgrade upgrade:upgrades.keySet()) {
            result = ((com.rdvdev2.TimeTravelMod.common.timemachine.upgrade.TimeMachineUpgrade) upgrade).runHook(Optional.of(result), clazz, this, args);
        }
        return result;
    }

    private void runVoidHooks(Runnable original, Class<? extends TimeMachineHook<Void>> clazz, Object... args) {
        for(TimeMachineUpgrade upgrade:upgrades.keySet()) {
            if (((com.rdvdev2.TimeTravelMod.common.timemachine.upgrade.TimeMachineUpgrade) upgrade).isExclusiveHook(clazz)) {
                ((com.rdvdev2.TimeTravelMod.common.timemachine.upgrade.TimeMachineUpgrade) upgrade).runVoidHook(clazz, this, args);
                return;
            }
        }
        boolean done = false;
        for(TimeMachineUpgrade upgrade:upgrades.keySet()) {
            if (((com.rdvdev2.TimeTravelMod.common.timemachine.upgrade.TimeMachineUpgrade) upgrade).runVoidHook(clazz, this, args))
                done = true;
        }
        if (!done) original.run();
    }

    public HashSet<BlockPos> getUpgradePos(TimeMachineUpgrade upgrade) {
        return upgrades.get(upgrade);
    }

    @Override
    public TranslatableText getName() {
        return tm.getName();
    }

    @Override
    public TranslatableText getDescription() {
        return tm.getDescription();
    }

    @Override
    public BlockState[] getUpgradeBlocks() {
        return tm.getUpgradeBlocks();
    }

    @Override
    public TimeMachineUpgrade[] getCompatibleUpgrades() {
        return tm.getCompatibleUpgrades();
    }

    @Override
    public com.rdvdev2.TimeTravelMod.api.timemachine.TimeMachine hook(World world, BlockPos controllerPos, Direction side) throws IncompatibleTimeMachineHooksException {
        return this;
    }

    @Override
    public Map<TimeMachineUpgrade, HashSet<BlockPos>> getUpgrades(World world, BlockPos controllerPos, Direction side) {
        return this.upgrades;
    }

    @Override
    public List<Entity> getEntitiesInside(World world, BlockPos controllerPos, Direction side) {
        return tm.getEntitiesInside(world, controllerPos, side);
    }

    @Override
    public void doCooldown(World worldIn, BlockPos controllerPos, Direction side) {
        tm.doCooldown(worldIn, controllerPos, side);
    }
}
