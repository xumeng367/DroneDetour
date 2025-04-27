package com.dronesky.detour;

import java.util.List;

public class MapUtils {

    /**
     * 判断线段是否经过多边形
     * @param start 起点坐标
     * @param end 终点坐标
     * @param polygon 多边形区域（点的集合）
     * @return 是否穿过多边形
     */
    public static boolean isLineIntersectPolygon(MyLatLng start, MyLatLng end, List<MyLatLng> polygon) {
        int size = polygon.size();
        for (int i = 0; i < size; i++) {
            MyLatLng p1 = polygon.get(i);
            MyLatLng p2 = polygon.get((i + 1) % size); // 多边形是闭合的

            // 判断线段 (start, end) 是否和多边形的边 (p1, p2) 相交
            if (isSegmentsIntersect(start, end, p1, p2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 线段相交检测
     * @param p1 线段1的起点
     * @param p2 线段1的终点
     * @param q1 线段2的起点
     * @param q2 线段2的终点
     * @return 是否相交
     */
    private static boolean isSegmentsIntersect(MyLatLng p1, MyLatLng p2, MyLatLng q1, MyLatLng q2) {
        double d1 = crossProduct(q1, q2, p1);
        double d2 = crossProduct(q1, q2, p2);
        double d3 = crossProduct(p1, p2, q1);
        double d4 = crossProduct(p1, p2, q2);

        return (d1 * d2 < 0) && (d3 * d4 < 0);
    }

    /**
     * 计算向量叉乘
     */
    private static double crossProduct(MyLatLng a, MyLatLng b, MyLatLng c) {
        return (b.longitude - a.longitude) * (c.latitude - a.latitude)
                - (b.latitude - a.latitude) * (c.longitude - a.longitude);
    }
}