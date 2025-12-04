package net.ellie.bolt;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.contexts.PortAudioContext;
import net.ellie.bolt.dsp.DspThread;
import net.ellie.bolt.dsp.windows.HannWindow;
import net.ellie.bolt.gui.FrequencyWidget;
import net.ellie.bolt.gui.Panadapter;
import net.ellie.bolt.gui.PlayPauseButton;
import net.ellie.bolt.gui.Waterfall;
import net.ellie.bolt.input.CloseableInputSource;
import net.ellie.bolt.input.InputSourceFactory;
import net.ellie.bolt.input.InputThread;
import net.ellie.bolt.jni.portaudio.PortAudioJNI.DeviceInfo;
import net.ellie.bolt.jni.rtlsdr.RTLSDR;
import net.ellie.bolt.util.UnitFormatter;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.ImVec2;
import imgui.extension.implot.ImPlot;
import imgui.type.ImInt;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bolt {
    private static final Logger logger = LoggerFactory.getLogger(Bolt.class);

    public static Bolt instance;

    public static Bolt getInstance() {
        if (instance == null) {
            instance = new Bolt();
        }
        return instance;
    }

    private long window;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private Bolt() {
    }

    private Waterfall waterfall;
    private FrequencyWidget frequencyWidget = new FrequencyWidget();
    private PlayPauseButton playPauseButton = new PlayPauseButton();
    private Panadapter panadapter = new Panadapter();

    public boolean pipelineRunning = false;

    private CloseableInputSource inputSource;
    private InputThread inputThread;
    private DspThread dspThread;

    public static void run() {
        Bolt bolt = Bolt.getInstance();
        bolt.start();
        bolt.loop();
        bolt.stop();
    }

    public void start() {
       if (!GLFW.glfwInit()) {
           throw new RuntimeException("Failed to initialize GLFW");
       }

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);

        if (Configuration.getMsaaSamples() > 1) {
            GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, Configuration.getMsaaSamples());
        }

        window = GLFW.glfwCreateWindow(1280, 720, "Bolt SDR", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1); // Enable v-sync
        GLFW.glfwShowWindow(window);

        GL.createCapabilities();
        if (Configuration.getMsaaSamples() > 1) {
            GL11.glEnable(GL13.GL_MULTISAMPLE);
        }

        ImGui.createContext();
        imGuiGlfw.init(window, true);
        imGuiGl3.init("#version 330 core");
        ImPlot.createContext();

        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);

        ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setOversampleH(3);
        fontConfig.setOversampleV(3);
        fontConfig.setPixelSnapH(false);

        ImFontAtlas fontAtlas = io.getFonts();
        fontAtlas.addFontDefault(fontConfig);
        try {
            ByteBuffer robotoRegular = Resources.readResourceToByteBuffer("/fonts/Roboto-Regular.ttf");
            ByteBuffer robotoMedium = Resources.readResourceToByteBuffer("/fonts/Roboto-Medium.ttf");
            ByteBuffer robotoSemiBold = Resources.readResourceToByteBuffer("/fonts/Roboto-SemiBold.ttf");
            ByteBuffer robotoBold = Resources.readResourceToByteBuffer("/fonts/Roboto-Bold.ttf");
            byte[] rr = new byte[robotoRegular.remaining()];
            robotoRegular.get(rr);
            byte[] rm = new byte[robotoMedium.remaining()];
            robotoMedium.get(rm);
            byte[] rs = new byte[robotoSemiBold.remaining()];
            robotoSemiBold.get(rs);
            byte[] rb = new byte[robotoBold.remaining()];
            robotoBold.get(rb);
            Fonts.robotoSemiBold24 = fontAtlas.addFontFromMemoryTTF(rs, 24.0f, fontConfig);
            Fonts.robotoBold16 = fontAtlas.addFontFromMemoryTTF(rb, 16.0f, fontConfig);
            Fonts.robotoSemiBold16 = fontAtlas.addFontFromMemoryTTF(rs, 16.0f, fontConfig);
            Fonts.robotoMedium16 = fontAtlas.addFontFromMemoryTTF(rm, 16.0f, fontConfig);
            Fonts.robotoRegular16 = fontAtlas.addFontFromMemoryTTF(rr, 16.0f, fontConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }

        fontAtlas.build();
        fontConfig.destroy();

        ImGuiStyle style = ImGui.getStyle();
        style.setAntiAliasedLines(true);
        style.setAntiAliasedLinesUseTex(true);
        style.setAntiAliasedFill(true);

        waterfall = new Waterfall(1280, 720);
        waterfall.initialize();
        frequencyWidget.initialize();

        PortAudioContext.getInstance().getPortAudioJNI().initialize();
    }

    public void loop() {
        boolean firstTime = true;
        float[] fftData = new float[Configuration.getFftSize()];

        waterfall.update(fftData);
        panadapter.update(fftData);

        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();

            if (pipelineRunning) {
                try {
                    dspThread.getWaterfallOutputBuffer().read(fftData, 0, fftData.length);
                } catch (InterruptedException e) {
                    logger.error("Main loop interrupted during waterfall read", e);
                }
            }

            if (pipelineRunning) {
                waterfall.update(fftData);
                panadapter.update(fftData);
            }

            imGuiGlfw.newFrame();
            imGuiGl3.newFrame();
            ImGui.newFrame();

            ImGui.setNextWindowPos(0, 0);
            ImGui.setNextWindowSize(ImGui.getMainViewport().getSizeX(), ImGui.getMainViewport().getSizeY());
            ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0);
            ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0);
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);

            int windowFlags = ImGuiWindowFlags.NoDocking |
                    ImGuiWindowFlags.NoTitleBar |
                    ImGuiWindowFlags.NoCollapse |
                    ImGuiWindowFlags.NoResize |
                    ImGuiWindowFlags.NoMove |
                    ImGuiWindowFlags.NoBringToFrontOnFocus |
                    ImGuiWindowFlags.NoNavFocus;

            ImGui.begin("DockSpace", windowFlags);
            ImGui.popStyleVar(3);
            int dockspaceId = ImGui.getID("MyDockSpace");
            if (firstTime) {
                ImGui.dockSpace(dockspaceId);
                imgui.internal.ImGui.dockBuilderRemoveNode(dockspaceId);
                imgui.internal.ImGui.dockBuilderAddNode(dockspaceId, imgui.internal.flag.ImGuiDockNodeFlags.DockSpace);
                imgui.internal.ImGui.dockBuilderSetNodeSize(dockspaceId, ImGui.getMainViewport().getSizeX(), ImGui.getMainViewport().getSizeY());

                ImInt topId = new ImInt();
                ImInt bottomId = new ImInt();
                ImInt sidebarId = new ImInt();
                ImInt mainId = new ImInt();
                
                imgui.internal.ImGui.dockBuilderSplitNode(dockspaceId, ImGuiDir.Up, 0.2f, topId, bottomId);
                
                imgui.internal.ImGui.dockBuilderSplitNode(bottomId.get(), ImGuiDir.Left, 0.25f, sidebarId, mainId);

                imgui.internal.ImGui.dockBuilderDockWindow("TopBar", topId.get());
                imgui.internal.ImGui.dockBuilderDockWindow("Sidebar", sidebarId.get());
                imgui.internal.ImGui.dockBuilderDockWindow("Waterfall", mainId.get());
                
                imgui.internal.ImGui.dockBuilderFinish(dockspaceId);
                firstTime = false;
            }

            ImGui.end();

            ImGui.begin("TopBar", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);
            ImGui.setWindowPos(0, 0);
            ImGui.setWindowSize(ImGui.getMainViewport().getSizeX(), 0);

            playPauseButton.render();
            if (!pipelineRunning && playPauseButton.isPlaying()) {
                inputSource = InputSourceFactory.createInputSource();
                inputThread = new InputThread(inputSource);
                dspThread = new DspThread(inputThread.getBuffer(), new HannWindow());
                inputThread.start();
                dspThread.start();
                pipelineRunning = true;
            } else if (pipelineRunning && !playPauseButton.isPlaying()) {
                inputThread.stop();
                dspThread.stop();
                pipelineRunning = false;
            }

            ImGui.sameLine();
            ImGui.dummy(10, 0);
            ImGui.sameLine();
            
            frequencyWidget.render();

            ImVec2 parent_pos = ImGui.getWindowPos();
            ImVec2 parent_size = ImGui.getWindowSize();

            ImVec2 new_window_pos = new ImVec2(
                parent_pos.x,
                parent_pos.y + parent_size.y
            );

            ImGui.setNextWindowPos(new_window_pos);
            ImGui.end();

            ImGui.begin("LeftSidebar", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);
            ImGui.setWindowSize(250, ImGui.getMainViewport().getSizeY() - parent_size.y);

            if (ImGui.collapsingHeader("Input Source", ImGuiTreeNodeFlags.DefaultOpen)) {
                if (ImGui.beginCombo("##inputsource", Configuration.getInputDevice())) {
                    if (ImGui.selectable("Audio", Configuration.getInputDevice().equals("Audio"))) {
                        Configuration.setInputDevice("Audio");
                    }
                    if (Configuration.getInputDevice().equals("Audio")) {
                        ImGui.setItemDefaultFocus();
                    }

                    if (ImGui.selectable("File", Configuration.getInputDevice().equals("File"))) {
                        Configuration.setInputDevice("File");
                    }
                    if (Configuration.getInputDevice().equals("File")) {
                        ImGui.setItemDefaultFocus();
                    }

                    if (ImGui.selectable("RTL-SDR", Configuration.getInputDevice().equals("RTL-SDR"))) {
                        Configuration.setInputDevice("RTL-SDR");
                    }
                    if (Configuration.getInputDevice().equals("RTL-SDR")) {
                        ImGui.setItemDefaultFocus();
                    }

                    if (ImGui.selectable("Dummy", Configuration.getInputDevice().equals("Dummy"))) {
                        Configuration.setInputDevice("Dummy");
                    }
                    if (Configuration.getInputDevice().equals("Dummy")) {
                        ImGui.setItemDefaultFocus();
                    }

                    ImGui.endCombo();
                }

                ImGui.beginDisabled(pipelineRunning);
                if (Configuration.getInputDevice().equals("RTL-SDR")) {
                    if (ImGui.beginCombo("##RTLSDRDevice", RTLSDR.getDeviceName(Configuration.getRtlSdrConfig().getRtlSdrDeviceIndex()))) { // TODO: Dont poll every frame for device name
                        int deviceCount = RTLSDR.getDeviceCount();
                        for (int i = 0; i < deviceCount; i++) {
                            String deviceName = RTLSDR.getDeviceName(i);
                            if (ImGui.selectable(deviceName, Configuration.getRtlSdrConfig().getRtlSdrDeviceIndex() == i)) {
                                Configuration.getRtlSdrConfig().setRtlSdrDeviceIndex(i);
                            }
                            if (Configuration.getRtlSdrConfig().getRtlSdrDeviceIndex() == i) {
                                ImGui.setItemDefaultFocus();
                            }
                        }
                        ImGui.endCombo();
                    }

                    ImGui.text("Sample Rate");

                    if (ImGui.beginCombo("##SampleRate", UnitFormatter.formatFrequency(Configuration.getRtlSdrConfig().getRtlSdrSampleRate()))) {
                        for (int rate : Configuration.getRtlSdrConfig().getRtlSdrSampleRates()) {
                            if (ImGui.selectable(UnitFormatter.formatFrequency(rate), Configuration.getRtlSdrConfig().getRtlSdrSampleRate() == rate)) {
                                Configuration.getRtlSdrConfig().setRtlSdrSampleRate(rate);
                            }
                            if (Configuration.getRtlSdrConfig().getRtlSdrSampleRate() == rate) {
                                ImGui.setItemDefaultFocus();
                            }
                        }
                        ImGui.endCombo();
                    }
                }
                ImGui.endDisabled();
            }
            
            if (ImGui.collapsingHeader("Output", ImGuiTreeNodeFlags.DefaultOpen)) {
                if (ImGui.beginCombo("##AudioOutputDevice", Configuration.getAudioOutputDevice())) {
                    PortAudioContext portAudioContext = PortAudioContext.getInstance();
                    for (DeviceInfo device : portAudioContext.getPortAudioJNI().enumerateDevices()) {
                        if (ImGui.selectable(device.name(), Configuration.getAudioOutputDevice().equals(device.name()))) {
                            Configuration.setAudioOutputDevice(device.name());
                        }
                        if (Configuration.getAudioOutputDevice().equals(device.name())) {
                            ImGui.setItemDefaultFocus();
                        }
                    }
                    ImGui.endCombo();
                }
            }
            
            ImGui.end();

            ImGui.begin("RightSidebar", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);            
            ImGui.setWindowPos(ImGui.getMainViewport().getSizeX() - 35, parent_size.y);
            ImGui.setWindowSize(35, ImGui.getMainViewport().getSizeY() - parent_size.y);
            ImGui.text("Max");
            float[] waterfallMax = new float[] { (float) Configuration.getWaterfallMaxDb() };
            if (ImGui.vSliderFloat("##Max", new ImVec2(20, 150), waterfallMax, -100.0f, 0.0f, "##")) {
                Configuration.setWaterfallMaxDb(waterfallMax[0]);
            }
            ImGui.dummy(0, 10);
            ImGui.text("Min");
            float[] waterfallMin = new float[] { (float) Configuration.getWaterfallMinDb() };
            if (ImGui.vSliderFloat("##Min", new ImVec2(20, 150), waterfallMin, -100.0f, 0.0f, "##")) {
                Configuration.setWaterfallMinDb(waterfallMin[0]);
            }
            ImGui.end();

            // create center window for waterfall and panadapter
            ImGui.begin("CenterWindow", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);
            ImGui.setWindowPos(250, parent_size.y);
            ImGui.setWindowSize(ImGui.getMainViewport().getSizeX() - 285, ImGui.getMainViewport().getSizeY() - parent_size.y);
            panadapter.render();
            waterfall.render();
            ImGui.end();

            // ImGui.begin("Waterfall");
            // ImGui.end();

            // ImGui.begin("Panadapter");
            // panadapter.update(fftData);
            // ImGui.end();

            ImGui.render();
            GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            GLFW.glfwSwapBuffers(window);
        }
    }

    public void stop() {
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
