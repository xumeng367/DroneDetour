package com.dronesky.detour;

import android.util.Log;

import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

public class KeyPointExtractor {
    private static final String TAG = "KeyPointExtractor";
    public static final double ONE_METER_OFFSET = 0.00000899322;
    public static double SAFETY_DISTANCE_METERS = 30; // 20m 安全距离

    /**
     * 提取关键路径点，包括起点、终点、禁飞区边界点（增加安全距离）
     */
    public static List<MyLatLng> extractKeyPoints(MyLatLng start, MyLatLng end, List<Polygon> noFlyZones, double bufferMeters) {
        List<MyLatLng> keyPoints = new ArrayList<>();
        // 添加起点和终点
        keyPoints.add(start);
        keyPoints.add(end);
        GeometryFactory factory = new GeometryFactory();

        LineString startEndLine = factory.createLineString(new Coordinate[]{
                new Coordinate(start.latitude, start.longitude),
                new Coordinate(end.latitude, end.longitude)
        });

        // 提取禁飞区边界点（带 20m 安全距离）
        for (Polygon zone : noFlyZones) {
            if (zone.intersects(startEndLine)) {
                 Log.d(TAG,"Intersect 和禁飞区相交：" + zone);
                // 获取 Polygon 的边界
                keyPoints.addAll(getBufferedPolygonBoundaryPoints(zone, bufferMeters));
            } else {
                 Log.d(TAG,"Intersect 和禁飞区不相交，不处理" + zone);
            }
        }
        return keyPoints;
    }

    /**
     * 获取禁飞区边界点，并增加安全距离（20m 缓冲区）
     */
    private static List<MyLatLng> getBufferedPolygonBoundaryPoints(Polygon polygon, double bufferMeters) {
        List<MyLatLng> boundaryPoints = new ArrayList<>();
//         Log.d(TAG,"getBufferedPolygonBoundaryPoints 原始polygon = " + polygon);
        // 计算缓冲区（20m）
        double minDistance = GeoUtils.getShortestDistanceBetweenEdges(polygon, DetourGoHomeManager.getsInstance().getGeoFencePolygon());
        double finalBufferMeters = minDistance > bufferMeters ? bufferMeters : (bufferMeters != 0 ? ((minDistance > 2) ? minDistance - 1 : 1) : 0);
        double bufferDegrees = metersToDegrees(finalBufferMeters);
        Geometry bufferedPolygon = polygon.buffer(bufferDegrees); // 扩展禁飞区
         Log.d(TAG,"getBufferedPolygonBoundaryPoints 最新距离 minDistance = " + minDistance + ". bufferMeters = " + bufferMeters + ", finalBufferMeters = " + finalBufferMeters);
        if (bufferedPolygon instanceof Polygon expandedZone) {
//             Log.d(TAG,"getBufferedPolygonBoundaryPoints bufferedPolygon = " + expandedZone);
            Coordinate[] coordinates = expandedZone.getExteriorRing().getCoordinates();
            for (Coordinate coord : coordinates) {
                boundaryPoints.add(new MyLatLng(coord.x, coord.y)); // GeoTools 使用 x=lon, y=lat
            }
        }
        return boundaryPoints;
    }

    /**
     * 将米转换为经纬度（近似计算）
     */
    private static double metersToDegrees(double meters) {
        return ONE_METER_OFFSET * meters;
    }
}