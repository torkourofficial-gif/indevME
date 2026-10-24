package mindev;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/** Java ME entry point for the first M3G porting slice. */
public final class MindevMidlet extends MIDlet {
	private MindevCanvas canvas;

	protected void startApp() {
		if (this.canvas == null) {
			this.canvas = new MindevCanvas();
		}
		Display.getDisplay(this).setCurrent(this.canvas);
		this.canvas.start();
	}

	protected void pauseApp() {
		if (this.canvas != null) {
			this.canvas.stop();
		}
	}

	protected void destroyApp(boolean unconditional) {
		if (this.canvas != null) {
			this.canvas.stop();
		}
	}
}
