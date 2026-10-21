package com.autospeer;

import com.autospeer.mixin.MinecraftInvoker;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Bucket-swap Lunge.
 *
 * <p>A spear stab needs a full attack charge ({@code minimum_attack_charge = 1.0}), but the charge is
 * measured with the attack speed of the item that was held during the last tick. A water bucket has
 * no attack speed modifier, so it is fully charged after 5 ticks, much faster than the spear itself.
 * Switching to the spear and stabbing inside one tick keeps the bucket charge, and the Lunge
 * enchantment fires on the stab.
 *
 * <p>Cycle while the key is held: hold the bucket until charged, wait a random reaction time, switch
 * to the spear and stab, keep the spear in hand for a random 0-1 extra ticks, then back to the bucket.
 */
public class AutoSpeerClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autospeer", "main"));

	// All timings in ticks (20 ticks = 1 s), both bounds inclusive. After the swap back, the bucket
	// needs 6 ticks to charge, so one cycle takes 7 + spear hold + reaction ticks (here 0.35-0.4 s).
	private static final int SPEAR_HOLD_MIN = 0;
	private static final int SPEAR_HOLD_MAX = 1;
	private static final int REACTION_MIN = 0;
	private static final int REACTION_MAX = 0;

	private enum Phase { BUCKET, SPEAR }

	private static KeyMapping lungeKey;

	// Hotbar slot that was selected before the key was pressed, restored on release. -1 when idle.
	private static int previousSlot = -1;
	private static boolean warnedMissing = false;
	private static Phase phase = Phase.BUCKET;
	// Ticks to wait after the bucket is charged, before the stab.
	private static int reactionTicks;
	// Ticks the spear stays in hand after the stab.
	private static int spearHoldTicks;

	@Override
	public void onInitializeClient() {
		lungeKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.autospeer.lunge", InputConstants.KEY_F, CATEGORY));

		// Start of tick runs before vanilla handles key presses and before the player tick resets the
		// attack charge after a slot change.
		ClientTickEvents.START_CLIENT_TICK.register(AutoSpeerClient::tick);
	}

	private static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.gameMode == null) {
			previousSlot = -1;
			return;
		}

		// F is also vanilla "swap item with offhand". While both share a key, the offhand swap is skipped.
		if (lungeKey.same(mc.options.keySwapOffhand)) {
			while (mc.options.keySwapOffhand.consumeClick()) {
			}
		}

		Inventory inventory = player.getInventory();
		if (!lungeKey.isDown() || mc.gui.screen() != null) {
			if (previousSlot != -1) {
				inventory.setSelectedSlot(previousSlot);
				previousSlot = -1;
			}
			warnedMissing = false;
			return;
		}

		int bucketSlot = findBucketSlot(inventory);
		int spearSlot = findSpearSlot(inventory);
		if (bucketSlot == -1 || spearSlot == -1) {
			if (!warnedMissing) {
				player.sendOverlayMessage(Component.translatable("message.autospeer.missing"));
				warnedMissing = true;
			}
			return;
		}

		if (previousSlot == -1) {
			previousSlot = inventory.getSelectedSlot();
			phase = Phase.BUCKET;
			reactionTicks = randomTicks(REACTION_MIN, REACTION_MAX);
		}

		if (phase == Phase.SPEAR) {
			if (spearHoldTicks-- > 0) {
				return;
			}
			phase = Phase.BUCKET;
			reactionTicks = randomTicks(REACTION_MIN, REACTION_MAX);
		}

		// Hold the bucket so the attack charge builds up with its fast attack speed.
		if (inventory.getSelectedSlot() != bucketSlot) {
			inventory.setSelectedSlot(bucketSlot);
			return;
		}

		ItemStack spear = inventory.getItem(spearSlot);
		if (player.isUsingItem() || player.isHandsBusy() || player.cannotAttackWithItem(spear, 0)) {
			return;
		}
		if (reactionTicks-- > 0) {
			return;
		}

		// Same tick: spear in hand and stab (sends slot + STAB packet). The spear stays in hand afterwards.
		inventory.setSelectedSlot(spearSlot);
		if (((MinecraftInvoker) mc).autospeer$startAttack()) {
			phase = Phase.SPEAR;
			spearHoldTicks = randomTicks(SPEAR_HOLD_MIN, SPEAR_HOLD_MAX);
		} else {
			// Stab refused (for example miss cooldown after a click into air), retry from the bucket.
			inventory.setSelectedSlot(bucketSlot);
		}
	}

	private static int randomTicks(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	private static int findBucketSlot(Inventory inventory) {
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			if (inventory.getItem(slot).is(Items.WATER_BUCKET)) {
				return slot;
			}
		}
		return -1;
	}

	// Prefers a spear with Lunge, falls back to any spear.
	private static int findSpearSlot(Inventory inventory) {
		int fallback = -1;
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.has(DataComponents.PIERCING_WEAPON)) {
				continue;
			}
			if (stack.getEnchantments().keySet().stream().anyMatch(holder -> holder.is(Enchantments.LUNGE))) {
				return slot;
			}
			if (fallback == -1) {
				fallback = slot;
			}
		}
		return fallback;
	}
}
