package com.dronesky.detour;

import android.text.TextUtils;

import com.cloudcenter.maplib.utils.CoordinateTransformUtil;
import com.cloudcenter.netlib.api.ApronApi;
import com.cloudcentury.base.HandlerTimer;
import com.cloudcentury.base.HandlerTimerTask;
import com.cloudcentury.base.MainHandler;
import com.cloudcentury.base.utils.CommonUtil;
import com.cloudcentury.base.utils.ThreadPool;
import com.cloudcentury.djiremotecontrol.R;
import com.cloudcentury.djiremotecontrol.bean.apron.NonFlyZoneBean;
import com.cloudcentury.djiremotecontrol.constances.configs.DJiConfig;
import com.cloudcentury.djiremotecontrol.constances.configs.UCloudConfig;
import com.cloudcentury.djiremotecontrol.controller.UMqttController;
import com.cloudcentury.djiremotecontrol.controller.dji.UFlightController;
import com.cloudcentury.djiremotecontrol.controller.dji.UWayPointController;
import com.cloudcentury.djiremotecontrol.link.ULTELinkManager;
import com.cloudcentury.djiremotecontrol.mqtt.proto.Base;
import com.cloudcentury.djiremotecontrol.mqtt.proto.FlightController;
import com.cloudcentury.djiremotecontrol.mqtt.proto.MissionControl;
import com.cloudcentury.djiremotecontrol.mqtt.proto.SandCRemoteCtlData;
import com.cloudcentury.djiremotecontrol.preciselanding.FreeSkyPreciseLandWaypointManager;
import com.cloudcentury.djiremotecontrol.utils.LocationUtils;
import com.cloudcentury.djiremotecontrol.utils.MessageAlertUtils;
import com.cloudcentury.drone.common.FsLocationCoordinate2D;
import com.cloudcentury.drone.waypoint.FsWaypointMission;
import com.cloudcentury.fslog.FsLog;

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
    private HandlerTimer mHandlerTimer = null;
    private HandlerTimerTask mHandlerTimerTask;

    private DetourGoHomeManager() {

    }

    public static DetourGoHomeManager getsInstance() {
        return sInstance;
    }

    public void startCheckIfNeedDetourWhenGoHome() {
        if (!ENABLE) {
            FsLog.d(TAG, "startCheckIfNeedDetourWhenGoHome 功能未启用 ");
            return;
        }
        if (!hasValidNonFlyZones()) {
            FsLog.d(TAG, "startCheckIfNeedDetourWhenGoHome 没有禁飞区，不处理");
            return;
        }
        FsLog.d(TAG, "startCheckIfNeedDetourWhenGoHome");
        stopCheckIfNeedDetourWhenGoHome();
        mHandlerTimer = new HandlerTimer();
        mHandlerTimerTask = new DetourHandlerTimeTask();
        mHandlerTimer.schedule(mHandlerTimerTask, 0, 5000);
    }

    public void stopCheckIfNeedDetourWhenGoHome() {
        if (!ENABLE) {
            FsLog.d(TAG, "stopCheckIfNeedDetourWhenGoHome 功能未启用 ");
            return;
        }
        FsLog.d(TAG, "stopCheckIfNeedDetourWhenGoHome");
        if (mHandlerTimer != null) {
            mHandlerTimer.cancel();
        }
        mHandlerTimerTask = null;
        mHandlerTimer = null;
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
            FsLog.d(TAG, "isNeedDetourFlying 未开启");
            return false;
        }
        if (start.latitude == end.latitude && start.longitude == end.longitude) {
            FsLog.d(TAG, "isNeedDetourFlying 开始和终点相同，不需要绕行");
            return false;
        }
        boolean isCurrentInsideNoFlyZone = GeoUtils.isInsideNoFlyZone(start, getNonFlyZones());
        boolean isCurrentInsideFence = GeoUtils.isInsidePolygon(start, getGeoFencePolygon());
        if (isCurrentInsideNoFlyZone || !isCurrentInsideFence) {
            FsLog.d(TAG, "isNeedDetourFlying 当前点是否在禁飞区：" + isCurrentInsideNoFlyZone + ", 是否在围栏内 = " + isCurrentInsideFence + " ，不需要绕行");
            return false;
        }
        boolean isPathInGeoFence = GeoUtils.isPathWithinSafeZone(start, end, mGeoFencePolygon);
        boolean intersectsNoFlyZone = GeoUtils.intersectsNoFlyZone(start, end, mNonFlyZones);
        FsLog.d(TAG, "isNeedDetourFlying 是否经过围栏外 = " + !isPathInGeoFence + ", 是否经过禁飞区 = " + intersectsNoFlyZone);
        return intersectsNoFlyZone || !isPathInGeoFence;
    }

    /**
     * 获取机场禁飞区
     *
     * @param airportId
     */
    public void getAirportJFQ(String airportId) {
        FsLog.d(TAG, "getAirportJFQ airportId = " + airportId);
        if (!ENABLE) {
            FsLog.d(TAG, "getAirportJFQ 功能未启用 ");
            return;
        }
        ApronApi.getAirportJFQ(airportId)
                .subscribe(baseModel -> {
                    boolean success = baseModel.isSuccess();
                    String resultExt = baseModel.getResultExt();
                    FsLog.d(TAG, "getAirportJFQ resultExt = " + resultExt + ", success = " + success);
                    List<NonFlyZoneBean> nonFlyZoneBeans = CommonUtil.jsonToList(resultExt, NonFlyZoneBean.class);
                    FsLog.d(TAG, "getAirportJFQ nonFlyZoneBeans  size= " + nonFlyZoneBeans.size());
                    FsLog.d(TAG, "getAirportJFQ nonFlyZoneBeans  size= " + nonFlyZoneBeans);
                    mNonFlyZones.clear();
                    for (NonFlyZoneBean nonFlyZoneBean : nonFlyZoneBeans) {
                        if (nonFlyZoneBean.getGeometryList() != null && !nonFlyZoneBean.getGeometryList().isEmpty()) {
                            mNonFlyZones.add(GeoUtils.createPolygon(nonFlyZoneBean.getNonFlyList()));
                        }
                    }
                    FsLog.d(TAG, "getAirportJFQ mNonFlyZones  size= " + mNonFlyZones.size());
                    FsLog.d(TAG, "getAirportJFQ mNonFlyZones  size= " + mNonFlyZones);

                }, throwable -> {
                    throwable.getMessage();
                    FsLog.d(TAG, "获取禁飞区异常：" + throwable.getMessage());
                });
    }

    /**
     * 返航的时候，检查是否需要绕飞
     */
    public void checkIfNeedDetourWhenGoHome() {
        FsLog.d(TAG, "checkIfNeedDetourWhenGoHome");
        if (!ENABLE) {
            FsLog.d(TAG, "checkIfNeedDetourWhenGoHome 功能未启用 ");
            return;
        }
        if (mNonFlyZones == null || mNonFlyZones.isEmpty()) {
            FsLog.d(TAG, "checkIfNeedDetourWhenGoHome 没有禁飞区信息，不处理");
            return;
        }
        FsLocationCoordinate2D homeLocation = LocationUtils.getHomeLocation();
        FsLocationCoordinate2D currentLocation = LocationUtils.getUavCurrentLocation();
        if (homeLocation == null || currentLocation == null) {
            FsLog.d(TAG, "checkIfNeedDetourWhenGoHome 经纬度异常，不处理");
            return;
        }

        //判断是否经过禁飞区
        MyLatLng startLocation = new MyLatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        MyLatLng endLocation = new MyLatLng(homeLocation.getLatitude(), homeLocation.getLongitude());
        List<MyLatLng> initPath = new ArrayList<>();
        initPath.add(startLocation);
        initPath.add(endLocation);
        if (GeoUtils.isInsideNoFlyZone(startLocation, mNonFlyZones)) {
            FsLog.d(TAG, "checkIfNeedDetourWhenGoHome 当前位置在禁飞区内，不处理");
            stopCheckIfNeedDetourWhenGoHome();
            return;
        }

        if (!isNeedDetourFlying(startLocation, endLocation)) {
            FsLog.d(TAG, "checkIfNeedDetourWhenGoHome 不经过禁飞区，不处理");
            stopCheckIfNeedDetourWhenGoHome();
            return;
        }

        List<MyLatLng> path = calculateDetourPath(initPath);
        if (path == null || path.isEmpty()) {
            handleDetourFailing();
            return;
        }

        sendDetourPathToServer(path);

        if (path.size() > 3) {
            path.remove(0);
            FsLog.d(TAG, "checkIfNeedDetourWhenGoHome 去掉第一个点，无人机当前位置");
        }

        FsLog.d(TAG, "checkIfNeedDetourWhenGoHome 最终绕飞路线：" + path.size());
        FsLog.d(TAG, "checkIfNeedDetourWhenGoHome 最终绕飞路线：" + path);

        if (DJiConfig.getInstance().isFlightModeGoHome()) {
            UFlightController.getInstance().cancelGoHome(error -> {
                FsLog.d(TAG, "checkIfNeedDetourWhenGoHome 取消返航：" + error);
                if (error == null) {
                    stopCheckIfNeedDetourWhenGoHome();
                    realExecuteDetour(path);
                } else {
                    MessageAlertUtils.sendMessageErrorAlert(R.string.tips_detour_fail);
                    MessageAlertUtils.sendMessageErrorAlert(R.string.tips_detour_cancle_gohome_fail);
                }
            });
        } else {
            stopCheckIfNeedDetourWhenGoHome();
            realExecuteDetour(path);
        }
    }


    private void handleDetourFailing() {
        FsLog.d(TAG, "handleDetourFailing 绕飞失败：");
        MessageAlertUtils.sendMessageErrorAlert(R.string.tips_detour_fail);
        MessageAlertUtils.sendMessageFatalAlert(R.string.tips_detour_fail);
        if (DJiConfig.getInstance().isFlightModeGoHome()) {
            UFlightController.getInstance().cancelGoHome(error -> {
                FsLog.d(TAG, "handleDetourFailing 取消返航：" + error);
                if (error != null) {
                    MessageAlertUtils.sendMessageErrorAlert(R.string.tips_detour_cancle_gohome_fail);
                    MessageAlertUtils.sendMessageFatalAlert(R.string.tips_detour_cancle_gohome_fail);
                }
            });
        }
    }

    public List<MyLatLng> calculateDetourPath(MyLatLng start, MyLatLng end) {
        FsLog.d(TAG, "calculateDetourPath start = " + start + ", end = " + end);
        List<MyLatLng> paths = new ArrayList<>();
        paths.add(start);
        paths.add(end);
        return calculateDetourPath(paths);
    }

    public List<MyLatLng> calculateDetourPath(List<MyLatLng> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            FsLog.d(TAG, "calculateDetourPath 无有效点");
            return null;
        }
        double directDistance = getDistance(waypoints);
        boolean isCrossOutSideFence = GeoUtils.isPathWithinSafeZone(waypoints, mGeoFencePolygon);
        boolean intersectsNoFlyZone = GeoUtils.intersectsNoFlyZone(waypoints, mNonFlyZones);
        FsLog.d(TAG, "calculateDetourPath isCrossOutSideFence = " + isCrossOutSideFence + ", intersectsNoFlyZone = " + intersectsNoFlyZone);
        List<MyLatLng> detourFencePath = null;
        if (isCrossOutSideFence) {
            long fenceStarTime = System.currentTimeMillis();
            List<Polygon> geoFences = new ArrayList<>();
            geoFences.add(mGeoFencePolygon);
            FsLog.d(TAG, "calculateDetourPath 航线经过安全围栏外，先进行绕飞处理");
            detourFencePath = GraphUtils.findMultiSegmentPath(waypoints, geoFences, true);
            long fenceEndTime = System.currentTimeMillis();
            FsLog.d(TAG, "calculateDetourPath 围栏外绕飞路线 耗时：" + (fenceEndTime - fenceStarTime));
            if (detourFencePath == null) {
                FsLog.d(TAG, "calculateDetourPath 围栏外的情况绕飞失败 ");
                handleDetourFailing();
                return null;
            } else {
                FsLog.d(TAG, "calculateDetourPath 围栏外 绕飞路线：" + detourFencePath.size() + ",耗时：" + (fenceEndTime - fenceStarTime));
                FsLog.d(TAG, "calculateDetourPath 围栏外 绕飞路线：" + detourFencePath);
                FsLog.d(TAG, "calculateDetourPath 围栏外 直飞距离：" + directDistance + ", 绕飞后距离：" + getDistance(detourFencePath));
            }
        } else {
            detourFencePath = waypoints;
        }

        long noFlyZoneStartTime = System.currentTimeMillis();
        FsLog.d(TAG, "calculateDetourPath 开始计算绕飞路线");

        List<MyLatLng> path = GraphUtils.findMultiSegmentPath(detourFencePath, mNonFlyZones, false);
        long noFlyZoneEndTime = System.currentTimeMillis();
        if (path == null || path.isEmpty()) {
            handleDetourFailing();
            return null;
        }
        FsLog.d(TAG, "calculateDetourPath 禁飞区 绕飞路线：" + path.size() + ",耗时：" + (noFlyZoneEndTime - noFlyZoneStartTime));
        FsLog.d(TAG, "calculateDetourPath 禁飞区 绕飞路线：" + path);
        FsLog.d(TAG, "calculateDetourPath 禁飞区 直飞距离：" + directDistance + ", 绕飞后距离：" + getDistance(path));

        FsLog.d(TAG, "calculateDetourPath 最终绕飞路线：" + path.size());
        FsLog.d(TAG, "calculateDetourPath 最终绕飞路线：" + path);

        boolean isCrossOutSideFenceWithPath = GeoUtils.isPathWithinSafeZone(path, mGeoFencePolygon);
        boolean intersectsNoFlyZoneWithPath = GeoUtils.intersectsNoFlyZone(path, mNonFlyZones);
        FsLog.d(TAG, "calculateDetourPath 最终绕飞路线 是否经过围栏外：" + isCrossOutSideFenceWithPath + ", 是否经过禁飞区：" + intersectsNoFlyZoneWithPath);
        if (isCrossOutSideFenceWithPath || intersectsNoFlyZoneWithPath) {
            FsLog.d(TAG, "calculateDetourPath 进行递归");
            return calculateDetourPath(path);
        }
        FsLog.d(TAG, "calculateDetourPath 路线安全，返回");
        return path;
    }

    private void realExecuteDetour(List<MyLatLng> path) {
        MainHandler.post(new Runnable() {
            @Override
            public void run() {
                FsWaypointMission mission = FreeSkyPreciseLandWaypointManager.getInstance().getDetourMissionGoHome(path);
                FreeSkyPreciseLandWaypointManager.getInstance().autoExecuteMission(mission);
                FsLog.d(TAG, "realExecuteDetour 执行绕飞路线");
                MessageAlertUtils.sendMessageErrorAlert(R.string.tips_detour_success);
                MessageAlertUtils.sendMessageNormalAlert(R.string.tips_detour_success);
                mIsDetouringGoHome = true;
            }
        }, 2000);
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

    public void stopDetouringGoHome() {
        FsLog.d(TAG, "stopDetouringGoHome");
        mIsDetouringGoHome = false;
        UWayPointController.getInstance().stopWaypointMission();
    }

    /**
     * 是否是绕路返航中
     *
     * @return
     */
    public boolean isDetouringGoHome() {
        return mIsDetouringGoHome;
    }

    /**
     * 是否是航线绕飞
     *
     * @return
     */
    public boolean isDetouringWaypoint() {
        return mIsDetouringWaypoint;
    }

    public void setDetouringWaypoint(boolean detouringWaypoint) {
        this.mIsDetouringWaypoint = detouringWaypoint;
    }

    public int getDetouringWaypointSize() {
        return mDetouringWaypointSize;
    }

    public void setDetouringWaypointSize(int detouringWaypointSize) {
        this.mDetouringWaypointSize = detouringWaypointSize;
    }

    private static class DetourHandlerTimeTask extends HandlerTimerTask {

        @Override
        public void run() {
            FsLog.d(TAG, "");
            String stateError = ULTELinkManager.getInstance().getStateErrorMsg();
            boolean isLteEnhancedTransmissionEnabled = ULTELinkManager.getInstance().isLteEnhancedTransmissionEnabled();
            int lteLinkQuality = ULTELinkManager.getInstance().getCurrentLTELinkQuality();
            int upLinkSignalQuality = DJiConfig.getInstance().getUpLinkSignalQuality();
            boolean isLteWorking = ULTELinkManager.getInstance().isLteEnhancedTransmissionEnabled() && TextUtils.isEmpty(ULTELinkManager.getInstance().getStateErrorMsg()) && lteLinkQuality >= 60;
            FsLog.d(TAG, "DetourHandlerTimeTask stateError = " + stateError + ", isLteEnhancedTransmissionEnabled  = " + isLteEnhancedTransmissionEnabled + ", lteLinkQuality = " + lteLinkQuality + ", isLteWorking = " + isLteWorking + ", upLinkSignalQuality = " + upLinkSignalQuality);
            if (DJiConfig.getInstance().getUpLinkSignalQuality() >= 80 || isLteWorking) {
                FsLog.d(TAG, "DetourHandlerTimeTask 符合条件，进行绕飞判断");
                ThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        DetourGoHomeManager.getsInstance().checkIfNeedDetourWhenGoHome();
                    }
                });
            } else {
                MessageAlertUtils.sendMessageErrorAlert(R.string.tips_detour_signal_weak);
            }
        }
    }

    /**
     * 同步绕飞路径到后台
     */
    public void sendDetourPathToServer(List<MyLatLng> path) {
        FsLog.d(TAG, "sendDetourPathToServer 上报绕飞路线");
        FlightController.DetourFlyingAction.Builder detourFlyingActionBuilder = FlightController.DetourFlyingAction.newBuilder();
        FlightController.DetourFlyingAction.Feedback.Builder detourFlyingActionFeedbackBuilder = FlightController.DetourFlyingAction.Feedback.newBuilder();
        for (MyLatLng latLng : path) {
            MissionControl.Waypoint.Builder waypointBuilder = MissionControl.Waypoint.newBuilder();
            waypointBuilder.setCoordinate84(Base.Coordinate84.newBuilder().setLat(latLng.latitude).setLng(latLng.longitude).build());
            detourFlyingActionFeedbackBuilder.addWaypoints(waypointBuilder.build());
        }
        detourFlyingActionBuilder.setFeedback(detourFlyingActionFeedbackBuilder.build());
        FlightController.DetourFlyingAction detourFlyingAction = detourFlyingActionBuilder.build();

        FsLog.d(TAG, "sendDetourPathToServer 上报的路径信息：" + detourFlyingAction);

        SandCRemoteCtlData.ControlFeedback.Builder remoteControlFeedbackbuilder = SandCRemoteCtlData.ControlFeedback.newBuilder();

        remoteControlFeedbackbuilder.setCompid(2);
        remoteControlFeedbackbuilder.setMsgId(126);
        remoteControlFeedbackbuilder.setCustomData(detourFlyingAction.toByteString());

        SandCRemoteCtlData.ControlFeedback feedback = remoteControlFeedbackbuilder.build();
        byte[] data = feedback.toByteArray();
        String client = UCloudConfig.getInstance().getClintId();
        UMqttController.getInstance().sendMessage(data, "app-ser/" + client + "/remoteControl", 2, false);
    }

    public void updateGeoFencePolygon() {
        List<List<Double>> apronFencePoints = UCloudConfig.getInstance().getApronFencePoints();
        if (apronFencePoints == null || apronFencePoints.isEmpty()) {
            FsLog.d(TAG, "updateGeoFencePolygon 围栏为空");
            mGeoFencePolygon = null;
            return;
        }

        List<MyLatLng> geoFenceList = new ArrayList<>();
        for (List<Double> points : apronFencePoints) {
            if (points != null && points.size() == 2) {
                FsLocationCoordinate2D fsLocationCoordinate2D = new FsLocationCoordinate2D(points.get(1), points.get(0));
                FsLocationCoordinate2D wgs84LocationCoordinate2D = CoordinateTransformUtil.transformGCJ02ToWGS84(fsLocationCoordinate2D);
                geoFenceList.add(new MyLatLng(wgs84LocationCoordinate2D.getLatitude(), wgs84LocationCoordinate2D.getLongitude()));
            }
        }

        FsLog.d(TAG, "updateGeoFencePolygon 围栏数据：size = " + geoFenceList.size());
        FsLog.d(TAG, "updateGeoFencePolygon 围栏数据：" + geoFenceList);
        mGeoFencePolygon = GeoUtils.createPolygon(geoFenceList);
    }

    public void reset() {
        FsLog.d(TAG, "release");
        mIsDetouringGoHome = false;
        mIsDetouringWaypoint = false;
        mDetouringWaypointSize = 0;
        stopCheckIfNeedDetourWhenGoHome();
    }
}
