package net.fabricmc.telepistons.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.telepistons.Telepistons;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.entity.PistonBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(PistonBlockEntityRenderer.class)
public class PistonRendererMixin {

    private static final float HALF_BLOCK = 0.5f;
    private static final float QUARTER_BLOCK = 0.25f;
    private static final float EXTEND_RATE = 0.5f;

    @Environment(EnvType.CLIENT)
    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/block/entity/PistonBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V")
    private void render(PistonBlockEntity pistonBlockEntity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int overlay, CallbackInfo info) {
        if (!pistonBlockEntity.isSource()) return;

        World world = pistonBlockEntity.getWorld();
        if (world == null) return;

        BlockPos blockPos = pistonBlockEntity.getPos();
        Direction dir = pistonBlockEntity.getMovementDirection();
        float dist = getRenderDistance(pistonBlockEntity, tickDelta);

        BlockModelRenderer.enableBrightnessCache();
        matrixStack.push();

        if (Telepistons.squishArm) {
            applySquishTransform(matrixStack, pistonBlockEntity, dir, dist);
        } else {
            applyStandardTransform(matrixStack, pistonBlockEntity, tickDelta, dir);
        }

        applyRotationTransform(matrixStack, pistonBlockEntity, dir);
        renderPistonArm(world, pistonBlockEntity.getCachedState(), blockPos, matrixStack, vertexConsumerProvider);

        matrixStack.pop();
        BlockModelRenderer.disableBrightnessCache();
    }

    private float getRenderDistance(PistonBlockEntity pistonBlockEntity, float tickDelta) {
        return 1 - (Math.abs(pistonBlockEntity.getRenderOffsetX(tickDelta))
                + Math.abs(pistonBlockEntity.getRenderOffsetY(tickDelta))
                + Math.abs(pistonBlockEntity.getRenderOffsetZ(tickDelta)));
    }

    private void applySquishTransform(MatrixStack matrixStack, PistonBlockEntity pistonBlockEntity, Direction dir, float dist) {
        boolean extending = pistonBlockEntity.isExtending();
        Vec3f squishFactors = getSquishFactors(dir);

        matrixStack.translate(HALF_BLOCK, HALF_BLOCK, HALF_BLOCK);

        if (extending) {
            squishFactors.lerp(new Vec3f(1f, 1f, 1f), dist);
            translateAndScale(matrixStack, dir, squishFactors, QUARTER_BLOCK, true);
        } else {
            Vec3f squish = new Vec3f(1f, 1f, 1f);
            squish.lerp(squishFactors, dist);
            translateAndScale(matrixStack, dir, squish, -QUARTER_BLOCK, false);
        }

        matrixStack.translate(-HALF_BLOCK, -HALF_BLOCK, -HALF_BLOCK);
    }

    private Vec3f getSquishFactors(Direction dir) {
        if (dir.getOffsetX() != 0) {
            return new Vec3f(Telepistons.squishFactorsX.getX(), Telepistons.squishFactorsX.getY(), Telepistons.squishFactorsX.getZ());
        } else if (dir.getOffsetY() != 0) {
            return new Vec3f(Telepistons.squishFactorsY.getX(), Telepistons.squishFactorsY.getY(), Telepistons.squishFactorsY.getZ());
        } else {
            return new Vec3f(Telepistons.squishFactorsZ.getX(), Telepistons.squishFactorsZ.getY(), Telepistons.squishFactorsZ.getZ());
        }
    }

    private void translateAndScale(MatrixStack matrixStack, Direction dir, Vec3f squishFactors, float offset, boolean extending) {
        matrixStack.translate(offset * dir.getOffsetX(), offset * dir.getOffsetY(), offset * dir.getOffsetZ());

        if (extending) {
            matrixStack.translate(-dir.getOffsetX(), -dir.getOffsetY(), -dir.getOffsetZ());
        }

        matrixStack.scale(squishFactors.getX(), squishFactors.getY(), squishFactors.getZ());
    }

    private void applyStandardTransform(MatrixStack matrixStack, PistonBlockEntity pistonBlockEntity, float tickDelta, Direction dir) {
        matrixStack.translate(EXTEND_RATE * pistonBlockEntity.getRenderOffsetX(tickDelta),
                EXTEND_RATE * pistonBlockEntity.getRenderOffsetY(tickDelta),
                EXTEND_RATE * pistonBlockEntity.getRenderOffsetZ(tickDelta));

        if (!pistonBlockEntity.isExtending()) {
            matrixStack.translate(-HALF_BLOCK * dir.getOffsetX(), -HALF_BLOCK * dir.getOffsetY(), -HALF_BLOCK * dir.getOffsetZ());
        }
    }

    private void applyRotationTransform(MatrixStack matrixStack, PistonBlockEntity pistonBlockEntity, Direction dir) {
        matrixStack.translate(HALF_BLOCK, HALF_BLOCK, HALF_BLOCK);
        matrixStack.multiply(Telepistons.getRotationQuaternion(pistonBlockEntity.isExtending() ? dir : dir.getOpposite()));
        matrixStack.translate(-HALF_BLOCK, -HALF_BLOCK, -HALF_BLOCK);
    }

    private void renderPistonArm(World world, BlockState state, BlockPos blockPos, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
        MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer().render(
                world,
                Telepistons.pistonArmBakedModel,
                state,
                blockPos,
                matrixStack,
                vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(state)),
                false,
                Random.create(),
                1L,
                0
        );
    }

    @Shadow
    private void renderModel(BlockPos blockPos, BlockState blockState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, World world, boolean bl, int i) {

    }
}
