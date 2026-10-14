package com.apex.module;

import com.apex.module.impl.*;
import com.apex.util.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
	private final List<Module> modules = new ArrayList<>();

	public ModuleManager() {
		// combat
		modules.add(new KillAura());
		modules.add(new Criticals());
		modules.add(new Velocity());
		modules.add(new AutoTotem());
		// movement
		modules.add(new Sprint());
		modules.add(new Bhop());
		modules.add(new NoFall());
		// visuals
		modules.add(new ESP());
		modules.add(new Tracers());
		modules.add(new JumpCircles());
		modules.add(new Trail());
		modules.add(new HitParticles());
		modules.add(new Fullbright());
		modules.add(new Orbit());
		modules.add(new KillEffects());
		modules.add(new PopEffects());
		modules.add(new AmbientParticles());
		// hud
		Watermark wm = new Watermark();
		wm.enabled = true;
		modules.add(wm);
		ModuleList ml = new ModuleList();
		ml.enabled = true;
		modules.add(ml);
		HitLog hl = new HitLog();
		hl.enabled = true;
		modules.add(hl);
		modules.add(new SpotifyHud());
	}

	public List<Module> all() { return modules; }

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> c) {
		for (Module m : modules) if (c.isInstance(m)) return (T) m;
		return null;
	}

	public List<Module> in(Category c) {
		List<Module> out = new ArrayList<>();
		for (Module m : modules) if (m.category == c) out.add(m);
		return out;
	}

	public void tick() {
		for (Module m : modules) if (m.enabled) m.onTick();
	}

	public void attack(Entity target) {
		for (Module m : modules) if (m.enabled) m.onAttack(target);
	}

	public void totemPop(Entity e) {
		for (Module m : modules) if (m.enabled) m.onTotemPop(e);
	}

	public void render3D(WorldRenderContext ctx) {
		MatrixStack m = ctx.matrixStack();
		Vec3d cam = ctx.camera().getPos();
		m.push();
		m.translate(-cam.x, -cam.y, -cam.z);
		RenderUtil.begin3D(1.5f);
		for (Module mod : modules) if (mod.enabled) mod.onRender3D(ctx, m);
		RenderUtil.end3D();
		m.pop();
	}

	public void hud(DrawContext ctx, float td) {
		for (Module m : modules) if (m.enabled) m.onHud(ctx, td);
	}
}
