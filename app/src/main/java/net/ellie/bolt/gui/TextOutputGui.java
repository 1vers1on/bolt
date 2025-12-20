package net.ellie.bolt.gui;

import net.ellie.bolt.decoder.buffer.CircularCharBuffer;

import imgui.ImGui;

public class TextOutputGui implements IGuiElement {
    private CircularCharBuffer charBuffer;
    private String text;

    public TextOutputGui(CircularCharBuffer charBuffer) {
        this.charBuffer = charBuffer;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void render() {
        int available = charBuffer.available();
        if (available > 0) {
            char[] temp = new char[available];
            int read = charBuffer.readNonBlocking(temp, 0, available);
            StringBuilder sb = new StringBuilder(text == null ? "" : text);
            for (int i = 0; i < read; i++) {
                sb.append(temp[i]);
            }
            text = sb.toString();
        }

        ImGui.beginChild("##byteoutput_child", 600.0f, 200.0f, true);
        ImGui.pushTextWrapPos(0.0f);
        ImGui.textUnformatted(text);
        ImGui.popTextWrapPos();
        if (ImGui.getScrollY() >= ImGui.getScrollMaxY()) {
            ImGui.setScrollHereY(1.0f);
        }

        ImGui.endChild();
    }

    public void setCircularCharBuffer(CircularCharBuffer charBuffer) {
        this.charBuffer = charBuffer;
    }

    public void clear() {
        this.text = "";
    }
}
