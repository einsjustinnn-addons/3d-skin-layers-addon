package de.einsjustinnn.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import net.labymod.api.loader.MinecraftVersion;
import net.labymod.api.models.version.Version;
import net.labymod.api.util.io.IOUtil;
import net.labymod.api.util.io.web.request.Response;
import net.labymod.api.util.io.web.request.types.FileRequest;
import net.labymod.api.util.io.zip.Zips;
import net.labymod.api.util.logging.Logging;

public class SkinLayersFiles {

  private static final Logging LOGGER = Logging.create(SkinLayersAddon.class);
  private static final Gson GSON = new Gson();

  private static final String MODRINTH_ID = "zV5r3pPn";
  private static final String SKINLAYERS_VERSION = "1.6.2";

  private final Map<Version, ModrinthArtifact> artifacts = new HashMap<>();

  public void registerArtifact(
      MinecraftVersion version,
      String modrinthVersionId
  ) {
    this.registerArtifact(version, modrinthVersionId, SKINLAYERS_VERSION);
  }

  public void registerArtifact(
      MinecraftVersion version,
      String modrinthVersionId,
      String skinLayers3dVersion
  ) {
    this.artifacts.put(version.version(), new ModrinthArtifact(
        modrinthVersionId,
        skinLayers3dVersion,
        version.version()
    ));
  }

  public Path downloadSkinLayers3d(Version version, Path directory) throws Exception {
    if (!IOUtil.exists(directory)) {
      Files.createDirectories(directory);
      return this.download(directory, version);
    }

    // list all jar files in the directory
    try (Stream<Path> files = Files.list(directory)) {
      List<Path> existingFiles = files.filter(
          path -> path.getFileName().toString().endsWith(".jar")
      ).toList();
      // if no jar files are present - download skinLayers3d
      if (existingFiles.isEmpty()) {
        return this.download(directory, version);
      }

      // if there are more than one jar files present, it's likely the user did something in this directory. we don't want that
      if (existingFiles.size() > 1) {
        IOUtil.delete(directory);
        Files.createDirectories(directory);
        return this.download(directory, version);
      }

      // if there is just one jar file, we can check if it's 1. skinLayers3d and 2. the correct version
      Path existingFile = existingFiles.get(0);
      AtomicBoolean valid = new AtomicBoolean(false);

      try {
        Zips.read(existingFile, (entry, bytes) -> {
          if (!entry.getName().equals("fabric.mod.json")) {
            return false;
          }

          JsonObject object = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8),
              JsonObject.class);
          String modVersion = object.get("version").getAsString();
          String id = object.get("id").getAsString();
          if (!id.equals("skinlayers3d")) {
            return true;
          }

          String fabricVersion = this.artifacts.get(version).getFabricVersion();
          if (modVersion.equals(fabricVersion)) {
            valid.set(true);
            return true;
          }

          LOGGER.info(
              "Installed 3D Skin Layers Version is outdated (installed: " + modVersion + ", latest: "
                  + fabricVersion + ")! Updating...");
          return true;
        });
      } catch (Exception e) {
        LOGGER.warn("Failed to read fabric.mod.json of local 3D Skin Layers to verify", e);
      }

      // if the installed skinLayers3d is up to date, we can return the existing file
      if (valid.get()) {
        LOGGER.info("The installed 3D Skin Layers is up to date!");
        return existingFile;
      }

      // otherwise we delete the existing file and download the latest version
      IOUtil.delete(existingFile);
      return this.download(directory, version);
    }
  }

  private Path download(Path directory, Version version) {
    ModrinthArtifact artifact = this.artifacts.get(version);
    if (artifact == null) {
      throw new IllegalStateException("No artifact registered for version " + version);
    }

    return this.download(directory, artifact);
  }

  private Path download(Path directory, ModrinthArtifact artifact) {
    LOGGER.info("Downloading 3D Skin Layers " + artifact.getFabricVersion() + "...");
    Path file = directory.resolve(
        "skinlayers3d-fabric-" + artifact.skinlayers3dVersion + "-mc" + artifact.minecrafVersion + ".jar");
    Response<Path> pathResponse = FileRequest.of(file).url(artifact.getDownloadUrl()).executeSync();
    if (pathResponse.hasException()) {
      throw new RuntimeException("Failed to download 3D Skin Layers", pathResponse.exception());
    }

    // create a file that indicates that this is a mod directory, as we don't want the user to put other files in here
    Path infoFile = directory.resolve("this is not a mod directory");
    if (!IOUtil.exists(infoFile)) {
      try {
        Files.createFile(infoFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    LOGGER.info("Successfully downloaded 3D Skin Layers " + artifact.getFabricVersion());

    // return the path to the downloaded file so we can hand it over to the mod loader
    return file;
  }

  private static class ModrinthArtifact {

    private final String modrinthVersionId;
    private final String skinlayers3dVersion;
    private final Version minecrafVersion;

    private ModrinthArtifact(
        String modrinthVersionId,
        String skinLayers3dVersion,
        Version minecrafVersion
    ) {
      this.modrinthVersionId = modrinthVersionId;
      this.skinlayers3dVersion = skinLayers3dVersion;
      this.minecrafVersion = minecrafVersion;
    }

    public String getFabricVersion() {
      return this.minecrafVersion + "-" + this.skinlayers3dVersion;
    }

    private String getDownloadUrl() {
      return "https://cdn.modrinth.com/data/" + MODRINTH_ID + "/versions/" + this.modrinthVersionId
          + "/skinlayers3d-fabric-" + this.skinlayers3dVersion + "-mc" + this.minecrafVersion + ".jar";
    }
  }
}
