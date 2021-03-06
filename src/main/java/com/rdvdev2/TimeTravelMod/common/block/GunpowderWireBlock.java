package com.rdvdev2.TimeTravelMod.common.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GunpowderWireBlock extends Block {

    protected static final VoxelShape[] SHAPES = new VoxelShape[]{Block.createCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D), Block.createCuboidShape(3.0D, 0.0D, 3.0D, 13.0D, 1.0D, 16.0D), Block.createCuboidShape(0.0D, 0.0D, 3.0D, 13.0D, 1.0D, 13.0D), Block.createCuboidShape(0.0D, 0.0D, 3.0D, 13.0D, 1.0D, 16.0D), Block.createCuboidShape(3.0D, 0.0D, 0.0D, 13.0D, 1.0D, 13.0D), Block.createCuboidShape(3.0D, 0.0D, 0.0D, 13.0D, 1.0D, 16.0D), Block.createCuboidShape(0.0D, 0.0D, 0.0D, 13.0D, 1.0D, 13.0D), Block.createCuboidShape(0.0D, 0.0D, 0.0D, 13.0D, 1.0D, 16.0D), Block.createCuboidShape(3.0D, 0.0D, 3.0D, 16.0D, 1.0D, 13.0D), Block.createCuboidShape(3.0D, 0.0D, 3.0D, 16.0D, 1.0D, 16.0D), Block.createCuboidShape(0.0D, 0.0D, 3.0D, 16.0D, 1.0D, 13.0D), Block.createCuboidShape(0.0D, 0.0D, 3.0D, 16.0D, 1.0D, 16.0D), Block.createCuboidShape(3.0D, 0.0D, 0.0D, 16.0D, 1.0D, 13.0D), Block.createCuboidShape(3.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D), Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 13.0D), Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D)};
    public static final EnumProperty<WireConnection> NORTH = RedstoneWireBlock.WIRE_CONNECTION_NORTH;
    public static final EnumProperty<WireConnection> EAST = RedstoneWireBlock.WIRE_CONNECTION_EAST;
    public static final EnumProperty<WireConnection> SOUTH = RedstoneWireBlock.WIRE_CONNECTION_SOUTH;
    public static final EnumProperty<WireConnection> WEST = RedstoneWireBlock.WIRE_CONNECTION_WEST;
    public static final BooleanProperty BURNED = BooleanProperty.of("burned");
    public static final Map<Direction, EnumProperty<WireConnection>> FACING_PROPERTY_MAP = Maps.newEnumMap(ImmutableMap.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST));
    private final Set<BlockPos> blocksNeedingUpdate = Sets.newHashSet();

    public GunpowderWireBlock() {
        super(FabricBlockSettings.of(Material.PART).nonOpaque().hardness(0).collidable(false).build());
        this.setDefaultState(this.getStateManager().getDefaultState().with(NORTH, WireConnection.NONE).with(EAST, WireConnection.NONE).with(SOUTH, WireConnection.NONE).with(WEST, WireConnection.NONE).with(BURNED, false));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, EntityContext context) {
        return SHAPES[getAABBIndex(state)];
    }
    
    private static int getAABBIndex(BlockState state) {
        int ret = 0;
        boolean north = state.get(NORTH) != WireConnection.NONE;
        boolean east = state.get(EAST) != WireConnection.NONE;
        boolean south = state.get(SOUTH) != WireConnection.NONE;
        boolean west = state.get(WEST) != WireConnection.NONE;
        if (north || south && !north && !east && !west) {
            ret |= 1 << Direction.NORTH.getHorizontal();
        }

        if (east || west && !north && !east && !south) {
            ret |= 1 << Direction.EAST.getHorizontal();
        }

        if (south || north && !east && !south && !west) {
            ret |= 1 << Direction.SOUTH.getHorizontal();
        }

        if (west || east && !north && !south && !west) {
            ret |= 1 << Direction.WEST.getHorizontal();
        }

        return ret;
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        WorldView iblockreader = ctx.getWorld();
        BlockPos blockpos = ctx.getBlockPos();
        return this.getDefaultState().with(WEST, this.getSide(iblockreader, blockpos, Direction.WEST)).with(EAST, this.getSide(iblockreader, blockpos, Direction.EAST)).with(NORTH, this.getSide(iblockreader, blockpos, Direction.NORTH)).with(SOUTH, this.getSide(iblockreader, blockpos, Direction.SOUTH));
    }
    
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction facing, BlockState neighborState, IWorld world, BlockPos pos, BlockPos neighborPos) {
        if (facing == Direction.DOWN || state.get(BURNED)) {
            return state;
        } else {
            return facing == Direction.UP ? state.with(WEST, this.getSide(world, pos, Direction.WEST)).with(EAST, this.getSide(world, pos, Direction.EAST)).with(NORTH, this.getSide(world, pos, Direction.NORTH)).with(SOUTH, this.getSide(world, pos, Direction.SOUTH)) : state.with(FACING_PROPERTY_MAP.get(facing), this.getSide(world, pos, facing));
        }
    }
    
    @Override
    public void method_9517(BlockState state, IWorld world, BlockPos pos, int flags) {
        try (BlockPos.PooledMutable blockpos$pooledmutableblockpos = BlockPos.PooledMutable.get()) {
            for(Direction enumfacing : Direction.Type.HORIZONTAL) {
                WireConnection redstoneside = state.get(FACING_PROPERTY_MAP.get(enumfacing));
                if (redstoneside != WireConnection.NONE && world.getBlockState(blockpos$pooledmutableblockpos.set(pos).setOffset(enumfacing)).getBlock() != this) {
                    blockpos$pooledmutableblockpos.setOffset(Direction.DOWN);
                    BlockState iblockstate = world.getBlockState(blockpos$pooledmutableblockpos);
                    if (iblockstate.getBlock() != Blocks.OBSERVER) {
                        BlockPos blockpos = blockpos$pooledmutableblockpos.offset(enumfacing.getOpposite());
                        BlockState iblockstate1 = iblockstate.getStateForNeighborUpdate(enumfacing.getOpposite(), world.getBlockState(blockpos), world, blockpos$pooledmutableblockpos, blockpos);
                        replaceBlock(iblockstate, iblockstate1, world, blockpos$pooledmutableblockpos, flags);
                    }

                    blockpos$pooledmutableblockpos.set(pos).setOffset(enumfacing).setOffset(Direction.UP);
                    BlockState iblockstate3 = world.getBlockState(blockpos$pooledmutableblockpos);
                    if (iblockstate3.getBlock() != Blocks.OBSERVER) {
                        BlockPos blockpos1 = blockpos$pooledmutableblockpos.offset(enumfacing.getOpposite());
                        BlockState iblockstate2 = iblockstate3.getStateForNeighborUpdate(enumfacing.getOpposite(), world.getBlockState(blockpos1), world, blockpos$pooledmutableblockpos, blockpos1);
                        replaceBlock(iblockstate3, iblockstate2, world, blockpos$pooledmutableblockpos, flags);
                    }
                }
            }
        }
    }

    private WireConnection getSide(WorldView worldIn, BlockPos pos, Direction face) {
        BlockPos blockpos = pos.offset(face);
        BlockState blockstate = worldIn.getBlockState(blockpos);
        BlockState stateUp = worldIn.getBlockState(pos.up());
        if (!stateUp.isSimpleFullBlock(worldIn, pos.up())) {
            boolean flag = Block.isSideSolidFullSquare(blockstate, worldIn, blockpos, Direction.UP) || blockstate.getBlock() == Blocks.HOPPER;
            if (flag && canConnectTo(worldIn.getBlockState(blockpos.up()))) {
                if (blockstate.isFullCube(worldIn, blockpos)) {
                    return WireConnection.UP;
                }

                return WireConnection.SIDE;
            }
        }

        return !canConnectTo(blockstate) && (blockstate.isSimpleFullBlock(worldIn, blockpos) || !canConnectTo(worldIn.getBlockState(blockpos.down()))) ? WireConnection.NONE : WireConnection.SIDE;
    }
    
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState blockstate = world.getBlockState(pos.down());
        return Block.isSideSolidFullSquare(blockstate, world, pos, Direction.UP) || blockstate.getBlock() == Blocks.HOPPER;
    }

    private BlockState updateSurroundingWires(World worldIn, BlockPos pos, BlockState state) {
        List<BlockPos> list = Lists.newArrayList(this.blocksNeedingUpdate);
        this.blocksNeedingUpdate.clear();

        for(BlockPos blockpos : list) {
            worldIn.updateNeighborsAlways(blockpos, this);
        }

        return state;
    }

    private void notifyWireNeighborsOfStateChange(World worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock() == this) {
            worldIn.updateNeighborsAlways(pos, this);

            for(Direction enumfacing : Direction.values()) {
                worldIn.updateNeighborsAlways(pos.offset(enumfacing), this);
            }

        }
    }

    @Override
    public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean mistery) {
        if (oldState.getBlock() != state.getBlock() && !worldIn.isClient) {
            this.updateSurroundingWires(worldIn, pos, state);

            for(Direction facing : Direction.Type.VERTICAL) {
                worldIn.updateNeighborsAlways(pos.offset(facing), this);
            }

            for(Direction facing : Direction.Type.HORIZONTAL) {
                this.notifyWireNeighborsOfStateChange(worldIn, pos.offset(facing));
            }

            for(Direction facing : Direction.Type.HORIZONTAL) {
                BlockPos blockpos = pos.offset(facing);
                if (worldIn.getBlockState(blockpos).isSimpleFullBlock(worldIn, blockpos)) {
                    this.notifyWireNeighborsOfStateChange(worldIn, blockpos.up());
                } else {
                    this.notifyWireNeighborsOfStateChange(worldIn, blockpos.down());
                }
            }

        }
    }
    
    @Override
    public void onBlockRemoved(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved && state.getBlock() != newState.getBlock()) {
            super.onBlockRemoved(state, world, pos, newState, moved);
            if (!world.isClient) {
                for(Direction facing : Direction.values()) {
                    world.updateNeighborsAlways(pos.offset(facing), this);
                }

                this.updateSurroundingWires(world, pos, state);

                for(Direction facing : Direction.Type.HORIZONTAL) {
                    this.notifyWireNeighborsOfStateChange(world, pos.offset(facing));
                }

                for(Direction facing : Direction.Type.HORIZONTAL) {
                    BlockPos blockpos = pos.offset(facing);
                    if (world.getBlockState(blockpos).isSimpleFullBlock(world, blockpos)) {
                        this.notifyWireNeighborsOfStateChange(world, blockpos.up());
                    } else {
                        this.notifyWireNeighborsOfStateChange(world, blockpos.down());
                    }
                }

            }
        }
    }
    
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos neighborPos, boolean moved) {
        if (!world.isClient) {
            if (state.canPlaceAt(world, pos)) {
                this.updateSurroundingWires(world, pos, state);
            } else {
                dropStacks(state, world, pos);
                world.removeBlock(pos, false);
            }
        }
    }

    protected boolean canConnectTo(BlockState state) {
        return state.getBlock() == this || state.getBlock() == Blocks.TNT;
    }
    
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        switch(rotation) {
            case CLOCKWISE_180:
                return state.with(NORTH, state.get(SOUTH)).with(EAST, state.get(WEST)).with(SOUTH, state.get(NORTH)).with(WEST, state.get(EAST));
            case COUNTERCLOCKWISE_90:
                return state.with(NORTH, state.get(EAST)).with(EAST, state.get(SOUTH)).with(SOUTH, state.get(WEST)).with(WEST, state.get(NORTH));
            case CLOCKWISE_90:
                return state.with(NORTH, state.get(WEST)).with(EAST, state.get(NORTH)).with(SOUTH, state.get(EAST)).with(WEST, state.get(SOUTH));
            default:
                return state;
        }
    }
    
    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        switch(mirror) {
            case LEFT_RIGHT:
                return state.with(NORTH, state.get(SOUTH)).with(SOUTH, state.get(NORTH));
            case FRONT_BACK:
                return state.with(EAST, state.get(WEST)).with(WEST, state.get(EAST));
            default:
                return super.mirror(state, mirror);
        }
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, BURNED);
    }

    @Environment(EnvType.CLIENT)
    public static int colorMultiplier(boolean burned) {
        float red, blue, green;

        if (!burned) {
            return 0x989898;
        } else {
            return 0x262626;
        }
    }

    public void setBurned(BlockPos pos, World world) {
        BlockState state = world.getBlockState(pos);
        world.setBlockState(pos, state.with(BURNED, true));
        world.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 0);
        if (world.getBlockState(pos.down()) == Blocks.TNT.getDefaultState()) {
            world.setBlockState(pos.down(), Blocks.AIR.getDefaultState());
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            TntBlock.primeTnt(world, pos.down());
            return;
        }
        for (Direction facing: Direction.Type.HORIZONTAL) {
            BlockPos neighboorPos = pos;
            switch (state.get(FACING_PROPERTY_MAP.get(facing))) {
                case UP:
                    neighboorPos = neighboorPos.up();
                case SIDE:
                    neighboorPos = neighboorPos.offset(facing);
                    if (!canConnectTo(world.getBlockState(neighboorPos)))
                        neighboorPos = neighboorPos.down();
                    if (!canConnectTo(world.getBlockState(neighboorPos)))
                        break;
                    if (world.getBlockState(neighboorPos).getBlock() == this && !world.getBlockState(neighboorPos).get(BURNED))
                        setBurned(neighboorPos, world);
                    else if (world.getBlockState(neighboorPos).getBlock() == Blocks.TNT) {
                        world.setBlockState(neighboorPos, Blocks.AIR.getDefaultState());
                        TntBlock.primeTnt(world, neighboorPos);
                    }
            }
        }
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.getStackInHand(hand).getItem() == Items.FLINT_AND_STEEL && !state.get(BURNED)) {
            if (player instanceof ServerPlayerEntity)
            player.getStackInHand(hand).damage(1, world.random, (ServerPlayerEntity) player);
            setBurned(pos, world);
            player.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 5, 0);
            return ActionResult.SUCCESS;
        } else return ActionResult.PASS;
    }


}