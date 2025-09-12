package com.emulator;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelFormat;

public class VideoF {
    private GraphicsContext gc;
    private final int width;
    private final int height;
    private final WritableImage image;

    public VideoF(int width, int height) {
        this.width = width;
        this.height = height;
        this.image = new WritableImage(width, height);
    }

    public void setGraphicsContext(GraphicsContext gc) {
        this.gc = gc;
    }

    /** Desenha o framebuffer ARGB32 na tela */
    public void draw(int[] framebuffer) {
        if (gc == null || framebuffer.length != width * height) return;
        PixelWriter writer = image.getPixelWriter();
        writer.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), framebuffer, 0, width);
        gc.drawImage(image, 0, 0);
    }
}