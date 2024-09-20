package net.fabricmc.telepistons;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

public class Telepistons implements ModInitializer {

	public static final String MOD_NAME = "telepistons";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	public static Identifier pistonArmModel;
	public static BakedModel pistonArmBakedModel;
	public static Random random = new Random();
	public static boolean emitSteam;
	public static boolean steamOverride = true;
	public static int particleCount;
	public static boolean squishArm;

	public static Vec3f squishFactorsX;
	public static Vec3f squishFactorsY;
	public static Vec3f squishFactorsZ;

	private static final float HALF_TURN = (float) Math.PI;
	private static final float QUART_TURN = (float) (Math.PI / 2.0f);

	@Override
	public void onInitialize() {
		registerResourcePacks();
		pistonArmModel = new Identifier(MOD_NAME, "block/piston_arm");
		ModelLoadingRegistry.INSTANCE.registerModelProvider((modelManager, out) -> out.accept(pistonArmModel));

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return new Identifier(MOD_NAME, "models");
			}

			@Override
			public void reload(ResourceManager manager) {
				readSettings(manager, "models", "piston_arm.json");
				readParticleSettings(manager, "models", "piston_particle.json");
				pistonArmBakedModel = BakedModelManagerHelper.getModel(MinecraftClient.getInstance().getBakedModelManager(), pistonArmModel);
			}
		});
	}

	/**
	 * Registers built-in resource packs for the mod.
	 */
	private void registerResourcePacks() {
		Identifier scissorPack = new Identifier(MOD_NAME, "scissor_pistons");
		Identifier bellowsPack = new Identifier(MOD_NAME, "bellows_pistons");
		Identifier stickySidesPack = new Identifier(MOD_NAME, "sticky_sides");
		Identifier enableSteam = new Identifier(MOD_NAME, "enable_steam");

		FabricLoader.getInstance().getModContainer(MOD_NAME).ifPresent(container -> {
			ResourceManagerHelper.registerBuiltinResourcePack(scissorPack, container, ResourcePackActivationType.NORMAL);
			ResourceManagerHelper.registerBuiltinResourcePack(bellowsPack, container, ResourcePackActivationType.NORMAL);
			ResourceManagerHelper.registerBuiltinResourcePack(stickySidesPack, container, ResourcePackActivationType.NORMAL);
			ResourceManagerHelper.registerBuiltinResourcePack(enableSteam, container, ResourcePackActivationType.DEFAULT_ENABLED);
		});
	}

	/**
	 * Reads settings from piston_arm.json and initializes related fields.
	 */
	private void readSettings(ResourceManager manager, String directory, String fileName) {
		Map<Identifier, Resource> resourceMap = manager.findResources(directory, path -> path.toString().endsWith(fileName));
		for (Map.Entry<Identifier, Resource> entry : resourceMap.entrySet()) {
			try (InputStream stream = entry.getValue().getInputStream();
				 BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				JsonObject json = JsonHelper.deserialize(streamReader);
				JsonObject settings = json.getAsJsonObject(MOD_NAME);

				squishArm = settings.get("squish").getAsBoolean();
				particleCount = Math.max(settings.get("particles").getAsInt(), 0);
				initializeSquishFactors(settings);

				LOGGER.info("Settings for piston arm successfully loaded.");
			} catch (Exception e) {
				particleCount = 0;
				squishArm = false;
				LOGGER.error("Error while loading settings from {}, using default values.", fileName, e);
			}
		}
	}

	/**
	 * Initializes squish factors based on the settings.
	 */
	private void initializeSquishFactors(JsonObject settings) {
		if (squishArm) {
			JsonArray factorArr = settings.getAsJsonArray("squishedScale");
			squishFactorsZ = new Vec3f(factorArr.remove(0).getAsFloat(), factorArr.remove(0).getAsFloat(), factorArr.remove(0).getAsFloat());

			// Swap and assign squish factors for X and Y based on Z
			squishFactorsX = new Vec3f(squishFactorsZ.getZ(), squishFactorsZ.getY(), squishFactorsZ.getX());
			squishFactorsY = new Vec3f(squishFactorsZ.getX(), squishFactorsZ.getZ(), squishFactorsZ.getY());
		}
	}

	/**
	 * Reads particle settings from piston_particle.json.
	 */
	private void readParticleSettings(ResourceManager manager, String directory, String fileName) {
		Map<Identifier, Resource> resourceMap = manager.findResources(directory, path -> path.toString().endsWith(fileName));

		steamOverride = false;
		for (Map.Entry<Identifier, Resource> entry : resourceMap.entrySet()) {
			try (InputStream stream = entry.getValue().getInputStream();
				 BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				JsonObject json = JsonHelper.deserialize(streamReader);
				JsonObject settings = json.getAsJsonObject(MOD_NAME);
				steamOverride = settings.get("particleOverride").getAsBoolean();

				LOGGER.info("Particle settings successfully loaded.");
			} catch (Exception e) {
				LOGGER.error("Error while loading particle settings from {}.", fileName, e);
			}
		}

		emitSteam = steamOverride && (particleCount > 0);
	}

	/**
	 * Generates a rotation quaternion based on the piston direction.
	 */
	public static Quaternion getRotationQuaternion(Direction dir) {
		return switch (dir) {
			case UP -> Quaternion.fromEulerXyz(QUART_TURN, 0.0f, 0.0f);
			case DOWN -> Quaternion.fromEulerXyz(-QUART_TURN, 0.0f, 0.0f);
			case NORTH -> Quaternion.fromEulerXyz(0.0f, 0.0f, 0.0f);
			case SOUTH -> Quaternion.fromEulerXyz(0.0f, HALF_TURN, 0.0f);
			case EAST -> Quaternion.fromEulerXyz(0.0f, -QUART_TURN, 0.0f);
			case WEST -> Quaternion.fromEulerXyz(0.0f, QUART_TURN, 0.0f);
		};
	}
}
