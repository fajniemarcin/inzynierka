package pl.projekt.projekt;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

@SuppressWarnings("MissingPermission")
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
{
    private SupportMapFragment mWidokMapy;
    private GoogleMap mMapa;
    private View mDekoracjaWidoku;
    private double mSzerokosc;
    private double mDlugosc;

    private static final int WYMAGANE_POZWOLENIA = 123;

    public static final String MENU = "MENU";
    public static final String IDENTYFIKACJA_ULIC = "IDENTYFIKACJA_ULIC";
    public static final String WYJSCIE = "WYJSCI0E";

    private String[] mElementyMenu;
    private DrawerLayout mWysuwaneMenu;
    private ListView mListaElementow;

    private double mWysokoscTelefonu;

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

        setContentView(R.layout.activity_maps);

        mSzerokosc = getIntent().getExtras().getDouble(WhereActivity.SZEROKOSC);
        mDlugosc = getIntent().getExtras().getDouble(WhereActivity.DLUGOSC);

        mWidokMapy = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        mWidokMapy.getMapAsync(this);

        initializeNavigationDrawer();

        mDekoracjaWidoku = getWindow().getDecorView();
    }

    private void initializeNavigationDrawer()
    {
        mElementyMenu = getResources().getStringArray(R.array.zawartosc_menu_pokaz_na_mapie);
        mWysuwaneMenu = (DrawerLayout) findViewById(R.id.wysuwane_menu_pokaz_na_mapie);
        mListaElementow = (ListView) findViewById(R.id.lista_elementow_pokaz_na_mapie);

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
                setMapType();
                break;
            case 1:
                backToMenu();
                break;
            case 2:
                currentLocationActivity();
                break;
            case 3:
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if(hasCameraPermissions())
                        streetIdentificationActivity();
                    else
                        requestCameraPermissions();
                }
                break;
            case 4:
                exitApplication();
                break;
            default:
                break;
        }

//        mListaElementow.setItemChecked(pozycja, true);
//        mListaElementow.setSelection(pozycja);
        mWysuwaneMenu.closeDrawer(mListaElementow);
    }

    private void setMapType()
    {
        String[] typyMap = getResources().getStringArray(R.array.zawartosc_dialogu_typ_mapy);
        int zaznaczonyTyp = mMapa.getMapType() - 1; //bo:   NORMAL_ID = 1 a w onClick 0
//                                                          SATELLITE_ID = 2 a w onClick 1
//                                                          TERRAIN_ID = 3 a w onClick 2
//                                                          HYBRID_ID = 4 a w onClick 3

        AlertDialog.Builder kontruktorDialoguTypMapy = new AlertDialog.Builder(this);

        kontruktorDialoguTypMapy
                .setTitle(R.string.tytul_mapa)
                .setNegativeButton(R.string.anuluj, null)
                .setSingleChoiceItems(typyMap, zaznaczonyTyp, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int dialogItem)
                    {
                        switch(dialogItem)
                        {
                            case 0:
                                mMapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                                break;
                            case 1:
                                mMapa.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                break;
                            case 2:
                                mMapa.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                                break;
                            case 3:
                                mMapa.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                break;
                            default:
                                break;
                        }
                        dialogInterface.dismiss();
                    }
                })
                .create()
                .show();
    }

    private void backToMenu()
    {
        Intent whereActivity = new Intent(this, WhereActivity.class);
        whereActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        whereActivity.putExtra(MENU, true);
        startActivity(whereActivity);
        finish();
    }

    private void currentLocationActivity()
    {
        setResult(RESULT_CANCELED);
        finish();
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
                            Toast.makeText(MapsActivity.this, R.string.alert_brak, Toast.LENGTH_SHORT).show();
                        else
                        {
                            mWysokoscTelefonu = Double.parseDouble(wprowadzonaWartosc);

                            if(mWysokoscTelefonu > 0)
                            {
                                streetActivity();
                                dialogWysokoscTelefonu.dismiss();
                            }

                            else
                                Toast.makeText(MapsActivity.this, R.string.alert_zero, Toast.LENGTH_SHORT).show();
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

    //wraca do WhereActivity, zamyka ją, i przechodzi do CameraActivity
    private void streetActivity() //identyfikacja ulic
    {
        Intent whereActivity = new Intent(this, WhereActivity.class);
        whereActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        whereActivity.putExtra(IDENTYFIKACJA_ULIC, true);
        whereActivity.putExtra(GeoMathematics.WYSOKOSC_TELEFONU, mWysokoscTelefonu);
        startActivity(whereActivity);
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
    public void onMapReady(GoogleMap googleMap)
    {
        mMapa = googleMap;

        mMapa.setMyLocationEnabled(true);

        LatLng wspolrzedne = new LatLng(mSzerokosc, mDlugosc);
        MarkerOptions opcjeWskaznika = new MarkerOptions();
        opcjeWskaznika
                .position(wspolrzedne)
                .title(mSzerokosc + " / " + mDlugosc);

        mMapa.addMarker(opcjeWskaznika);
        mMapa.moveCamera(CameraUpdateFactory.newLatLngZoom(wspolrzedne, 15));
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
            streetIdentificationActivity();
        }
        else
        {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                //użytkownik nie przyznał wszystkich pozwoleń
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                    Toast.makeText(this, R.string.kamera_brak_uprawnien, Toast.LENGTH_SHORT).show();

                    //jeśli użytkownik zaznaczył, aby nie był pytany ponownie o pozwolenia
                else
                    Toast.makeText(this, R.string.brak_uprawnien, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean hasCameraPermissions()
    {
        int pozwolenieID;

        String[] pozwolenia = new String[]
                {Manifest.permission.CAMERA};

        for(String pozwolenie : pozwolenia)
        {
            pozwolenieID = checkCallingOrSelfPermission(pozwolenie);

            if(pozwolenieID != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        return true;
    }

    private void requestCameraPermissions()
    {
        String[] pozwolenia = new String[]
                {Manifest.permission.CAMERA};

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(pozwolenia, WYMAGANE_POZWOLENIA);
    }
}
