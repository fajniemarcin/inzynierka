package pl.projekt.projekt;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.File;

public class CacheManaging
{
    private Context mKontekst;

    private static final String TAG = "TAG";

    private long mOpóźnienie;         //co ile ma być uruchamiany timer
    private Handler mUchwytTimera;      //obsługa timera

    private boolean mCzyWykonanCM = false;

    private Runnable mWykonanieTimera = new Runnable()
    {
        @Override
        public void run()
        {
            deleteCache(mKontekst);
            mUchwytTimera.postDelayed(mWykonanieTimera, mOpóźnienie);           //aby powtarzał się po każdym zakończeniu
        }
    };

    public CacheManaging(Context kontekst)
    {
        this.mKontekst = kontekst;
        deleteCache(mKontekst);

        startTimer();

        mCzyWykonanCM = true;
    }

    private void startTimer()
    {
        mOpóźnienie = 10000;
        mUchwytTimera = new Handler();
        mUchwytTimera.postDelayed(mWykonanieTimera, mOpóźnienie);
    }

    private static void deleteCache(Context kontekst)
    {
        try
        {
            File pamiecPodreczna = kontekst.getCacheDir();
            deleteCacheDirectory(pamiecPodreczna);

            Log.d(TAG, "Usunięto cache");
        }
        catch(Exception e)
        {
            Log.d(TAG, "Brak cache");
        }
    }

    private static boolean deleteCacheDirectory(File pamiecPodreczna)
    {
        if(pamiecPodreczna != null && pamiecPodreczna.isDirectory())
        {
            String[] podpliki = pamiecPodreczna.list();         //dzieci pamieciPodrecznej

            for(int i = 0; i < podpliki.length; i++)                //for(String podplik : podpliki)
            {
                boolean sukces = deleteCacheDirectory(new File(pamiecPodreczna, podpliki[i]));

                if(!sukces)
                    return false;
            }

            return pamiecPodreczna.delete();
        }
        else if(pamiecPodreczna != null && pamiecPodreczna.isFile())
            return pamiecPodreczna.delete();
        else
            return false;
    }

    protected void resume()
    {
        if(!mCzyWykonanCM)
        {
            mUchwytTimera.postDelayed(mWykonanieTimera, mOpóźnienie);
            mCzyWykonanCM = true;
        }
    }

    protected void pause()
    {
        if(mCzyWykonanCM)
        {
            mUchwytTimera.removeCallbacks(mWykonanieTimera);
            mCzyWykonanCM = false;
        }

        deleteCache(mKontekst);
    }
}
