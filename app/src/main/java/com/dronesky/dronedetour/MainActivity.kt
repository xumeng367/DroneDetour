package com.dronesky.dronedetour

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
import com.amap.api.maps.model.PolygonOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.dronesky.detour.DetourGoHomeManager
import com.dronesky.detour.GeoUtils
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

/**
 * for map drawing
 */
enum class MapMode {
    FENCE, NOFLYZONE, STARTPOINT, ENDPOINT,
}

class MainActivity : ComponentActivity() {
    private var TAG = "MainActivity"
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
    private lateinit var btn_cancel_set_point: Button
    private lateinit var btn_set_fence_point: Button

    private var noFlyZoneList = ArrayList<MyLatLng>()
    private var fencePoints = ArrayList<MyLatLng>()
    private var isPointStartStatus = false;
    private var isPointEndStatus = false;
    private lateinit var et_type: Spinner
    private var selectedType = MapMode.FENCE
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private var mPathPolyline: Polyline? = null;

    //no fly zones
    private var listBanAreas: MutableList<MutableList<MyLatLng>> = ArrayList()

    //fence
    private var fenceList: ArrayList<MyLatLng> = ArrayList()

    private var flypathPoints = ArrayList<MyLatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)

        initSpData()
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        buttonCreateStartPoint = findViewById(R.id.btn_set_start_point)
        buttonCreateEndPoint = findViewById(R.id.btn_set_end_point)
        setBanAreaButton = findViewById(R.id.btn_set_avoid_point)
        generateKeyPoint = findViewById(R.id.btn_set_key_points)
        btn_cancel_set_point = findViewById(R.id.btn_cancel_set_point)
        btn_set_fence_point = findViewById(R.id.btn_set_fence_point)
        et_type = findViewById(R.id.spinner_type)
        //modeList
        val types = arrayOf(
//            MapMode.FENCE.name,
            MapMode.NOFLYZONE.name,
            MapMode.STARTPOINT.name,
            MapMode.ENDPOINT.name
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        et_type.adapter = adapter
        et_type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                selectedType = MapMode.valueOf(types[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }


        btn_cancel_set_point.setOnClickListener {
            isCreatePointStatus = false
            noFlyZoneList.clear()
            fencePoints.clear()
            fenceList.clear()
            flypathPoints.clear()
            listBanAreas.clear()
            aMap?.clear()
            SpUtil.putString("fence_list_str", "")
            SpUtil.putString("ban_list_str", "")

        }

        setBanAreaButton.setOnClickListener {

            drawBanPolygon(noFlyZoneList)
            listBanAreas.add(ArrayList(noFlyZoneList))  // 创建副本
            noFlyZoneList.clear()
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

        }

        generateKeyPoint.setOnClickListener {
            drawBanPolygon(noFlyZoneList)
        }
        btn_set_fence_point.setOnClickListener {
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

            Log.d(TAG, "start calculate detour path")

            Thread {
                val listLatLng: ArrayList<MyLatLng> = ArrayList()
                DetourGoHomeManager.getsInstance().updateNoFlyZones(listBanAreas)
                val startTime = System.currentTimeMillis()

                val path = DetourGoHomeManager.getsInstance()
                    .calculateDetourPath(listOf(startPoint, endPoint))
                val endTime = System.currentTimeMillis()
                val costTime = (endTime - startTime)
                Log.d(TAG, "costTime =  $costTime")
                if (path == null || path.isEmpty()) {
                    ToastUtils.showToast("no available path cost：$costTime")
                } else {
                    ToastUtils.showToast("find path cost：$costTime")
                    listLatLng.addAll(path)
                }


                val distance = GeoUtils.haversine(
                    startPoint.latitude, startPoint.longitude, endPoint.latitude, endPoint.longitude
                )
                Log.d(TAG, "end distance：$distance")
                if (listLatLng.isEmpty()) {
                    Log.d(TAG, "can't find a path")
                } else {
                    MainHandler.post({
                        drawPathOnAMap(aMap!!, listLatLng)
                        ToastUtils.showToast("Path generation finished！");

                    }, 0)
                }


            }.start()
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
            listBanAreas.forEach {
                Log.d(TAG, "initSpData listBanAreas = " + it)
            }
        }


    }

    private fun clearPoints() {
        startPoint = MyLatLng(0.0, 0.0)
        endPoint = MyLatLng(0.0, 0.0)
        noFlyZoneList.clear()
    }


    private fun initMap() {
        if (aMap == null) {
            aMap = mapView.map
            aMap?.uiSettings?.isZoomControlsEnabled = true
            aMap?.uiSettings?.isMyLocationButtonEnabled = true
            aMap?.isMyLocationEnabled = true
            val latLng = LatLng(36.09406114093818, 120.38552731431761)
            // init camera position
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14f)
            aMap?.moveCamera(cameraUpdate)

            aMap?.setOnMapClickListener { latLng ->
                currentClickedPoint = latLng
                //", "", "", "")
                if (selectedType == MapMode.FENCE) {
                    fenceList.add(toMyLatLon(latLng))
                    val markerOptions = MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    aMap?.addMarker(markerOptions)
                    return@setOnMapClickListener
                } else if (selectedType == MapMode.NOFLYZONE) {
                    noFlyZoneList.add(toMyLatLon(latLng))
                    val markerOptions = MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    aMap?.addMarker(markerOptions)
                    return@setOnMapClickListener
                } else if (selectedType == MapMode.STARTPOINT) {
                    startMarker?.remove()
                    startPoint = toMyLatLon(latLng)
                    val markerOptions = MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    startMarker = aMap?.addMarker(markerOptions)
                    isPointStartStatus = false
                    return@setOnMapClickListener
                } else if (selectedType == MapMode.ENDPOINT) {
                    endMarker?.remove()
                    endPoint = toMyLatLon(latLng)
                    val markerOptions = MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    endMarker = aMap?.addMarker(markerOptions)
                    isPointEndStatus = false
                    var isLineIntersectPolygon = false
                    listBanAreas.forEach {
                        val testResult = MapUtils.isLineIntersectPolygon(startPoint, endPoint, it)
                        if (testResult) {
                            isLineIntersectPolygon = true;
                        }
                    }
                    ToastUtils.showToast(if (isLineIntersectPolygon) "Across NoFlyZones！！" else "You are safe")
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
                Log.d(TAG, "draw listBanAreas = $it")
                drawBanPolygon(it)

            }
        }

    }

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


    private fun checkAndRequestPermissions() {
        for (eachPermission in PermissionConstance.REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(
                    this, eachPermission
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for granted permission and remove from missing list
        if (requestCode == PermissionConstance.REQUEST_PERMISSION_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity, permissions[i]!!
                        )
                    ) {
                        Log.d(TAG, "permission error")
                    }
                    break
                }
            }
        }

    }

    private fun drawBanPolygon(points: MutableList<MyLatLng>) {
        Log.d(TAG, "drawBanPolygon " + points.joinToString())
        val newPoints = points.map { toLatLon(it) }
        val polygonOptions =
            PolygonOptions().addAll(newPoints).fillColor(Color.argb(76, 255, 0, 0)).strokeWidth(2f)
        aMap?.addPolygon(polygonOptions)
    }

    private fun drawFencePolygon(points: ArrayList<MyLatLng>) {
        Log.d(TAG, "drawFencePolygon " + points.joinToString())
        val newPoints = points.map { toLatLon(it) }
        val polygonOptions =
            PolygonOptions().addAll(newPoints).fillColor(Color.argb(30, 0, 0, 255)).strokeWidth(2f)
        aMap?.addPolygon(polygonOptions)
    }

    fun drawPathOnAMap(
        aMap: AMap, keyPoints: List<MyLatLng>, color: Int = 0xFF0000FF.toInt(),
        width: Float = 8f
    ) {
        if (keyPoints.isEmpty()) {
            Log.d(TAG, "drawPathOnAMap list is empty")
            return
        }

        mPathPolyline?.remove()
        val aMapPoints = keyPoints.map { LatLng(it.latitude, it.longitude) }
        keyPoints.forEach {
            Log.d(TAG, "Lat: ${it.latitude}, Lng: ${it.longitude}")
        }
        mPathPolyline = aMap.addPolyline(
            PolylineOptions().addAll(aMapPoints) //
                .color(color)
                .width(width)
        )
    }
}