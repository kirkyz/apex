package com.apex.module.impl;

import com.apex.module.Category;
import com.apex.module.Module;
import com.apex.util.Colors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/** Skeet-style event log in the top-left corner. */
public class HitLog extends Module {
	private record Entry(String text, long time) {}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	public HitLog() {
		super("HitLog", Category.HUD);
	}

	public static synchronized void push(String s) {
		ENTRIES.add(new Entry(s, System.currentTimeMillis()));
		while (ENTRIES.size() > 7) ENTRIES.remove(0);
	}

	@Override
	public void onAttack(Entity t) {
		if (mc.player == null || !(t instanceof LivingEntity)) return;
		push("Hit " + t.getName().getString() + " (" + String.format("%.1f", mc.player.distanceTo(t)) + "m)");
	}

	@Override
	public synchronized void onHud(DrawContext ctx, float td) {
		long now = System.currentTimeMillis();
		ENTRIES.removeIf(e -> now - e.time > 4000);
		int y = 8;
		for (Entry e : ENTRIES) {
			long age = now - e.time;
			int a = age > 3000 ? (int) (255 * (1 - (age - 3000) / 1000f)) : 255;
			if (a < 8) continue;
			int w = mc.textRenderer.getWidth(e.text) + 8;
			ctx.fill(8, y - 2, 8 + w, y + 10, Colors.alpha(0x0C0C0C, a * 200 / 255));
			ctx.fill(8, y - 2, 10, y + 10, Colors.alpha(Colors.wave(y * 0.01f), a));
			ctx.drawTextWithShadow(mc.textRenderer, e.text, 14, y, Colors.alpha(0xFFFFFF, a));
			y += 14;
		}
	}
}
