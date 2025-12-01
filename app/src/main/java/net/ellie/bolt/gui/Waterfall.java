package net.ellie.bolt.gui;

import java.awt.Color;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import imgui.ImGui;
import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.gui.colormap.Colormaps;

public class Waterfall implements IGuiElement {
    private int waterfallTextureId;
    private int width, height;
    private byte[] pixelData;

    private ByteBuffer uploadBuffer;
    private final Object lock = new Object();
    private volatile boolean dirty = false;

    public Waterfall(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixelData = new byte[width * height * 3];
        this.uploadBuffer = BufferUtils.createByteBuffer(pixelData.length);
    }

    @Override
    public void initialize() {
        waterfallTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, waterfallTextureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB,
                width, height, 0,
                GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    private void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;

        synchronized (lock) {
            this.width = newWidth;
            this.height = newHeight;
            this.pixelData = new byte[width * height * 3];
            this.uploadBuffer = BufferUtils.createByteBuffer(pixelData.length);
            dirty = true;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, waterfallTextureId);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB,
                width, height, 0,
                GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    public void update(float[] fftData) {
        synchronized (lock) {
            if (pixelData == null || width <= 0 || height <= 0) return;

            int rowStride = width * 3;
            if (pixelData.length > rowStride) {
                System.arraycopy(pixelData, 0, pixelData, rowStride, pixelData.length - rowStride);
            }

            if (fftData.length == width) {
                for (int x = 0; x < width; x++) {
                    float power = fftData[x];
                    Color color = Colormaps.mapValue(Configuration.getColormap(), power);
                    pixelData[x * 3] = (byte) color.getRed();
                    pixelData[x * 3 + 1] = (byte) color.getGreen();
                    pixelData[x * 3 + 2] = (byte) color.getBlue();
                }
            } else {
                if (width == 1) {
                    float power = fftData[0];
                    Color color = Colormaps.mapValue(Configuration.getColormap(), power);
                    pixelData[0] = (byte) color.getRed();
                    pixelData[1] = (byte) color.getGreen();
                    pixelData[2] = (byte) color.getBlue();
                } else {
                    for (int x = 0; x < width; x++) {
                        float fftIndexFloat = (float) x / (width - 1) * (fftData.length - 1);
                        int fftIndex1 = (int) fftIndexFloat;
                        int fftIndex2 = Math.min(fftIndex1 + 1, fftData.length - 1);
                        float fraction = fftIndexFloat - fftIndex1;

                        float power1 = fftData[fftIndex1];
                        float power2 = fftData[fftIndex2];
                        float power = power1 + fraction * (power2 - power1);

                        Color color = Colormaps.mapValue(Configuration.getColormap(), power);
                        pixelData[x * 3] = (byte) color.getRed();
                        pixelData[x * 3 + 1] = (byte) color.getGreen();
                        pixelData[x * 3 + 2] = (byte) color.getBlue();
                    }
                }
            }

            dirty = true;
        }
    }

    @Override
    public void render() {
        float availX = Math.max(1f, ImGui.getContentRegionAvailX());
        float availY = Math.max(1f, ImGui.getContentRegionAvailY());
        int newWidth = Math.max(2, Math.round(availX));
        int newHeight = Math.max(1, Math.round(availY));

        if (newWidth != width || newHeight != height) {
            resize(newWidth, newHeight);
        }

        if (dirty && waterfallTextureId != 0) {
            synchronized (lock) {
                uploadBuffer.clear();
                uploadBuffer.put(pixelData);
                uploadBuffer.flip();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, waterfallTextureId);
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height,
                        GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, uploadBuffer);
                dirty = false;
            }
        }

        ImGui.image(waterfallTextureId, (float) width, (float) height);
    }
}