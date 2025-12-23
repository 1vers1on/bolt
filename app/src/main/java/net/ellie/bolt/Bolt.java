package net.ellie.bolt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.ellie.bolt.audio.AudioConsumerThread;
import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.contexts.PortAudioContext;
import net.ellie.bolt.decoder.DecoderOutputTypes;
import net.ellie.bolt.decoder.DecoderThread;
import net.ellie.bolt.dsp.DspThread;
import net.ellie.bolt.dsp.IIRFilterDesigns;
import net.ellie.bolt.dsp.IWindow;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.attributes.ConstantAttribute;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;
import net.ellie.bolt.dsp.pipelineSteps.Average;
import net.ellie.bolt.dsp.pipelineSteps.Decimator;
import net.ellie.bolt.dsp.pipelineSteps.FrequencyShifter;
import net.ellie.bolt.dsp.pipelineSteps.Hysteresis;
import net.ellie.bolt.dsp.pipelineSteps.IIRFilter;
import net.ellie.bolt.dsp.pipelineSteps.RealWaterfall;
import net.ellie.bolt.dsp.pipelineSteps.Threshold;
import net.ellie.bolt.dsp.pipelineSteps.WAVRecorder;
import net.ellie.bolt.dsp.pipelineSteps.demodulators.SSBDemodulator;
import net.ellie.bolt.dsp.windows.HannWindow;
import net.ellie.bolt.gui.AudioScope;
import net.ellie.bolt.gui.ByteOutputGui;
import net.ellie.bolt.gui.FrequencyWidget;
import net.ellie.bolt.gui.Panadapter;
import net.ellie.bolt.gui.PlayPauseButton;
import net.ellie.bolt.gui.TextOutputGui;
import net.ellie.bolt.gui.Waterfall;
import net.ellie.bolt.input.CloseableInputSource;
import net.ellie.bolt.input.InputSourceFactory;
import net.ellie.bolt.input.InputThread;
import net.ellie.bolt.jni.portaudio.PortAudioJNI.DeviceInfo;
import net.ellie.bolt.jni.rtlsdr.RTLSDR;
import net.ellie.bolt.util.Buffers;
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

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

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
    private AudioConsumerThread outputThread;
    private DecoderThread decoderThread;

    private CircularFloatBuffer waterfallBuffer;

    private TextOutputGui textOutputGui;
    private ByteOutputGui byteOutputGui;

    private AudioScope audioScope;

    private CircularFloatBuffer audioOutputBuffer = Buffers.EMPTY_FLOAT_BUFFER;

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

        ByteBuffer iconBuffer;
        try (InputStream is = Bolt.class.getResourceAsStream("/images/icon.png")) {
            if (is == null)
                throw new RuntimeException("Resource not found");
            byte[] bytes = is.readAllBytes();
            iconBuffer = MemoryUtil.memAlloc(bytes.length);
            iconBuffer.put(bytes).flip();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load icon resource", e);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            ByteBuffer image = STBImage.stbi_load_from_memory(iconBuffer, w, h, comp, 4);
            if (image == null)
                throw new RuntimeException("Failed to load icon: " + STBImage.stbi_failure_reason());

            GLFWImage.Buffer icon = GLFWImage.malloc(1);
            icon.width(w.get(0));
            icon.height(h.get(0));
            icon.pixels(image);

            GLFW.glfwSetWindowIcon(window, icon);

            STBImage.stbi_image_free(image);
            icon.free();
        }

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

        waterfall = new Waterfall(2048, 720);
        waterfall.initialize();
        frequencyWidget.initialize();
        waterfallBuffer = new CircularFloatBuffer(Configuration.getFftSize() * 2);
        PortAudioContext.getInstance().getPortAudioJNI().initialize();

        textOutputGui = new TextOutputGui(Buffers.EMPTY_CHAR_BUFFER);
        byteOutputGui = new ByteOutputGui(Buffers.EMPTY_BYTE_BUFFER);

        audioScope = new AudioScope();
    }

    private void stopPipeline() {
        logger.info("Stopping pipeline");
        pipelineRunning = false;

        if (inputThread != null)
            inputThread.stop();
        if (dspThread != null)
            dspThread.stop();
        if (outputThread != null)
            outputThread.stop();

        if (dspThread != null) {
            for (var step : dspThread.getPipelineSteps()) {
                step.reset();
            }
        }

        if (decoderThread != null)
            decoderThread.stop();

        inputThread = null;
        dspThread = null;
        outputThread = null;
    }

    private boolean isInputSourceDecodeOnly() {
        String device = Configuration.getInputDevice();
        return device.equals("PortAudio") || device.equals("File");
    }

    public void loop() {
        boolean firstTime = true;
        float[] fftData = new float[Configuration.getFftSize()];

        waterfall.update(fftData);
        panadapter.update(fftData);

        int previousFrequency = Configuration.getTargetFrequency();

        float[] audioData = new float[4096];

        long lastFrameTime = System.nanoTime();
        float deltaTime = 0.0f;
        int frameCount = 0;
        float fps = 0.0f;
        long lastFpsUpdate = System.nanoTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();

            // read into audio data from dsp output buffer
            audioOutputBuffer.readNonBlocking(audioData, 0, audioData.length);
            audioScope.update(audioData, Configuration.getSampleRate());

            long now = System.nanoTime();
            deltaTime = (now - lastFrameTime) / 1_000_000_000.0f;
            lastFrameTime = now;
            frameCount++;
            if (now - lastFpsUpdate >= 1_000_000_000L) {
                fps = frameCount / ((now - lastFpsUpdate) / 1_000_000_000.0f);
                frameCount = 0;
                lastFpsUpdate = now;
            }

            if (pipelineRunning) {
                waterfallBuffer.readNonBlocking(fftData, 0, fftData.length);

                waterfall.update(fftData);
                panadapter.update(fftData);
            }

            // if (previousFrequency != Configuration.getTargetFrequency()) {
            //     previousFrequency = Configuration.getTargetFrequency();
            //     if (dspThread != null) {
            //         if (demod != null) {
            //             demod.setFrequencyOffsetHz(
            //                     Configuration.getTargetFrequency()
            //                             - Configuration.getRtlSdrConfig().getRtlSdrCenterFrequency());
            //             System.out
            //                     .println(
            //                             "Set frequency offset to "
            //                                     + (Configuration.getTargetFrequency()
            //                                             - Configuration.getRtlSdrConfig().getRtlSdrCenterFrequency())
            //                                     + " Hz");
            //         }
            //     }
            // }

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
                imgui.internal.ImGui.dockBuilderSetNodeSize(dockspaceId, ImGui.getMainViewport().getSizeX(),
                        ImGui.getMainViewport().getSizeY());

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
                logger.info("Starting pipeline");
                inputSource = InputSourceFactory.createInputSource();
                inputThread = new InputThread(inputSource, Configuration.getSampleRate());
                dspThread = new DspThread(inputThread.getBuffer(), 512,
                        inputSource.isComplex() ? NumberType.COMPLEX : NumberType.REAL);
                dspThread.clearPipeline();

                if (inputSource.isComplex()) {
            int inputSampleRate = inputSource.getSampleRate();
            int outputSampleRate = Configuration.getSampleRate();
            int decimationFactor = inputSampleRate / outputSampleRate;

            dspThread.addPipelineStep(new net.ellie.bolt.dsp.pipelineSteps.Waterfall(
                waterfallBuffer,
                new ConstantAttribute<IWindow>(new HannWindow()),
                new ConstantAttribute<Integer>(Configuration.getFftSize())));

            double[][] iir = IIRFilterDesigns.butterworthLowpass2(
                inputSampleRate,
                3000.0);

            dspThread.addPipelineStep(new IIRFilter(
                new ConstantAttribute<double[]>(iir[0]),
                new ConstantAttribute<double[]>(iir[1]), dspThread.getPipeline()));

            dspThread.addPipelineStep(
                new SSBDemodulator(
                new ConstantAttribute<Boolean>(true),
                new ConstantAttribute<Double>((double) inputSampleRate),
                new ConstantAttribute<Double>((double) (Configuration.getTargetFrequency()
                    - Configuration.getRtlSdrConfig().getRtlSdrCenterFrequency())), dspThread.getPipeline()));

            if (decimationFactor > 1) {
            dspThread.addPipelineStep(new Decimator(
                new ConstantAttribute<Integer>(decimationFactor),
                new ConstantAttribute<Double>((double)inputSampleRate), dspThread.getPipeline(), true));
            }

            dspThread.addPipelineStep(new WAVRecorder(
            new ConstantAttribute<Double>((double) Configuration.getSampleRate())));

            dspThread.buildPipeline();

            outputThread = new AudioConsumerThread(dspThread.getAudioOutputBuffer());
            inputThread.start();
            dspThread.start();
            outputThread.start();
                } else {
                    dspThread.addPipelineStep(new RealWaterfall(
                            waterfallBuffer,
                            new ConstantAttribute<IWindow>(new HannWindow()),
                            new ConstantAttribute<Integer>(Configuration.getFftSize())));

                    dspThread.addPipelineStep(new WAVRecorder(
                        new ConstantAttribute<Double>((double) Configuration.getSampleRate())));

                    // dspThread.addPipelineStep(new Goertzel(
                    //         new ConstantAttribute<Integer>(400),
                    //         new ConstantAttribute<Integer>(inputSource.getSampleRate()),
                    //         new ConstantAttribute<Integer>(512)));

                    // dspThread.addPipelineStep(new Decimator(
                    //     new ConstantAttribute<Integer>(8),
                    //     new ConstantAttribute<Integer>(inputSource.getSampleRate())));

                    dspThread.addPipelineStep(new Threshold(
                        new ConstantAttribute<Double>(0.3)));

                    dspThread.addPipelineStep(new Average(
                        new ConstantAttribute<Integer>(128),
                        dspThread.getPipeline()));

                    dspThread.addPipelineStep(new Hysteresis(
                        new ConstantAttribute<Double>(0.1),
                        new ConstantAttribute<Double>(0.05)));

                    dspThread.buildPipeline();

                    audioOutputBuffer = dspThread.getAudioOutputBuffer();

                    decoderThread = new DecoderThread(dspThread.getAudioOutputBuffer(), Configuration.getSampleRate());
                    waterfall.setMarkerFrequency(1000);

                    inputThread.start();
                    dspThread.start();
                }
                pipelineRunning = true;
            } else if (pipelineRunning && !playPauseButton.isPlaying()) {
                stopPipeline();
            }

            ImGui.sameLine();
            ImGui.dummy(10, 0);
            ImGui.sameLine();

            frequencyWidget.render();

            ImVec2 parent_pos = ImGui.getWindowPos();
            ImVec2 parent_size = ImGui.getWindowSize();

            ImVec2 new_window_pos = new ImVec2(
                    parent_pos.x,
                    parent_pos.y + parent_size.y);

            ImGui.setNextWindowPos(new_window_pos);
            ImGui.end();

            ImGui.begin("LeftSidebar",
                    ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);
            ImGui.setWindowSize(250, ImGui.getMainViewport().getSizeY() - parent_size.y);

            if (ImGui.collapsingHeader("Input Source", ImGuiTreeNodeFlags.DefaultOpen)) {
                if (ImGui.beginCombo("##inputsource", Configuration.getInputDevice())) {
                    if (ImGui.selectable("PortAudio", Configuration.getInputDevice().equals("PortAudio"))) {
                        Configuration.setInputDevice("PortAudio");
                    }
                    if (Configuration.getInputDevice().equals("PortAudio")) {
                        ImGui.setItemDefaultFocus();
                    }

                    if (ImGui.selectable("Javax Audio", Configuration.getInputDevice().equals("JavaxAudio"))) {
                        Configuration.setInputDevice("JavaxAudio");
                    }
                    if (Configuration.getInputDevice().equals("JavaxAudio")) {
                        ImGui.setItemDefaultFocus();
                    }

                    if (ImGui.selectable("OpenAL", Configuration.getInputDevice().equals("OpenAL"))) {
                        Configuration.setInputDevice("OpenAL");
                    }
                    if (Configuration.getInputDevice().equals("OpenAL")) {
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
                    // TODO: Dont poll every frame for device name
                    if (ImGui.beginCombo("##RTLSDRDevice",
                            RTLSDR.getDeviceName(Configuration.getRtlSdrConfig().getRtlSdrDeviceIndex()))) {
                        int deviceCount = RTLSDR.getDeviceCount();
                        for (int i = 0; i < deviceCount; i++) {
                            String deviceName = RTLSDR.getDeviceName(i);
                            if (ImGui.selectable(deviceName,
                                    Configuration.getRtlSdrConfig().getRtlSdrDeviceIndex() == i)) {
                                Configuration.getRtlSdrConfig().setRtlSdrDeviceIndex(i);
                            }
                            if (Configuration.getRtlSdrConfig().getRtlSdrDeviceIndex() == i) {
                                ImGui.setItemDefaultFocus();
                            }
                        }
                        ImGui.endCombo();
                    }

                    ImGui.text("Sample Rate");

                    if (ImGui.beginCombo("##SampleRate",
                            UnitFormatter.formatFrequency(Configuration.getRtlSdrConfig().getRtlSdrSampleRate()))) {
                        for (int rate : Configuration.getRtlSdrConfig().getRtlSdrSampleRates()) {
                            if (ImGui.selectable(UnitFormatter.formatFrequency(rate),
                                    Configuration.getRtlSdrConfig().getRtlSdrSampleRate() == rate)) {
                                Configuration.getRtlSdrConfig().setRtlSdrSampleRate(rate);
                            }
                            if (Configuration.getRtlSdrConfig().getRtlSdrSampleRate() == rate) {
                                ImGui.setItemDefaultFocus();
                            }
                        }
                        ImGui.endCombo();
                    }
                }

                if (PortAudioContext.getInstance()
                        .getDeviceInfoByIndex(Configuration.getPortAudioConfig().getDeviceIndex()) == null) {
                    Configuration.getPortAudioConfig().setDeviceIndex(0);
                }

                if (Configuration.getInputDevice().equals("PortAudio")) {
                    ImGui.text("Sample Rate");
                    if (ImGui.beginCombo("##AudioSampleRate",
                            UnitFormatter.formatFrequency(Configuration.getPortAudioConfig().getSampleRate()))) {
                        int[] commonRates = new int[] { 8000, 16000, 22050, 32000, 44100, 48000, 96000, 192000 };
                        for (int rate : commonRates) {
                            if (PortAudioContext.getInstance().getPortAudioJNI()
                                    .isFormatSupported(
                                            Configuration.getPortAudioConfig().getDeviceIndex(),
                                            Configuration.getPortAudioConfig().getChannelCount(),
                                            rate)) {
                                if (ImGui.selectable(UnitFormatter.formatFrequency(rate),
                                        Configuration.getPortAudioConfig().getSampleRate() == rate)) {
                                    Configuration.getPortAudioConfig().setSampleRate(rate);
                                }
                                if (Configuration.getPortAudioConfig().getSampleRate() == rate) {
                                    ImGui.setItemDefaultFocus();
                                }
                            }
                        }
                        ImGui.endCombo();
                    }

                    ImGui.text("Input Device");
                    if (ImGui.beginCombo("##AudioInputDevice", PortAudioContext.getInstance()
                            .getDeviceInfoByIndex(Configuration.getPortAudioConfig().getDeviceIndex()).name())) {
                        PortAudioContext portAudioContext = PortAudioContext.getInstance();
                        for (DeviceInfo device : portAudioContext.getPortAudioJNI().enumerateDevices()) {
                            if (ImGui.selectable(device.name(),
                                    Configuration.getPortAudioConfig().getDeviceIndex() == device.index())) {
                                logger.info("Selected PortAudio input device with properties: " + device);
                                Configuration.getPortAudioConfig().setDeviceIndex(device.index());
                            }
                            if (Configuration.getPortAudioConfig().getDeviceIndex() == device.index()) {
                                ImGui.setItemDefaultFocus();
                            }
                        }
                        ImGui.endCombo();
                    }
                }

                if (Configuration.getInputDevice().equals("JavaxAudio")) {
                    ImGui.text("Input Device");
                    if (ImGui.beginCombo("##JavaxAudioInputDevice",
                            Configuration.getJavaxAudioConfig().getDeviceName())) {
                        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
                        for (Mixer.Info info : mixers) {
                            Mixer m = AudioSystem.getMixer(info);

                            Line.Info[] targetLines = m.getTargetLineInfo();
                            if (targetLines.length > 0) {
                                if (ImGui.selectable(info.getName(),
                                        Configuration.getJavaxAudioConfig().getDeviceName().equals(info.getName()))) {
                                    Configuration.getJavaxAudioConfig().setDeviceName(info.getName());
                                }
                                if (Configuration.getJavaxAudioConfig().getDeviceName().equals(info.getName())) {
                                    ImGui.setItemDefaultFocus();
                                }
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
                        if (ImGui.selectable(device.name(),
                                Configuration.getAudioOutputDevice().equals(device.name()))) {
                            Configuration.setAudioOutputDevice(device.name());
                        }
                        if (Configuration.getAudioOutputDevice().equals(device.name())) {
                            ImGui.setItemDefaultFocus();
                        }
                    }
                    ImGui.endCombo();
                }
            }

            if (ImGui.collapsingHeader("Recording", ImGuiTreeNodeFlags.DefaultOpen)) {
                if (ImGui.button("Start WAV Recording")) {
                    if (dspThread != null) {
                        dspThread.getPipeline().getFirstPipelineStepOfType(WAVRecorder.class).startRecording();
                    }
                }

                if (ImGui.button("Stop WAV Recording")) {
                    if (dspThread != null) {
                        try {
                            dspThread.getPipeline().getFirstPipelineStepOfType(WAVRecorder.class).stopRecordingAndSave("recordings/", dspThread.getPipeline());
                        } catch (IOException e) {
                            logger.error("Failed to save WAV recording: {}", e.getMessage());
                        }
                    }
                }
            }

            if (ImGui.collapsingHeader("Debug", ImGuiTreeNodeFlags.DefaultOpen)) {
                ImGui.text("Input Source: " + (inputSource != null ? inputSource.getName() : "None"));
                ImGui.text("Pipeline Running: " + pipelineRunning);
                ImGui.text(String.format("Delta Time: %.3f ms", deltaTime * 1000.0f));
                ImGui.text(String.format("FPS: %.1f", fps));
            }

            ImGui.end();

            ImGui.begin("RightSidebar",
                    ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);
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

            if (!isInputSourceDecodeOnly()) {
                ImGui.begin("SDR", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);
                ImGui.setWindowPos(250, parent_size.y);
                ImGui.setWindowSize(ImGui.getMainViewport().getSizeX() - 285,
                        ImGui.getMainViewport().getSizeY() - parent_size.y);
                panadapter.render();
                waterfall.render();
                ImGui.end();
            } else {
                ImGui.begin("Decoder", ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);
                ImGui.setWindowPos(250, parent_size.y);
                ImGui.setWindowSize(ImGui.getMainViewport().getSizeX() - 285,
                        ImGui.getMainViewport().getSizeY() - parent_size.y);
                if (decoderThread != null && decoderThread.getOutputType() == DecoderOutputTypes.TEXT) {
                    textOutputGui.setCircularCharBuffer(decoderThread.getTextOutputBuffer());
                    textOutputGui.render();
                } else if (decoderThread != null && decoderThread.getOutputType() == DecoderOutputTypes.RAW_BYTES) {
                    byteOutputGui.setCircularByteBuffer(decoderThread.getRawByteOutputBuffer());
                    byteOutputGui.render();
                }

                audioScope.render();

                waterfall.render();
                ImGui.end();
            }

            ImGui.render();
            GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            GLFW.glfwSwapBuffers(window);
        }
    }

    public void stop() {
        stopPipeline();

        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
