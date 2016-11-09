package pl.projekt.projekt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2
{
    private static final String TAG = "OpenCV";
    public static final String WYJSCIE = "WYJSCIE";

    static
    {
        if(!OpenCVLoader.initDebug())
            Log.d(TAG, "OpenCv niezaładowane");
        else
            Log.d(TAG, "OpenCV załadowane");
    }

    private View mDekoracjaWidoku;
    private JavaCameraView mPodgladKamery;
    private Mat mRGBa;
    private GeoMathematicsPOM mWskazanaUlica;
    private CacheManaging mZarzadzaniePamieciaPodreczna;

    private String[] mElementyMenu;
    private DrawerLayout mWysuwaneMenu;
    private ListView mListaElementow;

    private double mWysokoscTelefonu;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch(status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV załadowane pomyślnie");
                    mPodgladKamery.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
        {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }

        setContentView(R.layout.activity_camera);

        initializeCameraPreview();
        initializeNavigationDrawer();

        mWskazanaUlica = new GeoMathematicsPOM(CameraActivity.this);
        mZarzadzaniePamieciaPodreczna = new CacheManaging(CameraActivity.this);

        mDekoracjaWidoku = getWindow().getDecorView();
    }

    private void initializeCameraPreview()
    {
        mPodgladKamery = (JavaCameraView) findViewById(R.id.podgladKamery);

        mPodgladKamery.setVisibility(SurfaceView.VISIBLE);
        mPodgladKamery.setCvCameraViewListener(this);
    }

    private void initializeNavigationDrawer()
    {
        mElementyMenu = getResources().getStringArray(R.array.zawartosc_menu_identyfikacja_ulic);
        mWysuwaneMenu = (DrawerLayout) findViewById(R.id.wysuwane_menu_identyfikacja_ulic);
        mListaElementow = (ListView) findViewById(R.id.lista_elementow_identyfikacja_ulic);

        //ustawienie adaptera dla listy elementów
        mListaElementow.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_element, mElementyMenu));

        //ustawienie nasłuchiwacza kliknięcia elementu listy
        mListaElementow.setOnItemClickListener(new ListView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
            {
                selectElement(position);
            }
        });
    }

    private void selectElement(int pozycja)
    {
        switch(pozycja)
        {
            case 0:
                changePhoneHeight();
                break;
            case 1:
                backToMenu();
                break;
            case 2:
                currentLocationActivity();
                break;
            case 3:
                exitApplication();
                break;
            default:
                break;
        }

//        mListaElementow.setItemChecked(pozycja, true);
//        mListaElementow.setSelection(pozycja);
        mWysuwaneMenu.closeDrawer(mListaElementow);
    }

    private void changePhoneHeight()
    {
        LayoutInflater wypelniacz = getLayoutInflater();

        View widokDialogu = wypelniacz.inflate(R.layout.alert_dialog_edit_text, null);
        final EditText wysokoscTelefonuET = (EditText) widokDialogu.findViewById(R.id.wysokoscTelefonu);

        //wycentrowany tytul
        View widokTytulu = wypelniacz.inflate(R.layout.alert_dialog_title, null);
        TextView tytul = (TextView) widokTytulu.findViewById(R.id.tytul);

        AlertDialog.Builder kontruktorDialoguWysokoscTelefonu = new AlertDialog.Builder(this);

        kontruktorDialoguWysokoscTelefonu
                .setCustomTitle(tytul)
//                .setTitle(R.string.tytul) //nie moeżna wycentrować
                .setMessage(R.string.wiadomosc)
                .setView(widokDialogu)
                .setPositiveButton(R.string.zatwierdz, null)
                .setNegativeButton(R.string.anuluj, null);

        final AlertDialog dialogWysokoscTelefonu = kontruktorDialoguWysokoscTelefonu.create();

        //aby zachować dialog gdy użytkownik nie poda żadnej liczby
        dialogWysokoscTelefonu.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface dialogInterface)
            {
                Button zatwierdz = dialogWysokoscTelefonu.getButton(AlertDialog.BUTTON_POSITIVE);

                zatwierdz.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        String wprowadzonaWartosc = wysokoscTelefonuET.getText().toString();

                        if(wprowadzonaWartosc.equals(""))
                            Toast.makeText(CameraActivity.this, R.string.alert_brak, Toast.LENGTH_SHORT).show();
                        else
                        {
                            mWysokoscTelefonu = Double.parseDouble(wprowadzonaWartosc);

                            if(mWysokoscTelefonu > 0)
                            {
                                mWskazanaUlica.setPhoneHeihgt(mWysokoscTelefonu);
                                dialogWysokoscTelefonu.dismiss();
                            }
                            else
                                Toast.makeText(CameraActivity.this, R.string.alert_zero, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        dialogWysokoscTelefonu.show();

        //wycentrowanie wiadomości
        TextView wiadomosc = (TextView) dialogWysokoscTelefonu.findViewById(android.R.id.message);
        wiadomosc.setGravity(Gravity.CENTER);
    }

    private void backToMenu()
    {
//        mWskazanaUlica.stopThread();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void currentLocationActivity()
    {
        Intent whereActivity = new Intent(this, WhereActivity.class);
        startActivityForResult(whereActivity, 0);
        finish();
    }

    private void exitApplication()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            finishAffinity();
        else
        {
            Intent menuActivity = new Intent(this, MenuActivity.class);
            menuActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            menuActivity.putExtra(WYJSCIE, true);
            startActivity(menuActivity);
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus)
        {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                mDekoracjaWidoku.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if(mPodgladKamery != null)
            mPodgladKamery.disableView();

        mWskazanaUlica.pause();
        mZarzadzaniePamieciaPodreczna.pause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(!OpenCVLoader.initDebug())
        {
            Log.d(TAG, "OpenCV nie znalezione");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        }
        else
        {
            Log.d(TAG, "OpenCV znalezione");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        mWskazanaUlica.resume();
        mZarzadzaniePamieciaPodreczna.resume();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if(mPodgladKamery != null)
            mPodgladKamery.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        mRGBa = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped()
    {
        mRGBa.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        mRGBa = inputFrame.rgba();

        return mRGBa;
    }
}