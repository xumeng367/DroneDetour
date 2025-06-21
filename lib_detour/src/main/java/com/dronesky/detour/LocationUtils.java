package com.dronesky.detour;


import android.location.Location;


/**
 * Create by xumeng on 2021/11/16
 */
public class LocationUtils {

    /**
     * Calculate the distance between two points
     *
     * @param lon1
     * @param lat1
     * @param lon2
     * @param lat2
     * @return
     */
    public static double getDistance(double lon1, double lat1, double lon2, double lat2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }


    /**
     * Calculate the direction between two latitude and longitude points
     *
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff = Math.toRadians(longitude2 - longitude1);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);
        double deg = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
        // 控制区间在 [-180, 180]
        if (deg > 180) {
            deg = deg - 360;
        } else {
            if (deg < -180) {
                deg = deg + 360;
            }
        }
        return deg;
    }

}
