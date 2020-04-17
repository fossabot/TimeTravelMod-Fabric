package com.rdvdev2.TimeTravelMod.common.world.dimension.oldwest;

import com.rdvdev2.TimeTravelMod.ModBiomes;
import com.rdvdev2.TimeTravelMod.api.dimension.AbstractTimeLineDimension;
import com.rdvdev2.TimeTravelMod.common.world.dimension.oldwest.biome.OldWestBiomeSource;
import net.minecraft.block.BlockState;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSourceType;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;

public class OldWestDimension extends AbstractTimeLineDimension { // TODO: Fix beds
    private DimensionType type;
    private World world;

    public OldWestDimension(World world, DimensionType type) {
        super(world, type, 0.0F);
        this.world = world;
        this.type = type;
    }

    @Override
    public boolean hasSkyLight() {
        return true;
    }

    
    
    @Override
    public ChunkGenerator<?> createChunkGenerator() {
        BiomeSourceType<VanillaLayeredBiomeSourceConfig, OldWestBiomeSource> oldwestLayered = ModBiomes.ProviderTypes.OLDWEST_LAYERED;
        VanillaLayeredBiomeSourceConfig oldWestBiomeSourceSettings = oldwestLayered.getConfig(this.world.getLevelProperties()).setGeneratorSettings(new OverworldChunkGeneratorConfig());
        return ChunkGeneratorType.SURFACE.create(this.world, oldwestLayered.applyConfig(oldWestBiomeSourceSettings), new OverworldChunkGeneratorConfig());
    }
    
    @Override
    public BlockPos getSpawningBlockInChunk(ChunkPos chunkPos, boolean checkValid) {
        for(int i = chunkPos.getStartX(); i <= chunkPos.getEndX(); ++i) {
            for(int j = chunkPos.getStartZ(); j <= chunkPos.getEndZ(); ++j) {
                BlockPos blockpos = this.getTopSpawningBlockPosition(i, j, checkValid);
                if (blockpos != null) {
                    return blockpos;
                }
            }
        }

        return null;
    }
    
    @Override
    public BlockPos getTopSpawningBlockPosition(int p_206921_1_, int p_206921_2_, boolean checkValid) {
        BlockPos.Mutable blockpos$mutableblockpos = new BlockPos.Mutable(p_206921_1_, 0, p_206921_2_);
        Biome biome = this.world.getBiome(blockpos$mutableblockpos);
        BlockState iblockstate = biome.getSurfaceConfig().getTopMaterial();
        if (checkValid && !BlockTags.VALID_SPAWN.contains(iblockstate.getBlock())) {
            return null;
        } else {
            Chunk chunk = this.world.getChunk(p_206921_1_ >> 4, p_206921_2_ >> 4);
            int i = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, p_206921_1_ & 15, p_206921_2_ & 15);
            if (i < 0) {
                return null;
            } else if (chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, p_206921_1_ & 15, p_206921_2_ & 15) > chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, p_206921_1_ & 15, p_206921_2_ & 15)) {
                return null;
            } else {
                for(int j = i + 1; j >= 0; --j) {
                    blockpos$mutableblockpos.set(p_206921_1_, j, p_206921_2_);
                    BlockState iblockstate1 = this.world.getBlockState(blockpos$mutableblockpos);
                    if (!iblockstate1.getFluidState().isEmpty()) {
                        break;
                    }

                    if (iblockstate1.equals(iblockstate)) {
                        return blockpos$mutableblockpos.up().toImmutable();
                    }
                }

                return null;
            }
        }
    }

    /**
     * Calculates the angle of sun and moon in the sky relative to a specified time (usually worldTime)
     *
     * @param worldTime
     * @param partialTicks
     */
    @Override
    public float getSkyAngle(long worldTime, float partialTicks) {
        int i = (int)(worldTime % 24000L);
        float f = ((float)i + partialTicks) / 24000.0F - 0.25F;
        if (f < 0.0F) {
            ++f;
        }

        if (f > 1.0F) {
            --f;
        }

        float f1 = 1.0F - (float)((Math.cos((double)f * Math.PI) + 1.0D) / 2.0D);
        f = f + (f1 - f) / 3.0F;
        return f;
    }
    
    @Override
    public boolean hasVisibleSky() {
        return true;
    }
    
    @Override
    public boolean hasGround() {
        return true;
    }
    
    /**
     * Return Vec3D with biome specific fog color
     *
     * @param p_76562_1_
     * @param p_76562_2_
     */
    @Override
    public Vec3d getFogColor(float p_76562_1_, float p_76562_2_) {
        float f = MathHelper.cos(p_76562_1_ * ((float)Math.PI * 2F)) * 2.0F + 0.5F;
        f = MathHelper.clamp(f, 0.0F, 1.0F);
        float f1 = 0.7529412F;
        float f2 = 0.84705883F;
        float f3 = 1.0F;
        f1 = f1 * (f * 0.94F + 0.06F);
        f2 = f2 * (f * 0.94F + 0.06F);
        f3 = f3 * (f * 0.91F + 0.09F);
        return new Vec3d((double)f1, (double)f2, (double)f3);
    }
    
    @Override
    public boolean canPlayersSleep() {
        return true;
    }
    
    /**
     * Returns true if the given X,Z coordinate should show environmental fog.
     *
     * @param x
     * @param z
     */
    @Override
    public boolean isFogThick(int x, int z) {
        return false;
    }
    
    @Override
    public DimensionType getType() {
        return this.type;
    }
}
