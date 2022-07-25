package net.fabricmc.telepistons;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.telepistons.simpleLibs.simpleConfig.SimpleConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;

public class Telepistons implements ModInitializer {
	
	public static final Block PISTON_ARM = new PistonArm(FabricBlockSettings.of(Material.METAL).hardness(80.0f));
	public static Random random = new Random();
	public static boolean emitSteam;
	public static boolean steamOverride = true;
	public static int particleCount;
	public static boolean squishArm;

	public static Vec3f squishFactorsX;
	public static Vec3f squishFactorsY;
	public static Vec3f squishFactorsZ;

	@Override
	public void onInitialize() {
        Registry.register(Registry.BLOCK, new Identifier("telepistons", "piston_arm"), PISTON_ARM);
		//BlockRenderLayerMap.INSTANCE.putBlock(Telepistons.PISTON_ARM, RenderLayer.getTranslucent());

		Identifier scissorPack = new Identifier("telepistons","scissor_pistons");
		Identifier bellowsPack = new Identifier("telepistons","bellows_pistons");
		Identifier stickySidesPack = new Identifier("telepistons","sticky_sides");
		Identifier enableSteam = new Identifier("telepistons","enable_steam");
		FabricLoader.getInstance().getModContainer("telepistons").ifPresent(container -> {
			ResourceManagerHelper.registerBuiltinResourcePack(scissorPack, container, ResourcePackActivationType.NORMAL);
			ResourceManagerHelper.registerBuiltinResourcePack(bellowsPack, container, ResourcePackActivationType.NORMAL);
			ResourceManagerHelper.registerBuiltinResourcePack(stickySidesPack, container, ResourcePackActivationType.NORMAL);
			ResourceManagerHelper.registerBuiltinResourcePack(enableSteam, container, ResourcePackActivationType.DEFAULT_ENABLED);
		});

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
			new SimpleSynchronousResourceReloadListener() {
				@Override
				public Identifier getFabricId(){
					return new Identifier("telepistons","models");
				}

				@Override
				public void reload(ResourceManager manager){
					Map<Identifier, Resource> resourceMap = manager.findResources("models", path -> path.toString().endsWith("piston_arm.json"));

					for(Map.Entry<Identifier, Resource> entry : resourceMap.entrySet()){
						System.out.println(entry.getValue().getResourcePackName());
						try(InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
							BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
							JsonObject json = JsonHelper.deserialize(streamReader);

							JsonObject settings = json.get("telepistons").getAsJsonObject();

							squishArm = settings.get("squish").getAsBoolean();
							particleCount = Math.max(settings.get("particles").getAsInt(), 0);
							if(squishArm) {
								JsonArray factorArr = settings.get("squishedScale").getAsJsonArray();
								squishFactorsZ = new Vec3f(
										factorArr.remove(0).getAsFloat(),
										factorArr.remove(0).getAsFloat(),
										factorArr.remove(0).getAsFloat());

								squishFactorsX = new Vec3f(
										squishFactorsZ.getZ(),
										squishFactorsZ.getY(),
										squishFactorsZ.getX());

								squishFactorsY = new Vec3f(
										squishFactorsZ.getX(),
										squishFactorsZ.getZ(),
										squishFactorsZ.getY());
							}

							System.out.println("[Telepistons] Read settings successfully");
						} catch(Exception e) {
							particleCount = 0;
							squishArm = false;
							System.out.println("Error:\n" + e);
							System.out.println("[Telepistons] Error while trying to read settings, using standard values");
						}
					}

					resourceMap = manager.findResources("models", path -> path.toString().endsWith("piston_particle.json"));

					steamOverride = false;
					for(Map.Entry<Identifier, Resource> entry : resourceMap.entrySet()){
						try(InputStream stream = manager.getResource(entry.getKey()).get().getInputStream()) {
							BufferedReader streamReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
							JsonObject json = JsonHelper.deserialize(streamReader);

							JsonObject settings = json.get("telepistons").getAsJsonObject();
							steamOverride = settings.get("particleOverride").getAsBoolean();

							System.out.println("[Telepistons] Read particle setting successfully");
						} catch(Exception e) {
							System.out.println("[Telepistons] Particle setting file erroneous");
						}
					}

					emitSteam = steamOverride ? (particleCount > 0) : false;
				}
			}
		);
	}
}
