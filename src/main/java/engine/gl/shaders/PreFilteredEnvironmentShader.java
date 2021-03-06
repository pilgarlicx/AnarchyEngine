/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.shaders;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

import org.joml.Matrix4f;

import engine.gl.entities.CubeMapCamera;
import engine.gl.shaders.data.Attribute;
import engine.gl.shaders.data.UniformFloat;
import engine.gl.shaders.data.UniformInteger;
import engine.gl.shaders.data.UniformMatrix4;
import engine.gl.shaders.data.UniformSampler;

public class PreFilteredEnvironmentShader extends ShaderProgram {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformSampler envMap = new UniformSampler("envMap");
	private UniformFloat roughness = new UniformFloat("roughness");
	private UniformInteger resolution = new UniformInteger("resolution");

	@Override
	protected void setupShader() {
		super.addShader(new Shader("assets/shaders/PreFilteredEnvironment.vs", GL_VERTEX_SHADER));
		super.addShader(new Shader("assets/shaders/PreFilteredEnvironment.fs", GL_FRAGMENT_SHADER));
		super.setAttributes(new Attribute(0, "position"));
		super.storeUniforms(projectionMatrix, viewMatrix, envMap, roughness, resolution);
	}

	@Override
	protected void loadInitialData() {
		super.start();
		envMap.loadTexUnit(0);
		super.stop();
	}

	public void loadviewMatrix(CubeMapCamera camera) {
		viewMatrix.loadMatrix(camera.getViewMatrix());
	}

	public void loadProjectionMatrix(Matrix4f projection) {
		projectionMatrix.loadMatrix(projection);
	}

	public void loadRoughness(float r) {
		roughness.loadFloat(r);
	}

	public void loadResolution(int resolution) {
		this.resolution.loadInteger(resolution);
	}

}
