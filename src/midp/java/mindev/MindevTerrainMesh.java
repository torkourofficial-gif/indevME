package mindev;

import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.IndexBuffer;
import javax.microedition.m3g.Material;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.TriangleArray;
import javax.microedition.m3g.VertexArray;
import javax.microedition.m3g.VertexBuffer;

/** Converts a bounded Indev-style block array to a single M3G triangle mesh. */
final class MindevTerrainMesh {
	private static final int MAX_RENDERED_BLOCKS = 1024;
	private static final int MAX_HORIZONTAL_SIZE = 16;
	private static final int MAX_HEIGHT = 64;

	private MindevTerrainMesh() {
	}

	static Mesh build(int width, int height, int length, byte[] blocks) {
		if (width < 1 || width > MAX_HORIZONTAL_SIZE || length < 1 || length > MAX_HORIZONTAL_SIZE
			|| height < 1 || height > MAX_HEIGHT || width * height * length > MAX_RENDERED_BLOCKS) {
			throw new IllegalArgumentException("Terrain dimensions exceed the MIDP mesh budget");
		}
		if (blocks == null || blocks.length < width * height * length) {
			throw new IllegalArgumentException("Block array is smaller than its dimensions");
		}

		int maxVertices = width * height * length * 6 * 6;
		short[] positions = new short[maxVertices * 3];
		byte[] colors = new byte[maxVertices * 3];
		byte[] normals = new byte[maxVertices * 3];
		int vertexCount = 0;

		for (int z = 0; z < length; ++z) {
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					int blockId = blockAt(width, length, blocks, x, y, z);
					if (blockId == 0) {
						continue;
					}

					int x0 = x - width / 2;
					int y0 = y;
					int z0 = z - length / 2;
					int x1 = x0 + 1;
					int y1 = y0 + 1;
					int z1 = z0 + 1;
					int top = topColor(blockId);
					int side = sideColor(blockId);

					if (blockAt(width, length, blocks, x, y + 1, z) == 0) {
						vertexCount = addQuad(positions, colors, normals, vertexCount,
							x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0,
							0, 127, 0, top);
					}
					if (blockAt(width, length, blocks, x, y - 1, z) == 0) {
						vertexCount = addQuad(positions, colors, normals, vertexCount,
							x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1,
							0, -127, 0, darken(side));
					}
					if (blockAt(width, length, blocks, x - 1, y, z) == 0) {
						vertexCount = addQuad(positions, colors, normals, vertexCount,
							x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0,
							-127, 0, 0, side);
					}
					if (blockAt(width, length, blocks, x + 1, y, z) == 0) {
						vertexCount = addQuad(positions, colors, normals, vertexCount,
							x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1,
							127, 0, 0, side);
					}
					if (blockAt(width, length, blocks, x, y, z - 1) == 0) {
						vertexCount = addQuad(positions, colors, normals, vertexCount,
							x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0,
							0, 0, -127, side);
					}
					if (blockAt(width, length, blocks, x, y, z + 1) == 0) {
						vertexCount = addQuad(positions, colors, normals, vertexCount,
							x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1,
							0, 0, 127, side);
					}
				}
			}
		}

		if (vertexCount == 0) {
			return null;
		}

		VertexArray positionArray = new VertexArray(vertexCount, 3, 2);
		positionArray.set(0, vertexCount, positions);
		VertexArray colorArray = new VertexArray(vertexCount, 3, 1);
		colorArray.set(0, vertexCount, colors);
		VertexArray normalArray = new VertexArray(vertexCount, 3, 1);
		normalArray.set(0, vertexCount, normals);

		VertexBuffer vertices = new VertexBuffer();
		vertices.setPositions(positionArray, 1.0f, null);
		vertices.setColors(colorArray);
		vertices.setNormals(normalArray);

		IndexBuffer triangles = new TriangleArray(0, vertexCount);
		Appearance appearance = new Appearance();
		Material material = new Material();
		material.setVertexColorTrackingEnable(true);
		appearance.setMaterial(material);
		return new Mesh(vertices, triangles, new Appearance[] { appearance });
	}

	private static int blockAt(int width, int length, byte[] blocks, int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0) {
			return 0;
		}
		int index = (y * length + z) * width + x;
		if (x >= width || z >= length || index < 0 || index >= blocks.length) {
			return 0;
		}
		return blocks[index] & 255;
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
		if (blockId == 8 || blockId == 9) {
			return 0x477bb5;
		}
		return 0x777777;
	}

	private static int darken(int rgb) {
		return ((rgb >> 17 & 127) << 16) | ((rgb >> 9 & 127) << 8) | (rgb >> 1 & 127);
	}

	private static int addQuad(short[] positions, byte[] colors, byte[] normals, int count,
		int ax, int ay, int az, int bx, int by, int bz,
		int cx, int cy, int cz, int dx, int dy, int dz,
		int nx, int ny, int nz, int rgb) {
		count = addVertex(positions, colors, normals, count, ax, ay, az, nx, ny, nz, rgb);
		count = addVertex(positions, colors, normals, count, bx, by, bz, nx, ny, nz, rgb);
		count = addVertex(positions, colors, normals, count, cx, cy, cz, nx, ny, nz, rgb);
		count = addVertex(positions, colors, normals, count, ax, ay, az, nx, ny, nz, rgb);
		count = addVertex(positions, colors, normals, count, cx, cy, cz, nx, ny, nz, rgb);
		count = addVertex(positions, colors, normals, count, dx, dy, dz, nx, ny, nz, rgb);
		return count;
	}

	private static int addVertex(short[] positions, byte[] colors, byte[] normals, int index,
		int x, int y, int z, int nx, int ny, int nz, int rgb) {
		int offset = index * 3;
		positions[offset] = (short) x;
		positions[offset + 1] = (short) y;
		positions[offset + 2] = (short) z;
		colors[offset] = (byte) (rgb >> 16 & 255);
		colors[offset + 1] = (byte) (rgb >> 8 & 255);
		colors[offset + 2] = (byte) (rgb & 255);
		normals[offset] = (byte) nx;
		normals[offset + 1] = (byte) ny;
		normals[offset + 2] = (byte) nz;
		return index + 1;
	}
}
