package com.example.cian.cianmeasureapp;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Delayed;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;



public class Main2Activity extends AppCompatActivity implements Node.OnTapListener, Scene.OnUpdateListener, View.OnClickListener {
    private static final String TAG = Main2Activity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;


    DatabaseReference reff;
    Measurement test;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1001;
    private static final int REQUEST_CODE_SPEECH_INPUT2 = 1002;
    private static final int REQUEST_CODE_SPEECH_INPUT3 = 1003;
    private static final int REQUEST_CODE_SPEECH_INPUT4 = 1004;
    private static final int Selected = 100;


    private SpeechRecognizer mySpeechRecognizer;
    private TextToSpeech myTTS;
    private ArFragment arFragment;
    private AnchorNode lastAnchorNode;
    private TextView txtDistance;
    Button btnClear; //Button that when clicked removes existing nodes and resets to initial state
    ModelRenderable cubeRenderable;
    ArrayList<Float> listOfArrays1 = new ArrayList<>(); //Co-Ordinates of first anchor stored here
    ArrayList<Float> listOfArrays2 = new ArrayList<>(); //Co-Ordinates of second anchor stored here
    Vector3 point1, point2;
    double currentDistance;
    private String userlocation, userDescription, userReview, userRating;
    Button btnUpload, btnSnap;
    TextView YourUrlImage;
    FirebaseStorage storage;
    StorageReference storageRef, imageRef;
    ProgressDialog progressDialog;
    UploadTask uploadTask;
    Uri uriImage;




    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ux);
        storage= FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        //YourUrlImage = (TextView) findViewById(R.id.myUrl);

        btnUpload = findViewById(R.id.upload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChooseImage();
                upload();
            }
            private void ChooseImage() {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, Selected);
            }
        });
        txtDistance = findViewById(R.id.txtLength);

        btnClear = findViewById(R.id.clear);
        btnClear.setOnClickListener(v -> onClear());

        reff=FirebaseDatabase.getInstance().getReference().child("Test");

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);


        test=new Measurement();

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

                    if (cubeRenderable == null)
                    {
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
                                    double d = getDistanceMeters(listOfArrays1, listOfArrays2); //calculate distance between nodes
                                    txtDistance.setText("Distance: " + String.valueOf(round(d, 3)) +"m"); //Display distance between nodes
                                    currentDistance=d;
                                }
                                else
                                    {
                                    listOfArrays1.clear(); //empty first list of poses
                                    listOfArrays1.addAll(listOfArrays2); //store second list of poses in first list of poses
                                    listOfArrays2.clear(); //empty second list of poses
                                    listOfArrays2.add(pose.tx()); //Store x component of pose's translation
                                    listOfArrays2.add(pose.ty()); //Store y component of pose's translation
                                    listOfArrays2.add(pose.tz()); //Store z component of pose's translation
                                    double d = getDistanceMeters(listOfArrays1, listOfArrays2); //calculate distance between nodes
                                    txtDistance.setText("Distance: " + String.valueOf(round(d,3)) + "m"); //Display the distance
                                    currentDistance=d;
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
                                    MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(255, 0, 0)) //Create a rectangular prism and use difference vector to extend the necessary length
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
                                    speakDistance();

                            }
                        }

                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                        mySpeechRecognizer.startListening(intent);

                    }
                });
        findViewById(R.id.btnSnap).setOnClickListener(this);


    }

    /*CAPTURE CODE*/

    /*
    generateFilename Method:
    A unique file name is needed for each picture we take.
    The filename for the picture is generated using the standard pictures directory.
    Each image name is based on the time, so they won't overwrite each other.
    */
    private String generateFilename() {
        String date = new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + date + "_screenshot.jpg";
    }


    /*
    saveBitmapToDisk Method: Writes out the bitmap to the file.
    */
    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();

        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }



    /*
    takePhoto Method:
    Uses the PixelCopy API to capture a screenshot of the ArSceneView.
    It is asynchronous since it actually happens between frames.
    When the listener is called, the bitmap is saved to the disk, and then a snackbar is shown with an intent to open the image in the Pictures application.
    */
    private void takePhoto() {
        final String filename = generateFilename();
        ArSceneView view = arFragment.getArSceneView();

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.

        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(Main2Activity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Screenshot done!!!", Snackbar.LENGTH_LONG);
                snackbar.setAction("Save screenshot", v -> {
                    File photoFile = new File(filename);

                    Uri photoURI = FileProvider.getUriForFile(Main2Activity.this,
                            Main2Activity.this.getPackageName() + ".cianmchale.app.provider",
                            photoFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                    intent.setDataAndType(photoURI, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);

                });
                snackbar.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
                snackbar.show();
            } else {
                Toast toast = Toast.makeText(Main2Activity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    public void onClick(View v) {

        takePhoto();

    }


    public static double round(double value, int places)
    {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    private void speakDistance()
    {
        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(myTTS.getEngines().size() == 0)
                {
                    Toast.makeText(Main2Activity.this, "There is no TTS engine on your device", Toast.LENGTH_LONG).show();
                    finish();
                }
                else
                {
                    myTTS.setLanguage(Locale.ENGLISH);
                    speak("The distance is " + round(currentDistance, 2) +"metres");
                }
            }
        });
    }

    private void speak(String message)
    {
        if(Build.VERSION.SDK_INT >= 21)
        {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else
        {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
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


    private void upload()
    {
            //Toast.makeText(this, "Cannot upload without measurement being taken", Toast.LENGTH_SHORT).show();

            location();
            String location = userlocation;
            double distance = currentDistance;

            //description();
            String description = userDescription;
            //review();
            String review = userReview;
            //rating();
            String rating = userRating;

            test.setLocation(location);
            String x = Double.toString(distance);
            test.setDistance(x);
            test.setDescription(description);
            test.setReview(review);
            test.setRating(rating);
            reff.push().setValue(test);

    }

    private void location() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Where is being measured?");
        startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);

    }

    private void description() {
        Intent intent1 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent1.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent1.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());
        intent1.putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the measurement");
        startActivityForResult(intent1, REQUEST_CODE_SPEECH_INPUT2);

    }

    private void review() {
        Intent intent2 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent2.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());
        intent2.putExtra(RecognizerIntent.EXTRA_PROMPT, "Give a review");
        startActivityForResult(intent2, REQUEST_CODE_SPEECH_INPUT3);
    }

    private void rating() {
        Intent intent3 = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent3.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent3.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());
        intent3.putExtra(RecognizerIntent.EXTRA_PROMPT, "Give a rating out of 10");
        startActivityForResult(intent3, REQUEST_CODE_SPEECH_INPUT4);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Selected: {
                if (resultCode == RESULT_OK && null != data) {
                    uriImage = data.getData();
                    UploadFoto();
                }
                break;

            }

            case REQUEST_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    userlocation = result.get(0);
                    description();
                }
                break;

            }
            case REQUEST_CODE_SPEECH_INPUT2: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result1 = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    userDescription = result1.get(0);
                    review();
                }
                break;
            }
            case REQUEST_CODE_SPEECH_INPUT3: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result2 = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    userReview = result2.get(0);
                    rating();
                }
                break;
            }
            case REQUEST_CODE_SPEECH_INPUT4: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result3 = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    userRating = result3.get(0);
                }
                break;
            }

        }

    }

    public void UploadFoto() {

        imageRef = storageRef.child("picture/" +new Date().toString());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMax(100);
        progressDialog.setMessage("Uploading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();
        progressDialog.setCancelable(false);

        uploadTask = imageRef.putFile(uriImage);

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                progressDialog.incrementProgressBy((int) progress);
            }
        });

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception exception) {
                Toast.makeText(getApplicationContext(),"Failed!",Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!urlTask.isSuccessful());

                Uri downloadUrl = urlTask.getResult();
                String urlIMAGE = downloadUrl.toString();
                progressDialog.dismiss();

                //YourUrlImage.setText("Your Download URl : "+urlIMAGE);

            }
        });
    }

    private double getDistanceMeters(ArrayList<Float> list1, ArrayList<Float> list2) //method to get distance between poses
    {

        double dx = list1.get(0) - list2.get(0); //distance between poses at x co-ordinate
        double dy = list1.get(1) - list2.get(1); //distance between poses at y co-ordinate
        double dz = list1.get(2) - list2.get(2); //distance between poses at z co-ordinate
        return (double) Math.sqrt(dx * dx + dy * dy + dz * dz);
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