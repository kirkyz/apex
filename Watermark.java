package com.apex.module.impl;

import com.apex.module.Category;
import com.apex.module.Module;
import com.apex.util.Colors;
import com.apex.util.RenderUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

public class Watermark extends Module {
	public Watermark() {
		super("Watermark", Category.HUD);
	}

	@Override
	public void onHud(DrawContext ctx, float td) {
		if (mc.player == null) return;
		TextRenderer tr = mc.textRenderer;
		int ping = 0;
		if (mc.getNetworkHandler() != null) {
			PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
			if (e != null) ping = e.getLatency();
		}
		String rest = " | " + mc.getSession().getUsername() + " | " + mc.getCurrentFps() + " fps | " + ping + "ms";
		String brand = "apex";
		int w = tr.getWidth(brand) + tr.getWidth(rest) + 12;
		int h = 16;
		int x = ctx.getScaledWindowWidth() - w - 8, y = 8;

		ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
		ctx.fill(x, y, x + w, y + h, 0xE00C0C0C);
		RenderUtil.skeetBar(ctx, x, y, w, 2);

		int cx = x + 6, ty = y + 5;
		for (int i = 0; i < brand.length(); i++) {
			String ch = String.valueOf(brand.charAt(i));
			ctx.drawTextWithShadow(tr, ch, cx, ty, Colors.wave(i * 0.1f));
			cx += tr.getWidth(ch);
		}
		ctx.drawTextWithShadow(tr, rest, cx, ty, 0xFFDDDDDD);
	}
}
