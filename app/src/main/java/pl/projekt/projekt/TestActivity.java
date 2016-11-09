package pl.projekt.projekt;

import android.app.Activity;
import android.os.Bundle;

public class TestActivity extends Activity
{
    private static final String TAG = "TAG";

    private GeoMathematicsPOM mGM;
    private CacheManaging mCM;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        mGM = new GeoMathematicsPOM(this);
        mCM = new CacheManaging(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mGM.pause();
        mCM.pause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mGM.resume();
        mCM.resume();
    }
}
