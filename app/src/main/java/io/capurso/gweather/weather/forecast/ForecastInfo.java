package io.capurso.gweather.weather.forecast;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds information about the forecast of a given day. The more "detailed" information is
 * shown while in landscape mode or when clicking on a list item in portrait mode.
 *
 * Implements Parcelable so it can persist across screen rotations.
 */
public class ForecastInfo implements Parcelable{
    public String day, weatherDesc, lowHigh, formalDesc, windDir, iconUrl;
    public int aveWind, maxWind, humidity;
    public double rainIn, snowIn;

    public ForecastInfo(){

    }

    public ForecastInfo(String day, String weatherDesc, String lowHigh, String iconUrl){
        this.day = day;
        this.weatherDesc = weatherDesc;
        this.lowHigh = lowHigh;
        this.iconUrl = iconUrl;
    }

    //Reconstruct in the same order the data was written out in
    public ForecastInfo(Parcel parcel){
        day = parcel.readString();
        weatherDesc = parcel.readString();
        lowHigh = parcel.readString();
        iconUrl = parcel.readString();
        formalDesc = parcel.readString();
        windDir = parcel.readString();
        aveWind = parcel.readInt();
        maxWind = parcel.readInt();
        humidity = parcel.readInt();
        rainIn = parcel.readDouble();
        snowIn = parcel.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(day);
        dest.writeString(weatherDesc);
        dest.writeString(lowHigh);
        dest.writeString(iconUrl);
        dest.writeString(formalDesc);
        dest.writeString(windDir);
        dest.writeInt(aveWind);
        dest.writeInt(maxWind);
        dest.writeInt(humidity);
        dest.writeDouble(rainIn);
        dest.writeDouble(snowIn);
    }

    //Formally used to call the constructor to recreate an AddressInfo from a parcel
    public static final Creator CREATOR = new Creator() {
        @Override
        public ForecastInfo createFromParcel(Parcel source) {
            return new ForecastInfo(source);
        }

        @Override
        public ForecastInfo[] newArray(int size) {
            return new ForecastInfo[size];
        }
    };
    
    @Override
    public String toString(){
        return "Day: " + day + "\nCondition: " + weatherDesc + "\nLow/High: " + lowHigh 
                + "\nHumidity: " + humidity + "\nRain In: " + rainIn + "\nAvg Wind: " + aveWind
                + "\nMax Wind: " + maxWind + "\nWind Dir: " + windDir + "\nSnow In: " + snowIn
                + "\nIcon URL: " + iconUrl + "\nFull Desc: " + formalDesc;
    }
}
