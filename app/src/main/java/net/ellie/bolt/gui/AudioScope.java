package net.ellie.bolt.gui;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.flag.ImPlotFlags;
import imgui.extension.implot.flag.ImPlotStyleVar;

public class AudioScope implements IGuiElement {
    private float[] samples;
    private float sampleRate = 48000f;
    private float timeDivMs = 10f; // default 10 ms/div

    private float[] windowView;

    @Override
    public void initialize() {
    }

    /**
     * Update the scope with new audio data.
     * 
     * @param samples      Latest PCM samples (mono). If stereo, provide one
     *                     channel.
     * @param sampleRateHz Sample rate of the buffer (Hz).
     */
    public void update(float[] samples, float sampleRateHz) {
        if (samples == null || samples.length == 0) {
            this.samples = null;
            this.windowView = null;
            return;
        }
        this.samples = samples;
        this.sampleRate = sampleRateHz > 0 ? sampleRateHz : this.sampleRate;
        rebuildWindowView();
    }

    /**
     * Set milliseconds per division for horizontal scaling.
     */
    public void setTimeDivMs(float msPerDiv) {
        if (msPerDiv > 0) {
            this.timeDivMs = msPerDiv;
        }
    }

    private void rebuildWindowView() {
        if (samples == null || samples.length == 0) {
            windowView = null;
            return;
        }
        float divisions = 10f; // typical oscilloscope grid width
        float spanSeconds = (timeDivMs * divisions) / 1000f;
        int samplesToShow = Math.max(1, Math.min(samples.length, (int) (spanSeconds * sampleRate)));
        if (windowView == null || windowView.length != samplesToShow) {
            windowView = new float[samplesToShow];
        }
        int start = samples.length - samplesToShow;
        System.arraycopy(samples, start, windowView, 0, samplesToShow);
    }

    @Override
    public void render() {
        float w = ImGui.getContentRegionAvailX();
        float h = 200f;

        if (samples == null || samples.length == 0) {
            ImGui.text("AudioScope: no data");
            return;
        }

        ImPlot.pushStyleVar(ImPlotStyleVar.PlotPadding, new ImVec2(0, 0));
        ImPlot.pushStyleVar(ImPlotStyleVar.PlotBorderSize, 0f);

        int plotFlags = ImPlotFlags.NoTitle | ImPlotFlags.NoLegend | ImPlotFlags.NoMouseText
                | ImPlotFlags.NoMenus | ImPlotFlags.NoBoxSelect | ImPlotFlags.NoFrame;

        rebuildWindowView();

        if (ImPlot.beginPlot("AudioScope", new ImVec2(w, h), plotFlags)) {
            ImPlot.plotLine("Audio", windowView, windowView.length);
            ImPlot.endPlot();
        }

        ImPlot.popStyleVar(2);
    }
}
