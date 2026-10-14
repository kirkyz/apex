package com.apex.gui;

import com.apex.ApexClient;
import com.apex.module.Category;
import com.apex.module.Module;
import com.apex.module.impl.SpotifyHud;
import com.apex.setting.BoolSetting;
import com.apex.setting.ModeSetting;
import com.apex.setting.NumberSetting;
import com.apex.setting.Setting;
import com.apex.spotify.SpotifyManager;
import com.apex.util.Colors;
import com.apex.util.Config;
import com.apex.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Skeet-inspired menu: left tab strip, grouped boxes, gradient accents. */
public class ApexScreen extends Screen {
	private static final int W = 470, H = 350, BG = 0xFF141414;

	// hit kinds
	private static final int K_MODULE = 0, K_BOOL = 1, K_SLIDER = 2, K_MODE = 3;
	private static final int K_CONNECT = 10, K_DISCONNECT = 11, K_PREV = 12, K_PLAY = 13, K_NEXT = 14, K_SEEK = 15, K_VOL = 16;

	private static final class Hit {
		int x, y, w, h, kind;
		Module module;
		Setting setting;
	}

	private final Category[] tabs = Category.values();
	private int tab, scroll, maxScroll, wx, wy;
	private Module listening;
	private Hit dragging;
	private int localVol = -1;
	private final List<Hit> hits = new ArrayList<>();
	private float[] px, py, pvx, pvy;
	private TextFieldWidget idField;

	public ApexScreen() {
		super(Text.literal("Apex"));
	}

	// ------------------------------------------------------------------ setup

	@Override
	protected void init() {
		Random r = new Random();
		px = new float[80];
		py = new float[80];
		pvx = new float[80];
		pvy = new float[80];
		for (int i = 0; i < px.length; i++) {
			px[i] = r.nextFloat() * width;
			py[i] = r.nextFloat() * height;
			pvx[i] = (r.nextFloat() - 0.5f) * 0.3f;
			pvy[i] = -0.1f - r.nextFloat() * 0.4f;
		}
		wx = (width - W) / 2;
		wy = (height - H) / 2;
		int cx = wx + 76, cy = wy + 12, cw = W - 86;
		idField = new TextFieldWidget(textRenderer, cx + 8, cy + 14, cw - 16, 14, Text.literal("Client ID"));
		idField.setMaxLength(64);
		idField.setText(SpotifyManager.I.clientId);
		idField.setChangedListener(s -> SpotifyManager.I.setClientId(s.trim()));
		idField.setFocused(false);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		Config.save();
		super.close();
	}

	private boolean spotifyTab() {
		return tabs[tab] == Category.SPOTIFY;
	}

	// ------------------------------------------------------------------ render

	@Override
	public void render(DrawContext ctx, int mx, int my, float delta) {
		hits.clear();
		wx = (width - W) / 2;
		wy = (height - H) / 2;

		ctx.fill(0, 0, width, height, 0x66000000);

		// background particles
		for (int i = 0; i < px.length; i++) {
			px[i] += pvx[i];
			py[i] += pvy[i];
			if (py[i] < 0) { py[i] = height; px[i] = (float) (Math.random() * width); }
			if (px[i] < 0) px[i] = width;
			if (px[i] > width) px[i] = 0;
			int s = 1 + (i % 2);
			ctx.fill((int) px[i], (int) py[i], (int) px[i] + s, (int) py[i] + s,
					Colors.alpha(Colors.skeet(i / (float) px.length), 150));
		}

		// frame
		ctx.fill(wx - 1, wy - 1, wx + W + 1, wy + H + 1, 0xFF000000);
		ctx.fill(wx, wy, wx + W, wy + H, 0xFF3A3A3A);
		ctx.fill(wx + 1, wy + 1, wx + W - 1, wy + H - 1, 0xFF0F0F0F);
		ctx.fill(wx + 3, wy + 3, wx + W - 3, wy + H - 3, BG);
		RenderUtil.skeetBar(ctx, wx + 1, wy + 1, W - 2, 2);

		// tabs
		int th = 50;
		for (int i = 0; i < tabs.length; i++) {
			int ty = wy + 8 + i * th;
			boolean sel = i == tab;
			if (sel) {
				ctx.fill(wx + 3, ty, wx + 69, ty + th, 0xFF1B1B1B);
				ctx.fill(wx + 3, ty, wx + 5, ty + th, Colors.wave(i * 0.1f));
			}
			boolean hov = mx >= wx + 3 && mx < wx + 69 && my >= ty && my < ty + th;
			ctx.drawCenteredTextWithShadow(textRenderer, tabs[i].label, wx + 36, ty + th / 2 - 4,
					sel ? 0xFFFFFFFF : hov ? 0xFFAAAAAA : 0xFF666666);
		}
		ctx.fill(wx + 69, wy + 3, wx + 70, wy + H - 3, 0xFF262626);

		int cx = wx + 76, cy = wy + 12, cw = W - 86, bottom = wy + H - 6;

		if (spotifyTab()) {
			renderSpotify(ctx, mx, my, delta, cx, cy, cw);
		} else {
			ctx.enableScissor(cx - 4, cy - 4, cx + cw + 4, bottom);
			renderModules(ctx, cx, cy, cw, bottom);
			ctx.disableScissor();
		}

		// footer hint
		if (listening != null) {
			ctx.drawCenteredTextWithShadow(textRenderer, "press a key for " + listening.name + " (ESC = unbind)",
					wx + W / 2, wy + H + 4, 0xFFFFCC55);
		}
	}

	// ---------- module tabs

	private void renderModules(DrawContext ctx, int cx, int cy, int cw, int bottom) {
		List<Module> mods = ApexClient.modules.in(tabs[tab]);
		int colW = (cw - 10) / 2;
		int[] colY = {cy - scroll, cy - scroll};
		for (Module m : mods) {
			int col = colY[0] <= colY[1] ? 0 : 1;
			int gx = cx + col * (colW + 10);
			int h = heightOf(m);
			drawModule(ctx, m, gx, colY[col], colW, h);
			colY[col] += h + 8;
		}
		int total = Math.max(colY[0], colY[1]) + scroll - cy;
		maxScroll = Math.max(0, total - (bottom - cy));
		scroll = MathHelper.clamp(scroll, 0, maxScroll);
	}

	private int rowH(Setting s) {
		return s instanceof NumberSetting ? 24 : s instanceof ModeSetting ? 28 : 14;
	}

	private int heightOf(Module m) {
		int h = 14 + 14 + 6;
		for (Setting s : m.settings) h += rowH(s);
		return h;
	}

	private void groupBox(DrawContext ctx, int x, int y, int w, int h, String title) {
		ctx.drawBorder(x, y + 4, w, h - 4, 0xFF2C2C2C);
		int tw = textRenderer.getWidth(title);
		ctx.fill(x + 6, y + 3, x + 12 + tw, y + 6, BG);
		ctx.drawText(textRenderer, title, x + 9, y, 0xFFD0D0D0, false);
	}

	private void checkbox(DrawContext ctx, int x, int y, boolean on) {
		ctx.fill(x, y, x + 9, y + 9, 0xFF000000);
		ctx.fill(x + 1, y + 1, x + 8, y + 8, 0xFF2A2A2A);
		if (on) {
			int c = Colors.wave(0f);
			ctx.fillGradient(x + 1, y + 1, x + 8, y + 8, c, Colors.darken(c));
		}
	}

	private void addHit(int x, int y, int w, int h, int kind, Module m, Setting s) {
		Hit hit = new Hit();
		hit.x = x; hit.y = y; hit.w = w; hit.h = h; hit.kind = kind; hit.module = m; hit.setting = s;
		hits.add(hit);
	}

	private String keyName(int key) {
		if (key == -1) return "none";
		String n = InputUtil.fromKeyCode(key, -1).getLocalizedText().getString().toUpperCase();
		return n.length() > 7 ? n.substring(0, 7) : n;
	}

	private void drawModule(DrawContext ctx, Module m, int gx, int gy, int colW, int h) {
		groupBox(ctx, gx, gy, colW, h, m.name);
		int ry = gy + 14;

		// enable row
		checkbox(ctx, gx + 8, ry, m.enabled);
		ctx.drawText(textRenderer, "Enabled", gx + 22, ry + 1, m.enabled ? 0xFFFFFFFF : 0xFF9A9A9A, false);
		String bind = listening == m ? "[...]" : "[" + keyName(m.key) + "]";
		ctx.drawText(textRenderer, bind, gx + colW - 8 - textRenderer.getWidth(bind), ry + 1, 0xFF6E6E6E, false);
		addHit(gx + 6, ry - 1, colW - 12, 11, K_MODULE, m, null);
		ry += 14;

		for (Setting s : m.settings) {
			if (s instanceof BoolSetting b) {
				checkbox(ctx, gx + 8, ry, b.get());
				ctx.drawText(textRenderer, b.name, gx + 22, ry + 1, b.get() ? 0xFFFFFFFF : 0xFF9A9A9A, false);
				addHit(gx + 6, ry - 1, colW - 12, 11, K_BOOL, m, b);
			} else if (s instanceof NumberSetting n) {
				ctx.drawText(textRenderer, n.name, gx + 8, ry, 0xFFBDBDBD, false);
				String val = n.step >= 1 ? String.valueOf(n.getI()) : String.format("%.2f", n.get());
				ctx.drawText(textRenderer, val, gx + colW - 8 - textRenderer.getWidth(val), ry, 0xFF8A8A8A, false);
				int sx = gx + 8, sy = ry + 11, sw = colW - 16;
				float t = (float) ((n.get() - n.min) / (n.max - n.min));
				ctx.fill(sx, sy, sx + sw, sy + 5, 0xFF000000);
				ctx.fill(sx + 1, sy + 1, sx + sw - 1, sy + 4, 0xFF2A2A2A);
				RenderUtil.gradientH(ctx, sx + 1, sy + 1, sx + 1 + (int) ((sw - 2) * t), sy + 4, Colors.C1, Colors.skeet(t));
				addHit(sx, sy - 3, sw, 11, K_SLIDER, m, n);
			} else if (s instanceof ModeSetting md) {
				ctx.drawText(textRenderer, md.name, gx + 8, ry, 0xFFBDBDBD, false);
				int bx = gx + 8, by = ry + 10, bw = colW - 16;
				ctx.fill(bx, by, bx + bw, by + 13, 0xFF000000);
				ctx.fill(bx + 1, by + 1, bx + bw - 1, by + 12, 0xFF1F1F1F);
				ctx.drawText(textRenderer, md.get(), bx + 5, by + 3, 0xFFCFCFCF, false);
				ctx.drawText(textRenderer, "<>", bx + bw - 14, by + 3, 0xFF6E6E6E, false);
				addHit(bx, by, bw, 13, K_MODE, m, md);
			}
			ry += rowH(s);
		}
	}

	// ---------- spotify tab

	private void button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, int kind) {
		boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
		ctx.fill(x, y, x + w, y + h, 0xFF000000);
		ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, hov ? 0xFF2E2E2E : 0xFF1F1F1F);
		ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + (h - 8) / 2 + 1, hov ? 0xFFFFFFFF : 0xFFBBBBBB);
		addHit(x, y, w, h, kind, null, null);
	}

	private void renderSpotify(DrawContext ctx, int mx, int my, float delta, int cx, int cy, int cw) {
		SpotifyManager s = SpotifyManager.I;

		// connection box
		groupBox(ctx, cx, cy, cw, 100, "connection");
		idField.render(ctx, mx, my, delta);
		button(ctx, cx + 8, cy + 34, 70, 14, "connect", mx, my, K_CONNECT);
		button(ctx, cx + 84, cy + 34, 70, 14, "disconnect", mx, my, K_DISCONNECT);
		boolean ok = s.status.startsWith("Connected");
		ctx.drawText(textRenderer, "status: " + s.status, cx + 8, cy + 54, ok ? 0xFF9ACD32 : 0xFFE0A030, false);
		ctx.drawText(textRenderer, "1) Create an app at developer.spotify.com (any name).", cx + 8, cy + 68, 0xFF6E6E6E, false);
		ctx.drawText(textRenderer, "2) Add redirect URI: http://127.0.0.1:8888/callback", cx + 8, cy + 78, 0xFF6E6E6E, false);
		ctx.drawText(textRenderer, "3) Paste its Client ID above and press connect.", cx + 8, cy + 88, 0xFF6E6E6E, false);

		// now playing box
		int gy = cy + 108;
		groupBox(ctx, cx, gy, cw, 134, "now playing");
		int ax = cx + 8, ay = gy + 14;
		if (s.connected && s.artTexture != null) {
			ctx.drawTexture(s.artTexture, ax, ay, 64, 64, 0, 0, 64, 64, 64, 64);
		} else {
			ctx.fill(ax, ay, ax + 64, ay + 64, 0xFF000000);
			ctx.fill(ax + 1, ay + 1, ax + 63, ay + 63, 0xFF1F1F1F);
			ctx.drawCenteredTextWithShadow(textRenderer, "\u266A", ax + 32, ay + 28, Colors.wave(0));
		}

		int tx = cx + 80, tw = cw - 96;
		if (s.connected && s.hasTrack) {
			SpotifyHud.marquee(ctx, textRenderer, s.title, tx, gy + 14, tw, 0xFFFFFFFF);
			SpotifyHud.marquee(ctx, textRenderer, s.artist, tx, gy + 26, tw, 0xFF9A9A9A);
			SpotifyHud.marquee(ctx, textRenderer, s.album, tx, gy + 38, tw, 0xFF666666);
		} else {
			ctx.drawText(textRenderer, s.connected ? "Nothing playing" : "Not connected", tx, gy + 14, 0xFF9A9A9A, false);
		}

		// progress / seek
		int by = gy + 56;
		long prog = s.progress();
		float f = s.durationMs <= 0 ? 0f : Math.min(1f, prog / (float) s.durationMs);
		ctx.fill(tx, by, tx + tw, by + 6, 0xFF000000);
		ctx.fill(tx + 1, by + 1, tx + tw - 1, by + 5, 0xFF2A2A2A);
		RenderUtil.gradientH(ctx, tx + 1, by + 1, tx + 1 + (int) ((tw - 2) * f), by + 5, Colors.C1, Colors.skeet(f));
		addHit(tx, by - 3, tw, 12, K_SEEK, null, null);
		ctx.drawText(textRenderer, SpotifyHud.fmt(prog) + " / " + SpotifyHud.fmt(s.durationMs), tx, by + 10, 0xFF777777, false);

		// transport buttons
		int bty = gy + 80;
		button(ctx, tx, bty, 32, 16, "<<", mx, my, K_PREV);
		button(ctx, tx + 36, bty, 32, 16, s.playing ? "||" : ">", mx, my, K_PLAY);
		button(ctx, tx + 72, bty, 32, 16, ">>", mx, my, K_NEXT);

		// volume
		int vx = tx + 120, vy = bty + 5, vw = Math.max(60, tw - 140);
		int vol = localVol >= 0 ? localVol : Math.max(0, s.volume);
		ctx.drawText(textRenderer, "vol " + vol, tx + 112 - textRenderer.getWidth("vol " + vol) + 20, bty + 4, 0xFF9A9A9A, false);
		vx = tx + 144;
		vw = tw - 144;
		ctx.fill(vx, vy, vx + vw, vy + 6, 0xFF000000);
		ctx.fill(vx + 1, vy + 1, vx + vw - 1, vy + 5, 0xFF2A2A2A);
		float vf = vol / 100f;
		RenderUtil.gradientH(ctx, vx + 1, vy + 1, vx + 1 + (int) ((vw - 2) * vf), vy + 5, Colors.C1, Colors.skeet(vf));
		addHit(vx, vy - 3, vw, 12, K_VOL, null, null);

		ctx.drawText(textRenderer, "Reading the song works on any account.", cx + 8, gy + 104, 0xFF6E6E6E, false);
		ctx.drawText(textRenderer, "Skip / pause / seek / volume need Spotify Premium.", cx + 8, gy + 114, 0xFF6E6E6E, false);
	}

	// ------------------------------------------------------------------ input

	private boolean inContent(double mx, double my) {
		return mx >= wx + 72 && my >= wy + 8 && my <= wy + H - 6;
	}

	private void setSlider(Hit h, double mx) {
		NumberSetting n = (NumberSetting) h.setting;
		double t = MathHelper.clamp((mx - h.x - 1) / (double) (h.w - 2), 0.0, 1.0);
		n.set(n.min + t * (n.max - n.min));
	}

	@Override
	public boolean mouseClicked(double mx, double my, int btn) {
		// tabs
		for (int i = 0; i < tabs.length; i++) {
			int ty = wy + 8 + i * 50;
			if (mx >= wx + 3 && mx < wx + 69 && my >= ty && my < ty + 50) {
				tab = i;
				scroll = 0;
				listening = null;
				idField.setFocused(false);
				return true;
			}
		}

		if (spotifyTab()) {
			boolean inField = idField.mouseClicked(mx, my, btn);
			idField.setFocused(inField);
			if (inField) return true;
		}

		if (inContent(mx, my)) {
			for (Hit h : hits) {
				if (mx < h.x || mx >= h.x + h.w || my < h.y || my >= h.y + h.h) continue;
				switch (h.kind) {
					case K_MODULE -> {
						if (btn == 0) h.module.toggle();
						else if (btn == 1) listening = h.module;
					}
					case K_BOOL -> { if (btn == 0) ((BoolSetting) h.setting).toggle(); }
					case K_SLIDER -> {
						if (btn == 0) {
							dragging = h;
							setSlider(h, mx);
						}
					}
					case K_MODE -> ((ModeSetting) h.setting).cycle(btn == 1 ? -1 : 1);
					case K_CONNECT -> SpotifyManager.I.connect();
					case K_DISCONNECT -> SpotifyManager.I.disconnect();
					case K_PREV -> SpotifyManager.I.previous();
					case K_PLAY -> SpotifyManager.I.togglePlay();
					case K_NEXT -> SpotifyManager.I.next();
					case K_SEEK -> {
						SpotifyManager sp = SpotifyManager.I;
						if (sp.hasTrack) {
							double t = MathHelper.clamp((mx - h.x) / (double) h.w, 0.0, 1.0);
							sp.seek((long) (t * sp.durationMs));
						}
					}
					case K_VOL -> {
						dragging = h;
						localVol = (int) (MathHelper.clamp((mx - h.x) / (double) h.w, 0.0, 1.0) * 100);
					}
				}
				return true;
			}
		}
		return super.mouseClicked(mx, my, btn);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
		if (dragging != null) {
			if (dragging.kind == K_SLIDER) setSlider(dragging, mx);
			else if (dragging.kind == K_VOL) localVol = (int) (MathHelper.clamp((mx - dragging.x) / (double) dragging.w, 0.0, 1.0) * 100);
			return true;
		}
		return super.mouseDragged(mx, my, btn, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int btn) {
		if (dragging != null && dragging.kind == K_VOL && localVol >= 0) {
			SpotifyManager.I.setVolume(localVol);
		}
		dragging = null;
		localVol = -1;
		return super.mouseReleased(mx, my, btn);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double amount) {
		if (!spotifyTab()) scroll = MathHelper.clamp(scroll - (int) (amount * 14), 0, maxScroll);
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (listening != null) {
			listening.key = keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode;
			listening = null;
			return true;
		}
		if (spotifyTab() && idField.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) idField.setFocused(false);
			else idField.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}
		if (ApexClient.openGui.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (spotifyTab() && idField.isFocused()) return idField.charTyped(chr, modifiers);
		return super.charTyped(chr, modifiers);
	}
}
