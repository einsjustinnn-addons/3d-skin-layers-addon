package de.einsjustinnn.core;

import java.nio.file.Path;
import java.util.Collection;
import net.labymod.api.Laby;
import net.labymod.api.addon.entrypoint.Entrypoint;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.modloader.ModLoaderDiscoveryEvent;
import net.labymod.api.loader.MinecraftVersions;
import net.labymod.api.models.addon.annotation.AddonEntryPoint;
import net.labymod.api.models.version.Version;
import net.labymod.api.util.logging.Logging;

@AddonEntryPoint(priority = 900)
public class SkinLayersEntryPoint implements Entrypoint {

  private static final Logging LOGGER = Logging.create(SkinLayersAddon.class);

  private final SkinLayersFiles skinLayers3dFiles = new SkinLayersFiles();

  public SkinLayersEntryPoint() {
    this.skinLayers3dFiles.registerArtifact(MinecraftVersions.V1_20_4, "kJmEO0xO");
    this.skinLayers3dFiles.registerArtifact(MinecraftVersions.V1_20_2, "czoUx8H7");
    this.skinLayers3dFiles.registerArtifact(MinecraftVersions.V1_20_1, "KHhjRppT");

    this.skinLayers3dFiles.registerArtifact(MinecraftVersions.V1_19_4, "Qjcg7Sz1");
    this.skinLayers3dFiles.registerArtifact(MinecraftVersions.V1_19_3, "mF64uUAf");
    this.skinLayers3dFiles.registerArtifact(MinecraftVersions.V1_19_2, "JLg8jqoe");

    Laby.labyAPI().eventBus().registerListener(this);
  }

  @Override
  public void initialize(Version version) {
    // NO-OP
  }

  @Subscribe
  public void onModLoaderDiscover(ModLoaderDiscoveryEvent event) {
    if (!event.modLoader().getId().equals("fabricloader")) {
      return;
    }

    // Use next best mod directory as base directory, should optimally be .minecraft\labymod-neo\fabric\{version}\mods\
    Collection<Path> modDirectoryPaths = event.modLoader().getModDirectoryPaths();
    Path modDirectory = null;
    for (Path modDirectoryPath : modDirectoryPaths) {
      modDirectory = modDirectoryPath;
      break;
    }

    if (modDirectory == null) {
      LOGGER.error("Could not find mod directory. Skipping 3D Skin Layers installation");
      return;
    }

    try {
      // Load skinlayers3d from a subfolder, this way we can easily do anything we want without unintentionally breaking other mods
      Path skinLayers3dDirectory = modDirectory.resolve("skinlayers3d");
      Path skinLayers3dFile = this.skinLayers3dFiles.downloadSkinLayers3d(
          Laby.labyAPI().labyModLoader().version(),
          skinLayers3dDirectory
      );

      // Add the path of the skinlayers3d jar file to the mod loader
      event.addAdditionalDiscovery(skinLayers3dFile);
    } catch (Exception e) {
      LOGGER.error("Failed to load 3D Skin Layers", e);
    }
  }
}
