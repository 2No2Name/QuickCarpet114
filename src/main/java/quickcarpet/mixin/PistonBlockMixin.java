package quickcarpet.mixin;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import quickcarpet.annotation.Feature;
import quickcarpet.utils.CarpetRegistry;
import quickcarpet.utils.IBlockState;
import quickcarpet.utils.IPistonBlockEntity;
import quickcarpet.utils.PistonBehaviors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Feature("movableBlockEntities")
@Mixin(PistonBlock.class)
public class PistonBlockMixin extends FacingBlock {

    protected PistonBlockMixin(Settings block$Settings_1) {
        super(block$Settings_1);
    }

    private ThreadLocal<List<BlockEntity>> list1_BlockEntities = new ThreadLocal<>(); //Unneccessary ThreadLocal if client and server use different PistonBlock instances

    @Feature("autoCraftingTable")
    @Inject(method = "isMovable", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;hasBlockEntity()Z"),
            cancellable = true)
    private static void craftingTableMoveable(BlockState state, World world, BlockPos pos, Direction pistonDirection,
                                              boolean allowDestroy, Direction moveDirection, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof CraftingTableBlock) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Feature("additionalMovableBlocks")
    @Inject(method = "isMovable", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;hasBlockEntity()Z"),
            cancellable = true)
    //Blocks overwritten to be pushable will be pushable without not hasBlockEntity check.
    private static void additionalBlocksMovable(BlockState state, World world, BlockPos pos, Direction pistonDirection,
                                                boolean allowDestroy, Direction moveDirection, CallbackInfoReturnable<Boolean> cir) {
        if (quickcarpet.settings.Settings.movableBlockOverrides && CarpetRegistry.PISTON_OVERRIDE_MOVABLE.contains(state.getBlock())) {
            cir.setReturnValue(true);
        }
    }

    @Feature("additionalMovableBlocks")
    @Inject(method = "isMovable", at = @At(value = "RETURN", ordinal = 3, shift = At.Shift.BEFORE))
    private static void additionalBlocksMovable2(BlockState blockState_1, World world_1, BlockPos blockPos_1, Direction direction_1,
                                                 boolean allowDestroy, Direction direction_2, CallbackInfoReturnable<Boolean> cir) {
        if(quickcarpet.settings.Settings.movableBlockOverrides){
            PistonBehavior override = IBlockState.getOverridePistonBehavior((IBlockState) blockState_1);
            if(override != null){
                boolean ret = (override == PistonBehavior.NORMAL) ||
                        (override == PistonBehavior.PUSH_ONLY && direction_1 == direction_2) ||
                        (override == PistonBehavior.DESTROY && allowDestroy) ||
                        (override == PistonBehaviors.WEAK_STICKY) ||
                        (override == PistonBehaviors.WEAK_STICKY_BREAKABLE);// && allowDestroy);
                cir.setReturnValue(ret);
            }
        }
    }

    @Feature("additionalMovableBlocks")
    @Inject(method = "isMovable", at = @At(value = "RETURN", ordinal = 0, shift = At.Shift.BEFORE))
    private static void additionalObsidianMovable(BlockState blockState_1, World world_1, BlockPos blockPos_1, Direction direction_1, boolean allowDestroy, Direction direction_2, CallbackInfoReturnable<Boolean> cir) {
        if(quickcarpet.settings.Settings.movableBlockOverrides){

            if ((!world_1.getWorldBorder().contains(blockPos_1)) || (blockPos_1.getY() < 0 || direction_1 == Direction.DOWN && blockPos_1.getY() == 0)) {
                return; //return false
            }

            PistonBehavior override = IBlockState.getOverridePistonBehavior((IBlockState) blockState_1);
            if(override != null){
                boolean ret = (override == PistonBehavior.NORMAL) ||
                                (override == PistonBehavior.PUSH_ONLY && direction_1 == direction_2) ||
                                (override == PistonBehavior.DESTROY && allowDestroy) ||
                                (override == PistonBehaviors.WEAK_STICKY) ||
                                (override == PistonBehaviors.WEAK_STICKY_BREAKABLE);// && allowDestroy);
                cir.setReturnValue(ret);
                cir.cancel();
            }
        }
    }

    @Inject(method = "isMovable", at = @At(value = "RETURN", ordinal = 3, shift = At.Shift.BEFORE))
    private static void movableCMD(BlockState blockState_1, World world_1, BlockPos blockPos_1,
                                   Direction direction_1, boolean boolean_1, Direction direction_2, CallbackInfoReturnable<Boolean> cir) {
        Block block_1 = blockState_1.getBlock();
        //Make CommandBlocks movable, either use instanceof CommandBlock or the 3 cmd block objects,
        if (quickcarpet.settings.Settings.movableBlockEntities && block_1 instanceof CommandBlock) {
            cir.setReturnValue(true);
        }
    }

    private static boolean isPushableTileEntityBlock(Block block) {
        //Making PISTON_EXTENSION (BlockPistonMoving) pushable would not work as its createNewTileEntity()-method returns null
        return block != Blocks.ENDER_CHEST && block != Blocks.ENCHANTING_TABLE &&
                block != Blocks.END_GATEWAY && block != Blocks.END_PORTAL && block != Blocks.MOVING_PISTON &&
                block != Blocks.SPAWNER;
    }

    @Redirect(method = "isMovable", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;hasBlockEntity()Z"))
    private static boolean ifHasBlockEntity(Block block) {
        return block.hasBlockEntity() && (!quickcarpet.settings.Settings.movableBlockEntities || !isPushableTileEntityBlock(block));
    }



    @Inject(method = "move", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Ljava/util/List;size()I", remap = false, ordinal = 4), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onMove(World world_1, BlockPos blockPos_1, Direction direction_1, boolean boolean_1,
                        CallbackInfoReturnable<Boolean> cir, BlockPos blockPos_2, PistonHandler pistonHandler_1,
                        List<BlockPos> list_1, List<BlockState> list_2, List list_3, int int_2, BlockState[] blockStates_1,
                        Direction direction_2, Set set_1) {
        //Get the blockEntities and remove them from the world before any magic starts to happen
        if (quickcarpet.settings.Settings.movableBlockEntities) {
            List<BlockEntity> list = new ArrayList<>();
            for (int i = 0; i < list_1.size(); ++i) {
                BlockPos blockpos = list_1.get(i);
                BlockEntity blockEntity = (list_2.get(i).getBlock().hasBlockEntity()) ? world_1.getBlockEntity(blockpos) : null;
                list.add(blockEntity);
                if (blockEntity != null) {
                    //hopefully this call won't have any side effects in the future, such as dropping all the BlockEntity's items
                    //we want to place this same(!) BlockEntity object into the world later when the movement stops again
                    world_1.removeBlockEntity(blockpos);
                    blockEntity.markDirty();
                }
            }
            list1_BlockEntities.set(list);
        }
    }

    @Inject(method = "move", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/world/World;setBlockEntity(Lnet/minecraft/util/math/BlockPos;" +
                    "Lnet/minecraft/block/entity/BlockEntity;)V", ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void setBlockEntityWithCarried(World world_1, BlockPos blockPos_1, Direction direction_1, boolean boolean_1,
                                           CallbackInfoReturnable<Boolean> cir, BlockPos blockPos_2, PistonHandler pistonHandler_1, List list_1,
                                           List list_2, List list_3, int int_2, BlockState[] blockStates_1, Direction direction_2, Set set_1,
                                           int int_3, BlockPos blockPos_4, BlockState blockState_1) {
        BlockEntity blockEntityPiston = PistonExtensionBlock.createBlockEntityPiston((BlockState) list_2.get(int_3),
                direction_1, boolean_1, false);
        if (quickcarpet.settings.Settings.movableBlockEntities)
            ((IPistonBlockEntity) blockEntityPiston).setCarriedBlockEntity(list1_BlockEntities.get().get(int_3));
        world_1.setBlockEntity(blockPos_4, blockEntityPiston);
    }

    @Redirect(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockEntity(Lnet/minecraft/util/math/BlockPos;" +
                    "Lnet/minecraft/block/entity/BlockEntity;)V",
            ordinal = 0))
    private void dontDoAnything(World world, BlockPos blockPos_6, BlockEntity blockEntityPiston) {
    }

    @Redirect(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/PistonExtensionBlock;createBlockEntityPiston(Lnet/minecraft/block/BlockState;" +
                    "Lnet/minecraft/util/math/Direction;ZZ)Lnet/minecraft/block/entity/BlockEntity;",
            ordinal = 0))
    private BlockEntity returnNull(BlockState blockState_1, Direction direction_1, boolean boolean_1, boolean boolean_2) {
        return null;
    }

    @Inject(method = "tryMove", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;addBlockAction(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V",
            ordinal = 1, shift = At.Shift.BEFORE))
    private void doubleRetraction(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (quickcarpet.settings.Settings.doubleRetraction) {
            world.setBlockState(pos, state.with(PistonBlock.EXTENDED, false), 2);
        }
    }


    @Feature("additionalMovableBlocks")
    @Redirect(method = "onBlockAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getPistonBehavior()Lnet/minecraft/block/piston/PistonBehavior;"))
    private PistonBehavior returnNormalWhenMovable(BlockState blockState){
        PistonBehavior pistonBehavior = blockState.getPistonBehavior();
        if(pistonBehavior == PistonBehaviors.WEAK_STICKY_BREAKABLE || pistonBehavior == PistonBehaviors.WEAK_STICKY)
            return PistonBehavior.NORMAL;
        return pistonBehavior;
    }
}
