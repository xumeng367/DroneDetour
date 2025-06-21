package com.dronesky.detour;

import android.util.Log;

import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * 避障绕飞管理
 */
public class DetourPathManager {
    private static final String TAG = "DetourGoHomeManager";
    public static final boolean ENABLE = true;
    private static final DetourPathManager sInstance = new DetourPathManager();
    private final List<Polygon> mNonFlyZones = new ArrayList<>();
    private Polygon mGeoFencePolygon;
    private boolean mIsDetouringGoHome = false;
    private boolean mIsDetouringWaypoint = false;//是否航线绕飞
    private int mDetouringWaypointSize = 0;//绕飞点

    private DetourPathManager() {

    }

    public static DetourPathManager getsInstance() {
        return sInstance;
    }




    /**
     * 获取禁飞区
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
     * 是否有禁飞区
     *
     * @return
     */
    public boolean hasValidNonFlyZones() {
        return !mNonFlyZones.isEmpty();
    }

    public boolean isNeedDetourFlying(MyLatLng start, MyLatLng end) {
        if (!ENABLE) {
            Log.d(TAG, "isNeedDetourFlying 未开启");
            return false;
        }
        if (start.latitude == end.latitude && start.longitude == end.longitude) {
            Log.d(TAG, "isNeedDetourFlying 开始和终点相同，不需要绕行");
            return false;
        }
        boolean isCurrentInsideNoFlyZone = GeoUtils.isInsideNoFlyZone(start, getNonFlyZones());
        boolean isCurrentInsideFence = GeoUtils.isInsidePolygon(start, getGeoFencePolygon());
        if (isCurrentInsideNoFlyZone || !isCurrentInsideFence) {
            Log.d(TAG, "isNeedDetourFlying 当前点是否在禁飞区：" + isCurrentInsideNoFlyZone + ", 是否在围栏内 = " + isCurrentInsideFence + " ，不需要绕行");
            return false;
        }
        boolean isPathInGeoFence = GeoUtils.isPathWithinSafeZone(start, end, mGeoFencePolygon);
        boolean intersectsNoFlyZone = GeoUtils.intersectsNoFlyZone(start, end, mNonFlyZones);
        Log.d(TAG, "isNeedDetourFlying 是否经过围栏外 = " + !isPathInGeoFence + ", 是否经过禁飞区 = " + intersectsNoFlyZone);
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
            Log.d(TAG, "calculateDetourPath 无有效点");
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
            Log.d(TAG, "calculateDetourPath 航线经过安全围栏外，先进行绕飞处理");
            detourFencePath = GraphUtils.findMultiSegmentPath(waypoints, geoFences, true);
            long fenceEndTime = System.currentTimeMillis();
            Log.d(TAG, "calculateDetourPath 围栏外绕飞路线 耗时：" + (fenceEndTime - fenceStarTime));
            if (detourFencePath == null) {
                Log.d(TAG, "calculateDetourPath 围栏外的情况绕飞失败 ");
                return null;
            } else {
                Log.d(TAG, "calculateDetourPath 围栏外 绕飞路线：" + detourFencePath.size() + ",耗时：" + (fenceEndTime - fenceStarTime));
                Log.d(TAG, "calculateDetourPath 围栏外 绕飞路线：" + detourFencePath);
                Log.d(TAG, "calculateDetourPath 围栏外 直飞距离：" + directDistance + ", 绕飞后距离：" + getDistance(detourFencePath));
            }
        } else {
            detourFencePath = waypoints;
        }

        long noFlyZoneStartTime = System.currentTimeMillis();
        Log.d(TAG, "calculateDetourPath 开始计算绕飞路线");

        List<MyLatLng> path = GraphUtils.findMultiSegmentPath(detourFencePath, mNonFlyZones, false);
        long noFlyZoneEndTime = System.currentTimeMillis();
        if (path == null || path.isEmpty()) {
            Log.d(TAG, "calculateDetourPath 禁飞区 fail");
            return null;
        }
        Log.d(TAG, "calculateDetourPath 禁飞区 绕飞路线：" + path.size() + ",耗时：" + (noFlyZoneEndTime - noFlyZoneStartTime));
        Log.d(TAG, "calculateDetourPath 禁飞区 绕飞路线：" + path);
        Log.d(TAG, "calculateDetourPath 禁飞区 直飞距离：" + directDistance + ", 绕飞后距离：" + getDistance(path));

        Log.d(TAG, "calculateDetourPath 最终绕飞路线：" + path.size());
        Log.d(TAG, "calculateDetourPath 最终绕飞路线：" + path);

        boolean isCrossOutSideFenceWithPath = GeoUtils.isPathWithinSafeZone(path, mGeoFencePolygon);
        boolean intersectsNoFlyZoneWithPath = GeoUtils.intersectsNoFlyZone(path, mNonFlyZones);
        Log.d(TAG, "calculateDetourPath 最终绕飞路线 是否经过围栏外：" + isCrossOutSideFenceWithPath + ", 是否经过禁飞区：" + intersectsNoFlyZoneWithPath);
        if (isCrossOutSideFenceWithPath || intersectsNoFlyZoneWithPath) {
            Log.d(TAG, "calculateDetourPath 进行递归");
            return calculateDetourPath(path);
        }
        Log.d(TAG, "calculateDetourPath 路线安全，返回");
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
        Log.d(TAG, "updateNoFlyZones: noFlyZones = " + noFlyZones.size());
        for (int i = 0 ; i < noFlyZones.size(); i++) {
            List<MyLatLng> noFlyZone = noFlyZones.get(i);
            Log.d(TAG, "updateNoFlyZones: noFlyZone = " + noFlyZone.size());
            Log.d(TAG, "updateNoFlyZones: noFlyZone22 = " + noFlyZone);
                Log.d(TAG, "updateNoFlyZones: 添加成功 = " + noFlyZone);
                mNonFlyZones.add(GeoUtils.createPolygon(noFlyZone));
        }
    }
    /**
     * 获取机场禁飞区
     * */

    public void reset() {
        Log.d(TAG, "release");
        mIsDetouringGoHome = false;
        mIsDetouringWaypoint = false;
        mDetouringWaypointSize = 0;
    }
}
