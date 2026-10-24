package mindev;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.Background;
import javax.microedition.m3g.Camera;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.IndexBuffer;
import javax.microedition.m3g.Light;
import javax.microedition.m3g.Material;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.TriangleArray;
import javax.microedition.m3g.Transform;
import javax.microedition.m3g.VertexArray;
import javax.microedition.m3g.VertexBuffer;
import javax.microedition.m3g.World;

/**
 * Small, bounded M3G scene used to establish the Java ME rendering path.
 * The terrain is deliberately only 8x8 columns while the original renderer
 * is replaced; geometry is merged into a single mesh to keep object overhead low.
 */
final class MindevCanvas extends Canvas implements Runnable {
	private static final int TERRAIN_SIZE = 8;
	private static final int WORLD_HEIGHT = 5;
	private static final int MAX_VERTICES = TERRAIN_SIZE * TERRAIN_SIZE * 5 * 6;
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
			this.world.addChild(createTerrain(createDemoBlocks()));
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

	private Mesh createTerrain(byte[] blocks) {
		short[] positions = new short[MAX_VERTICES * 3];
		byte[] colors = new byte[MAX_VERTICES * 3];
		byte[] normals = new byte[MAX_VERTICES * 3];
		int[] vertexCount = new int[] { 0 };

		for (int z = 0; z < TERRAIN_SIZE; ++z) {
			for (int x = 0; x < TERRAIN_SIZE; ++x) {
				int h = columnHeight(blocks, x, z);
				if (h == 0) {
					continue;
				}
				int x0 = x - TERRAIN_SIZE / 2;
				int z0 = z - TERRAIN_SIZE / 2;
				int x1 = x0 + 1;
				int z1 = z0 + 1;
				int topColor = topColor(blockAt(blocks, x, h - 1, z));

				addQuad(positions, colors, normals, vertexCount,
					x0, h, z0, x0, h, z1, x1, h, z1, x1, h, z0,
					0, 127, 0, topColor >> 16 & 255, topColor >> 8 & 255, topColor & 255);

				int west = x == 0 ? 0 : columnHeight(blocks, x - 1, z);
				int east = x == TERRAIN_SIZE - 1 ? 0 : columnHeight(blocks, x + 1, z);
				int north = z == 0 ? 0 : columnHeight(blocks, x, z - 1);
				int south = z == TERRAIN_SIZE - 1 ? 0 : columnHeight(blocks, x, z + 1);
				int sideColor = sideColor(blockAt(blocks, x, h - 1, z));

				if (west < h) {
					addQuad(positions, colors, normals, vertexCount,
						x0, west, z0, x0, west, z1, x0, h, z1, x0, h, z0,
						-127, 0, 0, sideColor >> 16 & 255, sideColor >> 8 & 255, sideColor & 255);
				}
				if (east < h) {
					addQuad(positions, colors, normals, vertexCount,
						x1, east, z1, x1, east, z0, x1, h, z0, x1, h, z1,
						127, 0, 0, sideColor >> 16 & 255, sideColor >> 8 & 255, sideColor & 255);
				}
				if (north < h) {
					addQuad(positions, colors, normals, vertexCount,
						x1, north, z0, x0, north, z0, x0, h, z0, x1, h, z0,
						0, 0, -127, sideColor >> 16 & 255, sideColor >> 8 & 255, sideColor & 255);
				}
				if (south < h) {
					addQuad(positions, colors, normals, vertexCount,
						x0, south, z1, x1, south, z1, x1, h, z1, x0, h, z1,
						0, 0, 127, sideColor >> 16 & 255, sideColor >> 8 & 255, sideColor & 255);
				}
			}
		}

		VertexArray positionArray = new VertexArray(vertexCount[0], 3, 2);
		positionArray.set(0, vertexCount[0], positions);
		VertexArray colorArray = new VertexArray(vertexCount[0], 3, 1);
		colorArray.set(0, vertexCount[0], colors);
		VertexArray normalArray = new VertexArray(vertexCount[0], 3, 1);
		normalArray.set(0, vertexCount[0], normals);

		VertexBuffer vertices = new VertexBuffer();
		vertices.setPositions(positionArray, 1.0f, null);
		vertices.setColors(colorArray);
		vertices.setNormals(normalArray);

		IndexBuffer triangles = new TriangleArray(0, vertexCount[0]);
		Appearance appearance = new Appearance();
		Material material = new Material();
		material.setColor(Material.AMBIENT, 0xffffff);
		material.setColor(Material.DIFFUSE, 0xffffff);
		material.setVertexColorTrackingEnable(true);
		appearance.setMaterial(material);
		return new Mesh(vertices, triangles, new Appearance[] { appearance });
	}

	/** Builds a tiny block array using the original Indev World.blocks layout. */
	private static byte[] createDemoBlocks() {
		byte[] blocks = new byte[TERRAIN_SIZE * WORLD_HEIGHT * TERRAIN_SIZE];
		for (int z = 0; z < TERRAIN_SIZE; ++z) {
			for (int x = 0; x < TERRAIN_SIZE; ++x) {
				int top = demoHeight(x, z);
				for (int y = 0; y < top; ++y) {
					int blockId = y == top - 1 ? 2 : (y == top - 2 ? 3 : 1);
					blocks[(y * TERRAIN_SIZE + z) * TERRAIN_SIZE + x] = (byte) blockId;
				}
			}
		}
		return blocks;
	}

	private static int columnHeight(byte[] blocks, int x, int z) {
		for (int y = WORLD_HEIGHT; y > 0; --y) {
			if (blockAt(blocks, x, y - 1, z) != 0) {
				return y;
			}
		}
		return 0;
	}

	private static int blockAt(byte[] blocks, int x, int y, int z) {
		return blocks[(y * TERRAIN_SIZE + z) * TERRAIN_SIZE + x] & 255;
	}

	private static int topColor(int blockId) {
		if (blockId == 2) {
			return 0x5c9e48;
		}
		if (blockId == 12) {
			return 0xd8c27a;
		}
		if (blockId == 8 || blockId == 9) {
			return 0x477bb5;
		}
		return 0x999999;
	}

	private static int sideColor(int blockId) {
		if (blockId == 2 || blockId == 3) {
			return 0x70583a;
		}
		if (blockId == 12) {
			return 0xc1a965;
		}
		return 0x777777;
	}

	private static int demoHeight(int x, int z) {
		int ridge = (x * 3 + z * 5 + x * z) & 3;
		return 1 + ridge;
	}

	private static void addQuad(short[] positions, byte[] colors, byte[] normals, int[] count,
		int ax, int ay, int az, int bx, int by, int bz,
		int cx, int cy, int cz, int dx, int dy, int dz,
		int nx, int ny, int nz, int red, int green, int blue) {
		count[0] = addVertex(positions, colors, normals, count[0], ax, ay, az, nx, ny, nz, red, green, blue);
		count[0] = addVertex(positions, colors, normals, count[0], bx, by, bz, nx, ny, nz, red, green, blue);
		count[0] = addVertex(positions, colors, normals, count[0], cx, cy, cz, nx, ny, nz, red, green, blue);
		count[0] = addVertex(positions, colors, normals, count[0], ax, ay, az, nx, ny, nz, red, green, blue);
		count[0] = addVertex(positions, colors, normals, count[0], cx, cy, cz, nx, ny, nz, red, green, blue);
		count[0] = addVertex(positions, colors, normals, count[0], dx, dy, dz, nx, ny, nz, red, green, blue);
	}

	private static int addVertex(short[] positions, byte[] colors, byte[] normals, int index,
		int x, int y, int z, int nx, int ny, int nz, int red, int green, int blue) {
		int offset = index * 3;
		positions[offset] = (short) x;
		positions[offset + 1] = (short) y;
		positions[offset + 2] = (short) z;
		colors[offset] = (byte) red;
		colors[offset + 1] = (byte) green;
		colors[offset + 2] = (byte) blue;
		normals[offset] = (byte) nx;
		normals[offset + 1] = (byte) ny;
		normals[offset + 2] = (byte) nz;
		return index + 1;
	}
}
