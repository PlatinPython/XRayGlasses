package platinpython.xrayglasses;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.Set;

public final class FakeLevel implements BlockAndTintGetter {
    private final Level level;
    private final Long2ObjectMap<BlockState> states = new Long2ObjectArrayMap<>();

    public FakeLevel(Level level, Set<BlockPos> set) {
        this.level = level;
        set.forEach(pos -> states.put(pos.asLong(), level.getBlockState(pos)));
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos pos) {
        return 15;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        long packedPos = pos.asLong();
        if (states.containsKey(packedPos)) {
            return IClientFluidTypeExtensions.of(states.get(packedPos).getFluidState()).getTintColor();
        }
        return -1;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return states.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return states.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState()).getFluidState();
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }
}
