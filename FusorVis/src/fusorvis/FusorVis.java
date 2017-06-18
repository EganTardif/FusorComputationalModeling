package fusorvis;

import fusorcompmodeling.*;

import java.util.List;

import javafx.application.Application;
import static javafx.application.Application.launch;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;

import static javafx.scene.input.KeyCode.*;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.PerspectiveCamera;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import com.interactivemesh.jfx.importer.stl.StlMeshImporter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.scene.control.ProgressBar;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import java.awt.event.MouseMotionListener;

/**
 *
 * @author guberti
 */
public class FusorVis extends Application {

    final Group root = new Group();
    final Xform chargeGroup = new Xform();
    final Xform componentGroup = new Xform();
    final Xform axisGroup = new Xform();
    final Xform referenceGroup = new Xform();
    GraphicsContext eFieldPixels;
    PixelWriter eFieldPixelWriter;
    final Xform world = new Xform();
    final PerspectiveCamera camera = new PerspectiveCamera(true);
    final Xform cameraXform = new Xform();
    final Xform cameraXform2 = new Xform();
    final Xform cameraXform3 = new Xform();

    TextFlow textFieldRoot = new TextFlow();
    Stage textFieldStage = new Stage();
    Stage eFieldStage = new Stage();

    SortedMap<String, TextLogicContainer> output = new TreeMap<>();

    Text consoleDump = new Text();

    String xmlFileName = "SimpleXML";

    double timeStepMCS = 1;

    Point[] points;
    List<GridComponent> parts;

    List<Point> markedPoints;

    Stage primaryStage;

    Box eFieldSlice;
    public boolean eFieldBuilt = false;
    Rotate[] eFieldTransforms;
    EFieldVisualizer eVis;

    // Efield generation stats
    double sliceWidth = 96 / 16; // -3 to 3
    double sliceHeight = 54 / 16;
    double imageConversionFactor = 256;
    int blockSideLength = 24;
    boolean autoUpdate = false;

    ProgressBar pb = new ProgressBar();

    private static final double CAMERA_INITIAL_DISTANCE = -450;
    private static final double CAMERA_INITIAL_X_ANGLE = 70.0;
    private static final double CAMERA_INITIAL_Y_ANGLE = 320.0;
    private static final double CAMERA_NEAR_CLIP = 0.1;
    private static final double CAMERA_FAR_CLIP = 10000.0;

    // Mouse + keyboard vars
    double ONE_FRAME = 1.0 / 24.0;
    double DELTA_MULTIPLIER = 200.0;
    double CONTROL_MULTIPLIER = 0.1;
    double SHIFT_MULTIPLIER = 0.1;
    double ALT_MULTIPLIER = 0.5;
    double mousePosX;
    double mousePosY;
    double mouseOldX;
    double mouseOldY;
    double mouseDeltaX;
    double mouseDeltaY;
    
    String eFieldString;
    
    double annodeVoltage = 0;
    double cathodeVoltage = -500;

    Controller c;

    // Render vars
    double electronRadius = 0.2;

    private void buildElectrons(Point[] points) {
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.BLACK);
        redMaterial.setSpecularColor(Color.DARKGREY);

        final PhongMaterial blackMaterial = new PhongMaterial();

        blackMaterial.setDiffuseColor(Color.DARKRED);
        blackMaterial.setSpecularColor(Color.RED);
        

        for (Point point : points) {
            final Sphere electron = new Sphere(electronRadius);
            electron.setTranslateX(point.x);
            electron.setTranslateY(point.y);
            electron.setTranslateZ(point.z);
            if (point.charge == 1) { // Positive charge
                electron.setMaterial(redMaterial); // Red
            } else { // Negative charge
                electron.setMaterial(blackMaterial); // Black
            }

            chargeGroup.getChildren().add(electron);
        }
        chargeGroup.setVisible(true);
        world.getChildren().addAll(chargeGroup);
    }

    private void buildAxes() { // Red = x, green = y, blue = z
        final PhongMaterial redMaterial = new PhongMaterial();
        redMaterial.setDiffuseColor(Color.DARKRED);
        redMaterial.setSpecularColor(Color.RED);

        final PhongMaterial greenMaterial = new PhongMaterial();
        greenMaterial.setDiffuseColor(Color.DARKGREEN);
        greenMaterial.setSpecularColor(Color.GREEN);

        final PhongMaterial blueMaterial = new PhongMaterial();
        blueMaterial.setDiffuseColor(Color.DARKBLUE);
        blueMaterial.setSpecularColor(Color.BLUE);

        final Box xAxis = new Box(240.0, 1, 1);
        final Box yAxis = new Box(1, 240.0, 1);
        final Box zAxis = new Box(1, 1, 240.0);
        final Sphere posXAxis = new Sphere(2.0);
        final Sphere posYAxis = new Sphere(2.0);
        final Sphere posZAxis = new Sphere(2.0);

        xAxis.setMaterial(redMaterial);
        yAxis.setMaterial(greenMaterial);
        zAxis.setMaterial(blueMaterial);
        posXAxis.setMaterial(redMaterial);
        posXAxis.setTranslateX(120);
        posYAxis.setMaterial(greenMaterial);
        posYAxis.setTranslateY(120);
        posZAxis.setMaterial(blueMaterial);
        posZAxis.setTranslateZ(120);

        axisGroup.getChildren().addAll(xAxis, yAxis, zAxis, posXAxis, posZAxis, posYAxis);
        world.getChildren().addAll(axisGroup);
    }

    private void buildReferencePoints(Point[] referencePoints) {

        for (Point p : referencePoints) {
            final PhongMaterial m = new PhongMaterial();
            m.setDiffuseColor(Color.BLACK);
            m.setDiffuseColor(Color.GREY);
            final Sphere s = new Sphere(0.2);
            s.setTranslateX(p.x);
            s.setTranslateY(p.y);
            s.setTranslateZ(p.z);

            referenceGroup.getChildren().add(s);
        }
        world.getChildren().addAll(referenceGroup);
    }

    private void buildCamera() {
        root.getChildren().add(cameraXform);
        cameraXform.getChildren().add(cameraXform2);
        cameraXform2.getChildren().add(cameraXform3);
        cameraXform3.getChildren().add(camera);
        cameraXform3.setRotateZ(180.0);
        
        output.put("Camera phi", new TextLogicContainer() {@Override
        public double calc() {return cameraXform.ry.getAngle();}});
        
        camera.setNearClip(CAMERA_NEAR_CLIP);
        camera.setFarClip(CAMERA_FAR_CLIP);
        camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
        cameraXform.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
        cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
    }

    private void buildScene() {
        root.getChildren().add(world);
    }

    private void buildTextWindow(Stage primaryStage) {
        compileOutput();
        consoleDump.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        textFieldRoot.getChildren().add(consoleDump);

        textFieldStage.setTitle("Model statistics");
        textFieldStage.setScene(new Scene(textFieldRoot));
        textFieldStage.initOwner(primaryStage);
        textFieldStage.initModality(Modality.APPLICATION_MODAL);
        textFieldStage.setAlwaysOnTop(false);
        
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        
        textFieldStage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 450);
        textFieldStage.setY(primaryScreenBounds.getMinY() + primaryScreenBounds.getHeight() - 600);
        
        textFieldStage.show();
        primaryStage.toFront();
    }

    private void buildEFieldStage(Stage primaryStage, Point[] points) {
        eFieldStage.setTitle("Electric Field");
        Group r = new Group();
        Scene s = new Scene(r, 96 * 16, 54 * 16);
        s.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                eFieldString = eVis.getHumanReadableVal(e.getX(), e.getY());
                compileOutput();
            }
        });
        eFieldStage.setScene(s);
        final Canvas canvas = new Canvas(96 * 16, 54 * 16);
        eFieldPixels = canvas.getGraphicsContext2D();
        eFieldPixelWriter = eFieldPixels.getPixelWriter();
        eVis = new EFieldVisualizer(sliceWidth, sliceHeight, imageConversionFactor, blockSideLength, eFieldPixelWriter, points);
        
        r.getChildren().add(canvas);

        eFieldStage.initOwner(primaryStage);
        eFieldStage.initModality(Modality.APPLICATION_MODAL);
        eFieldStage.show();
        primaryStage.toFront();
        updateEField(points);

        eFieldBuilt = true;

    }
    private void updateEField(Point[] points) {
        eVis.renderSlice(eFieldTransforms, eFieldSlice);
        compileOutput();
    }

    private void buildStage(Stage primaryStage) {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());

        primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    textFieldStage.setAlwaysOnTop(true);
                } else {
                    textFieldStage.setAlwaysOnTop(false);
                }
            }
        });
    }

    // Helper functions
    private double radToDeg(double radians) {
        return (radians * 180) / Math.PI;
    }

    private void handleMouse(Scene scene) {
        scene.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseOldX = me.getSceneX();
                mouseOldY = me.getSceneY();
            }
        });

        scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {

                mouseOldX = mousePosX;
                mouseOldY = mousePosY;
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseDeltaX = (mousePosX - mouseOldX);
                mouseDeltaY = (mousePosY - mouseOldY);

                double modifier = 1.0;
                double modifierFactor = 0.1;

                if (me.isControlDown()) {
                    modifier = 0.1;
                }
                if (me.isShiftDown()) {
                    modifier = 10.0;
                }
                if (me.isPrimaryButtonDown()) {
                    cameraXform.ry.setAngle(cameraXform.ry.getAngle() - mouseDeltaX * modifierFactor * modifier * 2.0);  // +
                    cameraXform.rx.setAngle(cameraXform.rx.getAngle() + mouseDeltaY * modifierFactor * modifier * 2.0);  // -

                } else if (me.isSecondaryButtonDown()) {
                    double z = camera.getTranslateZ();
                    double newZ = z + mouseDeltaX * modifierFactor * modifier;
                    camera.setTranslateZ(newZ);
                } else if (me.isMiddleButtonDown()) {
                    cameraXform2.t.setX(cameraXform2.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3);  // -
                    cameraXform2.t.setY(cameraXform2.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3);  // -
                }
                compileOutput();
            }
        });
    }
    boolean flag = false;
    public ArrayList<Sphere> Deuterons = new ArrayList();

    private void handleKeyboard(Scene scene, Stage stage) {
        final boolean moveCamera = true;

        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            double translateStep = 1.5;
            double rotateStep = 1;

            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    case Z: // CTRL+X closes window
                        if (event.isControlDown()) {
                            // Close down window
                            stage.close();
                        }
                        break;
                    case O:
                        if (event.isControlDown()) {
                            textFieldStage.setTitle("My New Stage Title");
                            textFieldStage.setScene(new Scene(textFieldRoot, 450, 450));
                            textFieldStage.show();
                        }
                        break;
                    case S:
                        System.out.println("KEY PRESSED");
                        if (event.isControlDown()) {
                            FileChooser fileChooser = new FileChooser();
                            System.out.println("IN FILE CHOOSER");
                            fileChooser.setTitle("Save file");
                            fileChooser.getExtensionFilters().add(new ExtensionFilter("JSON Files", "*.json"));
                            File savedFile = fileChooser.showSaveDialog(primaryStage);
                            if (savedFile != null) {
                                try {
                                    String txt = exportPointsAsJSON();
                                    savedFile.createNewFile();
                                    PrintStream stream = new PrintStream(savedFile, "UTF-8");
                                    stream.println(txt);
                                    stream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    System.out.println("An ERROR occurred while saving the file!"
                                            + savedFile.toString());
                                    return;
                                }
                                System.out.println("File saved: " + savedFile.toString());
                            } else {
                                System.out.println("File save cancelled.");
                            }
                        }
                        break;
                    case PAGE_UP: // Get larger
                        scaleElectrons(1.1);
                        break;
                    case PAGE_DOWN: // Get smaller
                        scaleElectrons(0.9);
                        break;
                    case H: // Toggle wireframe visibility
                        toggleXform(componentGroup);
                        break;
                    case C: // Toggle electron visibility
                        toggleXform(chargeGroup);
                        break;
                    case A: // Toggle axis visibility
                        toggleXform(axisGroup);
                        break;
                    case X:
                        if (event.isControlDown()) {
                            FileChooser fileChooser = new FileChooser();
                            System.out.println("IN FILE CHOOSER");
                            fileChooser.setTitle("Save file");
                            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.XML"));
                            File savedFile = fileChooser.showSaveDialog(primaryStage);
                            if (savedFile != null) {
                                try {
                                    String txt = printShapesXML.printShapes(parts);
                                    savedFile.createNewFile();
                                    PrintStream stream = new PrintStream(savedFile, "UTF-8");
                                    stream.println(txt);
                                    stream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    System.out.println("An ERROR occurred while saving the file!"
                                            + savedFile.toString());
                                    return;
                                }
                                System.out.println("File saved: " + savedFile.toString());
                            } else {
                                System.out.println("File save cancelled.");
                            }
                        }
                        break;
                    case P: // Seed points
                        // Insert code for setting up particles here

                        final PhongMaterial deuteronMaterial = new PhongMaterial();

                        deuteronMaterial.setDiffuseColor(Color.PURPLE);
                        Sphere deuteron = new Sphere(1.0);

                        deuteron.setMaterial(deuteronMaterial);
                        
                        if (!flag) {
                            c = new Controller(points,annodeVoltage,cathodeVoltage);
                        }
                        Deuterons.add(deuteron);
                        
                        Point pos = new Point();
                        pos.x = 0;
                        pos.y = 18;
                        pos.z = 6;
                        c.addAtom(pos,Double.valueOf("3.34449439655E-27"));
                        
                        // addAtom code ends here
                        deuteron.setTranslateX(pos.x);
                        deuteron.setTranslateY(pos.y);
                        deuteron.setTranslateZ(pos.z);
                        world.getChildren().add(deuteron);
                        
                        if (!flag) {
                            flag = true;
                        } else {
                            break;
                        }
                        Runnable r = new Runnable() {
                            public void run() {
                                // Code for updating positions goes here
                                c.stepAllForeward(points, 0.01);
                                    System.out.println("Running once, size of Deuterons is " + Deuterons.size());
                                    for(int i = 0; i < Deuterons.size(); i++){
                                        Deuterons.get(i).setTranslateX(c.atoms[i].position.x);
                                        Deuterons.get(i).setTranslateY(c.atoms[i].position.y);
                                        Deuterons.get(i).setTranslateZ(c.atoms[i].position.z);
                                    }
                                
                            }
                        };

                        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                        executor.scheduleAtFixedRate(r, 0, 2, TimeUnit.MILLISECONDS);
                        break;
                    case F:
                        if (event.isControlDown()) {
                            if (!eFieldBuilt) {
                                buildEFieldStage(primaryStage, points);
                            } else {
                                System.out.println("Updating e-field!");
                                updateEField(points);
                            }
                        } else {
                            if (!eFieldBuilt) {
                                buildEFieldSlice();
                                eFieldBuilt = true;
                            }
                        }
                        break;

                    case Q:
                    case W:
                    case E:
                        double step = translateStep;
                        if (event.isControlDown()) {
                            step *= -1;
                        }
                        switch (event.getCode()) {
                            case Q:
                                eFieldSlice.setTranslateX(eFieldSlice.getTranslateX() + step);
                                break;
                            case W:
                                eFieldSlice.setTranslateY(eFieldSlice.getTranslateY() + step);
                                break;
                            case E:
                                eFieldSlice.setTranslateZ(eFieldSlice.getTranslateZ() + step);
                                break;
                        }
                        if (autoUpdate) {
                            updateEField(points);
                        }
                        break;
                    case R:
                    case T:
                    case Y:
                        double rotStep = rotateStep;
                        if (event.isControlDown()) {
                            rotStep *= -1;
                        }
                        switch (event.getCode()) {
                            case R:
                                eFieldTransforms[0].setAngle(eFieldTransforms[0].getAngle() + rotStep);
                                break;
                            case T:
                                eFieldTransforms[1].setAngle(eFieldTransforms[1].getAngle() + rotStep);
                                break;
                            case Y:
                                eFieldTransforms[2].setAngle(eFieldTransforms[2].getAngle() + rotStep);
                                break;
                        }
                        if (autoUpdate) {
                            updateEField(points);
                        }
                        break;
                    case U:
                        double scaleStep;

                        if (event.isControlDown()) {
                            scaleStep = 0.95;
                        } else {
                            scaleStep = 1.05;
                        }
                        eFieldSlice.setScaleX(eFieldSlice.getScaleX() * scaleStep);
                        eFieldSlice.setScaleY(eFieldSlice.getScaleX() * scaleStep);
                        eFieldSlice.setScaleZ(eFieldSlice.getScaleX() * scaleStep);
                        if (autoUpdate) {
                            updateEField(points);
                        }
                        break;
                        
                    case I:
                        if (!eFieldBuilt) {
                            break;
                        }
                        if (event.isControlDown()) {
                            eFieldSlice.setScaleX(1.0);
                            eFieldSlice.setScaleY(1.0);
                            eFieldSlice.setScaleZ(1.0);
                            eFieldSlice.setTranslateX(0);
                            eFieldSlice.setTranslateY(0);
                            eFieldSlice.setTranslateZ(0);
                            for (int i = 0; i < 3; i++) {
                                eFieldTransforms[i].setAngle(0.0);
                            }
                        } else {
                            eFieldSlice.setVisible(!eFieldSlice.visibleProperty().getValue());
                        }
                        if (autoUpdate) {
                            updateEField(points);
                        }
                        break;
                }
            }
        });
    }

    private String exportPointsAsJSON() {
        JSONArray arr = new JSONArray();
        for (Point p : points) {
            JSONObject obj = new JSONObject();
            obj.put("x", p.x);
            obj.put("y", p.y);
            obj.put("z", p.z);
            obj.put("charge", p.charge);
            arr.put(obj);
        }
        return arr.toString();
    }

    private void toggleXform(Xform g) {
        g.setVisible(!g.visibleProperty().get());
    }

    public void scaleElectrons(double scale) {
        for (int i = 0; i < chargeGroup.getChildren().size(); i++) {
            double revScale = chargeGroup.getChildren().get(i).getScaleX() * scale;
            revScale = Math.min(revScale, 40*electronRadius);
            revScale = Math.max(revScale, electronRadius/5);
            
            chargeGroup.getChildren().get(i).setScaleX(revScale);
            chargeGroup.getChildren().get(i).setScaleY(revScale);
            chargeGroup.getChildren().get(i).setScaleZ(revScale);

        }
    }

    public void buildEFieldSlice() {

        output.put("E-Field Slice Width", new TextLogicContainer() {@Override
        public double calc() {return sliceWidth;}});
        
        output.put("E-Field Slice Width", new TextLogicContainer() {@Override
        public double calc() {return sliceWidth;}});
        
        final PhongMaterial planeMaterial = new PhongMaterial();
        planeMaterial.setDiffuseColor(new Color(0.5, 0.5, 0.5, 0.5));

        eFieldSlice = new Box(sliceWidth, sliceHeight, 0.025);
        eFieldSlice.setMaterial(planeMaterial);

        Rotate rx = new Rotate();
        rx.setAxis(Rotate.X_AXIS);
        Rotate ry = new Rotate();
        ry.setAxis(Rotate.Y_AXIS);
        Rotate rz = new Rotate();
        rz.setAxis(Rotate.Z_AXIS);

        eFieldTransforms = new Rotate[3];
        eFieldTransforms[0] = rx;
        eFieldTransforms[1] = ry;
        eFieldTransforms[2] = rz;

        eFieldSlice.getTransforms().addAll(eFieldTransforms);

        output.put("E-Field Slice Translation X", new TextLogicContainer() {@Override
        public double calc() {System.out.println("Updated efield translation to " + eFieldSlice.getTranslateX()); return eFieldSlice.getTranslateX();}});
        output.put("E-Field Slice Translation Y", new TextLogicContainer() {@Override
        public double calc() {return eFieldSlice.getTranslateY();}});
        output.put("E-Field Slice Translation Z", new TextLogicContainer() {@Override
        public double calc() {return eFieldSlice.getTranslateZ();}});

        output.put("E-Field Slice Rotation X", new TextLogicContainer() {@Override
        public double calc() {return rx.getPivotX();}});
        output.put("E-Field Slice Rotation Y", new TextLogicContainer() {@Override
        public double calc() {return ry.getPivotY();}});
        output.put("E-Field Slice Rotation Z", new TextLogicContainer() {@Override
        public double calc() {return rz.getPivotZ();}});
        output.put("Current E-field Value", new TextLogicContainer() {@Override
        public String getText() {return eFieldString;}});
        
        world.getChildren().add(eFieldSlice);
    }

    public void compileOutput() {
        Iterator it = output.entrySet().iterator();
        String textOutput = "";
        while (it.hasNext()) {
            Map.Entry<String, TextLogicContainer> pair = (Map.Entry) it.next();
            String val = pair.getValue().getText();
            System.out.println("Key is " + pair.getKey() + ", val is " + val);
            textOutput += pair.getKey() + ": " + val + "\n";
        }
        consoleDump.setText(textOutput);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        int pointCount = 1000;
        int optimizations = 20;
        
        double annodeVoltage = 0;
        double cathodeVoltage = -500;

        parts = new ArrayList<>();

        String jsonPath = "Circles.json";
        byte[] encoded = Files.readAllBytes(Paths.get(jsonPath));

        JSONArray pieceArr = new JSONArray(new String(encoded, Charset.defaultCharset()));
        for (int i = 0; i < pieceArr.length(); i++) {
            JSONObject infoObj = pieceArr.getJSONObject(i);
            if (infoObj.getString("type").equals("wire")) {
                Wire w = new Wire(infoObj.toString());
                parts.addAll(w.getAsGridComponents());
            } else if (infoObj.getString("type").equals("stl")) {
                StlMeshImporter imp = new StlMeshImporter();
                try {
                    System.out.println(infoObj.getString("filename"));
                    imp.read(new File(infoObj.getString("filename")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                double scaleFactor = infoObj.getDouble("scalefactor");
                JSONObject translationObj = infoObj.getJSONObject("positionadj");

                Point translation = new Point (
                        translationObj.getDouble("x"),
                        translationObj.getDouble("y"),
                        translationObj.getDouble("z"));
                
                boolean flip_yz = infoObj.getBoolean("flip_yz");
                boolean reflect = infoObj.getBoolean("flip_vertical");

                TriangleMesh mesh = imp.getImport();
                imp.close();
                float[] fA = null;
                fA = mesh.getPoints().toArray(fA);

                int[] iA = null;
                iA = mesh.getFaces().toArray(iA);

                for (int k = 0; k < iA.length; k += 6) {
                    Point p1 = new Point(fA[iA[k] * 3], fA[iA[k] * 3 + 1], fA[iA[k] * 3 + 2]);
                    Point p2 = new Point(fA[iA[k + 2] * 3], fA[iA[k + 2] * 3 + 1], fA[iA[k + 2] * 3 + 2]);
                    Point p3 = new Point(fA[iA[k + 4] * 3], fA[iA[k + 4] * 3 + 1], fA[iA[k + 4] * 3 + 2]);
                    Point[] verts = {p1, p2, p3};

                    for (Point p : verts) { // Apply transformations
                        if (flip_yz) {
                            double oldy = p.y;
                            p.y = p.z;
                            p.z = oldy;
                        }
                        if (reflect) {
                            p.y *= -1;
                        }
                        p.scale(scaleFactor);
                        p.sum(translation);
                    }

                    Triangle t = new Triangle(verts, infoObj.getInt("charge"));
                    parts.add(t);
                }
            }
        }
        long start = System.nanoTime();
        points = PointDistributer.shakeUpPoints(parts, pointCount, optimizations);
        
        EField.setkQ(annodeVoltage, cathodeVoltage, points);
        markedPoints = new ArrayList<>();

        double posAvgPotential = StatsGen.avgPotential(points, 1);
        double negAvgPotential = StatsGen.avgPotential(points, -1);

        Point q = new Point();
        q.x = 0.0;
        q.y = 0.0;
        q.z = 0.0;
  
        EField.setkQ(annodeVoltage, cathodeVoltage, points);

        output.put("Points", new TextLogicContainer() {@Override
        public double calc() {System.out.println("Updated point count"); return points.length;}});
        output.put("E-Field Slice Rotation Z", new TextLogicContainer() {@Override
        public double calc() {return parts.size();}});
        output.put("Optimizations", new TextLogicContainer() {@Override
        public double calc() {return optimizations;}});
        
        output.put("Avg. potential of pos. points", new TextLogicContainer() {@Override
        public double calc() {return posAvgPotential;}});
        output.put("Avg. potential of neg. points", new TextLogicContainer() {@Override
        public double calc() {return negAvgPotential;}});

        Point[] referencePoints = {};


        buildCamera();
        buildElectrons(points);
        buildAxes();
        buildReferencePoints(referencePoints);
        buildScene();
        buildStage(primaryStage);
        
        buildEFieldSlice();
        buildTextWindow(primaryStage);
        buildEFieldStage(primaryStage, points);
        
        Scene scene = new Scene(root, 1024, 768, true);
        scene.setFill(Color.GREY);
        handleMouse(scene);
        handleKeyboard(scene, primaryStage);

        primaryStage.setTitle("Fusor Electric Field Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();
        scene.setCamera(camera);
    }

     // Java main for when running without JavaFX launcher
    public static void main(String[] args) {
        launch(args);
    }
}
