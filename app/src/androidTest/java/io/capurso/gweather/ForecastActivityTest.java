package io.capurso.gweather;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;

import com.robotium.solo.Solo;

import org.mockito.Mockito;

import java.util.ArrayList;

import io.capurso.gweather.location.LocationWrapper;
import io.capurso.gweather.weather.WeatherManager;
import io.capurso.gweather.weather.forecast.ForecastInfo;

/**
 * Created by Nick on 10/18/2015.
 */
public class ForecastActivityTest extends ActivityInstrumentationTestCase2<ForecastActivity>{
    private static final String BUNDLE_KEY_FORECAST_LIST = "forecastList";
    private static final String BUNDLE_KEY_LOCATION_TITLE = "locationTitle";
    private static final String BUNDLE_KEY_LOCATION_WRAPPER = "locationWrapper";
    private static final String BUNDLE_KEY_CURR_TEMP = "currTemp";
    
    private Solo mSolo;

    public ForecastActivityTest() {
        super(ForecastActivity.class);
    }

    @Override
    protected void setUp() throws Exception{
        super.setUp();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        mSolo = new Solo(getInstrumentation(), getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        mSolo.finishOpenedActivities();
    }

    public void testPreconditions(){
        assertNotNull("((ForecastActivity)mSolo.getCurrentActivity()) is null", mSolo.getCurrentActivity());
    }

    public void testSupportToolbarShouldExist(){
        assertNotNull("Support toolbar is null", ((ForecastActivity)mSolo.getCurrentActivity()).getSupportActionBar());
    }

    public void testBaseWidgetsShouldExistInPortrait(){
        assertNotNull("Main layout is null", (mSolo.getCurrentActivity()).findViewById(R.id.llForecast));
        assertNotNull("RecyclerView is null", (mSolo.getCurrentActivity()).findViewById(R.id.rvForecast));
        assertNotNull("List Adapter is null", ((ForecastActivity)mSolo.getCurrentActivity()).getAdapter());
        assertNotNull("Forecast Array is null", ((ForecastActivity)mSolo.getCurrentActivity()).getForecastInfo());
        assertNotNull("Banner Manager is null", ((ForecastActivity)mSolo.getCurrentActivity()).getBannerManager());
        assertNotNull("LinearLayoutManager is null", ((ForecastActivity)mSolo.getCurrentActivity()).getRecyclerLayoutManager());
        assertEquals("LinearLayoutManager is horizontal in portrait mode", LinearLayoutManager.VERTICAL, ((ForecastActivity) mSolo.getCurrentActivity()).getRecyclerLayoutManager().getOrientation());
    }

    public void testBaseWidgetsShouldExistInLandscape(){
        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.sleep(500);

        assertNotNull("Main layout is null", (mSolo.getCurrentActivity()).findViewById(R.id.llForecast));
        assertNotNull("RecyclerView is null", (mSolo.getCurrentActivity()).findViewById(R.id.rvForecast));
        assertNotNull("List Adapter is null", ((ForecastActivity)mSolo.getCurrentActivity()).getAdapter());
        assertNotNull("Forecast Array is null", ((ForecastActivity)mSolo.getCurrentActivity()).getForecastInfo());
        assertNotNull("Banner Manager is null", ((ForecastActivity)mSolo.getCurrentActivity()).getBannerManager());
        assertNotNull("LinearLayoutManager is null", ((ForecastActivity)mSolo.getCurrentActivity()).getRecyclerLayoutManager());
        assertEquals("LinearLayoutManager is vertical in landscape mode", LinearLayoutManager.HORIZONTAL, ((ForecastActivity)mSolo.getCurrentActivity()).getRecyclerLayoutManager().getOrientation());
    }

    @UiThreadTest
    public void testOnSavedInstanceWithEmptyForecastInfo(){
        final Bundle outState = new Bundle();
        getInstrumentation().callActivityOnSaveInstanceState(( mSolo.getCurrentActivity()), outState);
        assertNull(outState.getParcelableArrayList(BUNDLE_KEY_FORECAST_LIST));
    }

    @UiThreadTest
    public void testOnSavedInstanceWithNonEmptyForecastInfo(){
        final String testString = "67F";
        final String testTitle = "New York";

        ArrayList<ForecastInfo> forecastInfos = new ArrayList<ForecastInfo>();

        forecastInfos.add(Mockito.mock(ForecastInfo.class));

        LocationWrapper mockWrapper = Mockito.mock(LocationWrapper.class);

        ((ForecastActivity)mSolo.getCurrentActivity()).setForecastInfo(forecastInfos);
        ((ForecastActivity)mSolo.getCurrentActivity()).setCurrTemp(testString);
        ((ForecastActivity)mSolo.getCurrentActivity()).setCurrLocation(mockWrapper);
        ((ForecastActivity)mSolo.getCurrentActivity()).setLocationTitle(testTitle);

        final Bundle outState = new Bundle();
        getInstrumentation().callActivityOnSaveInstanceState(( mSolo.getCurrentActivity()), outState);

        assertEquals("onSaveInstanceState Not Saving mForecastInfos correctly", forecastInfos, outState.getParcelableArrayList(BUNDLE_KEY_FORECAST_LIST));
        assertEquals("onSaveInstanceState Not Saving mLocationWrapper correctly", mockWrapper, outState.getParcelable(BUNDLE_KEY_LOCATION_WRAPPER));
        assertEquals("onSaveInstanceState Not Saving mCurrTemp correctly", testString, outState.getString(BUNDLE_KEY_CURR_TEMP));
        assertEquals("onSaveInstanceState Not Saving location title correctly", testTitle, outState.getString(BUNDLE_KEY_LOCATION_TITLE));
    }

    public void testSettingsActivityLaunched() {
        mSolo.clickOnMenuItem((mSolo.getCurrentActivity()).getResources().getString(R.string.action_settings));
        mSolo.sleep(1000);
        mSolo.assertCurrentActivity("Settings failed to open", SettingsActivity.class);
    }

    @UiThreadTest
    public void testPostRotationWithNoForecastInfo(){
        final Bundle outState = new Bundle();
        getInstrumentation().callActivityOnSaveInstanceState((mSolo.getCurrentActivity()), outState);

        mSolo.setActivityOrientation(Solo.LANDSCAPE);
        mSolo.sleep(500);

        assertNull("WeatherManager recreated post-rotation even though forecast was never found", ((ForecastActivity) mSolo.getCurrentActivity()).getWeatherManager());
    }

    public void testCurrentTempReceivedLandscape(){
        final String testTemp = "67F";
        WeatherManager weatherMan = Mockito.mock(WeatherManager.class);
        BannerManager bannerMan = Mockito.mock(BannerManager.class);

        Mockito.when(weatherMan.getCurrTemp()).thenReturn(testTemp);

        testBaseWidgetsShouldExistInLandscape();

        ((ForecastActivity)mSolo.getCurrentActivity()).setWeatherManager(weatherMan);
        ((ForecastActivity)mSolo.getCurrentActivity()).setBannerManager(bannerMan);
        ((ForecastActivity)mSolo.getCurrentActivity()).onCurrentTempReceived(testTemp);

        Mockito.verify(weatherMan, Mockito.times(1)).getCurrTemp();
        Mockito.verify(bannerMan, Mockito.times(0)).setCurrentTemp(Mockito.any(String.class));
    }

    public void testCurrentTempReceivedPortrait(){
        final String testTemp = "67F";
        WeatherManager weatherMan = Mockito.mock(WeatherManager.class);
        BannerManager bannerMan = Mockito.mock(BannerManager.class);

        Mockito.when(weatherMan.getCurrTemp()).thenReturn(testTemp);

        testBaseWidgetsShouldExistInPortrait();

        ((ForecastActivity)mSolo.getCurrentActivity()).setWeatherManager(weatherMan);
        ((ForecastActivity)mSolo.getCurrentActivity()).setBannerManager(bannerMan);
        ((ForecastActivity)mSolo.getCurrentActivity()).onCurrentTempReceived(testTemp);

        Mockito.verify(weatherMan, Mockito.times(1)).getCurrTemp();
        Mockito.verify(bannerMan, Mockito.times(1)).setCurrentTemp(Mockito.any(String.class));
    }
}
