package net.ellie.bolt.gui;

import net.ellie.bolt.decoder.buffer.CircularByteBuffer;

import java.util.ArrayList;
import java.util.List;

import imgui.ImGui;
import imgui.type.ImString;

public class ByteOutputGui implements IGuiElement {
    private final CircularByteBuffer byteBuffer;
    private final List<Byte> internalBuffer;

    public ByteOutputGui(CircularByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        this.internalBuffer = new ArrayList<>();
    }

    @Override
    public void initialize() {
    }


    private enum DisplayMode {
        HEX, BINARY, ASCII, DECIMAL, OCTAL
    }


    private DisplayMode displayMode = DisplayMode.HEX;

    @Override
public void render() {

    ImGui.text("Display Mode:");
    if (ImGui.radioButton("Hex", displayMode == DisplayMode.HEX)) {
        displayMode = DisplayMode.HEX;
    }
    ImGui.sameLine();
    if (ImGui.radioButton("Binary", displayMode == DisplayMode.BINARY)) {
        displayMode = DisplayMode.BINARY;
    }
    ImGui.sameLine();
    if (ImGui.radioButton("ASCII", displayMode == DisplayMode.ASCII)) {
        displayMode = DisplayMode.ASCII;
    }
    ImGui.sameLine();
    if (ImGui.radioButton("Decimal", displayMode == DisplayMode.DECIMAL)) {
        displayMode = DisplayMode.DECIMAL;
    }
    ImGui.sameLine();
    if (ImGui.radioButton("Octal", displayMode == DisplayMode.OCTAL)) {
        displayMode = DisplayMode.OCTAL;
    }

    int available = byteBuffer.available();
    if (available > 0) {
        byte[] temp = new byte[available];
        int read = byteBuffer.readNonBlocking(temp, 0, available);
        for (int i = 0; i < read; i++) {
            internalBuffer.add(temp[i]);
        }
    }


    StringBuilder sb = new StringBuilder();
    int bytesPerLine;
    switch (displayMode) {
        case HEX:
            bytesPerLine = 16;
            break;
        case BINARY:
            bytesPerLine = 8;
            break;
        case ASCII:
            bytesPerLine = 32;
            break;
        case DECIMAL:
            bytesPerLine = 12;
            break;
        case OCTAL:
            bytesPerLine = 16;
            break;
        default:
            bytesPerLine = 16;
    }
    int count = 0;
    for (Byte b : internalBuffer) {
        switch (displayMode) {
            case HEX:
                sb.append(String.format("%02X ", b));
                break;
            case BINARY:
                sb.append(String.format("%8s ", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
                break;
            case ASCII:
                char c = (char) (b & 0xFF);
                if (c >= 32 && c <= 126) {
                    sb.append(c);
                } else {
                    sb.append('.');
                }
                break;
            case DECIMAL:
                sb.append(String.format("%03d ", b & 0xFF));
                break;
            case OCTAL:
                sb.append(String.format("%03o ", b & 0xFF));
                break;
        }
        count++;
        if (count % bytesPerLine == 0) {
            sb.append("\n");
        }
    }
    ImString imString = new ImString(sb.toString(), sb.length() + 1);

    ImGui.beginChild("##byteoutput_child", 600.0f, 200.0f, true);
    ImGui.textUnformatted(imString.get());
    if (ImGui.getScrollY() >= ImGui.getScrollMaxY()) {
        ImGui.setScrollHereY(1.0f);
    }

    ImGui.endChild();
}

}
