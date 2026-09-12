package com.github.cooldood.modules.impl.combat;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClientTickEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.events.impl.RespawnEvent;
import com.github.cooldood.events.impl.WorldUnloadEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.ModuleManager;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.EnumChatFormatting;

import java.util.*;

@RegisterModule(
        name = "Anti Bot",
        description = "Detects and filters out bot entities (NPCs, Watchdog, Matrix bots).",
        category = Category.COMBAT,
        enabledByDefault = true
)
public class AntiBot extends Module {

    // ── SubModules ──────────────────────────────────────────────────────────
    @RegisterSubModule(name = "Basic")
    public static boolean basic = true;

    @RegisterSubModule(name = "Tab")
    public static boolean tab = true;

    public enum TabMode { Equals, Contains }
    @RegisterSubModule(name = "Tab Mode", parent = "Tab")
    public static TabMode tabMode = TabMode.Contains;

    @RegisterSubModule(name = "Entity ID")
    public static boolean entityID = true;

    @RegisterSubModule(name = "Color")
    public static boolean color = false;

    @RegisterSubModule(name = "Living Time")
    public static boolean livingTime = false;

    @RegisterSubModule(name = "Living Time Ticks", min = 1, max = 200, increment = 5, parent = "Living Time")
    public static double livingTimeTicks = 40;

    @RegisterSubModule(name = "Ground")
    public static boolean ground = true;

    @RegisterSubModule(name = "Air")
    public static boolean air = false;

    @RegisterSubModule(name = "Invalid Ground")
    public static boolean invalidGround = true;

    @RegisterSubModule(name = "Swing")
    public static boolean swing = false;

    @RegisterSubModule(name = "Health")
    public static boolean health = false;

    @RegisterSubModule(name = "Min Health", min = 0, max = 40, increment = 1, parent = "Health")
    public static double minHealth = 0.0;

    @RegisterSubModule(name = "Max Health", min = 0, max = 40, increment = 1, parent = "Health")
    public static double maxHealth = 20.0;

    @RegisterSubModule(name = "Derp")
    public static boolean derp = true;

    @RegisterSubModule(name = "Was Invisible")
    public static boolean wasInvisible = false;

    @RegisterSubModule(name = "Armor")
    public static boolean armor = false;

    @RegisterSubModule(name = "Ping")
    public static boolean ping = true;

    @RegisterSubModule(name = "Need Hit")
    public static boolean needHit = false;

    @RegisterSubModule(name = "Spawn In Combat")
    public static boolean spawnInCombat = false;

    @RegisterSubModule(name = "Duplicate In World")
    public static boolean duplicateInWorld = false;

    @RegisterSubModule(name = "Duplicate In Tab")
    public static boolean duplicateInTab = false;

    public enum DuplicateCompareMode { OnTime, WhenSpawn }
    @RegisterSubModule(name = "Duplicate Compare Mode")
    public static DuplicateCompareMode duplicateCompareMode = DuplicateCompareMode.OnTime;

    @RegisterSubModule(name = "NPC Detection")
    public static boolean experimentalNPCDetection = true;

    @RegisterSubModule(name = "Illegal Name")
    public static boolean illegalName = false;

    @RegisterSubModule(name = "Matrix Bot")
    public static boolean matrixBot = false;

    @RegisterSubModule(name = "Remove From World")
    public static boolean removeFromWorld = false;

    @RegisterSubModule(name = "Remove Interval", min = 5, max = 100, increment = 5, parent = "Remove From World")
    public static double removeInterval = 20;

    // ── Internal State Tracking ─────────────────────────────────────────────
    private static final Set<Integer> touchedGround = new HashSet<>();
    private static final Set<Integer> touchedAir = new HashSet<>();
    private static final Map<Integer, Integer> invalidGroundVL = new HashMap<>();
    private static final Set<Integer> swung = new HashSet<>();
    private static final Set<Integer> invisible = new HashSet<>();
    private static final Set<Integer> hasRemovedEntities = new HashSet<>();
    private static final Set<Integer> spawnedInCombat = new HashSet<>();
    private static final Set<Integer> hit = new HashSet<>();
    private static final Set<UUID> duplicate = new HashSet<>();
    private static final Map<EntityPlayer, double[]> matrixSamples = new HashMap<>();
    private static final Set<EntityPlayer> matrixNotAlwaysInRadius = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean matrixCollectSample = true;

    // ── Event Handlers ──────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (!ModuleManager.isEnabled(AntiBot.class) || C.p() == null || C.w() == null) return;
        handleMatrixBot();

        if (!removeFromWorld || C.p().ticksExisted <= 0 || C.p().ticksExisted % (int) removeInterval != 0) return;

        List<EntityPlayer> bots = new ArrayList<>();
        for (EntityPlayer player : C.w().playerEntities) {
            if (player != C.p() && isBot(player)) bots.add(player);
        }
        if (bots.isEmpty()) return;

        for (EntityPlayer bot : bots) {
            C.w().removeEntity(bot);
        }
    }

    @SubscribeEvent
    public static void onPacketReceive(PacketEvent.Receive event) {
        if (!ModuleManager.isEnabled(AntiBot.class) || C.p() == null || C.w() == null) return;
        Packet<?> packet = event.packet;

        if (packet instanceof S14PacketEntity) {
            handleEntityPacket((S14PacketEntity) packet);
        } else if (packet instanceof S0BPacketAnimation) {
            handleAnimationPacket((S0BPacketAnimation) packet);
        } else if (packet instanceof S38PacketPlayerListItem) {
            handlePlayerListPacket((S38PacketPlayerListItem) packet);
        } else if (packet instanceof S0CPacketSpawnPlayer) {
            handleSpawnPlayerPacket((S0CPacketSpawnPlayer) packet);
        } else if (packet instanceof S13PacketDestroyEntities) {
            for (int id : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                hasRemovedEntities.add(id);
            }
        }
    }

    @SubscribeEvent
    public static void onPacketSend(PacketEvent.Send event) {
        if (!ModuleManager.isEnabled(AntiBot.class) || C.p() == null || C.w() == null) return;
        if (event.packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity attackPacket = (C02PacketUseEntity) event.packet;
            if (attackPacket.getAction() == C02PacketUseEntity.Action.ATTACK) {
                Entity target = attackPacket.getEntityFromWorld(C.w());
                if (target instanceof EntityLivingBase) {
                    hit.add(target.getEntityId());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldUnloadEvent event) {
        clearAll();
    }

    @SubscribeEvent
    public static void onRespawn(RespawnEvent event) {
        clearAll();
    }

    // ── Matrix Bot Detection ────────────────────────────────────────────────
    private static void handleMatrixBot() {
        if (!matrixBot || C.w() == null || C.p() == null) return;

        if (matrixNotAlwaysInRadius.size() > 1000) matrixNotAlwaysInRadius.clear();
        matrixSamples.keySet().removeIf(player -> !C.w().loadedEntityList.contains(player) || matrixNotAlwaysInRadius.contains(player));

        for (Entity entity : C.w().loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == C.p()) continue;

            EntityPlayer player = (EntityPlayer) entity;
            if (!isInMatrixCheckArea(player, 10.0F) && !matrixNotAlwaysInRadius.contains(player)) {
                matrixNotAlwaysInRadius.add(player);
                matrixSamples.remove(player);
            }
        }

        if (matrixCollectSample) {
            matrixSamples.clear();
            for (Entity entity : C.w().loadedEntityList) {
                if (!(entity instanceof EntityPlayer) || entity == C.p()) continue;

                EntityPlayer player = (EntityPlayer) entity;
                if (matrixNotAlwaysInRadius.contains(player)) continue;

                matrixSamples.put(player, new double[]{player.posX, player.posZ});
            }
        } else {
            List<EntityPlayer> bots = new ArrayList<>();
            for (Map.Entry<EntityPlayer, double[]> entry : matrixSamples.entrySet()) {
                EntityPlayer player = entry.getKey();
                double[] sample = entry.getValue();
                if (player == null || matrixNotAlwaysInRadius.contains(player) || !C.w().loadedEntityList.contains(player)) continue;

                double xDiff = sample[0] - player.posX;
                double zDiff = sample[1] - player.posZ;
                double speed = Math.sqrt(xDiff * xDiff + zDiff * zDiff) * 10.0;

                if (isMatrixBot(player, speed)) bots.add(player);
            }

            for (EntityPlayer bot : bots) {
                C.w().removeEntity(bot);
                matrixSamples.remove(bot);
            }
        }

        matrixCollectSample = !matrixCollectSample;
    }

    private static boolean isMatrixBot(EntityPlayer player, double speed) {
        return player != C.p()
                && !matrixNotAlwaysInRadius.contains(player)
                && speed > 8.0
                && isInMatrixCheckArea(player, 5.0F);
    }

    private static boolean isInMatrixCheckArea(EntityPlayer player, float radius) {
        return C.p().getDistanceToEntity(player) <= radius
                && within(player.posY, C.p().posY - 1.5, C.p().posY + 1.5);
    }

    private static boolean within(double value, double min, double max) {
        return value >= min && value <= max;
    }

    // ── Packet Parsers ──────────────────────────────────────────────────────
    private static void handleEntityPacket(S14PacketEntity packet) {
        if (C.w() == null) return;
        Entity entity = packet.getEntity(C.w());
        if (!(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;
        int id = player.getEntityId();
        if (packet.getOnGround()) touchedGround.add(id);
        else touchedAir.add(id);

        if (packet.getOnGround()) {
            if (player.prevPosY != player.posY) invalidGroundVL.put(id, invalidGroundVL.getOrDefault(id, 0) + 1);
        } else {
            int vl = invalidGroundVL.getOrDefault(id, 0) / 2;
            if (vl <= 0) invalidGroundVL.remove(id);
            else invalidGroundVL.put(id, vl);
        }

        if (player.isInvisible()) invisible.add(id);
    }

    private static void handleAnimationPacket(S0BPacketAnimation packet) {
        if (C.w() == null) return;
        Entity entity = C.w().getEntityByID(packet.getEntityID());
        if (entity instanceof EntityLivingBase && packet.getAnimationType() == 0) {
            swung.add(entity.getEntityId());
        }
    }

    private static void handlePlayerListPacket(S38PacketPlayerListItem packet) {
        if (C.w() == null || C.mc.getNetHandler() == null) return;
        if (duplicateCompareMode != DuplicateCompareMode.WhenSpawn || packet.getAction() != S38PacketPlayerListItem.Action.ADD_PLAYER) return;

        for (S38PacketPlayerListItem.AddPlayerData entry : packet.getEntries()) {
            if (entry.getProfile() == null) continue;
            String name = entry.getProfile().getName();
            boolean duplicateWorld = duplicateInWorld && C.w().playerEntities.stream().anyMatch(player -> player.getName().equals(name));
            boolean duplicateTab = duplicateInTab && C.mc.getNetHandler().getPlayerInfoMap().stream().anyMatch(info -> info.getGameProfile() != null && info.getGameProfile().getName().equals(name));
            if (duplicateWorld || duplicateTab) duplicate.add(entry.getProfile().getId());
        }
    }

    private static void handleSpawnPlayerPacket(S0CPacketSpawnPlayer packet) {
        if (KillAura.target != null && !hasRemovedEntities.contains(packet.getEntityID())) {
            spawnedInCombat.add(packet.getEntityID());
        }
    }

    // ── Bot Checker API ─────────────────────────────────────────────────────
    public static boolean isBot(EntityLivingBase entity) {
        if (!ModuleManager.isEnabled(AntiBot.class)) return false;
        return isBotPlayer(entity);
    }

    public static boolean isBotPlayer(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity == C.p()) return false;
        if (!ModuleManager.isEnabled(AntiBot.class)) return false;

        EntityPlayer player = (EntityPlayer) entity;
        int id = player.getEntityId();

        if (matrixBot && isInvalidMatrixBotArmor(player)) return true;
        if (!basic) return false;

        if (experimentalNPCDetection) {
            String display = strip(player.getDisplayName().getUnformattedText()).toLowerCase(Locale.ROOT);
            if (display.contains("npc") || display.contains("cit-")) return true;
        }
        if (illegalName && (player.getName().contains(" ") || player.getDisplayName().getUnformattedText().contains(" "))) return true;
        if (color && !player.getDisplayName().getFormattedText().replace("§r", "").contains("§")) return true;
        if (livingTime && player.ticksExisted < (int) livingTimeTicks) return true;
        if (ground && !touchedGround.contains(id)) return true;
        if (air && !touchedAir.contains(id)) return true;
        if (spawnInCombat && spawnedInCombat.contains(id)) return true;
        if (swing && !swung.contains(id)) return true;
        if (health && (player.getHealth() > maxHealth || player.getHealth() < minHealth)) return true;
        if (entityID && (id >= 1000000000 || id <= -1)) return true;
        if (derp && (player.rotationPitch > 90.0F || player.rotationPitch < -90.0F)) return true;
        if (wasInvisible && invisible.contains(id)) return true;
        if (armor && hasNoArmor(player)) return true;
        if (ping) {
            if (C.mc.getNetHandler() != null) {
                NetworkPlayerInfo info = C.mc.getNetHandler().getPlayerInfo(player.getUniqueID());
                if (info != null && info.getResponseTime() <= 0) return true;
            }
        }
        if (needHit && !hit.contains(id)) return true;
        if (invalidGround && invalidGroundVL.getOrDefault(id, 0) >= 10) return true;
        if (tab && !isInTab(player)) return true;
        if (duplicateCompareMode == DuplicateCompareMode.WhenSpawn && duplicate.contains(player.getGameProfile().getId())) return true;
        if (duplicateInWorld && duplicateCompareMode == DuplicateCompareMode.OnTime && hasDuplicateInWorld(player)) return true;
        if (duplicateInTab && duplicateCompareMode == DuplicateCompareMode.OnTime && hasDuplicateInTab(player)) return true;

        return player.getName().isEmpty() || player.getName().equals(C.p().getName());
    }

    private static boolean hasNoArmor(EntityPlayer player) {
        return player.inventory.armorInventory[0] == null
                && player.inventory.armorInventory[1] == null
                && player.inventory.armorInventory[2] == null
                && player.inventory.armorInventory[3] == null;
    }

    private static boolean isInvalidMatrixBotArmor(EntityPlayer player) {
        ItemStack helmet = player.inventory.armorInventory[3];
        ItemStack chestplate = player.inventory.armorInventory[2];
        if (helmet == null || chestplate == null) return true;
        if (!(helmet.getItem() instanceof ItemArmor) || !(chestplate.getItem() instanceof ItemArmor)) return true;

        int helmetColor = ((ItemArmor) helmet.getItem()).getColor(helmet);
        int chestplateColor = ((ItemArmor) chestplate.getItem()).getColor(chestplate);
        return !(chestplateColor > 0 && helmetColor > 0 && chestplateColor == helmetColor);
    }

    private static boolean isInTab(EntityPlayer player) {
        if (C.mc.getNetHandler() == null) return false;
        boolean equals = tabMode == TabMode.Equals;
        String targetName = strip(player.getDisplayName().getFormattedText());
        for (NetworkPlayerInfo info : C.mc.getNetHandler().getPlayerInfoMap()) {
            String networkName = strip(getNetworkName(info));
            if (equals ? targetName.equalsIgnoreCase(networkName) : targetName.contains(networkName)) return true;
        }
        return false;
    }

    private static String getNetworkName(NetworkPlayerInfo info) {
        if (info == null) return "";
        if (info.getDisplayName() != null) return info.getDisplayName().getFormattedText();
        return info.getGameProfile() == null ? "" : info.getGameProfile().getName();
    }

    private static boolean hasDuplicateInWorld(EntityPlayer player) {
        if (C.w() == null) return false;
        String name = player.getName();
        return C.w().loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityPlayer && name.equals(entity.getName()))
                .count() > 1;
    }

    private static boolean hasDuplicateInTab(EntityPlayer player) {
        if (C.mc.getNetHandler() == null) return false;
        String name = player.getName();
        return C.mc.getNetHandler().getPlayerInfoMap().stream()
                .filter(info -> info.getGameProfile() != null && name.equals(info.getGameProfile().getName()))
                .count() > 1;
    }

    private static String strip(String text) {
        if (text == null) return "";
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        return stripped == null ? "" : stripped;
    }

    private static void clearAll() {
        hit.clear();
        swung.clear();
        touchedGround.clear();
        touchedAir.clear();
        invalidGroundVL.clear();
        invisible.clear();
        hasRemovedEntities.clear();
        spawnedInCombat.clear();
        duplicate.clear();
        matrixSamples.clear();
        matrixNotAlwaysInRadius.clear();
        matrixCollectSample = true;
    }

    @Override
    protected void onEnable() {
        clearAll();
    }

    @Override
    protected void onDisable() {
        clearAll();
    }
}
