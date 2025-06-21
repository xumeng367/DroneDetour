package com.dronesky.detour;

import org.locationtech.jts.geom.*;

import java.util.List;

public class ClosestNoFlyZoneDetector {
    private static final double SEARCH_DISTANCE = 5000.0; // 向前延伸5km用于检测
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static double minDistance;

    /**
     * 查找当前航线方向最近相交的禁飞区
     */
    public static Polygon findClosestNoFlyZoneAlongPath(MyLatLng dronePos, double headingDegrees, List<Polygon> noFlyZones) {
        if (noFlyZones == null || noFlyZones.isEmpty()) {
            return null;
        }
        LineString flightPath = generatePathLine(dronePos, headingDegrees, SEARCH_DISTANCE);
        Point startPoint = geometryFactory.createPoint(new Coordinate(dronePos.latitude, dronePos.longitude));

        minDistance = Double.MAX_VALUE;
        Polygon closestZone = null;

        for (Polygon zone : noFlyZones) {
            if (flightPath.intersects(zone)) {
                Geometry intersection = flightPath.intersection(zone);
                Coordinate[] coords = intersection.getCoordinates();

                for (Coordinate coord : coords) {
                    double dist = startPoint.distance(geometryFactory.createPoint(coord)) * 111000; // 米
                    if (dist < minDistance) {
                        minDistance = dist;
                        closestZone = zone;
                    }
                }
            }
        }
        return closestZone;
    }

    /**
     * 根据航向和起点生成一条直线（模拟飞行路径）
     */
    private static LineString generatePathLine(MyLatLng start, double headingDegrees, double distanceMeters) {
        // 计算终点经纬度（大约值，适合短距离）
        double earthRadius = 6371000; // m
        double angularDistance = distanceMeters / earthRadius;

        double lat1 = Math.toRadians(start.latitude);
        double lon1 = Math.toRadians(start.longitude);
        double bearing = Math.toRadians(headingDegrees);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(angularDistance) +
                Math.cos(lat1) * Math.sin(angularDistance) * Math.cos(bearing));
        double lon2 = lon1 + Math.atan2(
                Math.sin(bearing) * Math.sin(angularDistance) * Math.cos(lat1),
                Math.cos(angularDistance) - Math.sin(lat1) * Math.sin(lat2));

        lat2 = Math.toDegrees(lat2);
        lon2 = Math.toDegrees(lon2);

        Coordinate startCoord = new Coordinate(start.latitude, start.longitude);
        Coordinate endCoord = new Coordinate(lat2, lon2);

        return geometryFactory.createLineString(new Coordinate[]{startCoord, endCoord});
    }

    public static double getMinDistance() {
        return minDistance;
    }

    /**
     * get min distance from safe fence
     */
    public static double getDistanceWithSafeFenceWhenFlying(MyLatLng dronePos, double headingDegrees, Polygon safeFence) {
        if (safeFence == null || safeFence.isEmpty()) {
            return Double.MAX_VALUE;
        }
        LineString flightPath = generatePathLine(dronePos, headingDegrees, SEARCH_DISTANCE);
        Coordinate startCoordinate = new Coordinate(dronePos.latitude, dronePos.longitude);
        Point startPoint = geometryFactory.createPoint(startCoordinate);

        double distanceToFence = Double.MAX_VALUE;

        if (flightPath.intersects(safeFence)) {
            Geometry intersection = flightPath.intersection(safeFence);
            Coordinate[] coords = intersection.getCoordinates();
//            Log.d("xss", "getDistanceWithSafeFenceWhenFlying: coords = " + coords.length);
//            Log.d("xss", "getDistanceWithSafeFenceWhenFlying: coords = " + startPoint);
//
            for (Coordinate coord : coords) {
//                Log.d("xss", "getDistanceWithSafeFenceWhenFlying: coord = " + coord + ", isValid = " + coord.isValid() + ", coord.equals(safeFence) " + coord.equals(startCoordinate));
                double dist = startPoint.distance(geometryFactory.createPoint(coord)) * 111000; // 米
//                Log.d("xss", "getDistanceWithSafeFenceWhenFlying: dist = " + dist);
                if (coord.equals(startCoordinate)) {
                    continue;
                }
                if (dist < distanceToFence) {
                    distanceToFence = dist;
                }
            }
        }
        return distanceToFence;
    }
}