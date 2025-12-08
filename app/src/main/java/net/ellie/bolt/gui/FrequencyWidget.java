package net.ellie.bolt.gui;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import net.ellie.bolt.Fonts;

public class FrequencyWidget implements IGuiElement {
    private int frequency = 101500000;

    @Override
    public void initialize() {
    }

    @Override
    public void render() {
        boolean changed = false;

        int[] digits = new int[9];
        int tmp = frequency;
        for (int i = 8; i >= 0; i--) {
            digits[i] = tmp % 10;
            tmp /= 10;
        }

        ImGui.pushFont(Fonts.getRobotoSemiBold24());
        
        ImGui.pushStyleColor(ImGuiCol.Button, ImGui.getStyle().getColor(ImGuiCol.FrameBg));
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImGui.getStyle().getColor(ImGuiCol.FrameBgHovered));
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImGui.getStyle().getColor(ImGuiCol.FrameBgActive));
        
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.ItemSpacing, 2, 2);

        ImGui.beginGroup();
        ImGui.sameLine();
        for (int i = 0; i < 9; i++) {
            ImGui.pushID(i);

            if (ImGui.button(String.valueOf(digits[i]), 24, 32)) {
                digits[i] = (digits[i] + 1) % 10;
                changed = true;
            }

            if (ImGui.isItemHovered() && ImGui.isMouseClicked(1)) {
                digits[i] = (digits[i] + 9) % 10;
                changed = true;
            }

            ImGui.popID();

            if (i == 2 || i == 5) {
                ImGui.sameLine();
                ImGui.text(".");
                ImGui.sameLine();
            } else if (i != 8) {
                ImGui.sameLine();
            }
        }

        ImGui.endGroup();

        ImGui.popStyleVar();
        ImGui.popStyleColor(3);
        ImGui.popFont();

        if (changed) {
            int newFrequency = 0;
            for (int i = 0; i < 9; i++) {
                newFrequency = newFrequency * 10 + digits[i];
            }
            frequency = newFrequency;
        }
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
}
