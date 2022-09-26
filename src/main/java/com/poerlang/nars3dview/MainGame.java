package com.poerlang.nars3dview;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.poerlang.nars3dview.items.line3d.Line3dMeshSegment;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.extension.implot.ImPlot;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.*;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;

import static com.poerlang.nars3dview.GUI.initFonts;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import org.opennars.entity.*;
import org.opennars.main.Nar;

public class MainGame extends InputAdapter implements ApplicationListener {
    public static MainGame inst;
    public static DecalBatch dbatch;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    public PerspectiveCamera cam;
    public static Array<Item3d> instances = new Array<Item3d>();
    SpriteBatch batch;
    Texture img;
    ExtendViewport extendViewport;
    public static Nar nar;
    public static boolean imGuiHover;
    int downScreenX;
    int downScreenY;
    private String glslVersion = null;
    private ModelBatch modelBatch;
    private Environment environment;
    private CameraInputController camController;
    private Stage stage;
    private BitmapFont font;
    private Label label;
    private StringBuilder stringBuilder;
    private Vector3 position = new Vector3();
    private Item3d oldSelectItem = null;
    private Item3d selectItem = null;
    private Long windowHandle = 0l;
    private ArrayList<Vector3> list = new ArrayList<Vector3>(3);
    private int printNum;

    @Override
    public void create() {
        createLabel();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        modelBatch = new ModelBatch();
        batch = new SpriteBatch();
        img = new Texture("task.png");
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(55f, 55f, 55f);
        cam.lookAt(0, 0, 0);
        cam.near = 0.01f;
        cam.far = 300f;
        dbatch = new DecalBatch(new CameraGroupStrategy(cam));
        Item3d.cam = cam;

        camController = new CameraInputController(cam);

        extendViewport = new ExtendViewport(100, 10,99999,99999,cam);
        Gdx.input.setInputProcessor(new InputMultiplexer(this, camController));
        cam.update();


//        Item3d mesh = new Item3d().toMesh();
//        add(mesh);
//
//        Item3d plane = new Item3d().toPlane();
//        plane.setPos(new Vector3(5,0,0));
//        add(plane);
//
//        Item3d line3d = new Item3d().toLine();
//        line3d.setLinePos(mesh.getPos(),plane.getPos(),mesh.getSize(),plane.getSize());
//        add(line3d);


        initGUI();
        inst = this;
    }

    private void initGUI() {
        GL.createCapabilities();
        GLFW.glfwSwapInterval(GLFW.GLFW_TRUE);

        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        ImGui.createContext();
        final ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);  // Enable Keyboard Controls
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);      // Enable Docking
        io.setConfigViewportsNoTaskBarIcon(true);
        initFonts(io);

        windowHandle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();

        imGuiGlfw.init(windowHandle, true);
        imGuiGl3.init(glslVersion);

        ImPlot.createContext();

        final long backupWindowPtr = GLFW.glfwGetCurrentContext();
        System.out.println(backupWindowPtr);
    }
    private void renderGUI() {

        imGuiGlfw.newFrame();
        ImGui.newFrame();


        GUI.showGUI(); // 绘制界面


        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupWindowPtr);
        }
        glfwPollEvents();
    }

    public static Item3d add(Item3d item3d) {
        instances.add(item3d);
        return item3d;
    }
    public static void clearInstances() {
        instances.clear();
    }

    private void createLabel() {
        stage = new Stage();
        font = new BitmapFont();
        label = new Label(" ", new Label.LabelStyle(font, Color.WHITE));
        label.setPosition(6f,6f, Align.bottom);
        stage.addActor(label);
        stringBuilder = new StringBuilder();
    }
    public static LinkedList<Item3d> plane3ds = new LinkedList<>();
    public static LinkedList<Item3d> visibles = new LinkedList<>();
    @Override
    public void render() {
        camController.update();
        extendViewport.apply();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        float deltaTime = Gdx.graphics.getDeltaTime();

        plane3ds.clear();
        visibles.clear();

        modelBatch.begin(cam);
        for (final Item3d instance : instances) {
            if (isVisible(cam, instance)) {
                visibles.add(instance);
                if (instance.isMesh()){
                    modelBatch.render(instance.mesh3d.meshModelInstance, environment);
                }else if(instance.isLine()){
                    instance.updateLine3d(deltaTime);
                    modelBatch.render(instance.line3d.meshModelInstanceLine, environment);
                }else if(instance.isPlane()){
                    plane3ds.add(instance);
                }
            }
        }
        modelBatch.end();

        renderPlanes();

        renderLabel();

        renderGUI();
    }

    private void renderLabel() {
        stringBuilder.setLength(0);
        stringBuilder.append("  FPS: ").append(Gdx.graphics.getFramesPerSecond());
        stringBuilder.append("  Visible: ").append(visibles.size());
        stringBuilder.append("  Selected Item: ").append(selectItem!=null ? getTermString(selectItem) : "none");
        label.setText(stringBuilder);
        stage.getViewport().apply();
        stage.draw();
    }

    private String getTermString(Item3d selectItem) {
        if(selectItem instanceof Concept){
            return selectItem.toString();
        }else if(selectItem instanceof TermLink){
            return ((TermLink) selectItem).getTarget().toString();
        }
        return " no to string function ";
    }

    private void renderPlanes() {
        for (int i = 0; i < plane3ds.size(); i++) {
            Item3d item3d = plane3ds.get(i);
            Decal decal = item3d.plane3d.decal;

            // billboarding for ortho cam :)
            // dir.set(-camera.direction.x, -camera.direction.y, -camera.direction.z);
            // decal.setRotation(dir, Vector3.Y);

            // billboarding for perspective cam
            decal.lookAt(cam.position, cam.up);

            dbatch.add(decal);
        }
        dbatch.flush();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if(imGuiHover) return true;
        downScreenX = screenX;
        downScreenY = screenY;
        //返回 false 代表其它同类事件函数可以继续执行，true 代表跳过其它同类事件
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if(imGuiHover) return true;
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if(imGuiHover) return true;
        // 拖拽在较小范围时才算点击
        boolean dragInSmallSize = (Math.abs(downScreenX - screenX) + Math.abs(downScreenY - screenY)) < 5;
        if (dragInSmallSize) {
            oldSelectItem = selectItem;
            selectItem = null;
            selectItem = getObject(screenX, screenY);
            if(selectItem != null){
                setSelected(selectItem);
            }else{
                setSelected(null);
            }
        }
        return false;
    }

    protected boolean isVisible(final Camera cam, final Item3d instance) {
        instance.getCenter(position);
        return cam.frustum.sphereInFrustum(position, instance.getSize());
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    public void setSelected(Item3d itemNow) {
        // 清除旧的选择：
        Item3d item3d;
        if (oldSelectItem != null) {
            oldSelectItem.unSelect();
            if (itemNow!=null && oldSelectItem.uid == itemNow.uid){
                itemNow.unSelect();
                selectItem = null;
                return;
            }
        }


        // 设置当前选中的物体的高亮颜色或材质：
        selectItem = itemNow;
        if (selectItem != null) {
            selectItem.select();
        }
    }

    public Item3d getObject(int screenX, int screenY) {
        Ray ray = cam.getPickRay(screenX, screenY);
        Item3d result = null;
        float distance = -1;
        for (int i = 0; i < visibles.size(); ++i) {

            //先比较大致距离，如果太大，则直接跳过
            final Item3d instance = instances.get(i);
            instance.getCenter(position);
            float dist2 = ray.origin.dst2(position);
            if (distance >= 0f && dist2 > distance) continue;

            //计算精准落点，是否落点命中半径内部或顶点组内部
            float size = instance.getSize();
            if (instance.isLine()){
                Line3dMeshSegment s = instance.line3d.getSegmentList().get(0);
                list.clear();
                list.add(s.a);
                list.add(s.b);
                list.add(s.c);
                list.add(s.a);
                list.add(s.c);
                list.add(s.d);
                if (Intersector.intersectRayTriangles(ray, list,  null)) {
                    result = instance;
                    distance = dist2;
                }
            }else{
                if (Intersector.intersectRaySphere(ray, position, size, null)) {
                    result = instance;
                    distance = dist2;
                }
            }
        }
        return result;
    }

    @Override
    public void dispose() {
        batch.dispose();
        img.dispose();
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }

    @Override
    public void resize(int width, int height) {
        extendViewport.update(width,height);
    }
}