package net.ellie.bolt.gui;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.flag.ImPlotFlags;
import imgui.extension.implot.flag.ImPlotAxisFlags;
import imgui.extension.implot.flag.ImPlotStyleVar;

public class Panadapter implements IGuiElement {
    private float[] fftData;

    @Override
    public void initialize() {
    }

    public void update(float[] fftData) {
        this.fftData = fftData;
    }

    @Override
    public void render() {
        float w = ImGui.getContentRegionAvailX();
        float h = 200;

        if (fftData != null) {
            ImPlot.pushStyleVar(ImPlotStyleVar.PlotPadding, new ImVec2(0, 0));
            ImPlot.pushStyleVar(ImPlotStyleVar.PlotBorderSize, 0f);

            int plotFlags = ImPlotFlags.NoTitle | ImPlotFlags.NoLegend | ImPlotFlags.NoMouseText
                          | ImPlotFlags.NoMenus | ImPlotFlags.NoBoxSelect | ImPlotFlags.NoFrame;
            // int axisFlags = ImPlotAxisFlags.NoDecorations;

            if (ImPlot.beginPlot("Frequency Line Graph", new ImVec2(w, h), plotFlags)) {
                // ImPlot.setupAxes(null, null, axisFlags, axisFlags);
                ImPlot.plotLine("FFT Data", fftData, fftData.length);
                ImPlot.endPlot();
            }

            ImPlot.popStyleVar(2);
        }
    }
}