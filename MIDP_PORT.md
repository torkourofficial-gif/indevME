# Java ME / M3G port

The first porting slice lives in `src/midp/java` and uses the Java package
`mindev`. In NetBeans, add that directory as a source root and configure the
project for CLDC 1.1, MIDP 2.0, and the JSR-184 M3G API exposed by the target
Jbed build. The MIDlet entry point is `mindev.MindevMidlet`.

`MindevCanvas` renders an 8x5x8 sample terrain as one M3G mesh. The reusable
`MindevTerrainMesh` accepts a compact byte array with the same block IDs and
layout as Indev's `World.blocks` array: `(y * length + z) * width + x`. It emits
only faces next to air, including cave faces, and limits a mesh to 1,024 blocks
to bound temporary geometry memory. The sample uses Indev grass (2), dirt (3),
and stone (1); the color palette is temporary until the texture atlas is ported.

The launch descriptor should name `mindev.MindevMidlet` as its `MIDlet-1`
class. NetBeans can compile this first slice with only the MIDlet/M3G APIs;
the original `src/game/java` and `src/main/java` trees are retained as source
references until their desktop APIs have been replaced.

This is a rendering foundation, not yet the complete game client. The desktop
entry point, input, texture loading, sound, and world simulation still depend on
the existing LWJGL/Eaglercraft layer. In particular, the original
`LevelGenerator` allocates a million-element `int` scratch array, and `World`
allocates two million-element `int` arrays plus a million-element `short`
array. Those arrays alone exceed an 8 MB heap, so world simulation needs a
bounded-memory port before the original generator can be used on the target.
