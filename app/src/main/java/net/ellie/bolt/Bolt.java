package net.ellie.bolt;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.ellie.bolt.gui.FrequencyWidget;
import net.ellie.bolt.gui.Waterfall;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.ImVec2;
import imgui.type.ImInt;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;

public class Bolt {
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

        window = GLFW.glfwCreateWindow(1280, 720, "Bolt SDR", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1); // Enable v-sync
        GLFW.glfwShowWindow(window);

        GL.createCapabilities();

        ImGui.createContext();
        imGuiGlfw.init(window, true);
        imGuiGl3.init("#version 330 core");

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
    }

    public void loop() {
        boolean firstTime = true;

        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();

            float[] dummyFftData = new float[1280];
            for (int i = 0; i < dummyFftData.length; i++) {
                dummyFftData[i] = (float) Math.random();
            }
            waterfall.update(dummyFftData);

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
            frequencyWidget.render();

            ImVec2 parent_pos = ImGui.getWindowPos();
            ImVec2 parent_size = ImGui.getWindowSize();

            ImVec2 new_window_pos = new ImVec2(
                parent_pos.x,
                parent_pos.y + parent_size.y
            );

            ImGui.setNextWindowPos(new_window_pos);
            ImGui.end();

            ImGui.begin("Sidebar", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize);
            ImGui.setWindowSize(250, ImGui.getMainViewport().getSizeY() - parent_size.y);

            if (ImGui.collapsingHeader("Input Source", ImGuiTreeNodeFlags.DefaultOpen)) {
                ImGui.text("Input device selection would go here.");

            }
            
            ImGui.end();

            // ImGui.begin("Waterfall");
            // waterfall.render();
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
