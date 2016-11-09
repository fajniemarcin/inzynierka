package pl.projekt.projekt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

@SuppressWarnings({"MissingPermission", "WrongConstant"})
public class WhereActivity extends Activity implements LocationListener
{
    private TextView mDostawcaTV;
    private TextView mSzerokoscTV;
    private TextView mDlugoscTV;
    private TextView mDokladnoscTV;
    private TextView mAdresTV;
    private TextView mBladGPSTV;
    private Button mPokazNaMapieButton;

    private double mWysokoscTelefonu;

    private String mDostawca;
    private double mSzerokosc;
    private double mDlugosc;
    private int mWidocznoscDokladnosci;
    private float mDokladnosc;
    private int mUstawienieAdresu;
    private int mKolorAdresu;
    private String mAdres;
    private int mWidocznoscBleduGPS;
    private String mBladGPS;
    private int mWidocznoscPrzycisku;

    private LocationManager mManagerPolozenia;
    private Location mPolozenie;
    private Criteria mKryteria;
    private Geocoder mGeocoder;

    private final static String DOSTAWCA = "DOSTAWCA";
    public final static String SZEROKOSC = "SZEROKOSC";
    public final static String DLUGOSC = "DLUGOSC";
    private final static String WIDOCZNOSC_DOKLADNOSCI = "WIDOCZNOSC DOKLADNOSCI";
    private final static String DOKLADNOSC = "DOKLADNOSC";
    private final static String USTAWIENIE_ADRESU = "USTAWIENIE ADRESU";
    private final static String KOLOR_ADRESU = "KOLOR ADRESU";
    private final static String ADRES = "ADRES";
    private final static String WIDOCZNOSC_BLEDU_GPS = "WIDOCZNOSC BLEDU GPS";
    private final static String BLAD_GPS = "BLAD GPS";
    private final static String WIDOCZNOSC_PRZYCISKU = "WIDOCZNOSC PRZYCISKU";

    private static final int WYMAGANE_POZWOLENIA = 123;

    public static final String WYJSCIE = "WYJSCIE";

    private String[] mElementyMenu;
    private DrawerLayout mWysuwaneMenu;
    private ListView mListaElementow;
    private ActionBarDrawerToggle mPrzelacznikMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(getIntent().getBooleanExtra(MapsActivity.MENU, false))
        {
            finish();
            return;
        }
        else if(getIntent().getBooleanExtra(MapsActivity.IDENTYFIKACJA_ULIC, false))
        {
            mWysokoscTelefonu = getIntent().getExtras().getDouble(GeoMathematics.WYSOKOSC_TELEFONU);

            streetActivity();
        }

        setContentView(R.layout.activity_where);

        initializeComponents();
        initializeNavigationDrawer();
        initializeDrawerToggle();

        mManagerPolozenia = (LocationManager) getSystemService(LOCATION_SERVICE); //usługi lokalizacyjne

        mKryteria = new Criteria(); //służą do wyszukiwania najlepszego dostawcy
        mDostawca = mManagerPolozenia.getBestProvider(mKryteria, true); //pobranie nazwy najlepszego dostawcy informacji o położeniu

        try
        {
            //wyświetlenie informacji na temat ostatniego znanego położenia
            if(mPolozenie == null)
                mPolozenie = mManagerPolozenia.getLastKnownLocation(mDostawca);
        }
        catch(IllegalArgumentException e)
        {
            mWidocznoscBleduGPS = View.VISIBLE;
            mBladGPSTV.setVisibility(mWidocznoscBleduGPS);
            mBladGPSTV.setText(R.string.gps_blad);
        }

        if(mPolozenie != null)
        {
            mSzerokosc = mPolozenie.getLatitude();
            mDlugosc = mPolozenie.getLongitude();

            mSzerokoscTV.setText(String.valueOf(mSzerokosc + "°"));
            mDlugoscTV.setText(String.valueOf(mDlugosc + "°"));
            mDostawcaTV.setText(mDostawca);
            mWidocznoscDokladnosci = View.GONE;
            mDokladnoscTV.setVisibility(mWidocznoscDokladnosci);
            mUstawienieAdresu = Gravity.CENTER;
            mAdresTV.setGravity(mUstawienieAdresu);
            mKolorAdresu = getResources().getColor(R.color.czerwony);
            mAdresTV.setTextColor(mKolorAdresu);
            mAdresTV.setText(getString(R.string.adres_blad));
            mWidocznoscBleduGPS = View.GONE;
            mBladGPSTV.setVisibility(mWidocznoscBleduGPS);
            mWidocznoscPrzycisku = View.VISIBLE;
            mPokazNaMapieButton.setVisibility(mWidocznoscPrzycisku);
        }
        else
        {
            mSzerokoscTV.setText(R.string.brak);
            mDlugoscTV.setText(R.string.brak);
            mDostawcaTV.setText(R.string.brak);
            mWidocznoscDokladnosci = View.GONE;
            mDokladnoscTV.setVisibility(mWidocznoscDokladnosci);
            mUstawienieAdresu = Gravity.NO_GRAVITY;
            mAdresTV.setGravity(mUstawienieAdresu);
            mKolorAdresu = getResources().getColor(R.color.bialy);
            mAdresTV.setTextColor(mKolorAdresu);
            mAdresTV.setText(R.string.brak);
            mWidocznoscBleduGPS = View.GONE;
            mBladGPSTV.setVisibility(mWidocznoscBleduGPS);
            mWidocznoscPrzycisku = View.VISIBLE;
            mPokazNaMapieButton.setVisibility(mWidocznoscPrzycisku);
        }

        //pozwala na określenie adresu na podstawie współrzędnych
        mGeocoder = new Geocoder(this, Locale.getDefault());
    }

    private void initializeComponents()
    {
        mSzerokoscTV = (TextView) findViewById(R.id.szerokosc);
        mDlugoscTV = (TextView) findViewById(R.id.dlugosc);
        mDostawcaTV = (TextView) findViewById(R.id.dostawca);
        mDokladnoscTV = (TextView) findViewById(R.id.dokladnosc);
        mAdresTV = (TextView) findViewById(R.id.adres);
        mBladGPSTV = (TextView) findViewById(R.id.bladGPS);
        mPokazNaMapieButton = (Button) findViewById(R.id.pokazNaMapieButton);

        mPokazNaMapieButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showLocationActivity();
            }
        });
    }

    private void initializeNavigationDrawer()
    {
        mElementyMenu = getResources().getStringArray(R.array.zawartosc_menu_gdzie_jestem);
        mWysuwaneMenu = (DrawerLayout) findViewById(R.id.wysuwane_menu_gdzie_jestem);
        mListaElementow = (ListView) findViewById(R.id.lista_elementow_gdzie_jestem);

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

    private void initializeDrawerToggle()
    {
        mPrzelacznikMenu = new ActionBarDrawerToggle(this, mWysuwaneMenu, R.string.otworz, R.string.zamknij);

        //pozwala na zmianę ikony przełącznika (na początku ikona menu, potem strzałka - bez tego sama ikona menu)
        mWysuwaneMenu.addDrawerListener(mPrzelacznikMenu);

        getActionBar().setDisplayHomeAsUpEnabled(true);
//        getActionBar().setHomeButtonEnabled(true);
    }

    private void showLocationActivity()
    {
        Intent mapsActivity = new Intent(this, MapsActivity.class);
        mapsActivity.putExtra(SZEROKOSC, mSzerokosc);
        mapsActivity.putExtra(DLUGOSC, mDlugosc);
        startActivityForResult(mapsActivity, 2);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        
        mWysuwaneMenu.addDrawerListener(mPrzelacznikMenu);

        try
        {
            //ustawienie własności odświeżania, automatyczne wywoływanie onLocationChanged, co 1000ms, co 1m
            mManagerPolozenia.requestLocationUpdates(mDostawca, 1000, 1, this);
        }
        catch(IllegalArgumentException e)
        {
            mWidocznoscBleduGPS = View.VISIBLE;
            mBladGPSTV.setVisibility(mWidocznoscBleduGPS);
            mBladGPSTV.setText(R.string.gps_blad);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if(mWysuwaneMenu != null)
            mWysuwaneMenu.removeDrawerListener(mPrzelacznikMenu);

        //wyrejestrowanie LocationListenera
        if(mManagerPolozenia != null)
            mManagerPolozenia.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        mPolozenie = location;

        //wyświetlenie informacji na temat aktualnego położenia
        if(mPolozenie != null)
        {
            mSzerokosc = mPolozenie.getLatitude();
            mDlugosc = mPolozenie.getLongitude();
            mDokladnosc = mPolozenie.getAccuracy();

            mSzerokoscTV.setText(String.valueOf(mSzerokosc + "°"));
            mDlugoscTV.setText(String.valueOf(mDlugosc + "°"));
            mDostawcaTV.setText(mDostawca);

            mWidocznoscPrzycisku = View.VISIBLE;
            mPokazNaMapieButton.setVisibility(mWidocznoscPrzycisku);

            try
            {
                List<Address> adresy = mGeocoder.getFromLocation(mSzerokosc, mDlugosc, 1);

//            pojedyncze części adresu:
                Address adres = adresy.get(0);
                StringBuilder bufor = new StringBuilder();

                bufor.append(getString(R.string.panstwo)).append(adres.getCountryName()).append("\n");
                bufor.append(getString(R.string.miejscowosc)).append(adres.getLocality()).append("\n");
                bufor.append(getString(R.string.ulica)).append(adres.getThoroughfare()).append("\n");
                bufor.append(getString(R.string.nr_budynku)).append(adres.getSubThoroughfare()).append("\n");
                bufor.append(getString(R.string.kod_pocztowy)).append(adres.getPostalCode()).append("\n");

                mAdres = bufor.toString();

//            cały adres:
//            for(Address adres : adresy)
//            {
//                for(int i=0, j=adres.getMaxAddressLineIndex(); i<=j; i++)
//                {
//                    mAdres += adres.getAddressLine(i) + "\n";
//                }
//                mAdres += "\n\n";
//            }
                mWidocznoscDokladnosci = View.VISIBLE;
                mDokladnoscTV.setVisibility(mWidocznoscDokladnosci);
                mDokladnoscTV.setText(String.format("(" + getString(R.string.dokladnosc) + " %1.2f m)", mDokladnosc));
                mUstawienieAdresu = Gravity.NO_GRAVITY;
                mAdresTV.setGravity(mUstawienieAdresu);
                mKolorAdresu = getResources().getColor(R.color.bialy);
                mAdresTV.setTextColor(mKolorAdresu);
                mAdresTV.setText(mAdres);
                mWidocznoscBleduGPS = View.GONE;
                mBladGPSTV.setVisibility(mWidocznoscBleduGPS);
            }
            catch(Exception e)
            {
                mWidocznoscDokladnosci = View.GONE;
                mDokladnoscTV.setVisibility(mWidocznoscDokladnosci);
                mUstawienieAdresu = Gravity.CENTER;
                mAdresTV.setGravity(mUstawienieAdresu);
                mKolorAdresu = getResources().getColor(R.color.czerwony);
                mAdresTV.setTextColor(mKolorAdresu);
                mAdresTV.setText(R.string.adres_blad);
                mWidocznoscBleduGPS = View.GONE;
                mBladGPSTV.setVisibility(mWidocznoscBleduGPS);
            }
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle)
    {

    }

    @Override
    public void onProviderEnabled(String s)
    {

    }

    @Override
    public void onProviderDisabled(String s)
    {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        String dostawca = mDostawcaTV.getText().toString();
        String szerokosc = mSzerokoscTV.getText().toString();
        String dlugosc = mDlugoscTV.getText().toString();
        String dokladnosc = mDokladnoscTV.getText().toString();
        String adres = mAdresTV.getText().toString();
        String bladGPS = mBladGPSTV.getText().toString();

        outState.putString(DOSTAWCA, dostawca);
        outState.putString(SZEROKOSC, szerokosc);
        outState.putString(DLUGOSC, dlugosc);
        outState.putInt(WIDOCZNOSC_DOKLADNOSCI, mWidocznoscDokladnosci);
        outState.putString(DOKLADNOSC, dokladnosc);
        outState.putInt(USTAWIENIE_ADRESU, mUstawienieAdresu);
        outState.putInt(KOLOR_ADRESU, mKolorAdresu);
        outState.putString(ADRES, adres);
        outState.putInt(WIDOCZNOSC_BLEDU_GPS, mWidocznoscBleduGPS);
        outState.putString(BLAD_GPS, bladGPS);
        outState.putInt(WIDOCZNOSC_PRZYCISKU, mWidocznoscPrzycisku);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        if(savedInstanceState != null)
        {
            String dostawca = savedInstanceState.getString(DOSTAWCA);
            String szerokosc = savedInstanceState.getString(SZEROKOSC);
            String dlugosc = savedInstanceState.getString(DLUGOSC);
            mWidocznoscDokladnosci = savedInstanceState.getInt(WIDOCZNOSC_DOKLADNOSCI);
            String dokladnosc = savedInstanceState.getString(DOKLADNOSC);
            mUstawienieAdresu = savedInstanceState.getInt(USTAWIENIE_ADRESU);
            mKolorAdresu = savedInstanceState.getInt(KOLOR_ADRESU);
            String adres = savedInstanceState.getString(ADRES);
            mWidocznoscBleduGPS = savedInstanceState.getInt(WIDOCZNOSC_BLEDU_GPS);
            String bladGPS = savedInstanceState.getString(BLAD_GPS);
            mWidocznoscPrzycisku = savedInstanceState.getInt(WIDOCZNOSC_PRZYCISKU);

            mDostawcaTV.setText(dostawca);
            mSzerokoscTV.setText(szerokosc);
            mDlugoscTV.setText(dlugosc);
            mDokladnoscTV.setVisibility(mWidocznoscDokladnosci);
//            if(mWidocznoscDokladnosci == View.VISIBLE)
            mDokladnoscTV.setText(dokladnosc);
            mAdresTV.setGravity(mUstawienieAdresu);
            mAdresTV.setTextColor(mKolorAdresu);
            mAdresTV.setText(adres);
            mBladGPSTV.setVisibility(mWidocznoscBleduGPS);
//            if(mWidocznoscBleduGPS == View.VISIBLE)
            mBladGPSTV.setText(bladGPS);
            mPokazNaMapieButton.setVisibility(mWidocznoscPrzycisku);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);

        //pozwala na zmianę ikony przełącznika (na początku ikona menu, potem strzałka - bez tego sama strzałka)
        mPrzelacznikMenu.syncState();
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig)
//    {
//        super.onConfigurationChanged(newConfig);
//
//        mPrzelacznikMenu.onConfigurationChanged(newConfig);
//    }

    //obsługa kliknięcia przełącznika
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(mPrzelacznikMenu.onOptionsItemSelected(item))
            return true;

        return super.onOptionsItemSelected(item);
    }

    private void selectElement(int pozycja)
    {
        switch(pozycja)
        {
            case 0:
                backToMenu();
                break;
            case 1:
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if(hasCameraPermissions())
                        streetIdentificationActivity();
                    else
                        requestCameraPermissions();
                }
                break;
            case 2:
                exitApplication();
                break;
            default:
                break;
        }

//        mListaElementow.setItemChecked(pozycja, true);
//        mListaElementow.setSelection(pozycja);
        mWysuwaneMenu.closeDrawer(mListaElementow);
    }

    private void backToMenu()
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
                            Toast.makeText(WhereActivity.this, R.string.alert_brak, Toast.LENGTH_SHORT).show();
                        else
                        {
                            mWysokoscTelefonu = Double.parseDouble(wprowadzonaWartosc);

                            if(mWysokoscTelefonu > 0)
                            {
                                streetActivity();
                                dialogWysokoscTelefonu.dismiss();
                            }
                            else
                                Toast.makeText(WhereActivity.this, R.string.alert_zero, Toast.LENGTH_SHORT).show();
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

    private void streetActivity() //identyfikacja ulic
    {
        Intent cameraActivity = new Intent(this, CameraActivity.class);
        cameraActivity.putExtra(GeoMathematics.WYSOKOSC_TELEFONU, mWysokoscTelefonu);
        startActivityForResult(cameraActivity, 1);
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