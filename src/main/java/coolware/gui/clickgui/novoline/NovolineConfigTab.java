package coolware.gui.clickgui.novoline;

import coolware.config.CloudConfigManager;
import coolware.config.ConfigManager;
import coolware.font.novoline.impl.Fonts;
import coolware.module.visuals.Theme;
import net.minecraft.client.gui.Gui;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static coolware.font.novoline.impl.Fonts.SF.SF_18.SF_18;
import static coolware.font.novoline.impl.Fonts.SF.SF_20.SF_20;

public class NovolineConfigTab extends NovolineTab {

    public enum ConfigCategory {
        LOCAL,
        ONLINE
    }

    private ConfigCategory activeCategory = ConfigCategory.LOCAL;
    private final List<NovolineConfigItem> configs = new CopyOnWriteArrayList<NovolineConfigItem>();
    private NovolineConfigTextField newConfigName;
    private NovolineConfigItem selectedConfig;

    public NovolineConfigTab(float posX, float posY) {
        super(null, posX, posY);
        CloudConfigManager.refreshAsync();
        refreshConfigs();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        int h = 0;
        if (opened) {
            h += 16; // category switch bar
            for (NovolineConfigItem config : configs) {
                h += config.getYPerConfig();
            }
            h += 1;
        }

        // Tab header
        Gui.drawRect((int) getPosX() - 1, (int) getPosY(), (int) getPosX() + 101, (int) getPosY() + 15 + h, new Color(29, 29, 29, 255).getRGB());
        SF_20.drawString("Configs", getPosX() + 4, getPosY() + 4, 0xFFFFFFFF, true);

        if (opened) {
            int catY = (int) (getPosY() + 15);
            // Category Selector Bar: [ Local ] [ Online ]
            Gui.drawRect((int) getPosX(), catY, (int) getPosX() + 100, catY + 16, new Color(35, 35, 35, 255).getRGB());

            int hudColor = Theme.getInstance().getColor(0);

            // Local tab button (0 to 50)
            boolean hoverLocal = mouseX >= getPosX() && mouseX <= getPosX() + 50 && mouseY >= catY && mouseY <= catY + 16;
            int localTextColor = activeCategory == ConfigCategory.LOCAL ? hudColor : (hoverLocal ? 0xFFFFFFFF : 0xFFAAAAAA);
            SF_18.drawCenteredString("Local", getPosX() + 25.0f, catY + 4.5f, localTextColor, true);
            if (activeCategory == ConfigCategory.LOCAL) {
                Gui.drawRect((int) getPosX() + 6, catY + 14, (int) getPosX() + 44, catY + 16, hudColor);
            }

            // Online tab button (50 to 100)
            boolean hoverOnline = mouseX > getPosX() + 50 && mouseX <= getPosX() + 100 && mouseY >= catY && mouseY <= catY + 16;
            int onlineTextColor = activeCategory == ConfigCategory.ONLINE ? hudColor : (hoverOnline ? 0xFFFFFFFF : 0xFFAAAAAA);
            SF_18.drawCenteredString("Online", getPosX() + 75.0f, catY + 4.5f, onlineTextColor, true);
            if (activeCategory == ConfigCategory.ONLINE) {
                Gui.drawRect((int) getPosX() + 56, catY + 14, (int) getPosX() + 94, catY + 16, hudColor);
            }

            // Subtle divider line
            Gui.drawRect((int) getPosX() + 50, catY + 3, (int) getPosX() + 51, catY + 13, new Color(60, 60, 60, 255).getRGB());

            for (NovolineConfigItem config : configs) {
                config.drawScreen(mouseX, mouseY);
            }
        }
    }

    public void setCategory(ConfigCategory category) {
        this.activeCategory = category;
        refreshConfigs();
    }

    public ConfigCategory getActiveCategory() {
        return activeCategory;
    }

    public void refreshConfigs() {
        configs.clear();
        setSelectedConfig(null);

        if (activeCategory == ConfigCategory.LOCAL) {
            // Local configs listing
            List<String> list = ConfigManager.listConfigs();
            for (String cfg : list) {
                configs.add(new NovolineConfigItem(cfg, this));
            }

            newConfigName = new NovolineConfigTextField("Config name", this);
            configs.add(newConfigName);

            configs.add(new NovolineConfigButton("Load", this, (configName) -> {
                if (!configName.isEmpty()) {
                    ConfigManager.loadConfig(configName);
                }
            }));

            configs.add(new NovolineConfigButton("Save", this, (ignored) -> {
                String nameToSave = getSelectedConfig() != null ? getSelectedConfig().getName() : newConfigName.getValue();
                if (nameToSave != null && !nameToSave.trim().isEmpty()) {
                    ConfigManager.saveConfig(nameToSave.trim());
                    refreshConfigs();
                }
            }));

            configs.add(new NovolineConfigButton("Delete", this, (configName) -> {
                if (!configName.isEmpty()) {
                    File file = new File(ConfigManager.getConfigDir(), configName.endsWith(".cfg") ? configName : configName + ".cfg");
                    if (file.exists()) {
                        file.delete();
                    }
                    refreshConfigs();
                }
            }));
        } else {
            // Online (Cloud) configs listing
            List<String> cloudList = CloudConfigManager.getCachedConfigs();
            if (cloudList.isEmpty()) {
                if (CloudConfigManager.isFetching()) {
                    configs.add(new NovolineConfigItem("Fetching...", this));
                } else {
                    configs.add(new NovolineConfigItem("No online cfgs", this));
                }
            } else {
                for (String cloudCfg : cloudList) {
                    String cleanName = cloudCfg.endsWith(".cfg") ? cloudCfg.substring(0, cloudCfg.length() - 4) : cloudCfg;
                    configs.add(new NovolineConfigItem(cleanName, this));
                }
            }

            configs.add(new NovolineConfigButton("Load / Import", this, (configName) -> {
                if (!configName.isEmpty() && !configName.equalsIgnoreCase("No online cfgs") && !configName.equalsIgnoreCase("Fetching...")) {
                    String fileName = configName.endsWith(".cfg") ? configName : configName + ".cfg";
                    new Thread(() -> {
                        CloudConfigManager.downloadAndLoad(fileName);
                    }, "CloudConfig-DownloadLoad").start();
                }
            }));

            configs.add(new NovolineConfigButton("Refresh", this, (ignored) -> {
                CloudConfigManager.refreshAsync();
                new Thread(() -> {
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException ignored1) {}
                    refreshConfigs();
                }).start();
            }));
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
        }

        if (opened) {
            int catY = (int) (getPosY() + 15);
            // Click category buttons
            if (mouseY >= catY && mouseY <= catY + 16 && mouseButton == 0) {
                if (mouseX >= getPosX() && mouseX <= getPosX() + 50) {
                    if (activeCategory != ConfigCategory.LOCAL) {
                        setCategory(ConfigCategory.LOCAL);
                        return;
                    }
                } else if (mouseX > getPosX() + 50 && mouseX <= getPosX() + 100) {
                    if (activeCategory != ConfigCategory.ONLINE) {
                        setCategory(ConfigCategory.ONLINE);
                        return;
                    }
                }
            }

            for (NovolineConfigItem config : configs) {
                config.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        for (NovolineConfigItem config : configs) {
            config.keyTyped(typedChar, keyCode);
        }
    }

    public List<NovolineConfigItem> getConfigs() {
        return configs;
    }

    public NovolineConfigItem getSelectedConfig() {
        return selectedConfig;
    }

    public void setSelectedConfig(NovolineConfigItem selectedConfig) {
        this.selectedConfig = selectedConfig;
    }
}
