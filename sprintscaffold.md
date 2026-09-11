# Minecraft Forge 1.8.8 / 1.8.9 Scaffold — "Normal" Bridging Mode Implementation

I need you to implement / recreate the **"Normal"** bridging rotation mode for a Scaffold / BlockFly module in a Minecraft Forge 1.8.8 / 1.8.9 client.

---

## 1. Overview & Behavior of "Normal" Mode

In the Scaffold module, rotation modes determine when and how the player's head / server rotations look at candidate blocks.

Unlike **Telly** (which continuously looks at blocks, jumps, or holds lock) or locked-yaw modes:
- **"Normal" Mode only calculates and applies server-side rotations when a block placement is actually needed / valid.**
- When the player is **not** actively needing to place a block (e.g. standing fully over solid ground or not over air), "Normal" mode does **NOT** lock or override player rotations, allowing the player to look around freely without jitter.
- Once the player moves over air or approaches an edge (`shouldPlaceBlock()` returns `true`), the module snaps/rotates to the best target block face, executes placement via raytrace, and does not hold an artificial rotation lock afterward.

---

## 2. BridgingMode Enum Definition

Add/maintain `Normal` as the primary standard mode:

```java
public enum BridgingMode {
    Normal,
    Telly,
    Telly_Plus
}

@RegisterSubModule(name = "Rotation Mode", parent = "Rotate")
public static BridgingMode bridgingMode = BridgingMode.Normal;
```

---

## 3. Condition to Rotate (`shouldRotate()`)

`Normal` mode only rotates when a block needs to be placed:

```java
private static boolean shouldPlaceBlock() {
    return WorldUtil.isOverAir()
            && (C.p().onGround || !shouldKeepY() || WorldUtil.isOverAir(C.p().getPositionVector().subtract(0, 1, 0)));
}

private static boolean shouldRotate() {
    return rotate && (bridgingMode != BridgingMode.Normal || shouldPlaceBlock());
}
```

---

## 4. Rotation Event Handling

In the `RotationEvent` listener:

1. Validate item in hand is a valid block.
2. Predict the candidate position if not yet over air:
   ```java
   Vec3 positionToRotateFrom = C.p().getPositionVector();
   if (!shouldPlaceBlock()) {
       Vec3 predictedNextPosition = getPredictedNextPosition();
       if (predictedNextPosition != null) positionToRotateFrom = predictedNextPosition;
   }
   targetBlock = getBestTargetBlock(positionToRotateFrom);
   if (targetBlock == null) return;
   ```
3. Check `shouldRotate()` before invoking `rotate(positionToRotateFrom, targetBlock, event)`:
   ```java
   if (shouldRotate()) rotate(positionToRotateFrom, targetBlock, event);
   if (!manualPlace) tryPlace = true;
   ```

---

## 5. Rotation Calculation & Fallback Behavior

Inside `rotate(Vec3 playerPosition, BlockTarget blockTarget, RotationEvent event)`:

1. **Raytrace for best GCD-applied Pitch and Yaw**:
   - Loop pitch (0° to 90°) and relative delta yaw.
   - Apply GCD fix (`RotationUtil.applyGcd(...)`).
   - Find the angle delta with the smallest change from `PlayerUtil.lastRotation()` that raytraces onto the intended `targetBlock` face.
2. **Apply Rotations**:
   - If a valid raytrace rotation is found, set `event.rotation = bestRotation;`.
3. **No Lock-On Fallback for Normal**:
   - Other modes might lock to `PlayerUtil.lastRotation()`, but for `BridgingMode.Normal`, do NOT force `event.rotation = PlayerUtil.lastRotation()` if no rotation is needed:
   ```java
   if (bridgingMode != BridgingMode.Normal) {
       event.rotation = PlayerUtil.lastRotation();
   }
   ```

---

## 6. Target Search & Placement Mechanics

Ensure standard placement safety:
- **`getBestTargetBlock()`**: Iterates surrounding block offsets around `targetY`, avoiding `DOWN` and blocked `UP` faces (`shouldKeepY()` / `sameY`), picking the closest valid interactable block edge.
- **`tryPlace()`**: Performs standard right-click placement onto the raytraced face, swings hand, handles stack deduction, and logs interaction timing.

---

## Acceptance Criteria
- When `bridgingMode == BridgingMode.Normal`:
  - Player only rotates to place when walking over air/edges.
  - Head/server rotation is not locked when walking on solid blocks.
  - No smooth telly easing is applied.
  - Clean placement and no ghost blocks / Grim DuplicateRotPlace flags.
