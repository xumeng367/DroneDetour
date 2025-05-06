package com.dronesky.detour;

import android.util.Log;

import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * 避障绕飞管理
 */
public class DetourGoHomeManager {
    private static final String TAG = "DetourGoHomeManager";
    public static final boolean ENABLE = true;
    private static final DetourGoHomeManager sInstance = new DetourGoHomeManager();
    private final List<Polygon> mNonFlyZones = new ArrayList<>();
    private Polygon mGeoFencePolygon;
    private boolean mIsDetouringGoHome = false;
    private boolean mIsDetouringWaypoint = false;//是否航线绕飞
    private int mDetouringWaypointSize = 0;//绕飞点

    private DetourGoHomeManager() {

    }

    public static DetourGoHomeManager getsInstance() {
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
