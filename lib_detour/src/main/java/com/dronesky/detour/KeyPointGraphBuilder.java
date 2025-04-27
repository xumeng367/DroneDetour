package com.dronesky.detour;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

public class KeyPointGraphBuilder {
    public SimpleWeightedGraph<MyLatLng, DefaultWeightedEdge> buildGraph(List<MyLatLng> keyPoints, List<Polygon> noFlyZones) {
        SimpleWeightedGraph<MyLatLng, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        // 添加关键点
        for (MyLatLng point : keyPoints) {
            graph.addVertex(point);
        }

        // 仅在关键点间添加边
        for (int i = 0; i < keyPoints.size(); i++) {
            for (int j = i + 1; j < keyPoints.size(); j++) {
                MyLatLng p1 = keyPoints.get(i);
                MyLatLng p2 = keyPoints.get(j);

                // 确保路径不过禁飞区
                if (!intersectsNoFlyZone(p1, p2, noFlyZones)) {
                    DefaultWeightedEdge edge = graph.addEdge(p1, p2);
                    if (edge != null) {
                        graph.setEdgeWeight(edge, p1.distanceTo(p2));
                    }
                }
            }

        }

        return graph;
    }

    private boolean intersectsNoFlyZone(MyLatLng p1, MyLatLng p2, List<Polygon> noFlyZones) {
        LineString line = new GeometryFactory().createLineString(new Coordinate[]{
            new Coordinate(p1.longitude, p1.latitude),
            new Coordinate(p2.longitude, p2.latitude)
        });

        for (Polygon zone : noFlyZones) {
            if (line.intersects(zone)) {
                return true;
            }
        }
        return false;
    }
}