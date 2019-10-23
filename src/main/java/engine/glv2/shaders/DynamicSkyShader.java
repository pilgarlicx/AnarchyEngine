/*
 * This file is part of Light Engine
 * 
 * Copyright (C) 2016-2019 Lux Vacuos
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package engine.glv2.shaders;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.entities.CubeMapCamera;
import engine.glv2.shaders.data.Attribute;
import engine.glv2.shaders.data.UniformBoolean;
import engine.glv2.shaders.data.UniformDynamicSky;
import engine.glv2.shaders.data.UniformFloat;
import engine.glv2.shaders.data.UniformMatrix4;
import engine.glv2.shaders.data.UniformVec3;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.DynamicSkybox;

public class DynamicSkyShader extends ShaderProgram {

	private UniformMatrix4 projectionMatrix = new UniformMatrix4("projectionMatrix");
	private UniformMatrix4 transformationMatrix = new UniformMatrix4("transformationMatrix");
	private UniformMatrix4 viewMatrix = new UniformMatrix4("viewMatrix");
	private UniformFloat time = new UniformFloat("time");
	private UniformVec3 lightPosition = new UniformVec3("lightPosition");
	private UniformBoolean renderSun = new UniformBoolean("renderSun");
	private UniformVec3 cameraPosition = new UniformVec3("cameraPosition");

	private UniformDynamicSky dynamicSky = new UniformDynamicSky("dynamicSky");

	private Matrix4f temp = new Matrix4f();

	public DynamicSkyShader() {
		super("assets/shaders/DynamicSkyShader.vs", "assets/shaders/DynamicSkyShader.fs", new Attribute(0, "position"),
				new Attribute(1, "textureCoords"), new Attribute(2, "normal"));
		super.storeUniforms(projectionMatrix, transformationMatrix, viewMatrix, time, lightPosition, renderSun,
				cameraPosition, dynamicSky);
		super.validate();
	}

	public void loadCamera(Camera camera, Matrix4f projection) {
		projectionMatrix.loadMatrix(projection);
		temp.set(camera.getViewMatrix().getInternal());
		temp._m30(0);
		temp._m31(0);
		temp._m32(0);
		viewMatrix.loadMatrix(temp);
		cameraPosition.loadVec3(camera.getPosition().getInternal());
	}

	public void loadCamera(CubeMapCamera camera) {
		projectionMatrix.loadMatrix(camera.getProjectionMatrix());
		temp.set(camera.getViewMatrix());
		temp._m30(0);
		temp._m31(0);
		temp._m32(0);
		viewMatrix.loadMatrix(temp);
		cameraPosition.loadVec3(camera.getPosition());
	}

	public void loadTransformationMatrix(Matrix4f mat) {
		transformationMatrix.loadMatrix(mat);
	}

	public void loadLightPosition(Vector3f pos) {
		lightPosition.loadVec3(pos);
	}

	public void renderSun(boolean val) {
		renderSun.loadBoolean(val);
	}

	public void loadDynamicSky(DynamicSkybox sky) {
		dynamicSky.loadLight(sky);
	}

}