package pl.projekt.projekt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MenuActivity extends Activity
{
    private Button mGdzieJestemMenuButton;
    private Button mIdentyfikacjaUlicMenuButton;
    private Button mWyjscieMenuButton;

    private boolean mCzyIdentyfikacjaUlic;

    private double mWysokoscTelefonu;

    private static final int WYMAGANE_POZWOLENIA = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
        {
            if(getIntent().getBooleanExtra(WhereActivity.WYJSCIE, false)
                    || getIntent().getBooleanExtra(CameraActivity.WYJSCIE, false)
                    || getIntent().getBooleanExtra(MapsActivity.WYJSCIE, false))
            {
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_menu);

        initializeComponents();
    }

    private void initializeComponents()
    {


        mGdzieJestemMenuButton = (Button) findViewById(R.id.gdzieJestemMenuButton);
        mIdentyfikacjaUlicMenuButton = (Button) findViewById(R.id.identyfikacjaUlicMenuButton);
        mWyjscieMenuButton = (Button) findViewById(R.id.wyjscieMenuButton);

        mGdzieJestemMenuButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mCzyIdentyfikacjaUlic = false;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if(hasLocationPermission())
                        currentLocationActivity();
                    else
                        requestLocationPermission();
                }
            }
        });

        mIdentyfikacjaUlicMenuButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mCzyIdentyfikacjaUlic = true;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if(hasCameraAndLocationPermission())
                        streetIdentificationActivity();
                    else
                        requestCameraAndLocationPermission();
                }
            }
        });

        mWyjscieMenuButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                exitApplication();
            }
        });
    }

    private void currentLocationActivity() //aktualna pozycja
    {
        Intent whereActivity = new Intent(this, WhereActivity.class);
        startActivityForResult(whereActivity, 0);
    }

    private void streetIdentificationActivity()
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

        //zachowanie dialogu gdy użytkownik nie poda żadnej liczby
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
                            Toast.makeText(MenuActivity.this, R.string.alert_brak, Toast.LENGTH_SHORT).show();
                        else
                        {
                            mWysokoscTelefonu = Double.parseDouble(wprowadzonaWartosc);

                            if(mWysokoscTelefonu > 0)
                            {
                                streetActivity();
                                dialogWysokoscTelefonu.dismiss();
                            }
                            else
                                Toast.makeText(MenuActivity.this, R.string.alert_zero, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        dialogWysokoscTelefonu.show();

        //wycentrowanie tytułu
//        TextView tytul = (TextView) dialogWysokoscTelefonu.findViewById(getResources().getIdentifier("alertTitle", "id", "android"));
//        tytul.setGravity(Gravity.CENTER);     //nie działa

        //wycentrowanie wiadomości
        TextView wiadomosc = (TextView) dialogWysokoscTelefonu.findViewById(android.R.id.message);
        wiadomosc.setGravity(Gravity.CENTER);
    }

    private void streetActivity() //identyfikacja ulic
    {
        Intent cameraActivity = new Intent(this, CameraActivity.class);
        cameraActivity.putExtra(GeoMathematics.WYSOKOSC_TELEFONU, mWysokoscTelefonu);
        startActivityForResult(cameraActivity, 1);
    }

    private void exitApplication() //wyjście
    {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean pozwolone = true;

        switch(requestCode)
        {
            //jesli uzytkownik na wszystko pozwoli
            case WYMAGANE_POZWOLENIA:
                for(int pozwolenieID : grantResults)
                {
                    pozwolone = pozwolone && (pozwolenieID == PackageManager.PERMISSION_GRANTED);
                }
                break;
            default:
                //jesi uzytkownik nie przyzna wszystkich pozwolen
                pozwolone = false;
                break;
        }

        if(pozwolone)
        {
            //uzytkownik przyznał wszystkie pozwolenia
            if(!mCzyIdentyfikacjaUlic)
                currentLocationActivity();
            else
                streetIdentificationActivity();

        }
        else
        {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                //użytkownik nie przyznał wszystkich pozwoleń
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                {
                    Toast.makeText(this, R.string.kamera_brak_uprawnien, Toast.LENGTH_SHORT).show();

                    if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                            || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION))
                        Toast.makeText(this, R.string.lokalizacja_brak_uprawnien, Toast.LENGTH_SHORT).show();
                }

                else if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                        || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION))
                    Toast.makeText(this, R.string.lokalizacja_brak_uprawnien, Toast.LENGTH_SHORT).show();

                    //jeśli użytkownik zaznaczył, aby nie był pytany ponownie o pozwolenia
                else
                    Toast.makeText(this, R.string.brak_uprawnien, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean hasLocationPermission()
    {
        int pozwolenieID;

        String[] pozwolenia = new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};

        for(String pozwolenie : pozwolenia)
        {
            pozwolenieID = checkCallingOrSelfPermission(pozwolenie);

            if(pozwolenieID != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        return true;
    }

    private void requestLocationPermission()
    {
        String[] pozwolenia = new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(pozwolenia, WYMAGANE_POZWOLENIA);
    }

    private boolean hasCameraAndLocationPermission()
    {
        int pozwolenieID;

        String[] pozwolenia = new String[]
                {Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};

        for(String pozwolenie : pozwolenia)
        {
            pozwolenieID = checkCallingOrSelfPermission(pozwolenie);

            if(pozwolenieID != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        return true;
    }

    private void requestCameraAndLocationPermission()
    {
        String[] pozwolenia = new String[]
                {Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(pozwolenia, WYMAGANE_POZWOLENIA);
    }
}
