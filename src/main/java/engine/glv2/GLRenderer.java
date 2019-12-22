﻿/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.glv2;

import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_GREATER;
import static org.lwjgl.opengl.GL11C.GL_LESS;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glClearColor;
import static org.lwjgl.opengl.GL11C.glClearDepth;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL11C.glDepthFunc;
import static org.lwjgl.opengl.GL11C.glDisable;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.luaj.vm2.LuaValue;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBClipControl;
import org.lwjgl.opengl.GL;

import engine.Game;
import engine.application.RenderableApplication;
import engine.gl.IPipeline;
import engine.gl.LegacyPipeline;
import engine.gl.Surface;
import engine.gl.light.DirectionalLightInternal;
import engine.gl.light.SpotLightInternal;
import engine.glv2.pipeline.MultiPass;
import engine.glv2.pipeline.PostProcess;
import engine.glv2.renderers.AnimInstanceRenderer;
import engine.glv2.renderers.InstanceRenderer;
import engine.glv2.shaders.ShaderIncludes;
import engine.glv2.v2.DeferredPipeline;
import engine.glv2.v2.EnvironmentRenderer;
import engine.glv2.v2.GPUProfiler;
import engine.glv2.v2.HandlesRenderer;
import engine.glv2.v2.IRenderingData;
import engine.glv2.v2.IrradianceCapture;
import engine.glv2.v2.PostProcessPipeline;
import engine.glv2.v2.PreFilteredEnvironment;
import engine.glv2.v2.RendererData;
import engine.glv2.v2.RenderingManager;
import engine.glv2.v2.RenderingSettings;
import engine.glv2.v2.SkyRenderer;
import engine.glv2.v2.Sun;
import engine.glv2.v2.lights.DirectionalLightHandler;
import engine.glv2.v2.lights.IDirectionalLightHandler;
import engine.glv2.v2.lights.IPointLightHandler;
import engine.glv2.v2.lights.ISpotLightHandler;
import engine.glv2.v2.lights.PointLightHandler;
import engine.glv2.v2.lights.SpotLightHandler;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;
import engine.lua.type.object.services.Lighting;
import engine.observer.RenderableWorld;

public class GLRenderer implements IPipeline {

	private boolean enabled;

	private EnvironmentRenderer envRenderer;
	private EnvironmentRenderer envRendererEntities;
	private IrradianceCapture irradianceCapture;
	private PreFilteredEnvironment preFilteredEnvironment;

	private SkyRenderer skyRenderer;
	private DynamicSkybox dynamicSkybox;

	private PointLightHandler pointLightHandler;
	private DirectionalLightHandler directionalLightHandler;
	private SpotLightHandler spotLightHandler;
	private RenderingManager renderingManager;

	private HandlesRenderer handlesRenderer;

	private DeferredPipeline dp;
	private PostProcessPipeline pp;
	private RenderingSettings renderingSettings;
	private RendererData rnd;
	private IRenderingData rd;

	private Matrix4f projMatrix;
	private Camera currentCamera;
	private Sun sun;

	private GLResourceLoader loader;

	private RenderableWorld renderableWorld;

	private int width, height;
	private Vector2f size = new Vector2f();

	private boolean useARBClipControl = false;

	public GLRenderer() {
		useARBClipControl = GL.getCapabilities().GL_ARB_clip_control;

		rnd = new RendererData();
		loader = new GLResourceLoader();
		sun = new Sun();
		rd = new IRenderingData();
		renderingSettings = new RenderingSettings();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

		ShaderIncludes.processIncludeFile("assets/shaders/includes/lighting.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/materials.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/common.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/global.isl");
		ShaderIncludes.processIncludeFile("assets/shaders/includes/color.isl");

		Game.userInputService().inputBeganEvent().connect((args) -> {
			if (args[0].get("KeyCode").eq_b(LuaValue.valueOf(GLFW.GLFW_KEY_F5))) {
				System.out.println("Reloading Shaders...");
				dp.reloadShaders();
				pp.reloadShaders();
			}
		});

		init();
	}

	public void init() {
		width = RenderableApplication.windowWidth;
		height = RenderableApplication.windowHeight;

		renderingManager = new RenderingManager();

		envRenderer = new EnvironmentRenderer(32);
		envRendererEntities = new EnvironmentRenderer(128);
		irradianceCapture = new IrradianceCapture();
		rnd.irradianceCapture = irradianceCapture.getCubeTexture();
		preFilteredEnvironment = new PreFilteredEnvironment();
		rnd.brdfLUT = preFilteredEnvironment.getBRDFLUT();
		rnd.environmentMap = preFilteredEnvironment.getTexture();
		skyRenderer = new SkyRenderer(loader);
		renderingManager.addRenderer(new InstanceRenderer());
		renderingManager.addRenderer(new AnimInstanceRenderer());
		handlesRenderer = new HandlesRenderer();
		dp = new MultiPass(width, height);
		pp = new PostProcess(width, height);
		projMatrix = Maths.createProjectionMatrix(width, height, 90, 0.1f, Float.POSITIVE_INFINITY, true);

		rnd.exposure = Game.lighting().getExposure();
		directionalLightHandler = new DirectionalLightHandler(width, height);
		pointLightHandler = new PointLightHandler(width, height);
		spotLightHandler = new SpotLightHandler(width, height);
		rnd.plh = pointLightHandler;
		rnd.dlh = directionalLightHandler;
		rnd.slh = spotLightHandler;
		rnd.rs = renderingSettings;
		size.set(width, height);
		enabled = true;
	}

	private void shadowPass() {
		// TODO: Render transparent shadows using an extra texture
		if (renderingSettings.shadowsEnabled) {
			GPUProfiler.start("Shadow Pass");
			synchronized (directionalLightHandler.getLights()) {
				for (DirectionalLightInternal l : directionalLightHandler.getLights()) {
					if (!l.shadows || !l.visible)
						continue;
					l.setPosition(currentCamera.getPosition().getInternal());
					l.update();
					l.getShadowMap().bind();
					glClear(GL_DEPTH_BUFFER_BIT);
					renderingManager.renderShadow(l.getLightCamera());
					l.getShadowMap().unbind();
				}
			}
			glCullFace(GL_FRONT);
			synchronized (spotLightHandler.getLights()) {
				for (SpotLightInternal l : spotLightHandler.getLights()) {
					if (!l.shadows || !l.visible)
						continue;
					l.update();
					l.getShadowMap().bind();
					glClear(GL_DEPTH_BUFFER_BIT);
					renderingManager.renderShadow(l.getLightCamera());
					l.getShadowMap().unbind();
				}
			}
			glCullFace(GL_BACK);
			GPUProfiler.end();
		}
	}

	private void environmentPass() {
		GPUProfiler.start("Environment Pass");
		GPUProfiler.start("Irradiance");
		GPUProfiler.start("CubeMap Render");
		envRenderer.renderIrradiance(skyRenderer, sun, rd, rnd);
		GPUProfiler.end();
		GPUProfiler.start("Irradiance Capture");
		irradianceCapture.render(envRenderer.getCubeTexture());
		GPUProfiler.end();
		GPUProfiler.end();
		GPUProfiler.start("Reflections");
		GPUProfiler.start("CubeMap Render");
		envRendererEntities.renderReflections(skyRenderer, sun, rd, rnd, renderingManager);
		GPUProfiler.end();
		GPUProfiler.start("PreFilteredEnvironment");
		preFilteredEnvironment.render(envRendererEntities.getCubeTexture().getTexture());
		GPUProfiler.end();
		GPUProfiler.end();
		GPUProfiler.end();
	}

	private void occlusionPass() {
		glClear(GL_DEPTH_BUFFER_BIT);
	}

	private void gBufferPass() {
		GPUProfiler.start("G-Buffer pass");
		if (useARBClipControl) {
			ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_ZERO_TO_ONE);
			glDepthFunc(GL_GREATER);
			glClearDepth(0.0);
		}
		dp.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		GPUProfiler.start("RenderingManager");
		renderingManager.render(rd, rnd, size);
		GPUProfiler.end();
		GPUProfiler.start("Skybox");
		skyRenderer.render(rnd, rd, sun.getLight().direction, true, true);
		GPUProfiler.end();
		dp.unbind();
		if (useARBClipControl) {
			glClearDepth(1.0);
			glDepthFunc(GL_LESS);
			ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_NEGATIVE_ONE_TO_ONE);
		}
		GPUProfiler.end();
	}

	private void deferredPass() {
		GPUProfiler.start("Lighting");
		GPUProfiler.start("Directional");
		directionalLightHandler.render(currentCamera, projMatrix, dp, renderingSettings);
		GPUProfiler.end();
		GPUProfiler.start("Point");
		pointLightHandler.render(currentCamera, projMatrix, dp, renderingSettings);
		GPUProfiler.end();
		GPUProfiler.start("Spot");
		spotLightHandler.render(currentCamera, projMatrix, dp, renderingSettings);
		GPUProfiler.end();
		GPUProfiler.end();
		GPUProfiler.start("Deferred Pass");
		dp.process(rnd, rd);
		GPUProfiler.end();
	}

	private void forwardPass() {
		GPUProfiler.start("Forward Pass");
		if (useARBClipControl) {
			ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_ZERO_TO_ONE);
			glDepthFunc(GL_GREATER);
			glClearDepth(0.0);
		}
		pp.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		dp.render(pp.getMain());
		GPUProfiler.start("RenderingManager");
		renderingManager.renderForward(rd, rnd);
		GPUProfiler.end();
		GPUProfiler.start("OutlineRendering");
		handlesRenderer.render(currentCamera, projMatrix, Game.selectedExtended(), size);
		GPUProfiler.end();
		pp.unbind();
		if (useARBClipControl) {
			glClearDepth(1.0);
			glDepthFunc(GL_LESS);
			ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_NEGATIVE_ONE_TO_ONE);
		}
		GPUProfiler.end();
	}

	private void postFXPass() {
		GPUProfiler.start("PostFX");
		pp.process(rnd, rd);
		GPUProfiler.end();
	}
	
	public RenderingSettings getRenderSettings() {
		return this.renderingSettings;
	}

	@Override
	public void render() {
		if (!enabled)
			return;
		if (this.renderableWorld == null)
			return;
		if (!Game.isLoaded())
			return;

		currentCamera = renderableWorld.getCurrentCamera();
		if (currentCamera == null)
			return;
		resetState();

		// Update Projection
		Maths.createProjectionMatrix(projMatrix, this.width, this.height, currentCamera.getFov(), 0.1f,
				Float.POSITIVE_INFINITY, useARBClipControl);

		// Set global time for clouds
		sun.update(dynamicSkybox);

		// currentCamera.getViewMatrix().getInternal().set(spotLightHandler.getLights().get(0).getLightCamera().getViewMatrix());

		// Update lighting data
		Lighting lighting = Game.lighting();
		if ( lighting != null && !lighting.isDestroyed() ) {
			rnd.ambient = lighting.getAmbient().toJOML();
			rnd.exposure = lighting.getExposure();
			rnd.gamma = lighting.getGamma();
			rnd.saturation = lighting.getSaturation();
		}

		rd.camera = currentCamera;
		rd.projectionMatrix = projMatrix;

		GPUProfiler.startFrame();

		renderingManager.preProcess(renderableWorld.getInstance());
		shadowPass();
		environmentPass();
		// occlusionPass();
		gBufferPass();
		deferredPass();
		forwardPass();
		postFXPass();
		renderingManager.end();

		pp.render();

		GPUProfiler.endFrame();

		rnd.previousViewMatrix.set(currentCamera.getViewMatrix().getInternal());
		rnd.previousProjectionMatrix.set(projMatrix);
	}

	public void setSize(int width, int height) {
		if (!enabled)
			return;
		if (this.width == width && this.height == height)
			return;
		this.width = width;
		this.height = height;
		this.size.set(width, height);
		dp.resize(width, height);
		pp.resize(width, height);
		directionalLightHandler.resize(width, height);
		pointLightHandler.resize(width, height);
		spotLightHandler.resize(width, height);
	}

	public void dispose() {
		envRenderer.dispose();
		envRendererEntities.dispose();
		dp.dispose();
		pp.dispose();
		directionalLightHandler.dispose();
		pointLightHandler.dispose();
		spotLightHandler.dispose();
		skyRenderer.dispose();
		irradianceCapture.dispose();
		preFilteredEnvironment.dispose();
		renderingManager.dispose();
		handlesRenderer.dispose();
	}

	@Override
	public void setRenderableWorld(RenderableWorld instance) {
		this.renderableWorld = instance;
		LegacyPipeline.set(this, instance);
	}

	@Override
	public RenderableWorld getRenderableWorld() {
		return this.renderableWorld;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public Surface getPipelineBuffer() {
		return pp.getFinalSurface();
	}

	@Override
	public IPointLightHandler getPointLightHandler() {
		return pointLightHandler;
	}

	@Override
	public IDirectionalLightHandler getDirectionalLightHandler() {
		return directionalLightHandler;
	}

	@Override
	public ISpotLightHandler getSpotLightHandler() {
		return spotLightHandler;
	}

	@Override
	public void setDyamicSkybox(DynamicSkybox dynamicSkybox) {
		this.dynamicSkybox = dynamicSkybox;
		skyRenderer.setDynamicSky(dynamicSkybox);
		if (dynamicSkybox != null)
			directionalLightHandler.addLight(sun.addLight());
		else
			directionalLightHandler.removeLight(sun.removeLight());
	}

	@Override
	public void setStaticSkybox(Skybox skybox) {
		skyRenderer.setStaticSky(skybox);
	}

	@Override
	public void reloadStaticSkybox() {
		skyRenderer.reloadStaticSkybox();
	}

	public void resetState() {
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glEnable(GL_DEPTH_TEST);
		glDisable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glClearColor(0.0f, 0, 0, 0.0f);
	}

}
