/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts.light;

import org.joml.Vector3f;
import org.luaj.vm2.LuaValue;

import engine.gl.IPipeline;
import engine.gl.lights.DirectionalLightInternal;
import engine.lua.lib.EnumType;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Vector3;
import engine.lua.type.object.LightBase;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;
import lwjgui.paint.Color;

public class DirectionalLight extends LightBase<DirectionalLightInternal> implements TreeViewable {

	private static final LuaValue C_SHADOWDISTANCE = LuaValue.valueOf("ShadowDistance");
	private static final LuaValue C_DIRECTION = LuaValue.valueOf("Direction");
	private static final LuaValue C_SHADOWMAPSIZE = LuaValue.valueOf("ShadowMapSize");

	public DirectionalLight() {
		super("DirectionalLight");

		// Lock position field (from LightBase)
		this.getField(C_POSITION).setLocked(true);

		// Define direction field
		this.defineField(C_DIRECTION, new Vector3(1, 1, 1), false);

		// Shadow distance
		this.defineField(C_SHADOWDISTANCE, LuaValue.valueOf(50), false);

		this.defineField(C_SHADOWMAPSIZE, LuaValue.valueOf(1024), false);
		this.getField(C_SHADOWMAPSIZE).setEnum(new EnumType("TextureSize"));

		this.changedEvent().connect((args) -> {
			LuaValue key = args[0];
			LuaValue value = args[1];

			if (light != null) {
				if (key.eq_b(C_INTENSITY)) {
					light.intensity = value.tofloat();
				} else if (key.eq_b(C_COLOR)) {
					Color color = ((Color3) value).toColor();
					light.color = new Vector3f(Math.max(color.getRed(), 1) / 255f, Math.max(color.getGreen(), 1) / 255f,
							Math.max(color.getBlue(), 1) / 255f);
				} else if (key.eq_b(C_SHADOWDISTANCE)) {
					light.setShadowDistance(value.toint());
				} else if (key.eq_b(C_DIRECTION)) {
					light.direction = ((Vector3) value).toJoml();
				} else if (key.eq_b(C_SHADOWS)) {
					light.shadows = value.toboolean();
				} else if(key.eq_b(C_SHADOWMAPSIZE)) {
					light.setSize(value.toint());
				}
			}
		});
	}

	@Override
	protected void destroyLight(IPipeline pipeline) {
		if (light == null || pipeline == null)
			return;

		if ( pipeline.getDirectionalLightHandler() != null )
			pipeline.getDirectionalLightHandler().removeLight(light);
		
		light = null;
		this.pipeline = null;

		System.out.println("Destroyed light");
	}

	@Override
	protected void makeLight(IPipeline pipeline) {
		// Add it to pipeline
		this.pipeline = pipeline;
		
		if (pipeline == null)
			return;

		if (light != null)
			return;

		// Create light
		Vector3f direction = ((Vector3) this.get(C_DIRECTION)).toJoml();
		float intensity = this.get(C_INTENSITY).tofloat();
		light = new DirectionalLightInternal(direction, intensity);
		light.distance = this.get(C_SHADOWDISTANCE).toint();

		// Color it
		Color color = ((Color3) this.get("Color")).toColor();
		light.color = new Vector3f(Math.max(color.getRed(), 1) / 255f, Math.max(color.getGreen(), 1) / 255f,
				Math.max(color.getBlue(), 1) / 255f);
		
		light.visible = this.get(C_VISIBLE).toboolean();

		light.shadowResolution = this.get(C_SHADOWMAPSIZE).toint();

		light.shadows = this.get(C_SHADOWS).toboolean();

		if ( pipeline.getDirectionalLightHandler() != null )
			pipeline.getDirectionalLightHandler().addLight(light);
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_light_directional;
	}
}
