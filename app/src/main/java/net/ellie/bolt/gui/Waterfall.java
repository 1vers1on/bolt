package net.ellie.bolt.gui;

import java.awt.Color;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.gui.colormap.Colormaps;

public class Waterfall implements IGuiElement {
    private Integer markerFrequency = null;

    public void setMarkerFrequency(int freq) {
        markerFrequency = freq;
    }

    public void clearMarkerFrequency() {
        markerFrequency = null;
    }
    private int waterfallTextureId;
    private int width, height;
    private byte[] pixelData;

    private ByteBuffer uploadBuffer;
    private final Object lock = new Object();
    private volatile boolean dirty = false;
    private float zoom = 1.0f;
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 8.0f;

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
        if (newWidth <= 0 || newHeight <= 0)
            return;

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
            if (pixelData == null || width <= 0 || height <= 0)
                return;

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

        float invZoom = 1.0f / Math.max(MIN_ZOOM, zoom);
        float halfRange = 0.5f * invZoom;
        float uMin = 0.5f - halfRange;
        float uMax = 0.5f + halfRange;
        float vMin = 0.0f;
        float vMax = 1.0f;
        ImGui.image(waterfallTextureId, (float) width, (float) height, uMin, vMin, uMax, vMax);

        int sampleRate = Configuration.getSampleRate();
        int centerFreq = Configuration.getRtlSdrConfig().getRtlSdrCenterFrequency();
        int targetFreq = Configuration.getTargetFrequency();

        float spanHz = sampleRate / Math.max(MIN_ZOOM, zoom);
        float startHz = centerFreq - spanHz / 2.0f;

        float clampedTarget = Math.max(startHz, Math.min(centerFreq + spanHz / 2.0f, targetFreq));
        float norm = (clampedTarget - startHz) / spanHz;
        float xLocal = norm * (width - 1);

        ImVec2 min = ImGui.getItemRectMin();
        ImVec2 max = ImGui.getItemRectMax();
        float xScreen = min.x + xLocal + 0.5f;
        float xLine = (float) Math.floor(xScreen) + 0.5f;
        float yTop = min.y;
        float yBottom = max.y;

        ImDrawList dl = ImGui.getWindowDrawList();

        int lineColor = ImGui.getColorU32(ImGui.getStyle().getColor(ImGuiCol.Text));
        dl.addLine(xLine, yTop, xLine, yBottom, lineColor, 2.0f);
        float handleHalf = 6.0f; // 12px wide
        float rectLeft = (float) Math.floor(xScreen - handleHalf);
        float rectRight = (float) Math.floor(xScreen + handleHalf);
        float rectBottom = (float) Math.floor(yBottom);
        float rectTop = rectBottom - 12.0f; // exact 12px height
        dl.addRectFilled(rectLeft, rectTop, rectRight, rectBottom,
                ImGui.getColorU32(ImGui.getStyle().getColor(ImGuiCol.FrameBgActive)));

        int bwStartOffset = Configuration.getBandwidthStartOffset();
        int bwEndOffset = Configuration.getBandwidthEndOffset();

        float bwStartHz = targetFreq + bwStartOffset;
        float bwEndHz = targetFreq + bwEndOffset;

        float bwMinHz = Math.min(bwStartHz, bwEndHz);
        float bwMaxHz = Math.max(bwStartHz, bwEndHz);

        bwMinHz = Math.max(startHz, Math.min(startHz + spanHz, bwMinHz));
        bwMaxHz = Math.max(startHz, Math.min(startHz + spanHz, bwMaxHz));

        float bwMinNorm = (bwMinHz - startHz) / spanHz;
        float bwMaxNorm = (bwMaxHz - startHz) / spanHz;
        float xLocalStart = bwMinNorm * (width - 1);
        float xLocalEnd = bwMaxNorm * (width - 1);

        float xScreenStart = min.x + xLocalStart + 0.5f;
        float xScreenEnd = min.x + xLocalEnd + 0.5f;
        float xHandleStart = (float) Math.floor(xScreenStart) + 0.5f;
        float xHandleEnd = (float) Math.floor(xScreenEnd) + 0.5f;

        float handleInset = 1.0f;
        float bandTop = rectTop - handleInset;
        float bandBottom = rectBottom + handleInset;
        int handleColor = ImGui.getColorU32(ImGui.getStyle().getColor(ImGuiCol.CheckMark));

        dl.addLine(xHandleStart, bandTop, xHandleEnd, bandTop, handleColor, 2.0f);
        dl.addLine(xHandleStart, bandBottom, xHandleEnd, bandBottom, handleColor, 2.0f);

        dl.addLine(xHandleStart, bandTop, xHandleStart, bandBottom, handleColor, 2.0f);
        dl.addLine(xHandleEnd, bandTop, xHandleEnd, bandBottom, handleColor, 2.0f);

        if (markerFrequency != null) {
            float markerHz = markerFrequency;
            markerHz = Math.max(startHz, Math.min(centerFreq + spanHz / 2.0f, markerHz));
            float markerNorm = (markerHz - startHz) / spanHz;
            float markerXLocal = markerNorm * (width - 1);
            float markerXScreen = min.x + markerXLocal + 0.5f;
            float markerXLine = (float) Math.floor(markerXScreen) + 0.5f;
            int markerColor = ImGui.getColorU32(ImGui.getStyle().getColor(ImGuiCol.PlotLines));
            dl.addLine(markerXLine, yTop, markerXLine, yBottom, markerColor, 2.0f);
        }

        boolean hovered = ImGui.isItemHovered();
        if (hovered) {
            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel != 0.0f) {
                float newZoom = zoom * (float) Math.pow(1.1f, wheel);
                zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
            }
        }
        if (hovered && ImGui.isMouseDown(0)) {
            ImVec2 mousePos = ImGui.getMousePos();
            float xWithin = Math.max(0f, Math.min(width - 1, mousePos.x - min.x - 0.5f));
            float newNorm = (width > 1) ? (xWithin / (width - 1)) : 0f;
            int newFreq = (int) (startHz + newNorm * spanHz);
            Configuration.setTargetFrequency(newFreq);
        }
    }
}