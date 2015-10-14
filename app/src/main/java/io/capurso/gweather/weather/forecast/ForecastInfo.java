package io.capurso.gweather.weather.forecast;


import android.os.Parcel;
import android.os.Parcelable;

public class ForecastInfo implements Parcelable{
    public String day, weatherDesc, lowHigh;
    public String iconUrl;

    public ForecastInfo(String day, String weatherDesc, String lowHigh, String iconUrl){
        this.day = day;
        this.weatherDesc = weatherDesc;
        this.lowHigh = lowHigh;
        this.iconUrl = iconUrl;
    }

    public ForecastInfo(Parcel parcel){
        day = parcel.readString();
        weatherDesc = parcel.readString();
        lowHigh = parcel.readString();
        iconUrl = parcel.readString();
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
}
