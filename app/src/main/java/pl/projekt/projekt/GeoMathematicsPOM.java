package pl.projekt.projekt;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

@SuppressWarnings("MissingPermission")
public class GeoMathematicsPOM
{
    private Context mKontekst;

    private static final String TAG = "TAG";
    //    private static final double WYSOKOSC_TELEFONU = 1.5;
    private static final double DLUGOSC_ROWNIKA = 40075704;    //m, z encyklopedii PWN (hasło: równik ziemski)
    //    private static final double PROMIEN_ZIEMII = 6378245;      //m, z encyklopedii PWN (hasło: Ziemia)
    private static final boolean WIDOCZNOSC_TESTOWYCH_TV = false;
    public static final String WYSOKOSC_TELEFONU = "WYSOKOSC_TELEFONU";

    private static final String GPS = LocationManager.GPS_PROVIDER;
    private static final String SIEC = LocationManager.NETWORK_PROVIDER;

    private double mWysokoscTelefonu;   //wysokość telefonu nad ziemią podana przez użytkownika
    private double mDystans;            //odleglość aktualnej pozycji użytkownika od pozycji docelowej
    private double mNachylenie;         //kąt między prostą zawierającą wysokość telefonu a prosta prostopadłą do telefonu
    //wskazującego miejsce docelowe
    private double mAktualnaSzerokosc;
    private double mAktualnaDlugosc;
    private double mWskazanaSzerokosc;
    private double mWskazanaDlugosc;
    private double mAzymut;            //namiar kompasu - kąt między kierunkiem północym a kierunkiem, w którym zwrócony jest kompas

    private String mWskazanaUlica;

    private LocationManager mManagerPolozenia;
    private Location mPolozenie;
    private Criteria mKryteria;
    private String mDostawca;
    private Geocoder mGeocoder;

    private double mX;                  //współrzędna X położenia telefonu
    private double mY;                  //-----||---- Y --------||--------
    private double mZ;                  //-----||---- Z --------||--------

    private SensorManager mManagerCzujnikow;
    private Sensor mAkcelerometr;
    private Sensor mMagnetometr;

    private boolean mCzyWykonanGM = false;

    private float[] mR;                 //macierz jednostkowa
    private float[] mI;                 //macierz obrotu
    private float[] mDaneAkcelerometru; //wektory X, Y, Z
    private float[] mDaneMagnetometru;  //wektory X, Y, Z
    private float[] mOrientacja;

    private TextView mAktualnaSzerokoscTestTV;
    private TextView mAktualnaDlugoscTestTV;
    private TextView mNachylenieTestTV;
    private TextView mDystansTestTV;
    private TextView mAzymutTestTV;
    private TextView mWskazanaSzerokoscTestTV;
    private TextView mWskazanaDlugoscTestTV;
    private TextView mWskazanaUlicaTestTV;

    private ImageView mLinieIV;
    private TextView mWskazanaUlicaTV;

    private HandlerThread mWatekCzujnikow;
    private Handler mUchwytNasluchwiaczaZdarzenCzujnikow;
    private Looper mPetlaNasluchiwaczaZdarzenCzujnikow;
    private static final String WATEK_CZUJNIKOW = "watek_czujnikow";

    private HandlerThread mWatekPolozenia;
    private Looper mPetlaNasluchiwaczaPolozenia;
    private static final String WATEK_POLOZENIA = "watek_polozenia";

    private boolean mGPSDostepny;
    private boolean mSiecDostepna;
//    private Handler mUchwytTimera;

    private Location mPolozenieGPS;
    private Location mPolozenieSiec;

    private boolean mGPSWidoczny;
    private long mCzasOdOstatniejZnalezionejLokalizacji;


    private GpsStatus.Listener mNasluchiwaczStatusuGPS = new GpsStatus.Listener()
    {
        @Override
        public void onGpsStatusChanged(int event)
        {
            if(mPolozenie != null)
                mGPSWidoczny = (SystemClock.elapsedRealtime()) - mCzasOdOstatniejZnalezionejLokalizacji < 3000;
            else
            {
                if(mSiecDostepna)
                    mManagerPolozenia.requestLocationUpdates(SIEC, 0, 0, mNasluchiwaczPolozenia, mPetlaNasluchiwaczaPolozenia);
                else
                    Toast.makeText(mKontekst, "Ni ma żadnego dostawcy lokalizacji", Toast.LENGTH_SHORT).show();
            }

            if(mGPSWidoczny)

        }
    };

//    private Runnable mWykonanieTimera = new Runnable()
//    {
//        @Override
//        public void run()
//        {
//            Log.d(TAG, "HANDLER");
//
//            mManagerPolozenia.removeUpdates(mNasluchiwaczPolozenia);
//
//            if(mGPSDostepny)
//                mPolozenieGPS = mManagerPolozenia.getLastKnownLocation(GPS);
//
//            if(mSiecDostepna)
//                mPolozenieSiec = mManagerPolozenia.getLastKnownLocation(SIEC);
//
//            //jesli oba położenia były ostatnio używanie
//            if(mPolozenieGPS != null && mPolozenieSiec != null)
//            {
//                Log.d(TAG, "TU ESTEM");
//                if(mPolozenieGPS.getTime() > mPolozenieSiec.getTime())
//                    findLocation(mPolozenieGPS);
//                else
//                    findLocation(mPolozenieSiec);
//                return;
//            }
//
//            if(mPolozenieGPS != null)
//            {
//                Log.d(TAG, "A TERA TU");
//                findLocation(mPolozenieGPS);
//                return;
//            }
//
//            if(mPolozenieSiec != null)
//            {
//                Log.d(TAG, "ABO TU");
//                findLocation(mPolozenieSiec);
//                return;
//            }
//
//            findLocation(null);
//        }
//    };

    private SensorEventListener mNasłuchiwaczZdarzenCzujnikow = new SensorEventListener()
    {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent)
        {
            switch(sensorEvent.sensor.getType())
            {
                case Sensor.TYPE_ACCELEROMETER:
                    mDaneAkcelerometru = sensorEvent.values.clone();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mDaneMagnetometru = sensorEvent.values.clone();
                    break;
                default:
                    return;
            }

            float x = mDaneAkcelerometru[0];
            float y = mDaneAkcelerometru[1];
            float z = mDaneAkcelerometru[2];

            normalizeXYZ(x, y, z);

            findInclination();
            findDistance();
            findAzimuth();
            findPointedLocation();
            findStreet();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    };

    private LocationListener mNasluchiwaczPolozenia = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            mPolozenie = location;

            mCzasOdOstatniejZnalezionejLokalizacji = SystemClock.elapsedRealtime();

            findLocation();
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
    };

    public GeoMathematicsPOM(Context kontekst)
    {
        this.mKontekst = kontekst;

        if(WIDOCZNOSC_TESTOWYCH_TV)
            initializeTestTVs();

        initializeAll();

        Log.d(TAG, "CZY JEST INTERNET: = " + mManagerPolozenia.isProviderEnabled(SIEC));
        Log.d(TAG, "CZY JEST GPS: = " + mManagerPolozenia.isProviderEnabled(GPS));


        findLocation();

//        findPointedLocation();
//        findStreet();

        mCzyWykonanGM = true;
    }

    private void initializeTestTVs()
    {
        mAktualnaSzerokoscTestTV = (TextView) ((Activity) mKontekst).findViewById(R.id.aktualnaSzerokoscTest);
        mAktualnaDlugoscTestTV = (TextView) ((Activity) mKontekst).findViewById(R.id.aktualnaDlugoscTest);
        mNachylenieTestTV = (TextView) ((Activity) mKontekst).findViewById(R.id.nachylenieTest);
        mDystansTestTV = (TextView) ((Activity) mKontekst).findViewById(R.id.dystansTest);
        mAzymutTestTV = (TextView) ((Activity) mKontekst).findViewById(R.id.azymutTest);
        mWskazanaSzerokoscTestTV = (TextView) ((Activity) mKontekst).findViewById(R.id.wskazanaSzerokoscTest);
        mWskazanaDlugoscTestTV = (TextView) ((Activity) mKontekst).findViewById(R.id.wskazanaDlugoscTest);
        mWskazanaUlicaTestTV = (TextView) ((Activity) mKontekst).findViewById(R.id.wskazanaUlicaTest);
    }

    private void initializeAll()
    {
        initializeLocation();

        initializeSensors();

        mR = new float[9];
        mI = new float[9];
        mDaneAkcelerometru = new float[3];
        mDaneMagnetometru = new float[3];
        mOrientacja = new float[3];

        mGeocoder = new Geocoder(mKontekst, Locale.getDefault());

        mLinieIV = (ImageView) ((Activity) mKontekst).findViewById(R.id.linie);
        mWskazanaUlicaTV = (TextView) ((Activity) mKontekst).findViewById(R.id.wskazanaUlica);

        mWysokoscTelefonu = ((Activity) mKontekst).getIntent().getExtras().getDouble(WYSOKOSC_TELEFONU);
    }

    private void initializeLocation()
    {
        mManagerPolozenia = (LocationManager) mKontekst.getSystemService(Context.LOCATION_SERVICE);

        mKryteria = new Criteria();
        mDostawca = mManagerPolozenia.getBestProvider(mKryteria, true);

        registerLocation();
    }

    private void initializeSensors()
    {
        mManagerCzujnikow = (SensorManager) mKontekst.getSystemService(Context.SENSOR_SERVICE);

        mAkcelerometr = mManagerCzujnikow.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometr = mManagerCzujnikow.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        registerSensors();
    }

    private void registerLocation()
    {
        mWatekPolozenia = new HandlerThread(WATEK_POLOZENIA);

        Log.d(TAG, "dupa1 przed startem czy zyje = " + mWatekPolozenia.isAlive());
        Log.d(TAG, "dupa1 przed startem stan = " + mWatekPolozenia.getState());

        Toast.makeText(mKontekst, "mWatekPolozenie czy zyje = " + mWatekPolozenia.isAlive(), Toast.LENGTH_SHORT).show();
        Toast.makeText(mKontekst, "mWatekPolozenie stan = " + mWatekPolozenia.getState(), Toast.LENGTH_SHORT).show();

        mWatekPolozenia.start();
        mPetlaNasluchiwaczaPolozenia = mWatekPolozenia.getLooper();

        Toast.makeText(mKontekst, "mWatekPolozenie wystartował", Toast.LENGTH_SHORT).show();
        Toast.makeText(mKontekst, "mWatekPolozenie czy zyje = " + mWatekPolozenia.isAlive(), Toast.LENGTH_SHORT).show();
        Toast.makeText(mKontekst, "mWatekPolozenie stan = " + mWatekPolozenia.getState(), Toast.LENGTH_SHORT).show();

        Log.d(TAG, "dupa1 i wystartował");
        Log.d(TAG, "dupa1 po starcie czy zyje = " + mWatekPolozenia.isAlive());
        Log.d(TAG, "dupa1 po starcie stan = " + mWatekPolozenia.getState());

        try
        {
            mGPSDostepny = mManagerPolozenia.isProviderEnabled(GPS);
            mSiecDostepna = mManagerPolozenia.isProviderEnabled(SIEC);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        if(!mGPSDostepny && !mSiecDostepna)
            Toast.makeText(mKontekst, "Ni ma dostawcy lokalizacji!", Toast.LENGTH_SHORT).show();

//        if(mGPSDostepny)
//            mManagerPolozenia.requestLocationUpdates(GPS, 0, 0, mNasluchiwaczPolozenia, mPetlaNasluchiwaczaPolozenia);

        mManagerPolozenia.addGpsStatusListener(mNasluchiwaczStatusuGPS);

//        mUchwytTimera = new Handler();
//        mUchwytTimera.postDelayed(mWykonanieTimera, 20000);

//        if(mDostawca != null)
//        {
//            mManagerPolozenia.requestLocationUpdates(mDostawca, 0, 0, mNasluchiwaczPolozenia, mPetlaNasluchiwaczaPolozenia);
//
////            if(mPolozenie == null)
////                mPolozenie = mManagerPolozenia.getLastKnownLocation(mDostawca);
//        }
    }

    private void registerSensors()
    {
        mWatekCzujnikow = new HandlerThread(WATEK_CZUJNIKOW);

        Log.d(TAG, "dupa przed startem czy zyje = " + mWatekCzujnikow.isAlive());
        Log.d(TAG, "dupa przed startem stan = " + mWatekCzujnikow.getState());

        mWatekCzujnikow.start();
        mPetlaNasluchiwaczaZdarzenCzujnikow = mWatekCzujnikow.getLooper();
        mUchwytNasluchwiaczaZdarzenCzujnikow = new Handler(mPetlaNasluchiwaczaZdarzenCzujnikow);

        Log.d(TAG, "dupa i wystartował");
        Log.d(TAG, "dupa po starcie czy zyje = " + mWatekCzujnikow.isAlive());
        Log.d(TAG, "dupa po starcie stan = " + mWatekCzujnikow.getState());

        mManagerCzujnikow.registerListener(mNasłuchiwaczZdarzenCzujnikow, mAkcelerometr,
                SensorManager.SENSOR_DELAY_NORMAL, mUchwytNasluchwiaczaZdarzenCzujnikow);
        mManagerCzujnikow.registerListener(mNasłuchiwaczZdarzenCzujnikow, mMagnetometr,
                SensorManager.SENSOR_DELAY_NORMAL, mUchwytNasluchwiaczaZdarzenCzujnikow);
    }

    private void findLocation()
    {
        Log.d(TAG, "findLocation: watek " + Thread.currentThread().getName());

        Toast.makeText(mKontekst, "findLocation: watek = " + Thread.currentThread().getName(), Toast.LENGTH_SHORT).show();

        if(mPolozenie != null)
        {
            mAktualnaSzerokosc = mPolozenie.getLatitude();
            mAktualnaDlugosc = mPolozenie.getLongitude();
        }
        else
        {
            mAktualnaSzerokosc = -999;
            mAktualnaDlugosc = -999;
        }

        if(WIDOCZNOSC_TESTOWYCH_TV)
        {
            ((Activity) mKontekst).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.d(TAG, "znajdzPolozenie: watek " + Thread.currentThread().getName());

                    mAktualnaSzerokoscTestTV.setText(String.valueOf(round(mAktualnaSzerokosc, 6) + "°"));
                    mAktualnaDlugoscTestTV.setText(String.valueOf(round(mAktualnaDlugosc, 6) + "°"));
                }
            });
        }
    }

    //normalizacja wskazań akcelerometru
    private void normalizeXYZ(float x, float y, float z)
    {
        double norma = Math.sqrt(x * x + y * y + z * z);

        mX = x / norma;
        mY = y / norma;
        mZ = z / norma;
    }

    private void findInclination()
    {
        Log.d(TAG, "findInclination: watek " + Thread.currentThread().getName());

        if(mAkcelerometr != null)
            mNachylenie = Math.toDegrees(Math.acos(mZ));    //acos(wysokość/przeciwprostokątna) = mNachylenie (w stopniach)
        else
            mNachylenie = -999;

        if(WIDOCZNOSC_TESTOWYCH_TV)
        {
            ((Activity) mKontekst).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.d(TAG, "znajdzNachylenie: watek " + Thread.currentThread().getName());

                    mNachylenieTestTV.setText(String.valueOf(round(mNachylenie, 6) + "°"));
                }
            });
        }
    }

    private void findDistance()
    {
        Log.d(TAG, "findDistance: watek " + Thread.currentThread().getName());

        if(mNachylenie > 0 && mNachylenie < 90)
            mDystans = mWysokoscTelefonu * Math.tan(Math.toRadians(mNachylenie)); //tg(mNachylenie) = mDystans/wysokosc (w metrach)
        else
            mDystans = -999;

        if(WIDOCZNOSC_TESTOWYCH_TV)
        {
            ((Activity) mKontekst).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.d(TAG, "znajdzDystans: watek " + Thread.currentThread().getName());

                    mDystansTestTV.setText(String.valueOf(round(mDystans, 6) + "m"));
                }
            });
        }
    }

    private void findAzimuth()
    {
        Log.d(TAG, "findAzimuth: watek " + Thread.currentThread().getName());

        if(mAkcelerometr != null && mMagnetometr != null)
        {
            SensorManager.getRotationMatrix(mR, mI, mDaneMagnetometru, mDaneAkcelerometru);
            SensorManager.getOrientation(mR, mOrientacja);

            mAzymut = ((-Math.toDegrees(mOrientacja[0]) + 360) % 360) + 90;

            if(mAzymut > 360)
                mAzymut = mAzymut - 360;
        }
        else
            mAzymut = -999;

//        if(mNachylenie <= 0 && mNachylenie >= 90)
//        {
//            clearHistory();
//            mAzymutUsredniony = Float.NaN;
//        }
//        else
//        {
//            setHistory();
//            mAzymutUsredniony = (float) Math.toDegrees(findAverageAzimuth());
//        }

        if(WIDOCZNOSC_TESTOWYCH_TV)
        {
            ((Activity) mKontekst).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.d(TAG, "znajdzAzymut: watek " + Thread.currentThread().getName());

                    mAzymutTestTV.setText(String.valueOf(round(mAzymut, 6) + "°"));
                }
            });
        }
    }

    private void findPointedLocation()
    {
        Log.d(TAG, "findPointedLocation: watek " + Thread.currentThread().getName());

        if(mAktualnaSzerokosc != -999 && mAktualnaDlugosc != -999 && mDystans != -999 && mAzymut != -999)
        {
//            mWskazanaSzerokosc = Math.toDegrees(Math.asin(
//                    Math.sin(Math.toRadians(mAktualnaSzerokosc)) * Math.cos(mDystans / PROMIEN_ZIEMII)
//                            + Math.cos(Math.toRadians(mAktualnaSzerokosc)) * Math.sin(mDystans / PROMIEN_ZIEMII)
//                            * Math.cos(Math.toRadians(mAzymut))));
//
//            mWskazanaDlugosc = Math.toDegrees(Math.toRadians(mAktualnaDlugosc) + Math.atan2(
//                    Math.sin(Math.toRadians(mAzymut)) * Math.sin(mDystans / PROMIEN_ZIEMII)
//                            * Math.cos(Math.toRadians(mAktualnaSzerokosc)), Math.cos(mDystans / PROMIEN_ZIEMII)
//                            - Math.sin(Math.toRadians(mAktualnaSzerokosc)) * Math.sin(Math.toRadians(mWskazanaSzerokosc))));


            double r;                                   //zmienna pomocnicza (w stopniach)
            double A;                                   //---------------||---------------
            double D = mDystans / (DLUGOSC_ROWNIKA / 360);  //---------------||---------------
            if((mAzymut >= 0 && mAzymut < 90) || (mAzymut >= 270 && mAzymut < 360))
            {
                r = 0;
                A = mAzymut - r;

                mWskazanaSzerokosc = mAktualnaSzerokosc
                        + (D / Math.sqrt(Math.pow(Math.cos(Math.toRadians(mAktualnaSzerokosc)), 2)
                        * Math.pow(Math.tan(Math.toRadians(A)), 2) + 1));

                mWskazanaDlugosc = mAktualnaDlugosc
                        + ((D * Math.tan(Math.toRadians(A))) / Math.sqrt(Math.pow(Math.cos(Math.toRadians(mAktualnaSzerokosc)), 2)
                        * Math.pow(Math.tan(Math.toRadians(A)), 2) + 1));
            }
            else if(mAzymut >= 90 && mAzymut < 270)
            {
                r = 180;
                A = mAzymut - r;

                mWskazanaSzerokosc = mAktualnaSzerokosc
                        - (D / Math.sqrt(Math.pow(Math.cos(Math.toRadians(mAktualnaSzerokosc)), 2)
                        * Math.pow(Math.tan(Math.toRadians(A)), 2) + 1));

                mWskazanaDlugosc = mAktualnaDlugosc
                        - ((D * Math.tan(Math.toRadians(A))) / Math.sqrt(Math.pow(Math.cos(Math.toRadians(mAktualnaSzerokosc)), 2)
                        * Math.pow(Math.tan(Math.toRadians(A)), 2) + 1));
            }
            else
            {
                mWskazanaSzerokosc = -999;
                mWskazanaDlugosc = -999;
            }
        }
        else
        {
            mWskazanaSzerokosc = -999;
            mWskazanaDlugosc = -999;
        }

        if(WIDOCZNOSC_TESTOWYCH_TV)
        {
            ((Activity) mKontekst).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.d(TAG, "znajdzWskazanePolozenie: watek " + Thread.currentThread().getName());

                    mWskazanaSzerokoscTestTV.setText(String.valueOf(round(mWskazanaSzerokosc, 6) + "°"));
                    mWskazanaDlugoscTestTV.setText(String.valueOf(round(mWskazanaDlugosc, 6) + "°"));
                }
            });
        }
    }

    private void findStreet()
    {
        Log.d(TAG, "findStreet: watek " + Thread.currentThread().getName());

        if(mWskazanaSzerokosc != -999 && mWskazanaDlugosc != -999)
        {
            try
            {
                List<Address> adresy = mGeocoder.getFromLocation(mWskazanaSzerokosc, mWskazanaDlugosc, 1);

                Address adres = adresy.get(0);
                mWskazanaUlica = adres.getThoroughfare();
//                mWskazanaUlica = adres.getLocality();
            }
            catch(Exception e)
            {
                mWskazanaUlica = "nie znaleziono";
            }
        }
        else
            mWskazanaUlica = "nie znaleziono";

        if(mWskazanaUlica != null)
        {
            ((Activity) mKontekst).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.d(TAG, "znajdzUlice: watek " + Thread.currentThread().getName());

                    if(!mWskazanaUlica.equals("nie znaleziono"))
                    {
                        mLinieIV.setVisibility(View.VISIBLE);
                        mWskazanaUlicaTV.setVisibility(View.VISIBLE);
                        mWskazanaUlicaTV.setText(mWskazanaUlica);
                    }
                    else
                    {
                        mWskazanaUlicaTV.setText("");
                        mWskazanaUlicaTV.setVisibility(View.GONE);
                        mLinieIV.setVisibility(View.GONE);
                    }
                }
            });
        }

        if(WIDOCZNOSC_TESTOWYCH_TV)
        {
            ((Activity) mKontekst).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.d(TAG, "znajdzUlice: watek " + Thread.currentThread().getName());

                    mWskazanaUlicaTestTV.setText(mWskazanaUlica);
                }
            });
            //+ " (z dokładnością do: " + Math.round(mPolozenie.getAccuracy()) + "m)");
        }
    }

    private double round(double liczba, int iloscMiejscPoPrzecinku)
    {
        double potega10 = Math.pow(10, iloscMiejscPoPrzecinku);

        if(iloscMiejscPoPrzecinku >= 0)
            return (Math.round(liczba * potega10)) / potega10;
        else
            return liczba;
    }

//                    private List<float[]> mHistoria = new ArrayList<float[]>();
//                    private int mHistoriaID;
//                    private int mMaxDlugoscHistorii = 40;
//                    private float mAzymutUsredniony = Float.NaN;
//
//                    private void clearHistory()
//                    {
//                        if(DEBUG)
//                            Log.d(TAG, "clearHistory()");
//
//                        mHistoria.clear();
//                        mHistoriaID = 0;
//                    }
//
//                    private void setHistory()
//                    {
//                        if(DEBUG)
//                            Log.d(TAG, "setHistory()");
//
//                        float[] historia = mR.clone();
//
//                        if(mHistoria.size() == mMaxDlugoscHistorii)
//                            mHistoria.remove(mHistoriaID);
//
//                        mHistoria.add(mHistoriaID++, historia);
//                        mHistoriaID = mHistoriaID % mMaxDlugoscHistorii;
//                    }
//
//                    private float[] average(List<float[]> wartosci)
//                    {
//                        float[] wynik = new float[9];
//
//                        for(float[] wartosc : wartosci)
//                        {
//                            for(int i=0; i<9; i++)
//                                wynik[i] = wynik[i] + wartosc[i];
//                        }
//
//                        for(int i=0; i<9; i++)
//                            wynik[i] = wynik[i] / wartosci.size();
//
//                        return wynik;
//                    }
//
//                    private float findAverageAzimuth()
//                    {
//                        if(DEBUG)
//                            Log.d(TAG, "findAverageAzimuth()");
//
//                        float[] sredniaHistorii = average(mHistoria);
//
//                        return (float) Math.atan2(-sredniaHistorii[2], -sredniaHistorii[5]);
//                    }


    public void setPhoneHeihgt(double wysokoscTelefonu)
    {
        this.mWysokoscTelefonu = wysokoscTelefonu;
    }

    protected void resume()
    {
        if(!mCzyWykonanGM)
        {
            registerLocation();

            registerSensors();

            mCzyWykonanGM = true;
        }
    }

    protected void pause()
    {
        unregisterLocation();

        unregisterSensors();

        mCzyWykonanGM = false;
    }

    public void unregisterLocation()
    {
        if(mManagerPolozenia != null)
            mManagerPolozenia.removeUpdates(mNasluchiwaczPolozenia);

        if(mWatekPolozenia.isAlive())
        {
            Log.d(TAG, "dupa1 przed stopem czy zyje = " + mWatekPolozenia.isAlive());
            Log.d(TAG, "dupa1 przed stopem stan = " + mWatekPolozenia.getState());

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                mWatekPolozenia.quitSafely();
            else
                mWatekPolozenia.quit();

            Log.d(TAG, "dupa1 i wylądował");
            Log.d(TAG, "dupa1 po stopie czy zyje = " + mWatekPolozenia.isAlive());
            Log.d(TAG, "dupa1 po stopie stan = " + mWatekPolozenia.getState());

            mWatekPolozenia = null;
        }
    }

    public void unregisterSensors()
    {
        if(mManagerCzujnikow != null)
            mManagerCzujnikow.unregisterListener(mNasłuchiwaczZdarzenCzujnikow);

        if(mWatekCzujnikow.isAlive())
        {
            Log.d(TAG, "dupa przed stopem czy zyje = " + mWatekCzujnikow.isAlive());
            Log.d(TAG, "dupa przed stopem stan = " + mWatekCzujnikow.getState());

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                mWatekCzujnikow.quitSafely();
            else
                mWatekCzujnikow.quit();

            Log.d(TAG, "dupa i wylądował");
            Log.d(TAG, "dupa po stopie czy zyje = " + mWatekCzujnikow.isAlive());
            Log.d(TAG, "dupa po stopie stan = " + mWatekCzujnikow.getState());

            mWatekCzujnikow = null;
        }
    }
}