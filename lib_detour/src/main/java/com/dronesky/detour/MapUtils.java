package com.dronesky.detour;

import java.util.List;

public class MapUtils {

    public static boolean isLineIntersectPolygon(MyLatLng start, MyLatLng end, List<MyLatLng> polygon) {
        int size = polygon.size();
        for (int i = 0; i < size; i++) {
            MyLatLng p1 = polygon.get(i);
            MyLatLng p2 = polygon.get((i + 1) % size);

            if (isSegmentsIntersect(start, end, p1, p2)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSegmentsIntersect(MyLatLng p1, MyLatLng p2, MyLatLng q1, MyLatLng q2) {
        double d1 = crossProduct(q1, q2, p1);
        double d2 = crossProduct(q1, q2, p2);
        double d3 = crossProduct(p1, p2, q1);
        double d4 = crossProduct(p1, p2, q2);

        return (d1 * d2 < 0) && (d3 * d4 < 0);
    }

    private static double crossProduct(MyLatLng a, MyLatLng b, MyLatLng c) {
        return (b.longitude - a.longitude) * (c.latitude - a.latitude)
                - (b.latitude - a.latitude) * (c.longitude - a.longitude);
    }
}