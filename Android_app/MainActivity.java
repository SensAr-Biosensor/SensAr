package citedef.ProyConicet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private JavaCameraView mOpenCvCameraView;
    private static final String    TAG                 = "OCVSample::Activity";

    private Mat                    mRgba, orig;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;
    private RelativeLayout            mFrameLayout;
    private android.support.v7.widget.GridLayout            buttonLayout;
    private TextView text1;
    private ImageView imageview;
    private ImageButton button;
    private Button Abrir, button_aceptar, button_cancelar;
    private float iniX, iniY, finX, finY;

    static final int REQUEST_FILE = 1;
    static final int REQUEST_SEND = 2;

    private SurfaceHolder mSurfaceHolder;

    GPSTracker mGPS;

    LocationManager locationManager;

    static final int PERMISSION_ACCESS_COARSE_LOCATION = 1;
    static final int PERMISSION_CAMERA = 2;
    static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 3;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

    };

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.v("TAG", "OpenCVloaded");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        /*if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialize success");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "OpenCV initialize failed");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        }*/
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {

            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (OpenCVLoader.initDebug()) {
                Log.i(TAG, "OpenCV initialize success");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } else {
                Log.i(TAG, "OpenCV initialize failed");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
            }
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mLoaderCallback);
        }
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            mGPS = new GPSTracker(this);
            if (!mGPS.canGetLocation()) {
                mGPS.showSettingsAlert();
            }


        }

    }

    /*static {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialize success");
        } else {
            Log.i(TAG, "OpenCV initialize failed");
        }
    }*/


    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
public String modo;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        modo = getIntent().getStringExtra("EXTRA");

        try {
            if (modo.equals("FOTO")) {
                qrfind = true;
            }
            else{
                msg("Centre el Codigo QR");
            }
        }
        catch (Exception e){
            msg("Centre el Codigo QR");
            modo = " ";
            Log.d("Exception", "intent");
        }

         // First check android version

//Check if permission is already granted
//thisActivity is your activity. (e.g.: MainActivity.this)
        boolean perm = true;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {

            if(!hasPermissions(this, PERMISSIONS)){
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }

            /*if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                // Give first an explanation, if needed.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    if (OpenCVLoader.initDebug()) {
                        Log.i(TAG, "OpenCV initialize success");
                        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                    } else {
                        Log.i(TAG, "OpenCV initialize failed");
                        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
                    }
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(FullscreenActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            PERMISSION_CAMERA);
                    }



            }



            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Give first an explanation, if needed.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(FullscreenActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_ACCESS_COARSE_LOCATION);
                }
            }

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Give first an explanation, if needed.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(FullscreenActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_WRITE_EXTERNAL_STORAGE);
                }
            }*/

        }


        mFrameLayout = (RelativeLayout) findViewById(R.id.frameLayout);
        buttonLayout = (android.support.v7.widget.GridLayout) findViewById(R.id.vertical_layout_button);
        text1 = (TextView) findViewById(R.id.textView);

        button = (ImageButton) findViewById(R.id.flash);
        button_aceptar = (Button) findViewById(R.id.button2);
        button_cancelar = (Button) findViewById(R.id.button3);
        Abrir = (Button) findViewById(R.id.Imagen);
        imageview = (ImageView) findViewById(R.id.ImageView);
                mFrameLayout.removeView(imageview);
        mFrameLayout.removeView(text1);

        mFrameLayout.addView(text1);
        mFrameLayout.removeView(button);
        mFrameLayout.removeView(buttonLayout);
        //mFrameLayout.removeView(button_aceptar);
        // mFrameLayout.removeView(button_cancelar);
        mFrameLayout.addView(button);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.disableFpsMeter();



    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    mGPS = new GPSTracker(this);
                    if (!mGPS.canGetLocation()) {
                        mGPS.showSettingsAlert();
                    }

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    finish();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
            case PERMISSION_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (OpenCVLoader.initDebug()) {
                        Log.i(TAG, "OpenCV initialize success");
                        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                    } else {
                        Log.i(TAG, "OpenCV initialize failed");
                        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
                    }

                } else {
                    finish();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }

            case PERMISSION_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    finish();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
/*
    @Override
    public void onLocationChanged(Location location) {

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
    }*/


    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    public void onCameraViewStarted(int width, int height) {
    }
    public void onCameraViewStopped() {
    }

    /*private Mat mHSV;
    private Mat green_hue_range;
    private Mat cropped;
    private boolean first = false;*/

    private boolean qrfind = false;
    private boolean find = false;
    private double[] valor = new double[]{0,0,0,0,0,0,0,0,0};


    private String[] estadoCuadrados = {"ESPERANDO","ESPERANDO","ESPERANDO"};
    int Area = 0;
    Rect [] area = new Rect[3];
    Mat [] roi = new Mat[3];
    /*Funcion para analizar los cuadrados*/
    public void analizeFrame(Mat analize, int cuadrado){
        //Mat mHSV = new Mat();

        //Log.v("Cuadrados:", Integer.toString(cuadrado) + estadoCuadrados[cuadrado]);
        Mat edge = new Mat();
        Mat mGray = new Mat();

        Mat ventGray = new Mat();

        Imgproc.cvtColor(analize, mGray, Imgproc.COLOR_RGBA2GRAY, 2);
        Imgproc.GaussianBlur(mGray, mGray, new Size(15,15) , 0, 0);
        Imgproc.Canny(mGray, mGray, 20.0, 100.0);

        //Imgproc.dilate(mGray, mGray, new Mat(), new Point(-1, 1), 1);

        //Core.bitwise_not(mGray, mGray);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();


        Imgproc.findContours(mGray.clone()   , contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        int cuadrados = 0;
        for(int i=0; i<contours.size(); i++) {
            Rect rect = Imgproc.boundingRect(contours.get(i));
            double k = (rect.height + 0.0) / rect.width;
            //Log.v("Area1", String.valueOf(rect.area()));

            try {
                if (0.9 < k && k < 1.2 && rect.area() > 5000 && cuadrados < 1) {
                    cuadrados++;
                    //Log.v("Area2", String.valueOf(rect.area()));
                    Area = (int)rect.area();
                    area[cuadrado] = rect;
                    Mat sbmat = analize.submat(rect);
                    roi[cuadrado] = analize.submat(rect);
                    /*List<Mat> colors = new ArrayList<>();
                    Mat r, g, b, cyan;
                    Core.split(sbmat, colors);

                    b = colors.get(2);
                    g = colors.get(1);
                    r = colors.get(0);
                    Core.bitwise_not(r, r);
                    valor[cuadrado + 3] = Core.mean(r).val[0] / (Core.mean(g).val[0] + Core.mean(b).val[0]);
                    valor[cuadrado] = Core.mean(r).val[0];*/

                    /*Nuevo Algortmo*/
                    /*
                    Imgproc.cvtColor(sbmat, ventGray, Imgproc.COLOR_RGBA2GRAY);
                    int cant_azul = 0;
                    for (int x = 0; x<rect.height; x ++){
                        for (int y=0; y<rect.width; y++){
                            double[] gris = ventGray.get(x,y);
                            double[] rojo = r.get(x,y);
                            double[] azul = b.get(x,y);
                            if ((gris[0]/255)>0.3) {
                                if ((rojo[0] / 255) < 0.8) {
                                    try {
                                        double ratio = rojo[0] / azul[0];
                                        if (ratio < 0.8)
                                            cant_azul++;
                                    } catch (Exception e) {
                                        Log.e("Exception:", "calculo ratio azul");
                                    }

                                }
                            }
                        }
                    }
                    valor[cuadrado + 6] = (float)cant_azul;
                    */

                    /*Esto me va a servir para el nuevo algoritmo*/

                    //int pix = Core.countNonZero(sbmat);
                    //cuadrados ++;
                    //Imgproc.drawContours(analize, , i, new Scalar(0, 255, 0), 3);

                    if (estadoCuadrados[cuadrado].equals("VACIO")) {
                        Imgproc.rectangle(analize, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 255, 0), 3);
                        estadoCuadrados[cuadrado] = "ESPERANDO";
                    } else {
                        Imgproc.rectangle(analize, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 3);
                        estadoCuadrados[cuadrado] = "LLENO";
                    }
                }

            }catch (ArrayIndexOutOfBoundsException e){
                e.printStackTrace();
                Log.v("Exception", "Index");
            }
        }
        if (cuadrados==0)
            estadoCuadrados[cuadrado] = "VACIO";
    }

    private String Modo = "QR";

    String file_procces, file_orig;

    public void foto(View v){
        if (!find) {
            if ((estadoCuadrados[0].equals("LLENO")) && (estadoCuadrados[1].equals("LLENO")) && (estadoCuadrados[2].equals("LLENO"))) { //&& (qrfind == true)){
                //final Mat copia = new Mat();
                //mRgba.copyTo(copia);
                find = true;
                qrfind = false;

                estadoCuadrados[0] = "VACIO";
                estadoCuadrados[1] = "VACIO";
                estadoCuadrados[2] = "VACIO";

                //mOpenCvCameraView.takePicture("test.jpeg");
                File path_procces = new File(this.getFilesDir(), "image.jpg");
                File path_orig = new File(this.getFilesDir(), "image_orig.jpg");

                //File path = new File(Environment.getExternalStorageDirectory() + "/Imagenes/");
                //path.mkdirs();
                //File file = new File(path, "image.png");
                //File file2 = new File(path, "image_orig.png");
                //file_procces = file.toString();
                //file_orig = file2.toString();

                file_procces = path_procces.toString();
                file_orig = path_orig.toString();
                Mat rgba = new Mat();
                Imgproc.cvtColor(copia, rgba, Imgproc.COLOR_BGRA2RGBA);

                boolean ret = Imgcodecs.imwrite(file_procces, rgba);
                if (ret)
                    Log.v("Grabo", "OK");

                else
                    Log.v("Grabo", "Fail");

                ret = Imgcodecs.imwrite(file_orig, orig);
                if (ret)
                    Log.v("Grabo", "OK");

                else
                    Log.v("Grabo", "Fail");


                flash_status = false;
                button.setImageResource(R.drawable.icons8_flash_off_50);

                mFrameLayout.removeAllViews();
                mFrameLayout.addView(imageview);
                mFrameLayout.addView(buttonLayout);

                //mFrameLayout.addView(button_aceptar);
                //mFrameLayout.addView(button_cancelar);

                final Bitmap newImage = Bitmap.createBitmap(copia.width(), copia.height(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(copia, newImage);
                imageview.setImageBitmap(newImage);
                //showMessage();


            }
            else{
                msg("Vuelva a intentar cuando observe tres cuadrados verdes");
            }

        }
    }

    private Mat copia;
    /*Aca analizo cada cuadro voy a hacer uno nuevo para cambiar el modo de funcionamiento*/
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if (!find) {
            if ((estadoCuadrados[0].equals("LLENO")) && (estadoCuadrados[1].equals("LLENO")) && (estadoCuadrados[2].equals("LLENO"))) { //&& (qrfind == true)){
                //final Mat copia = new Mat();
                copia = mRgba.clone();
                /*Antes cuando detectaba los cuadrados tomaba la imagen, ahora hay que hacer click entonces va a la funcion foto
                */

                //mRgba.copyTo(copia);
                /*find = true;
                qrfind = false;

                estadoCuadrados[0] = "VACIO";
                estadoCuadrados[1] = "VACIO";
                estadoCuadrados[2] = "VACIO";

                //mOpenCvCameraView.takePicture("test.jpeg");
                File path_procces = new File(this.getFilesDir(), "image.jpg");
                File path_orig = new File(this.getFilesDir(), "image_orig.jpg");

                //File path = new File(Environment.getExternalStorageDirectory() + "/Imagenes/");
                //path.mkdirs();
                //File file = new File(path, "image.png");
                //File file2 = new File(path, "image_orig.png");
                //file_procces = file.toString();
                //file_orig = file2.toString();

                file_procces = path_procces.toString();
                file_orig = path_orig.toString();
                Mat rgba = new Mat();
                Imgproc.cvtColor(copia, rgba, Imgproc.COLOR_BGRA2RGBA);

                boolean ret = Imgcodecs.imwrite(file_procces, rgba);
                if (ret)
                    Log.v("Grabo", "OK");

                else
                    Log.v("Grabo", "Fail");

                ret = Imgcodecs.imwrite(file_orig, orig);
                if (ret)
                    Log.v("Grabo", "OK");

                else
                    Log.v("Grabo", "Fail");

                this.runOnUiThread(new Runnable() {
                    public void run() {
                        mFrameLayout.removeAllViews();
                        mFrameLayout.addView(buttonLayout);
                        mFrameLayout.addView(imageview);
                        //mFrameLayout.addView(button_aceptar);
                        //mFrameLayout.addView(button_cancelar);

                        final Bitmap newImage = Bitmap.createBitmap(copia.width(), copia.height(), Bitmap.Config.ARGB_8888);

                        Utils.matToBitmap(copia, newImage);
                        imageview.setImageBitmap(newImage);
                        //showMessage();
                    }
                });
*/
            }

        }

        mRgba = inputFrame.rgba();

        try {
            mRgba.copyTo(orig);
        }
        catch (Exception e){
            Log.v("Exception", "Copy");
            orig = new Mat();
            mRgba.copyTo(orig);
        }

        Imgproc.cvtColor(orig, orig, Imgproc.COLOR_BGRA2RGBA);
        /*Mat mGray = new Mat();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY, 2);
        Imgproc.GaussianBlur(mGray, mGray, new Size(15,15) , 0, 0);
        Imgproc.Canny(mGray, mGray, 0.0, 40.0);

        Imgproc.dilate(mGray, mGray, new Mat(), new Point(-1, 1), 1);*/
        //mOpenCvCameraView.setFlash(true);

        int rows = mRgba.rows();
        int cols = mRgba.cols();

        /*Revisar*/
        /*Primero Hago QR*/
        if (Modo.equals("QR")){
            Mat qr;

            int size_x = cols/4;
            int size_y = rows/4;
            qr = mRgba.submat(rows/2-size_x, rows/2 + size_x, cols/2-size_y, cols / 2 + size_y);

            qr.convertTo(qr, -1, 2.0);

            if (qr_status) {
                try {
                    zxing(qr);

                } catch (ChecksumException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (FormatException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            else{
                qrfind = true;
                sResult = "No QR";
            }

            if (qrfind) {
                if (modo.equals("FOTO") || modo.equals("NORMAL")) {
                    Modo = "Analisis";
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Cambiar controles
                            text1.setText(R.string.Analisis);
                            msg("Haga Foco en los cuadrados de Analisis");
                        }
                    });

                }
                else {
                    createNotificationChannel();
                    startAlarm(true, false);
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Cambiar controles
                            mOpenCvCameraView.disableView();
                            //mFrameLayout.removeAllViews();
                            //mFrameLayout.addView(imageview);
                            timeMessage();
                        }
                    });
                    }
            }
        }
        else if(Modo.equals("Analisis")) {

            int cuadrado_y = rows / 2;
            int cuadrado_x = cols / 8;
            int size_x = cols / 6;
            int size_y = rows / 4;

            Mat analize1, analize2, analize3;

            mRgba.convertTo(mRgba, -1, 0.5);
            //analize = mRgba.submat(40, rows - 40, cols / 2 + 20, cols - 20);
            analize1 = mRgba.submat(cuadrado_y - size_y/2, cuadrado_y + size_y/2, 2 * cuadrado_x - size_x/2, 2 * cuadrado_x + size_x/2);
            analize1.convertTo(analize1, -1, 2.0);

            analize2 = mRgba.submat( cuadrado_y - size_y/2, cuadrado_y + size_y/2, 4 * cuadrado_x - size_x/2, 4 * cuadrado_x + size_x/2);
            analize2.convertTo(analize2, -1, 2.0);

            analize3 = mRgba.submat(cuadrado_y - size_y/2, cuadrado_y + size_y/2, 6 * cuadrado_x - size_x/2, 6 * cuadrado_x + size_x/2);
            analize3.convertTo(analize3, -1, 2.0);


            if (!find) {
                analizeFrame(analize1, 0);
                analizeFrame(analize2, 1);
                analizeFrame(analize3, 2);


            }
        }

        Log.v("Latitude", String.valueOf(mGPS.getLatitude()));
        Log.v("Longitud", String.valueOf(mGPS.getLongitude()));

        return mRgba;

    }



    ResultPoint points[];
    String sResult = "No QR";
    public void zxing(Mat qr) throws ChecksumException, FormatException {

        Bitmap bMap = Bitmap.createBitmap(qr.width(), qr.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(qr, bMap);
        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Reader reader = new QRCodeMultiReader();

        try {

            Result result = reader.decode(bitmap);
            sResult = result.getText();
            points = result.getResultPoints();
            Log.d("QRCode ", sResult);
            Point p0 = new Point((int)points[0].getX(),(int)points[0].getY());
            Point p1 = new Point((int)points[1].getX(),(int)points[1].getY());
            Point p2 = new Point((int)points[2].getX(),(int)points[2].getY());
            //Point p3 = new Point((int)points[3].getX(),(int)points[3].getY());

            Imgproc.line(qr,p0,p1,new Scalar(0xFF0000),4);
            Imgproc.line(qr,p1,p2,new Scalar(0xFF0000),4);
            //Imgproc.line(mRgba,p2,p3,new Scalar(0xFF0000),4);
            //Imgproc.line(mRgba,p3,p0,new Scalar(0xFF0000),4);
            //Imgproc.rectangle(mRgba, p1, p2, new Scalar(0xFF0000), 4);
            qrfind = true;

        }
        catch (NotFoundException e) {
            Log.d("QrCode ", "Code Not Found");
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }


    }

    Boolean detectionQRRunning = false;
    public class QRTask extends AsyncTask<Mat, Void, Boolean> {

        // run detection method in background thread
        // takes in parameter in the .execute(Mat mGray) call on the class that is created
        @Override
        protected Boolean doInBackground(Mat ...params) {
            detectionQRRunning = true;

            try {
                zxing(params[0]);

            } catch (ChecksumException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }// this integer is passes to the onPostExecute method below
            return true;
        }

        // result Integer is passed here after
        // this method is run on maing UI thread
        @Override
        protected void onPostExecute(Boolean result) {


            detectionQRRunning = false;

        }

    }

    public void showMessage() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Datos Obtenidos");

        /*Si el valor del standard cuandrante 3 es menor a 1,4 del cuadrante 1 (blanco) Mostrar Error, repita el ensayo. Podria poner "Algo salio mal. Repita la muestra"

Si es mayor a 1,4 salio bien la muestra, entonces comparar cuadrante 2 con cuadrante 3. Si el cuadrante 2 es mayor que el 3 que aprezca "El nivel de arsenico en la muestra supera al permitido." Si es menor "El nivel de Arsenico esta dentro del rango permitido."
 */


        if (valor[5] < 1.4*valor[3]){
            /*Error en la muestra*/
            alertDialog.setMessage("Algo salio mal, repita la muestra.");

            alertDialog.setNeutralButton("Repetir", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    find = false;
                    estadoCuadrados[0]="VACIO";estadoCuadrados[1]="VACIO";estadoCuadrados[2]="VACIO";
                    dialog.cancel();
                }
            });

        }

        else if(valor[4] > valor[5]){
            alertDialog.setMessage("El nivel de arsenico en la muestra supera al permitido.");
            find = false;
            estadoCuadrados[0]="VACIO";estadoCuadrados[1]="VACIO";estadoCuadrados[2]="VACIO";
            myDbHandler midb = new myDbHandler();
            SQLiteDatabase db = midb.opendb("sensar_database.db", MainActivity.this);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String id = sharedPreferences.getString(DatosMuestra.ID,"");

            ContentValues content = new ContentValues();
            content.put("LATITUD", String.valueOf(mGPS.getLatitude()));
            content.put("LONGITUD", String.valueOf(mGPS.getLongitude()));
            content.put("QR", sResult);
            content.put("[Valor Medio 1]",String.valueOf(valor[0]));
            content.put("[Valor Medio 2]",String.valueOf(valor[1]));
            content.put("[Valor Medio 3]",String.valueOf(valor[2]));
            /*Lo modifique para el nuevo algoritmo*/
            content.put("[Cuadrante 1]", String.valueOf(valor[3]));
            content.put("[Cuadrante 2]", String.valueOf(valor[4]));
            content.put("[Cuadrante 3]", String.valueOf(valor[5]));

            content.put("[Cant. Azul 1]", String.valueOf(valor[6]));
            content.put("[Cant. Azul 2]", String.valueOf(valor[7]));
            content.put("[Cant. Azul 3]", String.valueOf(valor[8]));
            content.put("PASO", "NO");
            content.put("ID", id);

            midb.updateRowbyId(db, getResources().getString(R.string.tabla_toma), content, id);
            db.close();

            alertDialog.setPositiveButton("OK",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    generar_archivo();
            }});
        }
        else{
            alertDialog.setMessage("El nivel de Arsenico esta dentro del rango permitido.");
            find = false;
            estadoCuadrados[0]="VACIO";estadoCuadrados[1]="VACIO";estadoCuadrados[2]="VACIO";
            myDbHandler midb = new myDbHandler();
            SQLiteDatabase db = midb.opendb("sensar_database.db", MainActivity.this);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String id = sharedPreferences.getString(DatosToma.ID,"");

            ContentValues content = new ContentValues();
            content.put("LATITUD", String.valueOf(mGPS.getLatitude()));
            content.put("LONGITUD", String.valueOf(mGPS.getLongitude()));
            content.put("QR", sResult);
            content.put("[Valor Medio 1]",String.valueOf(valor[0]));
            content.put("[Valor Medio 2]",String.valueOf(valor[1]));
            content.put("[Valor Medio 3]",String.valueOf(valor[2]));
            /*Cambio para probar nuevo algoritmo*/
            content.put("[Cuadrante 1]", String.valueOf(valor[3]));
            content.put("[Cuadrante 2]", String.valueOf(valor[4]));
            content.put("[Cuadrante 3]", String.valueOf(valor[5]));

            content.put("[Cant. Azul 1]", String.valueOf(valor[6]));
            content.put("[Cant. Azul 2]", String.valueOf(valor[7]));
            content.put("[Cant. Azul 3]", String.valueOf(valor[8]));

            content.put("PASO", "SI");
            content.put("ID", id);

            midb.updateRowbyId(db, getResources().getString(R.string.tabla_toma), content, id);
            db.close();

            alertDialog.setPositiveButton("OK",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    generar_archivo();
                }});
        }
        
        alertDialog.setCancelable(false);
        // Showing Alert Message
        alertDialog.show();
    }

    public void askMessage() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Desea Volver a tomar una muestra");

        // Setting Dialog Message
        alertDialog
                .setMessage("Quiere tomar una nueva muestra?");

        // On pressing Settings button
        alertDialog.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, DatosMuestra.class);
//Clear all activities and start new task
                startActivity(intent);
                finish();
                dialog.cancel();
            }
        });


        // on pressing cancel button
        alertDialog.setNegativeButton("Salir",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, CloseActivity.class);
//Clear all activities and start new task
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        dialog.cancel();
                    }
                });

        alertDialog.setCancelable(false);
        // Showing Alert Message
        alertDialog.show();
    }

    public void timeMessage() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Qr Aceptado");

        // Setting Dialog Message
        alertDialog
                .setMessage("En unos momentos le llegara una notificacion para tomar la muestra");

        // on pressing cancel button
        alertDialog.setPositiveButton("Aceptar",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, CloseActivity.class);
//Clear all activities and start new task
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        dialog.cancel();
                    }
                });

        alertDialog.setCancelable(false);
        // Showing Alert Message
        alertDialog.show();
    }



    private void showMessageManual(double value, double value2){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Datos Obtenidos");


        // Setting Dialog Message
        alertDialog
                .setMessage("QR: " + sResult + '\n' + "GPS: " + mGPS.getLatitude() + '\n' + "     "
                        +mGPS.getLongitude() + '\n' + "Valor 1: "+ String.valueOf(value)+'/'+ String.valueOf(value2));

        // On pressing Settings button
        alertDialog.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                final Bitmap newImage = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(image, newImage);
                imageview.setImageBitmap(newImage);
                dialog.cancel();
            }
        });


        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final Bitmap newImage = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(image, newImage);
                        imageview.setImageBitmap(newImage);
                        dialog.cancel();
                    }
                });

        alertDialog.setCancelable(false);
        // Showing Alert Message
        alertDialog.show();
    }

    ProgressDialog progress;

    public void aceptar(View v){

        progress = ProgressDialog.show(this, "Procesando...", "Espere!!!");  //show a progress dialog
        progress.setCancelable(true);
        progress.setCanceledOnTouchOutside(false);

        for (int cuadrado = 0; cuadrado < 3; cuadrado ++) {

            Mat ventGray = new Mat();

            List<Mat> colors = new ArrayList<>();
            Mat r,b,g;
            Core.split(roi[cuadrado], colors);

            b = colors.get(2);
            g = colors.get(1);
            r = colors.get(0);

            /*Nuevo Algortmo*/

            Imgproc.cvtColor(roi[cuadrado], ventGray, Imgproc.COLOR_RGBA2GRAY);
            int cant_azul = 0;
            for (int x = 0; x<area[cuadrado].height; x ++){
                for (int y=0; y<area[cuadrado].width; y++){
                    double[] gris = ventGray.get(x,y);
                    double[] rojo = r.get(x,y);
                    double[] azul = b.get(x,y);
                    if ((gris[0]/255)>0.3) {
                        if ((rojo[0] / 255) < 0.8) {
                            try {
                                double ratio = rojo[0] / azul[0];
                                if (ratio < 0.8)
                                    cant_azul++;
                            } catch (Exception e) {
                                Log.e("Exception:", "calculo ratio azul");
                            }

                        }
                    }
                }
            }
            valor[cuadrado + 6] = (float)cant_azul;

            Log.v("Rojo sin invertir",String.valueOf(Core.mean(r).val[0]));
            Core.bitwise_not(r, r);

            Log.v("Rojo invertido",String.valueOf(Core.mean(r).val[0]));

            //Core.bitwise_not(normalThresholding(r),r);
            //Core.multiply(r,new Scalar(255),r);
            valor[cuadrado + 3] = Core.mean(r).val[0] / (Core.mean(g).val[0] + Core.mean(b).val[0]);
            valor[cuadrado] = Core.mean(r).val[0];

        }

        try{
            progress.dismiss();}
        catch (Exception e){
            Log.v("Exception", "In progress Dialog");
        }

        mFrameLayout.removeAllViews();
        mFrameLayout.addView(mOpenCvCameraView);
        mFrameLayout.addView(button);
        mFrameLayout.addView(Abrir);
        mFrameLayout.addView(text1);
        showMessage();
    }

    public void cancelar(View v){
        find = false;
        estadoCuadrados[0]="VACIO";estadoCuadrados[1]="VACIO";estadoCuadrados[2]="VACIO";
        mFrameLayout.removeAllViews();
        mFrameLayout.addView(mOpenCvCameraView);
        mFrameLayout.addView(button);
        mFrameLayout.addView(Abrir);
        mFrameLayout.addView(text1);
    }

    boolean flash_status = false;
    public void flash(View v){
        flash_status = !flash_status;
        mOpenCvCameraView.setFlash(flash_status);

        if (!flash_status)
            button.setImageResource(R.drawable.icons8_flash_off_50);
        else
            button.setImageResource(R.drawable.icons8_flash_on_50);
    }
    boolean qr_status = true;

    public void qr(View v){
        qr_status = !qr_status;
        Button qrbutton = (Button) findViewById(R.id.qr);
        if (qr_status){
            qrbutton.setText("QR");
        }
        else{
            qrbutton.setText("No Qr");
        }
    }

    /*onActivityResult retorna cuando busque la imagen para analizar*/
    boolean abrir = false;
    Mat image;
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FILE) {
            Log.v("recibi", "form");
            //Uri file = data.getData();

            try {
                final Uri imageUri = data.getData();
                mFrameLayout.removeAllViews();

                mFrameLayout.addView(imageview);
                mFrameLayout.addView(Abrir);

                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                DisplayMetrics metrics = new DisplayMetrics();

                float aspectRatio = selectedImage.getWidth() /
                        (float) selectedImage.getHeight();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int height = metrics.heightPixels;
                int width = Math.round(height * aspectRatio);

                final Bitmap newselectedImage = Bitmap.createScaledBitmap(
                        selectedImage, width, height, false);

                image = new Mat(newselectedImage.getHeight(), newselectedImage.getWidth(), CvType.CV_8UC4);

                Utils.bitmapToMat(newselectedImage, image);
                abrir = true;
                final Bitmap newImage = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(image, newImage);
                imageview.setImageBitmap(newImage);


            } catch (Exception e) {
                e.printStackTrace();
            }
            /*File tempFile = new File(this.getFilesDir().getAbsolutePath(), "temp_image.jpg");

            //Copy URI contents into temporary file.
            try {
                if (tempFile.exists()) {
                    tempFile.delete();
                    tempFile.createNewFile();
                } else {
                    tempFile.createNewFile();
                }
                this.copyInputStreamToFile(this.getContentResolver().openInputStream(data.getData()), tempFile);
            } catch (IOException e) {
                //Log Error
            }
            image = Imgcodecs.imread(tempFile.getAbsolutePath());
            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2BGRA);*/

        }
        else if (requestCode == REQUEST_SEND){
            askMessage();
        }

    }

    public void openFile(View v){

        imageview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction()==MotionEvent.ACTION_DOWN) {

                    Log.e(TAG,"Down");
                    iniX = event.getX(0);
                    iniY = event.getY(0);

                    return true;
                }

                if (event.getAction()==MotionEvent.ACTION_MOVE){

                    Log.e(TAG,"Move");
                    finX = event.getX(0);
                    finY = event.getY(0);
                    if (finX < iniX)
                        finX = iniX + 1;
                    if (finY < iniY)
                        finY = iniY + 1;
                    final Bitmap newImage = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
                    Mat img2 = image.clone();
                    Imgproc.rectangle(img2, new Point(iniX,iniY), new Point(finX,finY), new Scalar(0,120,0),3);
                    Utils.matToBitmap(img2, newImage);
                    imageview.setImageBitmap(newImage);
                    return true;

                }
                if (event.getAction()==MotionEvent.ACTION_UP){
                    
                    finX = event.getX(0);
                    finY = event.getY(0);
                    if (finX < iniX)
                        finX = iniX + 1;
                    else if (finX == iniX) {
                        Log.v(TAG, "SIN CUADRADO");
                        showMessageManual(-1, -1);
                        return true;
                    }
                    if (finY < iniY)
                        finY = iniY + 1;
                    else if(finY == iniY){
                        Log.v(TAG, "SIN CUADRADO");
                        showMessageManual(-1, -1);
                        return true;
                    }
                    final Bitmap newImage = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
                    Mat img2 = image.clone();
                    Imgproc.rectangle(img2, new Point(iniX,iniY), new Point(finX,finY), new Scalar(0,120,0), 3);
                    Utils.matToBitmap(img2, newImage);
                    imageview.setImageBitmap(newImage);
                    Log.e(TAG,"Up");

                    Mat sbmat = image.submat((int)iniY, (int)finY, (int)iniX, (int)finX);
                    List<Mat> colors = new ArrayList<>();
                    Mat r, g, b, cyan;
                    Core.split(sbmat, colors);

                    b = colors.get(2);
                    g = colors.get(1);
                    r = colors.get(0);
                    Core.bitwise_not(r, r);
                    try {
                        double value = Core.mean(r).val[0] / (Core.mean(g).val[0] + Core.mean(b).val[0]);
                        double value2 = Core.mean(r).val[0];
                        showMessageManual(value, value2);
                    }
                    catch (Exception e){
                        Log.v(TAG, "DIVISION POR CERO");
                        showMessageManual(-1, -1);
                    }

                    return true;
                }


                return false;
            }
        });


        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath());
        intent.setDataAndType(uri, ("*/*"));
        startActivityForResult(intent, MainActivity.REQUEST_FILE);
    }

    String date;
    public void generar_archivo()
    {

        try{
            //String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            DateFormat df = new SimpleDateFormat("ddMMyy-HHmm");
            date = df.format(Calendar.getInstance().getTime());

            //String fileName = "Formulario"+ date +".csv";
            String fileName_excel = "Informe"+ date +".xls";

            //File f = new File(this.getFilesDir(), fileName);
            File f_excel = new File(this.getFilesDir(), fileName_excel);

            //String filePath = f.getPath();


            WorkbookSettings wbSettings = new WorkbookSettings();
            wbSettings.setLocale(new Locale("en", "EN"));
            WritableWorkbook workbook;

            //CSVWriter writer;
            //FileWriter mFileWriter;

            if(f_excel.exists()){
                f_excel.delete();
                //mFileWriter = new FileWriter(filePath);
                workbook = Workbook.createWorkbook(f_excel, wbSettings);
                //writer = new CSVWriter(mFileWriter);

            }
            else {
                //mFileWriter = new FileWriter(filePath);
                //writer = new CSVWriter(mFileWriter);
                workbook = Workbook.createWorkbook(f_excel, wbSettings);
            }

            WritableSheet sheet = workbook.createSheet("Tabla_Sensar", 0);
            //mFileWriter.write("sep=,\r\n");

            myDbHandler midb = new myDbHandler();
            SQLiteDatabase db = midb.opendb("sensar_database.db", this);



            Cursor c_encabezado = midb.seeRows(db, getResources().getString(R.string.tabla_presonales));

            String []titulo_encabezado = c_encabezado.getColumnNames();

            WritableCellFormat cellFormat = new WritableCellFormat();
            try {
                cellFormat.setLocked(false);
            }catch (Exception e){
                msg("ERROR EXCEL");
            }
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String user = sharedPreferences.getString(Login.NOMBRE,"");


            if (c_encabezado.moveToFirst()){
                while(!c_encabezado.isAfterLast()){
                    if (c_encabezado.getString(1).equals(user)){


                        try{
                            int fila = 0, cont = 0;
                            for (String dato:titulo_encabezado){
                                //Cont esta para no mostrar id y user*/
                                if (cont >1) {
                                    sheet.addCell(new Label(0, fila, dato, cellFormat));
                                    sheet.addCell(new Label(1, fila, c_encabezado.getString(c_encabezado.getColumnIndex(dato)), cellFormat));
                                    fila++;
                                }
                                cont ++;
                            }

                        }
                        catch (Exception e){
                            msg("ERROR EXCEL");
                        }

                    }
                    c_encabezado.moveToNext();
                }
            }
            c_encabezado.close();

            Cursor c = midb.seeRows(db, getResources().getString(R.string.tabla_toma));
            //writer.writeNext(columnames);
            String columnames[] = c.getColumnNames();
            try{
                int col = 0;
                for (String dato:columnames){
                    sheet.addCell(new Label(col, titulo_encabezado.length-2 + 1, dato));
                    col++;
                }

            }
            catch (Exception e){
                msg("ERROR EXCEL");
            }

            String [] text_to_file = new String[columnames.length];
            int fila = titulo_encabezado.length-2 + 2;

            if (c.moveToLast()) {

                //while (!c.isBeforeFirst()) {

                    for (int i = 0; i< columnames.length; i++)
                    {
                        String text;
                        text = c.getString(i);
                        if (text == null)
                            text = " ";
                        text_to_file[i] = text;
                    }
                    try{
                        int col = 0;
                        for (String dato:text_to_file){
                            sheet.addCell(new Label(col, fila, dato, cellFormat));
                            col++;
                        }

                    }
                    catch (Exception e){
                        msg("ERROR EXCEL");
                    }
                    //fila ++;
                    //writer.writeNext(text_to_file);
                    //c.moveToPrevious();
                //}
            }
/*
            WritableImage image_procces = new WritableImage(
                    15, 4,   //column, row
                    2, 2,   //width, height in terms of number of cells
                    new File(file_procces)); //Supports only 'png' images

            WritableImage image_orig = new WritableImage(
                    18, 4,   //column, row
                    2, 2,   //width, height in terms of number of cells
                    new File(file_orig)); //Supports only 'png' images


            sheet.addImage(image_procces);
            sheet.addImage(image_orig);*/
            sheet.getSettings().setProtected(true);
            //writer.close();
            workbook.write();
            try {
                workbook.close();
            }catch (Exception e){
                msg("ERROR EXCEL");
            }
            c.close();
            db.close();
            msg("Archivo Creado");
            sharemessage();
        }
        catch (IOException e){
            msg("NO SE PUDO CREAR EL ARCHIVO");
        }
    }

    private void sharemessage()
    {
        /*String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "Formulario.csv";
        String filePath = baseDir + File.separator + fileName;
        final File f = new File(filePath );*/

        String fileName = "Informe"+ date +".xls";
        //final File f = new File(this.getFilesDir(), fileName);
        //String filePath = f.getPath();

        final ArrayList<Uri> listUris = new ArrayList<Uri>();

        //final Uri uri = Uri.parse("content://com.autopyme.proyconicet/" + fileName);
        //final Uri uri2 = Uri.parse("content://com.autopyme.proyconicet/" + "image.png");

        listUris.add(Uri.parse("content://com.autopyme.proyconicet/" + fileName));
        listUris.add(Uri.parse("content://com.autopyme.proyconicet/" + "image.jpg"));
        listUris.add(Uri.parse("content://com.autopyme.proyconicet/" + "image_orig.jpg"));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Desea Compartir el archivo?");

        builder.setPositiveButton("Compartir", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
                i.setData(Uri.parse("mailto:"));
                i.putExtra(Intent.EXTRA_EMAIL, new String[] { "juangasulla@gmail.com" });
                i.putExtra(Intent.EXTRA_SUBJECT, "Resultados SENSAR");
                //i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                //i.setDataAndType(uri, "text/plain");
                //i.putExtra(Intent.EXTRA_STREAM, uri);
                i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, listUris);

                i.setType("text/plain");


                startActivityForResult(Intent.createChooser(i,"Compartir"), MainActivity.REQUEST_SEND );

            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // what ever you want to do with No option.
                askMessage();
            }
        });

        AlertDialog alertDialogShare = builder.create();


        alertDialogShare.show();

        //askMessage();
    }


    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }


    private void startNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AlarmNotificationReceiver.CHANNEL_ID);

        Intent myIntent = new Intent(this, MostrarDatos.class);
        myIntent.putExtra("EXTRA", "Login");
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                myIntent,
                FLAG_ONE_SHOT );

        builder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.icono_vacio)
                .setContentTitle("SenSar")
                .setContentIntent(pendingIntent)
                .setContentText("Pantalla Login")
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setContentInfo("Info");

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(2,builder.build());

    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SenSar";
            String description = "SenSar";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(AlarmNotificationReceiver.CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startAlarm(boolean isNotification, boolean isRepeat) {
        AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent myIntent;
        PendingIntent pendingIntent;

        // SET TIME HERE
        Calendar calendar= Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,17);
        calendar.set(Calendar.MINUTE,58);


        myIntent = new Intent(MainActivity.this,AlarmNotificationReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(this,0,myIntent,0);

        final long tiempo = 1000*60*1;
        if(!isRepeat)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() +tiempo, pendingIntent);
            else
                manager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+tiempo,pendingIntent);
        else
            manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY,pendingIntent);
    }


}
