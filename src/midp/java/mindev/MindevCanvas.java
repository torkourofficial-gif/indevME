package mindev;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.m3g.Background;
import javax.microedition.m3g.Camera;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Light;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;
import javax.microedition.m3g.World;

/** Canvas and camera for the initial Java ME rendering slice. */
final class MindevCanvas extends Canvas implements Runnable {
	private static final int TERRAIN_SIZE = 8;
	private static final int WORLD_HEIGHT = 5;
	private static final int FRAME_DELAY = 66;

	private final World world;
	private final Camera camera;
	private final Background background;
	private volatile Thread renderThread;
	private volatile boolean running;
	private float yaw;
	private String renderError;

	MindevCanvas() {
		setFullScreenMode(true);
		this.world = new World();
		this.camera = new Camera();
		this.background = new Background();
		this.background.setColor(0xff78a9d4);
		this.world.setBackground(this.background);
		this.world.addChild(this.camera);
		this.world.setActiveCamera(this.camera);

		Light ambient = new Light();
		ambient.setMode(Light.AMBIENT);
		ambient.setColor(0xffffff);
		ambient.setIntensity(1.0f);
		this.world.addChild(ambient);

		try {
			Mesh terrain = MindevTerrainMesh.build(TERRAIN_SIZE, WORLD_HEIGHT, TERRAIN_SIZE, createDemoBlocks());
			if (terrain != null) {
				this.world.addChild(terrain);
			}
		} catch (RuntimeException ex) {
			this.renderError = "M3G terrain init: " + ex.toString();
		}
	}

	synchronized void start() {
		if (this.renderThread == null) {
			this.running = true;
			this.renderThread = new Thread(this);
			this.renderThread.start();
		}
	}

	synchronized void stop() {
		this.running = false;
		this.renderThread = null;
	}

	public void run() {
		Thread current = Thread.currentThread();
		while (this.running && this.renderThread == current) {
			repaint();
			try {
				Thread.sleep(FRAME_DELAY);
			} catch (InterruptedException ex) {
				// The loop observes running on its next iteration.
			}
		}
	}

	protected void paint(Graphics g) {
		int width = getWidth();
		int height = getHeight();
		g.setColor(0xff78a9d4);
		g.fillRect(0, 0, width, height);

		if (this.renderError != null) {
			drawStatus(g, this.renderError);
			return;
		}

		try {
			Transform cameraTransform = new Transform();
			cameraTransform.postRotate(-22.0f, 1.0f, 0.0f, 0.0f);
			cameraTransform.postRotate(this.yaw, 0.0f, 1.0f, 0.0f);
			cameraTransform.setTranslation(0.0f, 5.0f, 12.0f);
			this.camera.setTransform(cameraTransform);
			this.camera.setPerspective(62.0f, (float) width / (float) height, 0.1f, 48.0f);

			Graphics3D graphics3D = Graphics3D.getInstance();
			graphics3D.bindTarget(g);
			try {
				graphics3D.clear(this.background);
				graphics3D.render(this.world);
			} finally {
				graphics3D.releaseTarget();
			}

			g.setColor(0xffffff);
			g.drawString("MINDEV M3G  LEFT/RIGHT", 3, 2, Graphics.TOP | Graphics.LEFT);
		} catch (RuntimeException ex) {
			this.renderError = "M3G render: " + ex.toString();
			drawStatus(g, this.renderError);
		}
	}

	protected void keyPressed(int keyCode) {
		int action = getGameAction(keyCode);
		if (action == LEFT) {
			this.yaw -= 8.0f;
		} else if (action == RIGHT) {
			this.yaw += 8.0f;
		}
	}

	private void drawStatus(Graphics g, String message) {
		g.setColor(0xffffff);
		g.drawString("MINDEV M3G", 3, 3, Graphics.TOP | Graphics.LEFT);
		g.drawString(message, 3, 22, Graphics.TOP | Graphics.LEFT);
	}

	/** Builds a tiny block array using the original Indev World.blocks layout. */
	private static byte[] createDemoBlocks() {
		byte[] blocks = new byte[TERRAIN_SIZE * WORLD_HEIGHT * TERRAIN_SIZE];
		for (int z = 0; z < TERRAIN_SIZE; ++z) {
			for (int x = 0; x < TERRAIN_SIZE; ++x) {
				int top = 1 + ((x * 3 + z * 5 + x * z) & 3);
				for (int y = 0; y < top; ++y) {
					int blockId = y == top - 1 ? 2 : (y == top - 2 ? 3 : 1);
					blocks[(y * TERRAIN_SIZE + z) * TERRAIN_SIZE + x] = (byte) blockId;
				}
			}
		}
		return blocks;
	}
}
