package com.dronesky.detour;

import android.util.Log;

import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * DetourPathManager
 */
public class DetourPathManager {
    private static final String TAG = "DetourPathManager";
    public static final boolean ENABLE = true;
    private static final DetourPathManager sInstance = new DetourPathManager();
    private final List<Polygon> mNonFlyZones = new ArrayList<>();
    private Polygon mGeoFencePolygon;
    private boolean mIsDetouringGoHome = false;
    private boolean mIsDetouringWaypoint = false;
    private int mDetouringWaypointSize = 0;

    private DetourPathManager() {

    }

    public static DetourPathManager getsInstance() {
        return sInstance;
    }


    /**
     * Obtain a no-fly zone
     *
     * @return
     */
    public List<Polygon> getNonFlyZones() {
        return mNonFlyZones;
    }

    public Polygon getGeoFencePolygon() {
        return mGeoFencePolygon;
    }

    /**
     * Is there a no-fly zone?
     *
     * @return
     */
    public boolean hasValidNonFlyZones() {
        return !mNonFlyZones.isEmpty();
    }

    public boolean isNeedDetourFlying(MyLatLng start, MyLatLng end) {
        if (!ENABLE) {
            Log.d(TAG, "isNeedDetourFlying not enable");
            return false;
        }
        if (start.latitude == end.latitude && start.longitude == end.longitude) {
            Log.d(TAG, "isNeedDetourFlying The start and end points are the same, and there is no need for a detour.");
            return false;
        }
        boolean isCurrentInsideNoFlyZone = GeoUtils.isInsideNoFlyZone(start, getNonFlyZones());
        boolean isCurrentInsideFence = GeoUtils.isInsidePolygon(start, getGeoFencePolygon());
        if (isCurrentInsideNoFlyZone || !isCurrentInsideFence) {
            Log.d(TAG, "isNeedDetourFlying isCurrentInsideNoFlyZone：" + isCurrentInsideNoFlyZone + ", isCurrentInsideFence = " + isCurrentInsideFence);
            return false;
        }
        boolean isPathInGeoFence = GeoUtils.isPathWithinSafeZone(start, end, mGeoFencePolygon);
        boolean intersectsNoFlyZone = GeoUtils.intersectsNoFlyZone(start, end, mNonFlyZones);
        Log.d(TAG, "isNeedDetourFlying isPathInGeoFence = " + isPathInGeoFence + ", intersectsNoFlyZone = " + intersectsNoFlyZone);
        return intersectsNoFlyZone || !isPathInGeoFence;
    }

    public List<MyLatLng> calculateDetourPath(MyLatLng start, MyLatLng end) {
        Log.d(TAG, "calculateDetourPath start = " + start + ", end = " + end);
        List<MyLatLng> paths = new ArrayList<>();
        paths.add(start);
        paths.add(end);
        return calculateDetourPath(paths);
    }

    public List<MyLatLng> calculateDetourPath(List<MyLatLng> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            Log.d(TAG, "calculateDetourPath invalid");
            return null;
        }
        double directDistance = getDistance(waypoints);
        boolean isCrossOutSideFence = GeoUtils.isPathWithinSafeZone(waypoints, mGeoFencePolygon);
        boolean intersectsNoFlyZone = GeoUtils.intersectsNoFlyZone(waypoints, mNonFlyZones);
        Log.d(TAG, "calculateDetourPath isCrossOutSideFence = " + isCrossOutSideFence + ", intersectsNoFlyZone = " + intersectsNoFlyZone);
        List<MyLatLng> detourFencePath = null;
        if (isCrossOutSideFence) {
            long fenceStarTime = System.currentTimeMillis();
            List<Polygon> geoFences = new ArrayList<>();
            geoFences.add(mGeoFencePolygon);
            Log.d(TAG, "calculateDetourPath After passing beyond the safety fence, the flight will first undergo a circling maneuver.");
            detourFencePath = GraphUtils.findMultiSegmentPath(waypoints, geoFences, true);
            long fenceEndTime = System.currentTimeMillis();
            Log.d(TAG, "calculateDetourPath The flight path around the fence and the duration of it：" + (fenceEndTime - fenceStarTime));
            if (detourFencePath == null) {
                Log.d(TAG, "calculateDetourPath The situation outside the fence failed to be bypassed. ");
                return null;
            } else {
                Log.d(TAG, "calculateDetourPath Outside the fence, the flight path around the perimeter size：" + detourFencePath.size() + ",cost：" + (fenceEndTime - fenceStarTime));
                Log.d(TAG, "calculateDetourPath Outside the fence, the flight path around the perimeter：" + detourFencePath);
                Log.d(TAG, "calculateDetourPath Distance from the fence to the direct flight point：" + directDistance + ", The distance after circling away：" + getDistance(detourFencePath));
            }
        } else {
            detourFencePath = waypoints;
        }

        long noFlyZoneStartTime = System.currentTimeMillis();
        Log.d(TAG, "calculateDetourPath Start calculating the circling route");

        List<MyLatLng> path = GraphUtils.findMultiSegmentPath(detourFencePath, mNonFlyZones, false);
        long noFlyZoneEndTime = System.currentTimeMillis();
        if (path == null || path.isEmpty()) {
            Log.d(TAG, "calculateDetourPath fail");
            return null;
        }
        Log.d(TAG, "calculateDetourPath No-fly zone  -  Flight avoidance route size：" + path.size() + ", cost：" + (noFlyZoneEndTime - noFlyZoneStartTime));
        Log.d(TAG, "calculateDetourPath No-fly zone  -  Flight avoidance route：" + path);
        Log.d(TAG, "calculateDetourPath No-fly zone  Direct flight distance：" + directDistance + ", The distance after detour：" + getDistance(path));

        Log.d(TAG, "calculateDetourPath Final route size：" + path.size());
        Log.d(TAG, "calculateDetourPath Final route size：" + path);

        boolean isCrossOutSideFenceWithPath = GeoUtils.isPathWithinSafeZone(path, mGeoFencePolygon);
        boolean intersectsNoFlyZoneWithPath = GeoUtils.intersectsNoFlyZone(path, mNonFlyZones);
        Log.d(TAG, "calculateDetourPath isCrossOutSideFenceWithPath：" + isCrossOutSideFenceWithPath + ", intersectsNoFlyZoneWithPath：" + intersectsNoFlyZoneWithPath);
        if (isCrossOutSideFenceWithPath || intersectsNoFlyZoneWithPath) {
            return calculateDetourPath(path);
        }
        return path;
    }

    public double getDistance(List<MyLatLng> paths) {
        if (paths.size() < 2) {
            return 0;
        }
        double distance = 0;
        for (int i = 0; i < paths.size() - 1; i++) {
            MyLatLng first = paths.get(i);
            MyLatLng second = paths.get(i + 1);
            distance += LocationUtils.getDistance(first.longitude, first.latitude, second.longitude, second.latitude);
        }
        return distance;
    }

    public void updateNoFlyZones(List<List<MyLatLng>> noFlyZones) {
        mNonFlyZones.clear();
        for (int i = 0; i < noFlyZones.size(); i++) {
            List<MyLatLng> noFlyZone = noFlyZones.get(i);
            if (noFlyZone.size() < 3) {
                Log.d(TAG, "updateNoFlyZones: invalid noFlyZones:" + noFlyZone);
            } else {
                mNonFlyZones.add(GeoUtils.createPolygon(noFlyZone));
            }
        }
    }

    public void reset() {
        Log.d(TAG, "release");
        mIsDetouringGoHome = false;
        mIsDetouringWaypoint = false;
        mDetouringWaypointSize = 0;
    }
}
