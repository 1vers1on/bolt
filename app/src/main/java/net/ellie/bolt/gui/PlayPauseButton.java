package net.ellie.bolt.gui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;

public class PlayPauseButton implements IGuiElement {
    private boolean isPlaying = false;

    @Override
    public void initialize() {

    }

    @Override
    public void render() {
        if (ImGui.button("##playpause", new ImVec2(32, 32))) {
            isPlaying = !isPlaying;
        }

        ImDrawList drawList = ImGui.getWindowDrawList();
        ImVec2 p0 = ImGui.getItemRectMin();
        ImVec2 p1 = ImGui.getItemRectMax();
        ImVec2 center = new ImVec2((p0.x + p1.x) / 2, (p0.y + p1.y) / 2);
        float radius = 10.0f;
        if (!isPlaying) {
            ImVec2[] points = new ImVec2[3];
            points[0] = new ImVec2(center.x - radius / 2, center.y - radius);
            points[1] = new ImVec2(center.x - radius / 2, center.y + radius);
            points[2] = new ImVec2(center.x + radius, center.y);
            drawList.addTriangleFilled(points[0].x, points[0].y, points[1].x, points[1].y, points[2].x, points[2].y, 0xFFFFFFFF);
        } else {
            float halfSize = radius;
            drawList.addRectFilled(center.x - halfSize, center.y - halfSize, center.x + halfSize, center.y + halfSize, 0xFFFFFFFF);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
