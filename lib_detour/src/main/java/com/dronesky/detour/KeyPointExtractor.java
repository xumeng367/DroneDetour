package com.dronesky.detour;

import android.util.Log;

import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

public class KeyPointExtractor {
    private static final String TAG = "KeyPointExtractor";
    public static final double ONE_METER_OFFSET = 0.00000899322;
    public static double SAFETY_DISTANCE_METERS = 30; // safe buffer

    /**
     * It does not intersect with the no-fly zone and does not handle the extraction of key path points,
     * including the starting point, the ending point, and the boundary points of the no-fly zone (with an increase in safety distance)
     */
    public static List<MyLatLng> extractKeyPoints(MyLatLng start, MyLatLng end, List<Polygon> noFlyZones, double bufferMeters) {
        List<MyLatLng> keyPoints = new ArrayList<>();
        keyPoints.add(start);
        keyPoints.add(end);
        GeometryFactory factory = new GeometryFactory();

        LineString startEndLine = factory.createLineString(new Coordinate[]{
                new Coordinate(start.latitude, start.longitude),
                new Coordinate(end.latitude, end.longitude)
        });

        for (Polygon zone : noFlyZones) {
            if (zone.intersects(startEndLine)) {
                 Log.d(TAG,"Intersect intersect with the no-fly zone：" + zone);
                // get Polygon border
                keyPoints.addAll(getBufferedPolygonBoundaryPoints(zone, bufferMeters));
            } else {
                 Log.d(TAG,"Intersect Does not intersect with the no-fly zone and is not handled." + zone);
            }
        }
        return keyPoints;
    }

    /**
     * Obtain the boundary points of the no-fly zone and increase the safety distance (example 20m buffer zone)
     */
    private static List<MyLatLng> getBufferedPolygonBoundaryPoints(Polygon polygon, double bufferMeters) {
        List<MyLatLng> boundaryPoints = new ArrayList<>();
//         Log.d(TAG,"getBufferedPolygonBoundaryPoints 原始polygon = " + polygon);
        // calculate buffer
        double minDistance = GeoUtils.getShortestDistanceBetweenEdges(polygon, DetourPathManager.getsInstance().getGeoFencePolygon());
        double finalBufferMeters = minDistance > bufferMeters ? bufferMeters : (bufferMeters != 0 ? ((minDistance > 2) ? minDistance - 1 : 1) : 0);
        double bufferDegrees = metersToDegrees(finalBufferMeters);
        Geometry bufferedPolygon = polygon.buffer(bufferDegrees); // Expand the no-fly zone
         Log.d(TAG,"getBufferedPolygonBoundaryPoints  minDistance = " + minDistance + ". bufferMeters = " + bufferMeters + ", finalBufferMeters = " + finalBufferMeters);
        if (bufferedPolygon instanceof Polygon expandedZone) {
//             Log.d(TAG,"getBufferedPolygonBoundaryPoints bufferedPolygon = " + expandedZone);
            Coordinate[] coordinates = expandedZone.getExteriorRing().getCoordinates();
            for (Coordinate coord : coordinates) {
                boundaryPoints.add(new MyLatLng(coord.x, coord.y)); // GeoTools use x=lon, y=lat
            }
        }
        return boundaryPoints;
    }

    private static double metersToDegrees(double meters) {
        return ONE_METER_OFFSET * meters;
    }
}