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
                boolean intersectsNoFlyZone = GeoUtils.intersectsNoFlyZone(start, end, noFlyZones);
                Log.d(TAG, "foreach " + i + ", intersectsNoFlyZone = " + intersectsNoFlyZone);
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

                    Log.d(TAG, "Proceed to the next round of traversal without crossing the no-fly zone.");
                    continue;
                }
                long startTime = System.currentTimeMillis();
                // Calculate the current flight segment path
                List<MyLatLng> segmentPath = calculateShortestPath(start, end, noFlyZones, isCrossOutSideFence);
                long endTime = System.currentTimeMillis();
                Log.d(TAG, " point " + i + "," + (i + 1) + "，path generation cost：" + (endTime - startTime));
                if (segmentPath == null) {
                    Log.d(TAG, "fail ：i=" + i );
                    return null;
                } else {
                    Log.d(TAG, "find path：size " + segmentPath);
                    // Add path points (avoid adding the starting point repeatedly)
                    if (!fullPath.isEmpty()) {
                        segmentPath.remove(0);
                    }
                    fullPath.addAll(segmentPath);
                }

            }

            return fullPath;
        } catch (Throwable e) {
            e.printStackTrace();
            Log.d(TAG, "find path error：" + e.getMessage());
        }
        return null;
    }

    private static List<MyLatLng> calculateShortestPath(MyLatLng start, MyLatLng end, List<Polygon> noFlyZones, boolean isCrossOutSideFence) {
        Graph<MyLatLng, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        List<MyLatLng> validPoints = KeyPointExtractor.extractKeyPoints(start, end, noFlyZones, isCrossOutSideFence ? 0 : SAFETY_DISTANCE_METERS);
        Polygon geoFencePolygon = DetourPathManager.getsInstance().getGeoFencePolygon();
        List<MyLatLng> geoFencePoint = geoFencePolygon != null ? KeyPointExtractor.extractKeyPoints(start, end, Collections.singletonList(geoFencePolygon), 0) : new ArrayList<>();
        Log.d(TAG, "calculateShortestPath The number of key points for generating no-fly zones：" + validPoints.size());
        Log.d(TAG, "calculateShortestPath Determine the number of key points for the fence construction" + geoFencePoint.size());
        if (!isCrossOutSideFence) {
            validPoints.addAll(geoFencePoint);
        }
        Log.d(TAG, "calculateShortestPath Determine the total number of key points for the fence construction " + validPoints.size());

        // Only add edges between the key points
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
        //run  A*  search
        AStarShortestPath<MyLatLng, DefaultWeightedEdge> aStar = new AStarShortestPath<>(graph, MyLatLng::distanceTo);
        try {
            GraphPath<MyLatLng, DefaultWeightedEdge> graphPath = aStar.getPath(start, end);
            if (graphPath != null) {
                return aStar.getPath(start, end).getVertexList();
            }
        } catch (Throwable throwable) {
            Log.d(TAG, " AStarShortestPath find path failure：" + throwable.getMessage());
        }
        return null;
    }
}