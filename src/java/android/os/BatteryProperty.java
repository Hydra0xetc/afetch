package android.os;

public class BatteryProperty implements Parcelable {
  private long mValue;

  public long getLong() {
    return mValue;
  }

  public void readFromParcel(Parcel in) {
    mValue = in.readLong();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(mValue);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<BatteryProperty> CREATOR =
    new Creator<>() {
      @Override
      public BatteryProperty createFromParcel(Parcel in) {
        BatteryProperty p = new BatteryProperty();
        p.readFromParcel(in);
        return p;
      }

      @Override
      public BatteryProperty[] newArray(int size) {
        return new BatteryProperty[size];
      }
    };
}
