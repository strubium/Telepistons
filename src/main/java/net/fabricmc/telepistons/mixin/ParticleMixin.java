package net.fabricmc.telepistons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.telepistons.Telepistons;
import net.minecraft.block.PistonBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(PistonBlock.class)
public class ParticleMixin {

	private static final float PARTICLE_CENTER_OFFSET = 0.5f;
	private static final float PARTICLE_VELOCITY_MULTIPLIER = 0.125f;

	@Inject(at = @At("HEAD"), method = "move("
			+ "Lnet/minecraft/world/World;"
			+ "Lnet/minecraft/util/math/BlockPos;"
			+ "Lnet/minecraft/util/math/Direction;"
			+ "Z)Z")
	public void spawnParticles(World world, BlockPos pos, Direction dir, boolean retract, CallbackInfoReturnable<Boolean> info) {
		if (Telepistons.emitSteam) {
			generateParticles(world, pos, dir, retract);
		}
	}

	private void generateParticles(World world, BlockPos pos, Direction dir, boolean retract) {
		float dx = dir.getOffsetX();
		float dy = dir.getOffsetY();
		float dz = dir.getOffsetZ();

		for (int i = 0; i < Telepistons.particleCount; i++) {
			float velocityX = getRandomVelocity(dy, dz);
			float velocityY = getRandomVelocity(dx, dz);
			float velocityZ = getRandomVelocity(dy, dx);

			world.addParticle(ParticleTypes.CLOUD,
					pos.getX() + PARTICLE_CENTER_OFFSET + (dx * PARTICLE_CENTER_OFFSET),
					pos.getY() + PARTICLE_CENTER_OFFSET + (dy * PARTICLE_CENTER_OFFSET),
					pos.getZ() + PARTICLE_CENTER_OFFSET + (dz * PARTICLE_CENTER_OFFSET),
					velocityX,
					velocityY,
					velocityZ);
		}
	}

	private float getRandomVelocity(float offset1, float offset2) {
		return PARTICLE_VELOCITY_MULTIPLIER * (0.5f - Telepistons.random.nextFloat()) * Math.abs(offset1 + offset2);
	}
}
