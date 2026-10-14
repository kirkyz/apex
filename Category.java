package com.apex.module;

public enum Category {
	COMBAT("rage"),
	MOVEMENT("movement"),
	VISUALS("visuals"),
	HUD("hud"),
	SPOTIFY("spotify");

	public final String label;

	Category(String label) { this.label = label; }
}
