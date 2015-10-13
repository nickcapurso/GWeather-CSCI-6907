package io.capurso.gweather.location;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Nick on 10/3/2015.
 */
public class LocationWrapper implements Parcelable {
    public Location location;
    public String address;
    public String searchString;

    public LocationWrapper(Location location, String address, String searchString){
        this.location = location;
        this.address = address;
        this.searchString = searchString;
    }

    public LocationWrapper(Parcel parcel){
        location = new Location("");
        location.setLatitude(parcel.readDouble());
        location.setLongitude(parcel.readDouble());
        address = parcel.readString();
        searchString = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(location.getLatitude());
        dest.writeDouble(location.getLongitude());
        dest.writeString(address);
        dest.writeString(searchString);
    }

    //Formally used to call the constructor to recreate an AddressInfo from a parcel
    public static final Creator CREATOR = new Creator() {
        @Override
        public LocationWrapper createFromParcel(Parcel source) {
            return new LocationWrapper(source);
        }

        @Override
        public LocationWrapper[] newArray(int size) {
            return new LocationWrapper[size];
        }
    };
}
