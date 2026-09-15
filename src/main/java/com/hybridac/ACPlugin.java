package com.hybridac;

import com.hybridac.bootstrap.BootstrapContext;
import com.hybridac.bootstrap.PluginBootstrap;
import com.hybridac.config.HybridConfig;
import com.hybridac.storage.CredentialsStorage;
import org.bukkit.plugin.java.JavaPlugin;

public final class ACPlugin extends JavaPlugin {

    private PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        this.bootstrap = new PluginBootstrap(this);
        this.bootstrap.enable();
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.disable();
        }
    }

    public void reloadRuntime() {
        if (this.bootstrap != null) {
            this.bootstrap.reload();
        }
    }

    public HybridConfig runtimeConfig() {
        return this.bootstrap != null ? this.bootstrap.runtimeConfig() : null;
    }

    public BootstrapContext bootstrapContext() {
        return this.bootstrap != null ? this.bootstrap.context() : null;
    }

    public CredentialsStorage credentialsStorage() {
        return this.bootstrap != null ? this.bootstrap.credentialsStorage() : null;
    }

    public String mlEndpoint() {
        return this.bootstrap != null ? this.bootstrap.mlEndpoint() : null;
    }

    public String mlBearer() {
        return this.bootstrap != null ? this.bootstrap.mlBearer() : null;
    }
}
