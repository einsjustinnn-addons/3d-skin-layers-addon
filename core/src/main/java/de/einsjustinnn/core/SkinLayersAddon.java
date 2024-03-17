package de.einsjustinnn.core;

import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class SkinLayersAddon extends LabyAddon<SkinLayersConfiguration> {

  @Override
  protected void load() {
    this.registerSettingCategory();
  }

  @Override
  protected void enable() {
  }

  @Override
  protected Class<SkinLayersConfiguration> configurationClass() {
    return SkinLayersConfiguration.class;
  }
}
