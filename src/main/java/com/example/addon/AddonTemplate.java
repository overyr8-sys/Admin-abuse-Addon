package com.example.addon;

import com.example.addon.commands.CommandExample;
import com.example.addon.hud.HudExample;
import com.example.addon.hud.InventoryHud;
import com.example.addon.modules.Advertiser;
import com.example.addon.modules.AutoBuff;
import com.example.addon.modules.AutoGGModule;
import com.example.addon.modules.AutoLogin;
import com.example.addon.modules.Backtrack;
import com.example.addon.modules.ChatLogger;
import com.example.addon.modules.FrameDupe;
import com.example.addon.modules.HotbarManager;
import com.example.addon.modules.LegitCrystal;
import com.example.addon.modules.LegitKillAura;
import com.example.addon.modules.MsgAura;
import com.example.addon.modules.StashFinder;
import com.example.addon.modules.TNTPlacer;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Admin Abuse");
    public static final HudGroup HUD_GROUP = new HudGroup("Admin Abuse");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Admin Abuse Addon");

        // Modules
        Modules.get().add(new FrameDupe());
        Modules.get().add(new ChatLogger());
        Modules.get().add(new AutoGGModule());
        Modules.get().add(new LegitKillAura());
        Modules.get().add(new MsgAura());
        Modules.get().add(new AutoLogin());
        Modules.get().add(new StashFinder());
        Modules.get().add(new LegitCrystal());
        Modules.get().add(new Advertiser());
        Modules.get().add(new AutoBuff());
        Modules.get().add(new TNTPlacer());
        Modules.get().add(new HotbarManager());
        Modules.get().add(new Backtrack());

        // Commands
        Commands.add(new CommandExample());

        // HUD
        Hud.get().register(HudExample.INFO);
        Hud.get().register(InventoryHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
