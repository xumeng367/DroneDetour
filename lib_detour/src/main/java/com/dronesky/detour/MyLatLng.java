package com.dronesky.detour;

import java.util.Objects;

public class MyLatLng {
    public double latitude;
    public double longitude;

    public MyLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MyLatLng myLatLng = (MyLatLng) o;
        return Double.compare(latitude, myLatLng.latitude) == 0 && Double.compare(longitude, myLatLng.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double distanceTo(MyLatLng other) {
//        double latDiff = this.latitude - other.latitude;
//        double lonDiff = this.longitude - other.longitude;
//        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
        final double R = 6371e3; // 地球半径（米）
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dlat = Math.toRadians(other.latitude - this.latitude);
        double dlon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dlon / 2) * Math.sin(dlon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // m
    }


    @Override
    public String toString() {
        return "LatLng{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}