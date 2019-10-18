package engine.gl.light;

import org.joml.Vector3f;

import engine.Game;
import engine.InternalGameThread;
import engine.InternalRenderThread;
import engine.glv2.v2.lights.SpotLightCamera;
import engine.glv2.v2.lights.SpotLightShadowMap;

public class SpotLightInternal extends Light {
	public float outerFOV = 90;
	public float innerFOV = 70;
	public float radius = 10;
	public Vector3f direction = new Vector3f(1, 1, -1);
	public int shadowResolution = 512;
	public boolean shadows = true;;
	private SpotLightShadowMap shadowMap;
	private SpotLightCamera lightCamera;

	public SpotLightInternal(Vector3f direction, Vector3f position, float outerFOV, float innerFOV, float radius,
			float intensity) {
		this.direction.set(direction);
		this.setPosition(position);
		this.outerFOV = outerFOV;
		this.innerFOV = innerFOV;
		this.radius = radius;
		this.intensity = intensity;

		InternalGameThread.runLater(() -> {
			this.shadowResolution = Game.lighting().getShadowMapSize();
		});
	}

	public void init() {
		shadowMap = new SpotLightShadowMap(shadowResolution);
		lightCamera = new SpotLightCamera(outerFOV, shadowResolution);
		lightCamera.update(direction, getPosition());
	}

	public void update() {
		lightCamera.update(direction, getPosition());
	}

	public void setOuterFOV(float outerFOV) {
		this.outerFOV = outerFOV;
		lightCamera.setFov(outerFOV, shadowResolution);
	}

	public void setSize(int size) {
		this.shadowResolution = size;
		lightCamera.setFov(outerFOV, size);
		InternalRenderThread.runLater(() -> {
			shadowMap.resize(size);
		});
	}

	public void dispose() {
		if (shadowMap == null)
			return;
		shadowMap.dispose();
	}

	public SpotLightShadowMap getShadowMap() {
		return shadowMap;
	}

	public SpotLightCamera getLightCamera() {
		return lightCamera;
	}
}
