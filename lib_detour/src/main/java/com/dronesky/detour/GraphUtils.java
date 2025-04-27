package com.dronesky.detour;

import static com.dronesky.detour.KeyPointExtractor.SAFETY_DISTANCE_METERS;

import android.util.Log;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphUtils {
    private static final String TAG = "GraphUtils";

    /**
     * 计算完整航线路径（多段绕飞）
     */
    public static List<MyLatLng> findMultiSegmentPath(List<MyLatLng> waypoints, List<Polygon> noFlyZones, boolean isCrossOutSideFence) {
        try {
            List<MyLatLng> fullPath = new ArrayList<>();

            for (int i = 0; i < waypoints.size() - 1; i++) {
                MyLatLng start = waypoints.get(i);
                MyLatLng end = waypoints.get(i + 1);
                Log.d(TAG, "遍历" + i);
                boolean intersectsNoFlyZone = GeoUtils.intersectsNoFlyZone(start, end, noFlyZones);
                if (!intersectsNoFlyZone) {
                    if (!fullPath.isEmpty()) {
                        MyLatLng last = fullPath.get(fullPath.size() - 1);
                        if (last != start) {
                            fullPath.add(start);
                        }
                        fullPath.add(end);
                    } else {
                        fullPath.add(start);
                        fullPath.add(end);
                    }

                    Log.d(TAG, "不经过禁飞区，进行下一轮遍历");
                    continue;
                }
                long startTime = System.currentTimeMillis();
                // 计算当前航段路径
                List<MyLatLng> segmentPath = calculateShortestPath(start, end, noFlyZones, isCrossOutSideFence);
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "点" + i + "," + (i + 1) + "，生成路径耗时：" + (endTime - startTime));
                if (segmentPath == null) {
                    Log.d(TAG, "找到失败：i=" + i + "，添加收尾");
                    return null;
                } else {
                    Log.d(TAG, "找到绕飞路径：size = " + segmentPath.size());
                    Log.d(TAG, "找到绕飞路径：size " + segmentPath);
                    // 添加路径点（避免重复添加起点）
                    if (!fullPath.isEmpty()) {
                        segmentPath.remove(0);
                    }
                    fullPath.addAll(segmentPath);
                }

            }

            return fullPath;
        } catch (Throwable e) {
            e.printStackTrace();
            Log.d(TAG, "寻找路径异常：" + e.getMessage());
        }
        return null;
    }

    private static List<MyLatLng> calculateShortestPath(MyLatLng start, MyLatLng end, List<Polygon> noFlyZones, boolean isCrossOutSideFence) {
        Graph<MyLatLng, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        List<MyLatLng> validPoints = KeyPointExtractor.extractKeyPoints(start, end, noFlyZones, isCrossOutSideFence ? 0 : SAFETY_DISTANCE_METERS);
        Polygon geoFencePolygon = DetourGoHomeManager.getsInstance().getGeoFencePolygon();
        List<MyLatLng> geoFencePoint = geoFencePolygon != null ? KeyPointExtractor.extractKeyPoints(start, end, Collections.singletonList(geoFencePolygon), 0) : new ArrayList<>();
        Log.d(TAG, "calculateShortestPath 生成禁飞区关键点的数量：" + validPoints.size());
        Log.d(TAG, "calculateShortestPath 生成围栏关键点的数量" + geoFencePoint.size());
        if (!isCrossOutSideFence) {
            validPoints.addAll(geoFencePoint);
        }
        Log.d(TAG, "calculateShortestPath 生成围栏关键点总的数量" + validPoints.size());

        // 仅在关键点间添加边
        for (int i = 0; i < validPoints.size(); i++) {
            for (int j = i + 1; j < validPoints.size(); j++) {
                MyLatLng p1 = validPoints.get(i);
                MyLatLng p2 = validPoints.get(j);
                boolean isValidPath = !GeoUtils.intersectsNoFlyZone(p1, p2, noFlyZones);
                boolean isPathWithinSafeZone = GeoUtils.isPathWithinSafeZone(p1, p2, geoFencePolygon);
                if (isCrossOutSideFence) {
                    isValidPath = isPathWithinSafeZone;
                }
                // 确保路径不过禁飞区
                if (!p1.equals(p2) && isValidPath && isPathWithinSafeZone) {
                    graph.addVertex(p1);
                    graph.addVertex(p2);
                    DefaultWeightedEdge edge = graph.addEdge(p1, p2);
                    if (edge != null) {
                        graph.setEdgeWeight(edge, p1.distanceTo(p2));
                    }
                }
            }

        }
        // 3️⃣ 运行 A* 搜索
        AStarShortestPath<MyLatLng, DefaultWeightedEdge> aStar = new AStarShortestPath<>(graph, MyLatLng::distanceTo);
        try {
            GraphPath<MyLatLng, DefaultWeightedEdge> graphPath = aStar.getPath(start, end);
            if (graphPath != null) {
                return aStar.getPath(start, end).getVertexList();
            }
        } catch (Throwable throwable) {
            Log.d(TAG, " AStarShortestPath 寻找路径失败：" + throwable.getMessage());
        }
        return null;
    }
}