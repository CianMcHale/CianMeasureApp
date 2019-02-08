package com.example.cian.cianmeasureapp;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.ScaleController;
import com.google.ar.sceneform.ux.TransformableNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements Node.OnTapListener, Scene.OnUpdateListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private AnchorNode lastAnchorNode;
    private TextView txtDistance;
    Button btnClear; //Button that when clicked removes existing nodes and resets to initial state
    ModelRenderable cubeRenderable;
    ArrayList<Float> listOfArrays1 = new ArrayList<>(); //Co-Ordinates of first anchor stored here
    ArrayList<Float> listOfArrays2 = new ArrayList<>(); //Co-Ordinates of second anchor stored here
    private float time; //Will be used to store current time.
    Vector3 point1, point2;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        txtDistance = findViewById(R.id.txtLength);

        btnClear = findViewById(R.id.clear);
        btnClear.setOnClickListener(v -> onClear());

        MaterialFactory.makeTransparentWithColor(this, new Color(0F, 0F, 244F))
                .thenAccept(
                        material -> {
                            Vector3 vector3 = new Vector3(0.01f, 0.01f, 0.01f);
                            cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                            cubeRenderable.setShadowCaster(false);
                            cubeRenderable.setShadowReceiver(false);
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    Frame frame = arFragment.getArSceneView().getArFrame(); //Capture the state
                    Point center = getScreenCenter(arFragment); //Track point that the centre of the screen is looking at
                    List<HitResult> result = frame.hitTest(center.x, center.y); //Perform raycast from device in the centre direction

                    if (cubeRenderable == null) {
                        return;
                    }

                    for (HitResult hit : result) //Check if any plane was hit
                    {
                        Trackable trackable = hit.getTrackable(); //Creates an anchor if a plane was hit
                        if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) //Hits are sorted by depth. Only consider closest hit on a plane
                        {
                            if (lastAnchorNode == null) {
                                Anchor anchor = hit.createAnchor(); //Create anchor based at intersection between raycast and plane
                                AnchorNode anchorNode = new AnchorNode(anchor); //Position node in world space based of above anchor
                                anchorNode.setParent(arFragment.getArSceneView().getScene());

                                Pose pose = anchor.getPose(); //return pose of the anchor created
                                if (listOfArrays1.isEmpty()) {
                                    listOfArrays1.add(pose.tx()); //Store x component of pose's translation
                                    listOfArrays1.add(pose.ty()); //Store y component of pose's translation
                                    listOfArrays1.add(pose.tz()); //Store z component of pose's translation
                                }
                                TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                                transformableNode.setParent(anchorNode);
                                transformableNode.setRenderable(cubeRenderable);
                                transformableNode.select();
                                lastAnchorNode = anchorNode;
                            } else {
                                Anchor anchor = hit.createAnchor();
                                AnchorNode anchorNode = new AnchorNode(anchor);
                                anchorNode.setParent(arFragment.getArSceneView().getScene());

                                Pose pose = anchor.getPose();


                                if (listOfArrays2.isEmpty()) {
                                    listOfArrays2.add(pose.tx()); //Store x component of pose's translation
                                    listOfArrays2.add(pose.ty()); //Store y component of pose's translation
                                    listOfArrays2.add(pose.tz()); //Store z component of pose's translation
                                    float d = getDistanceMeters(listOfArrays1, listOfArrays2); //calculate distance between nodes
                                    txtDistance.setText("Distance: " + String.valueOf(d)); //Display distance between nodes
                                } else {
                                    listOfArrays1.clear(); //empty first list of poses
                                    listOfArrays1.addAll(listOfArrays2); //store second list of poses in first list of poses
                                    listOfArrays2.clear(); //empty second list of poses
                                    listOfArrays2.add(pose.tx()); //Store x component of pose's translation
                                    listOfArrays2.add(pose.ty()); //Store y component of pose's translation
                                    listOfArrays2.add(pose.tz()); //Store z component of pose's translation
                                    float d = getDistanceMeters(listOfArrays1, listOfArrays2); //calculate distance between nodes
                                    txtDistance.setText("Distance: " + String.valueOf(d)); //Display the distance
                                }

                                TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                                transformableNode.setParent(anchorNode);
                                transformableNode.setRenderable(cubeRenderable);
                                transformableNode.select();

                                Vector3 point1, point2;
                                point1 = lastAnchorNode.getWorldPosition();
                                point2 = anchorNode.getWorldPosition();

                                final Vector3 difference = Vector3.subtract(point1, point2); //Find vector extending between two nodes  and define a look rotation in terms of this vector
                                final Vector3 directionFromTopToBottom = difference.normalized();
                                final Quaternion rotationFromAToB =
                                        Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
                                MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(0, 255, 244)) //Create a rectangular prism and use difference vector to extend the necessary length
                                        .thenAccept(
                                                material -> {
                                                    ModelRenderable model = ShapeFactory.makeCube(
                                                            new Vector3(.01f, .01f, difference.length()),
                                                            Vector3.zero(), material);
                                                    Node node = new Node();
                                                    node.setParent(anchorNode);
                                                    node.setRenderable(model);
                                                    node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f)); //Set the world rotation of the node to the rotation calculated earlier
                                                    node.setWorldRotation(rotationFromAToB); //Set world position to midpoint between given points
                                                }
                                        );
                                lastAnchorNode = anchorNode;
                            }
                        }
                    }
                });
    }

    private void onClear() //Method used to remove nodes from scene and reset to initial state
    {
        List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for (Node node : children)
        {
            if (node instanceof AnchorNode)
            {
                if (((AnchorNode) node).getAnchor() != null)
                {
                    ((AnchorNode) node).getAnchor().detach();
                }
            }
            if (!(node instanceof Camera) && !(node instanceof Sun))
            {
                node.setParent(null);
            }
        }
        listOfArrays1.clear();
        listOfArrays2.clear();
        lastAnchorNode = null;
        point1 = null;
        point2 = null;
        txtDistance.setText("");
    }

    private float getDistanceMeters(ArrayList<Float> list1, ArrayList<Float> list2) //method to get distance between poses
    {

        float dx = list1.get(0) - list2.get(0); //distance between poses at x co-ordinate
        float dy = list1.get(1) - list2.get(1); //distance between poses at y co-ordinate
        float dz = list1.get(2) - list2.get(2); //distance between poses at z co-ordinate
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent)
    {
        Node node = hitTestResult.getNode();
        Box box = (Box) node.getRenderable().getCollisionShape();
        assert box != null;
        Vector3 renderableSize = box.getSize();
        Vector3 transformableNodeScale = node.getWorldScale();
        Vector3 finalSize =
                new Vector3(
                        renderableSize.x * transformableNodeScale.x,
                        renderableSize.y * transformableNodeScale.y,
                        renderableSize.z * transformableNodeScale.z);
        txtDistance.setText("Height: " + String.valueOf(finalSize.y));
        Log.e("FinalSize: ", String.valueOf(finalSize.x + " " + finalSize.y + " " + finalSize.z));
        //Toast.makeText(this, "Final Size is " + String.valueOf(finalSize.x + " " + finalSize.y + " " + finalSize.z), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpdate(FrameTime frameTime) //Frametime provides info for the current frame. Onupdate called once per frame before scene is updated
    {
        Frame frame = arFragment.getArSceneView().getArFrame();
    }

    private Point getScreenCenter(ArFragment arFragment) //method to get centre of screen
    {

        if(this.arFragment == null || this.arFragment.getView() == null)
        {
            return new android.graphics.Point(0,0);
        }

        int a = this.arFragment.getView().getWidth()/2;
        int b = this.arFragment.getView().getHeight()/2;
        return new android.graphics.Point(a, b);
    }

}