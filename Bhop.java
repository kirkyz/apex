package com.apex.module.impl;

import com.apex.module.Category;
import com.apex.module.Module;

public class Bhop extends Module {
	public Bhop() {
		super("Bhop", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		if (mc.player == null) return;
		boolean moving = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
		if (mc.player.isOnGround() && moving && !mc.player.isSneaking()) mc.player.jump();
	}
}
