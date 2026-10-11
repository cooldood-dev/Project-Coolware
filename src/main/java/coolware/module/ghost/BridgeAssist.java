package coolware.module.ghost;

import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.NumberSetting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class BridgeAssist extends Module {

    private final BooleanSetting prePlace = new BooleanSetting("Pre Place", this, false);
    private final NumberSetting edgeOffset = new NumberSetting("Edge Offset", this, 0.0, 0.0, 0.3, 0.01);
    private final NumberSetting unsneakDelay = new NumberSetting("Unsneak Delay", this, 50.0, 0.0, 300.0, 5.0);
    private final NumberSetting sneakOnJump = new NumberSetting("Sneak On Jump", this, 0.0, 0.0, 500.0, 10.0);
    private final BooleanSetting sneakKeyPressed = new BooleanSetting("Sneak Key Pressed", this, false);
    private final BooleanSetting holdingBlocks = new BooleanSetting("Holding Blocks", this, true);
    private final BooleanSetting lookingDown = new BooleanSetting("Looking Down", this, true);
    private final BooleanSetting notMovingForward = new BooleanSetting("Not Moving Forward", this, false);

    private boolean sneakingFromModule = false;
    private long unsneakTimer = -1;

    public BridgeAssist() {
        super("BridgeAssist", "Automatically sneaks at block edges while bridging (Ghost)", Category.GHOST, Keyboard.KEY_NONE);
    }

    @Override
    public void onDisable() {
        if (sneakingFromModule) {
            setSneak(false);
            sneakingFromModule = false;
        }
        unsneakTimer = -1;
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) {
            if (sneakingFromModule) {
                setSneak(false);
                sneakingFromModule = false;
            }
            return;
        }

        if (sneakKeyPressed.isEnabled() && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            if (sneakingFromModule) {
                setSneak(false);
                sneakingFromModule = false;
            }
            return;
        }

        if (holdingBlocks.isEnabled()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemBlock)) {
                if (sneakingFromModule) {
                    setSneak(false);
                    sneakingFromModule = false;
                }
                return;
            }
        }

        if (lookingDown.isEnabled() && mc.thePlayer.rotationPitch < 65.0f) {
            if (sneakingFromModule) {
                setSneak(false);
                sneakingFromModule = false;
            }
            return;
        }

        if (notMovingForward.isEnabled() && mc.thePlayer.movementInput.moveForward > 0) {
            if (sneakingFromModule) {
                setSneak(false);
                sneakingFromModule = false;
            }
            return;
        }

        boolean onEdge = isPlayerOverEdge();

        if (onEdge && mc.thePlayer.onGround) {
            unsneakTimer = -1;
            if (!sneakingFromModule) {
                setSneak(true);
                sneakingFromModule = true;
            }
        } else if (sneakingFromModule) {
            long delay = (long) unsneakDelay.getValue();
            if (delay <= 0) {
                setSneak(false);
                sneakingFromModule = false;
                unsneakTimer = -1;
            } else {
                if (unsneakTimer == -1) {
                    unsneakTimer = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - unsneakTimer >= delay) {
                    setSneak(false);
                    sneakingFromModule = false;
                    unsneakTimer = -1;
                }
            }
        }
    }

    private boolean isPlayerOverEdge() {
        AxisAlignedBB playerBB = mc.thePlayer.getEntityBoundingBox();
        double feetY = playerBB.minY - 0.01;
        
        // Check bounding box expanded with edgeOffset
        double offset = edgeOffset.getValue();
        AxisAlignedBB checkBB = new AxisAlignedBB(
                playerBB.minX - offset, feetY, playerBB.minZ - offset,
                playerBB.maxX + offset, feetY + 0.01, playerBB.maxZ + offset
        );

        List<AxisAlignedBB> collidingBoxes = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, checkBB);
        if (collidingBoxes.isEmpty()) {
            return true;
        }

        // Direct block under player center feet check
        BlockPos under = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ);
        return mc.theWorld.isAirBlock(under);
    }

    private void setSneak(boolean state) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), state);
    }
}
