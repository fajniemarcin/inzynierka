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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

@SuppressWarnings("MissingPermission")
public class GeoMathematics
{
    private Context mKontekst;

    private static final String TAG = "TAG";
    //    private static final double WYSOKOSC_TELEFONU = 1.5;
    private static final double DLUGOSC_ROWNIKA = 40075704;    //m, z encyklopedii PWN (hasło: równik ziemski)
    //    private static final double PROMIEN_ZIEMII = 6378245;      //m, z encyklopedii PWN (hasło: Ziemia)
    private static final boolean WIDOCZNOSC_TESTOWYCH_TV = false;
    public static final String WYSOKOSC_TELEFONU = "WYSOKOSC_TELEFONU";

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

    private Thread mWatekPoboczny;
    private volatile boolean mWatekDziala = true;

    private Runnable mWykonanieObliczen = new Runnable()
    {
        @Override
        public void run()
        {
            if(mWatekDziala)
            {
                findInclination();
                findDistance();
                findAzimuth();
                findPointedLocation();
                findStreet();
            }
        }
    };

    private LocationListener mNasluchiwaczPolozenia = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            mPolozenie = location;

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

    private SensorEventListener mNasłuchiwaczZdarzenCzujnika = new SensorEventListener()
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

            mWatekPoboczny = new Thread(mWykonanieObliczen, "watek_poboczny");

            Log.d(TAG, "dupa przed start czy przerwany = " + Thread.interrupted() + mWatekPoboczny.isInterrupted());
            Log.d(TAG, "dupa przed start czy zyje = " + mWatekPoboczny.isAlive());
            Log.d(TAG, "dupa przed start stan = " + mWatekPoboczny.getState());

            mWatekDziala = true;
            mWatekPoboczny.start();

            Log.d(TAG, "dupa po start czy przerwany = " + Thread.interrupted() + mWatekPoboczny.isInterrupted());
            Log.d(TAG, "dupa po start czy zyje = " + mWatekPoboczny.isAlive());
            Log.d(TAG, "dupa po start stan = " + mWatekPoboczny.getState());

            stopThread();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {

        }
    };

    public GeoMathematics(Context kontekst)
    {
        this.mKontekst = kontekst;

        if(WIDOCZNOSC_TESTOWYCH_TV)
            initializeTestTVs();

        initializeSensorsAndOthers();

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

    private void initializeSensorsAndOthers()
    {
        mManagerPolozenia = (LocationManager) mKontekst.getSystemService(Context.LOCATION_SERVICE);

        mKryteria = new Criteria();
        mDostawca = mManagerPolozenia.getBestProvider(mKryteria, true);

        if(mDostawca != null)
        {
            mManagerPolozenia.requestLocationUpdates(mDostawca, 1000, 1, mNasluchiwaczPolozenia);

            if(mPolozenie == null)
                mPolozenie = mManagerPolozenia.getLastKnownLocation(mDostawca);
        }

        mManagerCzujnikow = (SensorManager) mKontekst.getSystemService(Context.SENSOR_SERVICE);

//        mHandlerThread = new HandlerThread("sensorThread");
//        mHandlerThread.start();
//        mHandler = new Handler(mHandlerThread.getLooper());

        mAkcelerometr = mManagerCzujnikow.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mManagerCzujnikow.registerListener(mNasłuchiwaczZdarzenCzujnika, mAkcelerometr, SensorManager.SENSOR_DELAY_NORMAL);

        mMagnetometr = mManagerCzujnikow.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mManagerCzujnikow.registerListener(mNasłuchiwaczZdarzenCzujnika, mMagnetometr, SensorManager.SENSOR_DELAY_NORMAL);

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

    private void findLocation()
    {
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
            mAktualnaSzerokoscTestTV.setText(String.valueOf(round(mAktualnaSzerokosc, 6) + "°"));
            mAktualnaDlugoscTestTV.setText(String.valueOf(round(mAktualnaDlugosc, 6) + "°"));
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
        Log.d(TAG, "run: watek " + Thread.currentThread().getName());

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
                    mNachylenieTestTV.setText(String.valueOf(round(mNachylenie, 6) + "°"));
                    Log.d(TAG, "run: nachylenie - watek " + Thread.currentThread().getName());
                }
            });
        }
    }

    private void findDistance()
    {
        Log.d(TAG, "run: watek " + Thread.currentThread().getName());
        Log.d(TAG, "WYSOKOSC = " + mWysokoscTelefonu);

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
                    mDystansTestTV.setText(String.valueOf(round(mDystans, 6) + "m"));
                    Log.d(TAG, "run: dystans - watek " + Thread.currentThread().getName());
                }
            });
        }
    }

    private void findAzimuth()
    {
        Log.d(TAG, "run: watek " + Thread.currentThread().getName());

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
                    mAzymutTestTV.setText(String.valueOf(round(mAzymut, 6) + "°"));
                    Log.d(TAG, "run: azymut - watek " + Thread.currentThread().getName());
                }
            });
        }
    }

    private void findPointedLocation()
    {
        Log.d(TAG, "run: watek " + Thread.currentThread().getName());

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
                    mWskazanaSzerokoscTestTV.setText(String.valueOf(round(mWskazanaSzerokosc, 6) + "°"));
                    mWskazanaDlugoscTestTV.setText(String.valueOf(round(mWskazanaDlugosc, 6) + "°"));
                    Log.d(TAG, "run: szerokosci dlugosc - watek " + Thread.currentThread().getName());
                }
            });
        }
    }

    private void findStreet()
    {
        Log.d(TAG, "run: watek " + Thread.currentThread().getName());

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

        ((Activity) mKontekst).runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(!mWskazanaUlica.equals("nie znaleziono") && mPolozenie != null   )
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

                Log.d(TAG, "run: ulica - watek " + Thread.currentThread().getName());
            }
        });

        if(WIDOCZNOSC_TESTOWYCH_TV)
        {
            ((Activity) mKontekst).runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mWskazanaUlicaTestTV.setText(mWskazanaUlica);
                    Log.d(TAG, "run: ulica - watek " + Thread.currentThread().getName());
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
            if(mDostawca != null)
                mManagerPolozenia.requestLocationUpdates(mDostawca, 1000, 1, mNasluchiwaczPolozenia);

            mManagerCzujnikow.registerListener(mNasłuchiwaczZdarzenCzujnika, mAkcelerometr, SensorManager.SENSOR_DELAY_NORMAL);
            mManagerCzujnikow.registerListener(mNasłuchiwaczZdarzenCzujnika, mMagnetometr, SensorManager.SENSOR_DELAY_NORMAL);

            mCzyWykonanGM = true;
        }
    }

    protected void pause()
    {
        if(mManagerPolozenia != null)
            mManagerPolozenia.removeUpdates(mNasluchiwaczPolozenia);

        if(mManagerCzujnikow != null)
            mManagerCzujnikow.unregisterListener(mNasłuchiwaczZdarzenCzujnika);

        mCzyWykonanGM = false;
    }

    public void stopThread()
    {

        if(mWatekPoboczny != null)
        {
            mWatekDziala = false;
            try
            {
                Log.d(TAG, "pause: przed stopem czy przerwany = " + Thread.interrupted() + mWatekPoboczny.isInterrupted());
                Log.d(TAG, "pause: przed stopem czy zyje = " + mWatekPoboczny.isAlive());
                Log.d(TAG, "pause: przed stopem stan = " + mWatekPoboczny.getState());
                mWatekPoboczny.join();
            }
            catch(InterruptedException e)
            {
                Log.d(TAG, "stopThread: błąd join() = " + e.getMessage());
            }
            finally
            {
                mWatekPoboczny = null;
            }

            Log.d(TAG, "pause: przed po stopie = " + mWatekPoboczny);
        }

    }
}