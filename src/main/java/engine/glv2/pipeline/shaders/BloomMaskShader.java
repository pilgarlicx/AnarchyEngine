﻿/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2.pipeline.shaders;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;

import engine.glv2.shaders.data.UniformSampler;

public class BloomMaskShader extends BasePipelineShader {

	private UniformSampler image = new UniformSampler("image");

	@Override
	protected void setupShader() {
		super.setupShader();
		super.addShader(new Shader("assets/shaders/deferred/BloomMask.fs", GL_FRAGMENT_SHADER));
		super.storeUniforms(image);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		image.loadTexUnit(0);
		super.stop();
	}

}
