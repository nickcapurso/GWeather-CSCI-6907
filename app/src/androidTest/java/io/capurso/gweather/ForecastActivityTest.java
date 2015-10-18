package io.capurso.gweather;

import android.app.Instrumentation;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

/**
 * Created by Nick on 10/18/2015.
 */
public class ForecastActivityTest extends ActivityInstrumentationTestCase2<ForecastActivity>{
    private static final String BUNDLE_KEY_FORECAST_LIST = "forecastList";
    private static final String BUNDLE_KEY_LOCATION_TITLE = "locationTitle";
    private static final String BUNDLE_KEY_LOCATION_WRAPPER = "locationWrapper";
    private static final String BUNDLE_KEY_CURR_TEMP = "currTemp";

    private ForecastActivity mForecastActivity;
    private Instrumentation mInstrumentation;
    private Solo mSolo;

    public ForecastActivityTest() {
        super(ForecastActivity.class);
    }

    @Override
    protected void setUp() throws Exception{
        super.setUp();
        mForecastActivity = getActivity();
        mSolo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        mSolo.finishOpenedActivities();
    }

    public void testPreconditions(){
        assertNotNull("mForecastActivity is null", mForecastActivity);
    }

    public void testSupportToolbarShouldExist(){
        assertNotNull("Support toolbar is null", mForecastActivity.getSupportActionBar());
    }

    public void testBaseWidgetsShouldExist(){
        assertNotNull("Main layout is null", mForecastActivity.findViewById(R.id.llForecast));
        assertNotNull("RecyclerView is null", mForecastActivity.findViewById(R.id.rvForecast));
        assertNotNull("List Adapter is null", mForecastActivity.getAdapter());
        assertNotNull("Forecast Array is null", mForecastActivity.getForecastInfo());
        assertNotNull("Banner Manager is null", mForecastActivity.getBannerManager());
    }

    public void testOnSavedInstanceStateBundle(){
        mInstrumentation = getInstrumentation();

        final Bundle outState = new Bundle();
        mInstrumentation.callActivityOnSaveInstanceState(mForecastActivity, outState);

        if(mForecastActivity.getForecastInfo().size() == 0){
            assertNull(outState.getParcelableArrayList(BUNDLE_KEY_FORECAST_LIST));
        }else{
            assertNotNull("ForecastInfo size > 0, but array not saved in bundle", outState.getParcelableArrayList(BUNDLE_KEY_FORECAST_LIST));
            assertNotNull("ForecastInfo size > 0, but current location not saved in bundle", outState.getParcelable(BUNDLE_KEY_LOCATION_WRAPPER));
            assertNotNull("ForecastInfo size > 0, but current temp not saved in bundle", outState.getString(BUNDLE_KEY_CURR_TEMP));
            assertNotNull("ForecastInfo size > 0, but location title not saved in bundle", outState.getString(BUNDLE_KEY_LOCATION_TITLE));
        }
    }

    public void testSettingsActivityLaunched() {
        mSolo.clickOnMenuItem(mForecastActivity.getResources().getString(R.string.action_settings));
        mSolo.sleep(2000);
        mSolo.assertCurrentActivity("Settings failed to open", SettingsActivity.class);
    }
}
