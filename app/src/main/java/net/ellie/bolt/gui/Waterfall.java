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
    private final int width, height;
    private byte[] pixelData;

    public Waterfall(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixelData = new byte[width * height * 3];
    }

    @Override
    public void initialize() {
        waterfallTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, waterfallTextureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB,
                width, height, 0,
                GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    public void update(float[] fftData) {
        System.arraycopy(pixelData, 0, pixelData, width * 3, pixelData.length - width * 3);

        if (fftData.length == width) {
            for (int x = 0; x < width; x++) {
                float power = fftData[x];
                Color color = Colormaps.mapValue(Configuration.getColormap(), power);
                pixelData[x * 3] = (byte) color.getRed();
                pixelData[x * 3 + 1] = (byte) color.getGreen();
                pixelData[x * 3 + 2] = (byte) color.getBlue();
            }
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

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, waterfallTextureId);
        ByteBuffer buffer = BufferUtils.createByteBuffer(pixelData.length);
        buffer.put(pixelData).flip();
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
    }

    @Override
    public void render() {
        ImGui.image(waterfallTextureId, width, height);
    }
}
