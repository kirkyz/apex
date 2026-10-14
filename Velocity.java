package com.apex.module.impl;

import com.apex.module.Category;
import com.apex.module.Module;
import com.apex.setting.NumberSetting;

public class Velocity extends Module {
	public final NumberSetting horizontal = add(new NumberSetting("Horizontal %", 0, 0, 100, 1));
	public final NumberSetting vertical = add(new NumberSetting("Vertical %", 0, 0, 100, 1));

	public Velocity() {
		super("Velocity", Category.COMBAT);
	}
}
