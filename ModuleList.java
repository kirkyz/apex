package com.apex.module.impl;

import com.apex.ApexClient;
import com.apex.module.Category;
import com.apex.module.Module;
import com.apex.util.Colors;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleList extends Module {
	public ModuleList() {
		super("ModuleList", Category.HUD);
	}

	@Override
	public void onHud(DrawContext ctx, float td) {
		TextRenderer tr = mc.textRenderer;
		List<Module> on = new ArrayList<>();
		for (Module m : ApexClient.modules.all()) if (m.enabled && m.category != Category.HUD) on.add(m);
		on.sort(Comparator.comparingInt((Module m) -> tr.getWidth(m.name)).reversed());

		int right = ctx.getScaledWindowWidth() - 6;
		int y = 32;
		for (int i = 0; i < on.size(); i++) {
			String n = on.get(i).name;
			int w = tr.getWidth(n);
			int c = Colors.wave(i * 0.06f);
			ctx.fill(right - w - 4, y - 1, right + 2, y + 10, 0x90000000);
			ctx.fill(right + 1, y - 1, right + 3, y + 10, c);
			ctx.drawTextWithShadow(tr, n, right - w, y, c);
			y += 11;
		}
	}
}
