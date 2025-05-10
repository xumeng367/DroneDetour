package com.dronesky.detour;


import android.util.Log;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.util.List;

public class GeoUtils {
    private static final String TAG = "GeoUtils";
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    // 生成 Polygon（禁飞区）
    public static Polygon createPolygon(List<MyLatLng> coordinates) {
        Coordinate[] coords = new Coordinate[coordinates.size() + 1];
        for (int i = 0; i < coordinates.size(); i++) {
            coords[i] = new Coordinate(coordinates.get(i).latitude, coordinates.get(i).longitude);
        }
        coords[coordinates.size()] = new Coordinate(coordinates.get(0).latitude, coordinates.get(0).longitude);
        return geometryFactory.createPolygon(coords);
    }

    /**
     * 起点、终点是否经过禁飞区
     *
     * @param start
     * @param end
     * @param noFlyZones
     * @return
     */
    public static boolean intersectsNoFlyZone(MyLatLng start, MyLatLng end, List<Polygon> noFlyZones) {
        LineString path = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(start.latitude, start.longitude),
                new Coordinate(end.latitude, end.longitude)
        });

        for (Polygon noFlyZone : noFlyZones) {
            if (path.intersects(noFlyZone)) {
                return true;
            }
        }
        return false;
    }

    public static boolean intersectsNoFlyZone(List<MyLatLng> wayPoints, List<Polygon> noFlyZones) {
        for (int i = 0; i < wayPoints.size() - 1; i++) {
            MyLatLng start = wayPoints.get(i);
            MyLatLng end = wayPoints.get(i + 1);
            LineString path = geometryFactory.createLineString(new Coordinate[]{
                    new Coordinate(start.latitude, start.longitude),
                    new Coordinate(end.latitude, end.longitude)
            });
            Log.d(TAG, "Intersect 点 （ " + i + ", " + end + ")经过禁飞区");
            for (Polygon noFlyZone : noFlyZones) {
                if (path.intersects(noFlyZone)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 检查点是否在禁飞区内
     *
     * @param point
     * @param noFlyZones
     * @return
     */
    public static boolean isInsideNoFlyZone(MyLatLng point, List<Polygon> noFlyZones) {
        for (Polygon zone : noFlyZones) {
            if (zone.contains(GeoUtils.createPoint(point.latitude, point.longitude))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInsidePolygon(MyLatLng point, Polygon polygon) {
        if (polygon == null) {
            return true;
        }
        return polygon.contains(GeoUtils.createPoint(point.latitude, point.longitude));
    }


    /**
     * 检查从 start 到 end 的路径是否完全在安全区域 Polygon 内
     *
     * @param start
     * @param end
     * @param safeZone
     * @return
     */
    public static boolean isPathWithinSafeZone(MyLatLng start, MyLatLng end, Polygon safeZone) {
        if (safeZone == null) {
            return true;
        }
        Coordinate coordA = new Coordinate(start.latitude, start.longitude);
        Coordinate coordB = new Coordinate(end.latitude, end.longitude);

        LineString pathLine = geometryFactory.createLineString(new Coordinate[]{coordA, coordB});

        // 判断整条路径是否被安全区包含
        return safeZone.contains(pathLine);
    }

    public static boolean isPathWithinSafeZone(List<MyLatLng> wayPoints, Polygon safeZone) {
        for (int i = 0; i < wayPoints.size() - 1; i++) {
            MyLatLng start = wayPoints.get(i);
            MyLatLng end = wayPoints.get(i + 1);
            if (isPathWithinSafeZone(start, end, safeZone)) {
                return false;
            }
        }
        return true;
    }


    // 计算两点之间的球面距离（哈弗辛公式）
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // 地球半径 (米)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // 创建 JTS 点对象
    public static Point createPoint(double latitude, double longitude) {
        return geometryFactory.createPoint(new Coordinate(latitude, longitude));
    }

    public static double getShortestDistanceBetweenEdges(Polygon inner, Polygon outer) {
        // 外壳的边界
        if (outer == null) {
            return 0;
        }
        Geometry shellA = inner.getExteriorRing();
        Geometry shellB = outer.getExteriorRing();

        // 计算最近点
        Coordinate[] closestPoints = DistanceOp.nearestPoints(shellA, shellB);

        Coordinate firstCoordinate = closestPoints[0];
        Coordinate secondCoordinate = closestPoints[1];
        // 打印点
        Log.d(TAG, "getShortestDistanceBetweenEdges A边界点：" + firstCoordinate);
        Log.d(TAG, "getShortestDistanceBetweenEdges B边界点：" + secondCoordinate);

        double distance = haversine(firstCoordinate.x, firstCoordinate.y, secondCoordinate.x, secondCoordinate.y);
        Log.d(TAG, "getShortestDistanceBetweenEdges 最小距离：" + closestPoints[1]);
        return distance;
    }
}