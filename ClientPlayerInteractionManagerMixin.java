package com.apex.mixin;

import com.apex.ApexClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
	@Inject(method = "attackEntity", at = @At("HEAD"))
	private void apex$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
		if (ApexClient.modules != null) ApexClient.modules.attack(target);
	}
}
