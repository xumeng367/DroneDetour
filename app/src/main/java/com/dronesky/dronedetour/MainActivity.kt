package com.dronesky.dronedetour

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polygon
import com.amap.api.maps.model.PolygonOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.dronesky.detour.GeoUtils
import com.dronesky.detour.GraphUtils
import com.dronesky.detour.MapUtils
import com.dronesky.detour.MyLatLng
import com.dronesky.dronedetour.utils.MainHandler
import com.dronesky.dronedetour.utils.PermissionConstance
import com.dronesky.dronedetour.utils.SpUtil
import com.dronesky.dronedetour.utils.ToastUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun toMyLatLon(latLon: LatLng): MyLatLng {
    return MyLatLng(latLon.latitude, latLon.longitude)
}

fun toLatLon(myLatLon: MyLatLng): LatLng {
    return LatLng(myLatLon.latitude, myLatLon.longitude)
}

class MainActivity : ComponentActivity() {

    private lateinit var mapView: MapView
    private var aMap: AMap? = null
    private val missingPermission: ArrayList<String> = ArrayList()
    private var isCreatePointStatus = false
    private var startPoint = MyLatLng(0.0, 0.0)
    private var endPoint = MyLatLng(0.0, 0.0)
    private var currentClickedPoint = LatLng(0.0, 0.0)
    private lateinit var buttonCreateStartPoint: Button
    private lateinit var buttonCreateEndPoint: Button
    private lateinit var setBanAreaButton: Button
    private lateinit var generateKeyPoint: Button
    private lateinit var btn_set_point: Button
    private lateinit var btn_cancel_set_point: Button
    private lateinit var btn_set_fence_point: Button

    //禁飞区
    private var banPointList = ArrayList<MyLatLng>()

    //电子围栏
    private var fencePoints = ArrayList<MyLatLng>()
    private var isPointStartStatus = false;
    private var isPointEndStatus = false;
    private lateinit var et_type: Spinner
    private var selectedType = "电子围栏"
    private var TAG = "ObcaleActivity"
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private var mPathPolyline: Polyline? = null;
    private var mFlyPathLine: Polyline? = null;

    //禁飞区集合
    private var listBanAreas: ArrayList<ArrayList<MyLatLng>> = ArrayList()

    //电子围栏
    private var fenceList: ArrayList<MyLatLng> = ArrayList()

    private var flypathPoints = ArrayList<MyLatLng>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //高德地图隐私合规
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)

        initSpData()
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        buttonCreateStartPoint = findViewById(R.id.btn_set_start_point)
        buttonCreateEndPoint = findViewById(R.id.btn_set_end_point)
        setBanAreaButton = findViewById(R.id.btn_set_avoid_point)
        generateKeyPoint = findViewById(R.id.btn_set_key_points)
        btn_set_point = findViewById(R.id.btn_set_point)
        btn_cancel_set_point = findViewById(R.id.btn_cancel_set_point)
        btn_set_fence_point = findViewById(R.id.btn_set_fence_point)
        et_type = findViewById(R.id.spinner_type)
        // 定义下拉数据
        val types = arrayOf("电子围栏", "禁飞区", "起始点", "终点", "航线模式")
        // 创建适配器
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        // 设置下拉样式
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // 绑定适配器
        et_type.adapter = adapter
        // 监听选中事件
        et_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                selectedType = types[position] // 获取选中的值
                if (selectedType == "航线模式") {
                    flypathPoints.clear()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }



        btn_set_point.setOnClickListener {
            isCreatePointStatus = true
        }
        btn_cancel_set_point.setOnClickListener {
            isCreatePointStatus = false
            banPointList.clear()
            fencePoints.clear()
            fenceList.clear()
            flypathPoints.clear()
            aMap?.clear()
            SpUtil.putString("fence_list_str", "")
            SpUtil.putString("ban_list_str", "")

        }

        setBanAreaButton.setOnClickListener {

            // 绘制禁飞区域
            drawBanPolygon(banPointList)
            // 创建 banPointList 的副本并添加到 listBanAreas
            listBanAreas.add(ArrayList(banPointList))  // 创建副本
            // 清空 banPointList，以便重新使用
            banPointList.clear()
            //持久化数据
            val gson = Gson()
            val banJson = gson.toJson(listBanAreas)
            Log.d(TAG, "banJson = " + banJson)
            val listType = object : TypeToken<ArrayList<ArrayList<MyLatLng>>>() {}.getType()
            val fromJsonList: ArrayList<ArrayList<MyLatLng>> = gson.fromJson(banJson, listType)
            Log.d(TAG, "banJson = " + fromJsonList)
            Log.d(TAG, "banJson = " + fromJsonList.javaClass)
            Log.d(TAG, "banJson = " + fromJsonList.size)
            fromJsonList.forEach {
                Log.d(TAG, "banJson = " + it)
            }
            SpUtil.putString("ban_list_str", banJson)

            Log.d(TAG, "DDDDDDDDDDDDDDDD------PPPPlistBanAreas:${listBanAreas.size}")
        }

        generateKeyPoint.setOnClickListener {
            drawBanPolygon(banPointList)
//            listBanAreas.add(banPointList)
//            banPointList.clear()

        }
        btn_set_fence_point.setOnClickListener {
            //持久化数据
            val gson = Gson()
            val fenceJson = gson.toJson(fenceList)
            Log.d(TAG, "fenceJson = " + fenceJson)
            val listType = object : TypeToken<ArrayList<MyLatLng>>() {}.getType()
            val fromJsonList: ArrayList<MyLatLng> = gson.fromJson(fenceJson, listType)
            Log.d(TAG, "fromJsonList = " + fromJsonList)
            Log.d(TAG, "fromJsonList = " + fromJsonList.javaClass)
            Log.d(TAG, "fromJsonList = " + fromJsonList.size)
            fromJsonList.forEach {
                Log.d(TAG, "fromJsonList = " + it)
            }
            SpUtil.putString("fence_list_str", fenceJson)

            drawFencePolygon(fromJsonList)
        }
        buttonCreateStartPoint.setOnClickListener {
            isPointStartStatus = true
        }
        buttonCreateEndPoint.setOnClickListener {
            isPointEndStatus = true
        }

        generateKeyPoint.setOnClickListener {


            Thread {
                val type = 4
                var listLatLng: ArrayList<MyLatLng> = ArrayList()
                if (type == 4) {
                    val noFlyZones: MutableList<Polygon> = java.util.ArrayList()
                    Log.d(TAG, "listBanAreas = " + listBanAreas.size)
                }

            }.start()

            /* val keyPoints = planner.planKeyPoints(startPoint, endPoint)
             if (keyPoints.isNullOrEmpty()) {
                   Log.d(TAG, "DDDDDDDDDDDD----->无法找到有效路径")
             } else {
                   Log.d(TAG, "DDDDDDDDDDDD----->找到关键点路径 ")
                 var listLatLng: ArrayList<LatLng> = ArrayList()

                 keyPoints.forEach {
                       Log.d(TAG, "Lat: ${it.latitude}, Lng: ${it.longitude}")
                     listLatLng.add(LatLng(it.latitude, it.longitude))
                 }
                 drawPathOnAMap(aMap!!, listLatLng)
             }*/
        }

        checkAndRequestPermissions()

    }

    private fun initSpData() {
        val gson = Gson()
        val fenceJson = SpUtil.getString("fence_list_str", "")
        Log.d(TAG, "initSpData fenceJson = " + fenceJson)
        if (fenceJson.isNotEmpty()) {
            val listType = object : TypeToken<ArrayList<MyLatLng>>() {}.getType()
            fenceList = gson.fromJson(fenceJson, listType)
            Log.d(TAG, "initSpData fromJsonList = " + fenceList)
            Log.d(TAG, "initSpData fromJsonList = " + fenceList.size)
            fenceList.forEach {
                Log.d(TAG, "initSpData fromJsonList = " + it)
            }
        }

        val banJson = SpUtil.getString("ban_list_str", "")
        Log.d(TAG, "initSpData banJson = " + banJson)
        if (banJson.isNotEmpty()) {
            val listType = object : TypeToken<ArrayList<ArrayList<MyLatLng>>>() {}.getType()
            listBanAreas = gson.fromJson(banJson, listType)
            Log.d(TAG, "initSpData listBanAreas = " + listBanAreas)
            Log.d(TAG, "initSpData listBanAreas = " + listBanAreas.javaClass)
            Log.d(TAG, "initSpData listBanAreas = " + listBanAreas.size)
            listBanAreas.forEach {
                Log.d(TAG, "initSpData listBanAreas = " + it)
            }
        }


    }

    private fun clearPoints() {
        startPoint = MyLatLng(0.0, 0.0)
        endPoint = MyLatLng(0.0, 0.0)
        banPointList.clear()
    }


    private fun initMap() {
        if (aMap == null) {
            aMap = mapView.map
            // 地图UI相关设置
            aMap?.uiSettings?.isZoomControlsEnabled = true // 显示缩放按钮
            aMap?.uiSettings?.isMyLocationButtonEnabled = true // 显示定位按钮
            aMap?.isMyLocationEnabled = true // 开启定位图层
            // 设置初始定位和缩放级别
            val latLng = LatLng(36.09406114093818, 120.38552731431761)

            // 设置初始摄像头位置
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14f)
            aMap?.moveCamera(cameraUpdate)

            // 设置地图点击事件监听器
            aMap?.setOnMapClickListener { latLng ->
                currentClickedPoint = latLng
                //", "", "", "")
                if (selectedType == "电子围栏") {
                    //电子围栏
                    fenceList.add(toMyLatLon(latLng))
                    // 在点击的地方添加 Marker
                    val markerOptions = MarkerOptions()
                        .position(latLng)  // 获取点击的位置
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))  // 设置标记图标（可自定义）
                    aMap?.addMarker(markerOptions)  // 将标记添加到地图上
                    return@setOnMapClickListener
                } else if (selectedType == "禁飞区") {
                    banPointList.add(toMyLatLon(latLng))
                    val markerOptions = MarkerOptions()
                        .position(latLng)  // 获取点击的位置
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))  // 设置标记图标（可自定义）
                    aMap?.addMarker(markerOptions)  // 将标记添加到地图上
                    return@setOnMapClickListener
                } else if (selectedType == "起始点") {
                    //返航点
                    startMarker?.remove()
                    startPoint = toMyLatLon(latLng)
                    Log.d(
                        TAG,
                        "DDDDDDDDDDDDDDDDDDDDD------->起始点:${latLng.latitude}---${latLng.longitude}"
                    )
                    val markerOptions = MarkerOptions()
                        .position(latLng)  // 获取点击的位置
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))  // 设置标记图标（可自定义）
                    startMarker = aMap?.addMarker(markerOptions)  // 将标记添加到地图上
                    isPointStartStatus = false
                    return@setOnMapClickListener
                } else if (selectedType == "终点") {
                    //终点
                    endMarker?.remove()
                    endPoint = toMyLatLon(latLng)
                    Log.d(
                        TAG,
                        "DDDDDDDDDDDDDDDDDDDDD------->终点:${latLng.latitude}---${latLng.longitude}"
                    )
                    val markerOptions = MarkerOptions()
                        .position(latLng)  // 获取点击的位置
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))  // 设置标记图标（可自定义）
                    endMarker = aMap?.addMarker(markerOptions)  // 将标记添加到地图上
                    isPointEndStatus = false
                    var isLineIntersectPolygon = false
                    listBanAreas.forEach {
                        val testResult = MapUtils.isLineIntersectPolygon(startPoint, endPoint, it)
                        if (testResult) {
                            isLineIntersectPolygon = true;
                        }
                        Log.d(
                            TAG,
                            "isLineIntersectPolygon testResult = " + testResult + ", it = " + it
                        )
                    }
                    ToastUtils.showToast(if (isLineIntersectPolygon) "经过禁飞区,危险！！" else "路径安全，一路平安！！")
                    drawPathOnAMap(
                        aMap!!,
                        arrayListOf(startPoint, endPoint),
                        if (isLineIntersectPolygon) Color.RED else Color.GREEN
                    )
                    return@setOnMapClickListener
                }
            }
            drawFencePolygon(fenceList)
            listBanAreas.forEach {
                Log.d(TAG, "draw listBanAreas = " + it)
                drawBanPolygon(it)

            }
        }

    }

    // 生命周期方法同步，重要！
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }


    /**
     * 检查并申请所有权限
     */
    private fun checkAndRequestPermissions() {
        for (eachPermission in PermissionConstance.REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    eachPermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermission.add(eachPermission)
            }
        }
        initMap()
        if (missingPermission.isEmpty()) {
            initMap()
        } else {
            ActivityCompat.requestPermissions(
                this,
                missingPermission.toTypedArray<String>(),
                PermissionConstance.REQUEST_PERMISSION_CODE
            )
        }
    }

    /**
     * 权限回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for granted permission and remove from missing list
        if (requestCode == PermissionConstance.REQUEST_PERMISSION_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    //在用户已经拒绝授权的情况下，如果shouldShowRequestPermissionRationale返回false则
                    // 可以推断出用户选择了“不在提示”选项，在这种情况下需要引导用户至设置页手动授权
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity,
                            permissions[i]!!
                        )
                    ) {
                        Log.d(TAG, "permission error")
                    }
                    break
                }
            }
        }

    }

    private fun drawBanPolygon(points: ArrayList<MyLatLng>) {
        Log.d(TAG, "drawBanPolygon " + points.joinToString())
        val newPoints = points.map { toLatLon(it) }
        val polygonOptions = PolygonOptions()
            .addAll(newPoints)
            .fillColor(Color.argb(76, 255, 0, 0))
            .strokeWidth(2f)
        aMap?.addPolygon(polygonOptions)
    }

    private fun drawFencePolygon(points: ArrayList<MyLatLng>) {
        Log.d(TAG, "drawFencePolygon " + points.joinToString())
        val newPoints = points.map { toLatLon(it) }
        val polygonOptions = PolygonOptions()
            .addAll(newPoints)
            .fillColor(Color.argb(30, 0, 0, 255))
            .strokeWidth(2f)
        aMap?.addPolygon(polygonOptions)
    }


    /**
     * 在高德地图上绘制路径关键点的折线
     * @param aMap 高德地图实例
     * @param keyPoints 路径关键点列表（PathUtil.LatLng）
     * @param color 折线颜色（ARGB格式，例如0xFF0000FF为蓝色）
     * @param width 折线宽度（单位：像素）
     */
    fun drawPathOnAMap(
        aMap: AMap,
        keyPoints: List<MyLatLng>,
        color: Int = 0xFF0000FF.toInt(), // 默认蓝色
        width: Float = 8f // 默认宽度5像素
    ) {
        if (keyPoints.isEmpty()) {
            Log.d(TAG, "DDDDDDDDDDDDD------>关键点列表为空，无法绘制路径")
            return
        }

        mPathPolyline?.remove()
        // 将 PathUtil.LatLng 转换为 AMapLatLng
        val aMapPoints = keyPoints.map { LatLng(it.latitude, it.longitude) }

        // 打印路径信息
        Log.d(TAG, "DDDDDDDDDDDDD---->aMapPoints-size:${aMapPoints.size}")
        keyPoints.forEach {
            Log.d(TAG, "Lat: ${it.latitude}, Lng: ${it.longitude}")
        }
        // 在高德地图上绘制折线
        mPathPolyline = aMap.addPolyline(
            PolylineOptions()
                .addAll(aMapPoints) // 添加所有关键点
                .color(color) // 设置折线颜色
                .width(width)       // 设置折线宽度

        )
    }


}