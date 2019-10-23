package engine.glv2.v2;

import static org.lwjgl.opengl.GL11C.GL_BACK;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_FRONT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glCullFace;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL12C.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL14C.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.glv2.GLResourceLoader;
import engine.glv2.Maths;
import engine.glv2.entities.CubeMapCamera;
import engine.glv2.objects.Framebuffer;
import engine.glv2.objects.FramebufferBuilder;
import engine.glv2.objects.Renderbuffer;
import engine.glv2.objects.RenderbufferBuilder;
import engine.glv2.objects.Texture;
import engine.glv2.objects.TextureBuilder;
import engine.glv2.objects.VAO;
import engine.glv2.shaders.DynamicSkyShader;
import engine.glv2.shaders.SphereToCubeShader;
import engine.glv2.shaders.StaticSkyShader;
import engine.lua.type.object.insts.Camera;
import engine.lua.type.object.insts.DynamicSkybox;
import engine.lua.type.object.insts.Skybox;
import engine.tasks.TaskManager;

public class SkyRenderer {

	private final float SIZE = 1;

	private final float[] CUBE = { -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE,
			SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE,
			SIZE, SIZE, SIZE, SIZE, SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, -SIZE, -SIZE,
			SIZE, SIZE, -SIZE, -SIZE, SIZE, -SIZE, -SIZE, -SIZE, -SIZE, SIZE, SIZE, -SIZE, SIZE };

	private VAO dome, cube;
	private DynamicSkyShader dynamicSkyShader;
	private StaticSkyShader staticSkyShader;
	private Vector3f pos;

	private Matrix4f infMat, regMat;

	private DynamicSkybox dynamicSky;
	private Skybox staticSky;

	private Texture cubeTex;
	private Framebuffer framebuffer;
	private Renderbuffer depthBuffer;

	public SkyRenderer(GLResourceLoader loader) {
		dome = loader.loadObj("SkyDome");
		pos = new Vector3f();
		infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
		regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
		dynamicSkyShader = new DynamicSkyShader();
		staticSkyShader = new StaticSkyShader();
		cube = VAO.create();
		cube.bind();
		cube.createAttribute(0, CUBE, 3, GL_STATIC_DRAW);
		cube.unbind();
		cube.setVertexCount(CUBE.length / 3);
	}

	public void render(Camera camera, Matrix4f projection, Vector3f lightDirection, boolean renderSun,
			boolean infScale) {
		if (dynamicSky != null) {
			glCullFace(GL_FRONT);
			dynamicSkyShader.start();
			dynamicSkyShader.loadCamera(camera, projection);
			dynamicSkyShader.loadDynamicSky(dynamicSky);
			dynamicSkyShader.loadLightPosition(lightDirection);
			dynamicSkyShader.renderSun(renderSun);
			if (infScale)
				dynamicSkyShader.loadTransformationMatrix(infMat);
			else
				dynamicSkyShader.loadTransformationMatrix(regMat);
			dome.bind(0, 1, 2);
			glDrawElements(GL_TRIANGLES, dome.getIndexCount(), GL_UNSIGNED_INT, 0);
			dome.unbind(0, 1, 2);
			dynamicSkyShader.stop();
			glCullFace(GL_BACK);
		} else if (staticSky != null) {
			if (staticSky.getImage() != null) {
				staticSky.getImage().getTexture(); // Trigger image load
				if (staticSky.getImage().hasLoaded()) {
					staticSkyShader.start();
					staticSkyShader.loadCamera(camera, projection);
					staticSkyShader.loadSky(staticSky);
					if (infScale)
						staticSkyShader.loadTransformationMatrix(infMat);
					else
						staticSkyShader.loadTransformationMatrix(regMat);
					cube.bind(0);
					glActiveTexture(GL_TEXTURE0);
					glBindTexture(GL_TEXTURE_CUBE_MAP, cubeTex.getTexture());
					glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
					cube.unbind(0);
					staticSkyShader.stop();
				}
			}
		}
	}

	public void render(CubeMapCamera camera, Vector3f lightDirection, boolean renderSun, boolean infScale) {
		if (dynamicSky != null) {
			glCullFace(GL_FRONT);
			dynamicSkyShader.start();
			dynamicSkyShader.loadCamera(camera);
			dynamicSkyShader.loadDynamicSky(dynamicSky);
			dynamicSkyShader.loadLightPosition(lightDirection);
			dynamicSkyShader.renderSun(renderSun);
			if (infScale)
				dynamicSkyShader.loadTransformationMatrix(infMat);
			else
				dynamicSkyShader.loadTransformationMatrix(regMat);
			dome.bind(0, 1, 2);
			glDrawElements(GL_TRIANGLES, dome.getIndexCount(), GL_UNSIGNED_INT, 0);
			dome.unbind(0, 1, 2);
			dynamicSkyShader.stop();
			glCullFace(GL_BACK);
		} else if (staticSky != null) {
			if (staticSky.getImage() != null) {
				if (staticSky.getImage().hasLoaded()) {
					staticSkyShader.start();
					staticSkyShader.loadCamera(camera);
					staticSkyShader.loadSky(staticSky);
					if (infScale)
						staticSkyShader.loadTransformationMatrix(infMat);
					else
						staticSkyShader.loadTransformationMatrix(regMat);
					cube.bind(0);
					glActiveTexture(GL_TEXTURE0);
					glBindTexture(GL_TEXTURE_CUBE_MAP, cubeTex.getTexture());
					glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
					cube.unbind(0);
					staticSkyShader.stop();
				}
			}
		}
	}

	public void dispose() {
		dome.dispose();
		dynamicSkyShader.dispose();
	}

	public void setDynamicSky(DynamicSkybox dynamicSky) {
		if (this.dynamicSky == dynamicSky)
			return;
		this.dynamicSky = dynamicSky;
		if (dynamicSky != null) {
			infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
		} else {
			infMat = Maths.createTransformationMatrix(pos, -90, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, -90, 0, 0, 990);
		}
	}

	public void setStaticSky(Skybox staticSky) {
		if (this.staticSky == staticSky)
			return;
		this.staticSky = staticSky;
		if (staticSky != null) {
			infMat = Maths.createTransformationMatrix(pos, -90, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, -90, 0, 0, 990);

			TextureBuilder tb = new TextureBuilder();

			tb.genTexture(GL_TEXTURE_CUBE_MAP).bindTexture();
			tb.sizeTexture(512, 512);
			for (int i = 0; i < 6; i++)
				tb.texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGB16F, 0, GL_RGB, GL_FLOAT, 0);
			tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			tb.texParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			tb.texParameteri(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			tb.texParameteri(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			tb.texParameteri(GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
			tb.texParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			tb.texParameterf(GL_TEXTURE_LOD_BIAS, 0);
			tb.generateMipmap();
			cubeTex = tb.endTexture();
		} else {
			infMat = Maths.createTransformationMatrix(pos, 0, 0, 0, Integer.MAX_VALUE);
			regMat = Maths.createTransformationMatrix(pos, 0, 0, 0, 1500);
			TaskManager.addTaskRenderThread(() -> cubeTex.dispose());
		}
	}

	public void reloadStaticSkybox() {
		generateFramebuffer(512);
		CubeMapCamera camera = new CubeMapCamera(new Vector3f());
		SphereToCubeShader stc = new SphereToCubeShader();

		glDisable(GL_BLEND);

		framebuffer.bind();
		stc.start();
		cube.bind(0);
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, staticSky.getImage().getTexture().getID());
		stc.loadProjectionMatrix(camera.getProjectionMatrix());
		for (int i = 0; i < 6; i++) {
			glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
					cubeTex.getTexture(), 0);
			camera.switchToFace(i);
			stc.loadviewMatrix(camera.getViewMatrix());
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glDrawArrays(GL_TRIANGLES, 0, cube.getVertexCount());
		}
		cube.unbind(0);
		stc.stop();
		framebuffer.unbind();

		glBindTexture(GL_TEXTURE_CUBE_MAP, cubeTex.getTexture());
		glGenerateMipmap(GL_TEXTURE_CUBE_MAP);
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
		stc.dispose();
		disposeFramebuffer();
	}

	private void generateFramebuffer(int size) {
		RenderbufferBuilder rb = new RenderbufferBuilder();

		rb.genRenderbuffer().bindRenderbuffer().sizeRenderbuffer(size, size);
		rb.renderbufferStorage(GL_DEPTH_COMPONENT);
		depthBuffer = rb.endRenderbuffer();

		FramebufferBuilder fb = new FramebufferBuilder();
		fb.genFramebuffer().bindFramebuffer().sizeFramebuffer(size, size);
		fb.framebufferTexture2D(GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X, cubeTex, 0);
		fb.framebufferRenderbuffer(GL_DEPTH_ATTACHMENT, depthBuffer);
		framebuffer = fb.endFramebuffer();
	}

	private void disposeFramebuffer() {
		framebuffer.dispose();
		depthBuffer.dispose();
	}

}