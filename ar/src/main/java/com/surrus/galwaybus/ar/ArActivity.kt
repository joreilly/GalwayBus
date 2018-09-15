package com.surrus.galwaybus.ar

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Range
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.ar.core.*
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.orhanobut.logger.Logger
import com.surrus.galwaybus.ar.rendering.BackgroundRenderer
import com.surrus.galwaybus.ar.rendering.ObjectRenderer
import com.surrus.galwaybus.ar.rendering.PlaneRenderer
import com.surrus.galwaybus.ar.rendering.PointCloudRenderer
import com.surrus.galwaybus.model.BusStop
import com.surrus.galwaybus.model.Location
import com.surrus.galwaybus.ui.data.ResourceState
import com.surrus.galwaybus.ui.viewmodel.NearestBusStopsViewModel
import dagger.android.AndroidInjection
import java.io.IOException
import java.util.ArrayList
import javax.inject.Inject
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class ArActivity : AppCompatActivity(), GLSurfaceView.Renderer, SensorEventListener {

    @Inject
    lateinit var nearestBusStopsViewModelFactory: NearestBusStopsViewModelFactory

    private lateinit var nearestBusStopsViewModel : NearestBusStopsViewModel

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var mSession: Session

    private var mLocation: android.location.Location? = null

    private var busStopList: List<BusStop> = emptyList()

    val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private lateinit var mDisplayRotationHelper: DisplayRotationHelper

    private val mBackgroundRenderer = BackgroundRenderer()
    private val mVirtualObject = ObjectRenderer()
    private val mVirtualObjectShadow = ObjectRenderer()
    private val mPlaneRenderer = PlaneRenderer()
    private val mPointCloud = PointCloudRenderer()

    private val mAnchors = ArrayList<Anchor>()

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val mAnchorMatrix = FloatArray(16)


    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        nearestBusStopsViewModel = ViewModelProviders.of(this, nearestBusStopsViewModelFactory).get(NearestBusStopsViewModel::class.java)


        mDisplayRotationHelper = DisplayRotationHelper(/*context=*/this)

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true)
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.
        surfaceView.setRenderer(this)
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)

        var exception: Exception? = null
        var message: String? = null
        try {
            mSession = Session(/* context= */this)
        } catch (e: UnavailableArcoreNotInstalledException) {
            message = "Please install ARCore"
            exception = e
        } catch (e: UnavailableApkTooOldException) {
            message = "Please update ARCore"
            exception = e
        } catch (e: UnavailableSdkTooOldException) {
            message = "Please update this app"
            exception = e
        } catch (e: Exception) {
            message = "This device does not support AR"
            exception = e
        }


        if (message != null) {
            //showSnackbarMessage(message, true)
            Logger.e("Exception creating session", exception)
            return
        }

        // Create default config and check if supported.
        val config = Config(mSession)
        if (!mSession.isSupported(config)) {
            //showSnackbarMessage("This device does not support AR", true)
        }
        mSession.configure(config)

    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Standard Android full-screen functionality.
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }


    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            if (mSession != null) {
                //showLoadingMessage()
                // Note that order matters - see the note in onPause(), the reverse applies here.
                mSession.resume()
            }
            surfaceView.onResume()
            mDisplayRotationHelper.onResume()



            if (nearestBusStopsViewModel.getLocation() == null) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->

                    mLocation = location

                    var loc: Location? = null
                    if (location != null) {
                        loc = Location(location.latitude, location.longitude)
                    } else {
                        loc = Location(53.273849, -9.049695) // default if we can't get location
                    }
                    nearestBusStopsViewModel.setLocation(loc)
                    nearestBusStopsViewModel.setCameraPosition(loc)



                    nearestBusStopsViewModel.pollForNearestBusStopTimes()

                    nearestBusStopsViewModel.busStops.observe(this) {
                        if (it != null && it.status == ResourceState.SUCCESS) {
                            Logger.d("got bus stops")

                            busStopList = it.data!!

                            busStopList.forEach {
                                var busStopLocation = android.location.Location(it.shortName)
                                busStopLocation.latitude = it.latitude
                                busStopLocation.longitude = it.longitude

                                val distance = location.distanceTo(busStopLocation)
                                Logger.d(distance)

                                val bearing = mLocation!!.bearingTo(busStopLocation)


                                //val m = FloatArray(16)
                                //Matrix.rotateM(m, 0, b, 0f, 1f, 0f)

                                //azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                                //pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()


                                //val pose = Pose.makeRotation(m)
                                //val anchor = mSession.createAnchor(pose)
                                //mAnchors.add(anchor)

                            }


                        }
                    }


                    sensorManager.registerListener(this,
                            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                            SensorManager.SENSOR_DELAY_FASTEST)

                }
            } else {
                nearestBusStopsViewModel.setCameraPosition(nearestBusStopsViewModel.getLocation()!!)
            }


        } else {
            CameraPermissionHelper.requestCameraPermission(this)
        }
    }

    public override fun onPause() {
        super.onPause()

        nearestBusStopsViewModel.stopPolling()

        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        mDisplayRotationHelper.onPause()
        surfaceView.onPause()
        if (mSession != null) {
            mSession.pause()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }



    override fun onDrawFrame(gl: GL10?) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (mSession == null) {
            return
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession)

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            val frame = mSession.update()
            val camera = frame.camera

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
/*
            val tap = mQueuedSingleTaps.poll()
            if (tap != null && camera.trackingState == Trackable.TrackingState.TRACKING) {
                for (hit in frame.hitTest(tap!!)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon
                    val trackable = hit.trackable
                    if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                        // Cap the number of objects created. This avoids overloading both the
                        // rendering system and ARCore.
                        if (mAnchors.size >= 20) {
                            mAnchors.get(0).detach()
                            mAnchors.removeAt(0)
                        }
                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor is created on the Plane to place the 3d model
                        // in the correct position relative both to the world and to the plane.
                        mAnchors.add(hit.createAnchor())

                        // Hits are sorted by depth. Consider only closest hit on a plane.
                        break
                    }
                }
            }
*/

            // Draw background.
            mBackgroundRenderer.draw(frame)

            // If not tracking, don't draw 3d objects.
            if (camera.trackingState == Trackable.TrackingState.PAUSED) {
                return
            }

            // Get projection matrix.
            val projmtx = FloatArray(16)
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f)

            // Get camera matrix and draw.
            val viewmtx = FloatArray(16)
            camera.getViewMatrix(viewmtx, 0)

            // Compute lighting from average intensity of the image.
            val lightIntensity = frame.lightEstimate.pixelIntensity

            // Visualize tracked points.
            val pointCloud = frame.acquirePointCloud()
            mPointCloud.update(pointCloud)
            mPointCloud.draw(viewmtx, projmtx)

            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release()

            // Check if we detected at least one plane. If so, hide the loading message.
//            if (mMessageSnackbar != null) {
//                for (plane in mSession.getAllTrackables(Plane::class.java)) {
//                    if (plane.type == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING && plane.trackingState == Trackable.TrackingState.TRACKING) {
//                        hideLoadingMessage()
//                        break
//                    }
//                }
//            }

            // Visualize planes.
            //mPlaneRenderer.drawPlanes(mSession.getAllTrackables(Plane::class.java), camera.displayOrientedPose, projmtx)



            // Visualize anchors created by touch.
            val scaleFactor = 1.0f


            //frame.camera.pose.
            busStopList.forEach {


//                Matrix.multiplyMM(viewmtx, 0, viewmtx, 0, marker.getZeroMatrix(), 0)
//
//                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor)
//                mVirtualObject.draw(viewmtx, projmtx, lightIntensity)


            }


            for (anchor in mAnchors) {
                if (anchor.getTrackingState() != Trackable.TrackingState.TRACKING) {
                    continue
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                anchor.getPose().toMatrix(mAnchorMatrix, 0)

                // Update and draw the model and its shadow.
                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor)
                mVirtualObjectShadow.updateModelMatrix(mAnchorMatrix, scaleFactor)
                mVirtualObject.draw(viewmtx, projmtx, lightIntensity)
                mVirtualObjectShadow.draw(viewmtx, projmtx, lightIntensity)
            }





        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Logger.e( "Exception on the OpenGL thread", t)
        }

    }


//    fun getCalibrationMatrix(frame: Frame): FloatArray {
//        val t = FloatArray(3)
//        val m = FloatArray(16)
//
//        frame.camera.getPose().getTranslation(t, 0)
//        val z = frame.camera.getPose().getZAxis()
//        val zAxis = Vector3f(z[0], z[1], z[2])
//        zAxis.y = 0
//        zAxis.normalize()
//
//        val rotate = Math.atan2(zAxis.x, zAxis.z)
//
//        Matrix.setIdentityM(m, 0)
//        Matrix.translateM(m, 0, t[0], t[1], t[2])
//        Matrix.rotateM(m, 0, Math.toDegrees(rotate).toFloat(), 0f, 1f, 0f)
//        return m
//    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mDisplayRotationHelper.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/this)
        if (mSession != null) {
            mSession.setCameraTextureName(mBackgroundRenderer.textureId)
        }

        // Prepare the other rendering objects.
        try {
            mVirtualObject.createOnGlThread(/*context=*/this, "andy.obj", "andy.png")
            mVirtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f)

            mVirtualObjectShadow.createOnGlThread(/*context=*/this,
                    "andy_shadow.obj", "andy_shadow.png")
            mVirtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow)
            mVirtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f)
        } catch (e: IOException) {
            Logger.e( "Failed to read obj file")
        }

        try {
            mPlaneRenderer.createOnGlThread(/*context=*/this, "trigrid.png")
        } catch (e: IOException) {
            Logger.e( "Failed to read plane texture")
        }

        mPointCloud.createOnGlThread(/*context=*/this)
    }




    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        Logger.d(sensorEvent.values.zip("XYZ".toList()).fold("") { acc, pair -> "$acc${pair.second}: ${pair.first}\n" } )


        if (sensorEvent.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {

            var azimuth: Float
            var pitch: Float
            var bearing: Float
            var azimuthRange: Range<Float>
            var pitchRange: Range<Float>

            val rotationMatrixFromVector = FloatArray(16)
            val updatedRotationMatrix = FloatArray(16)
            val orientationValues = FloatArray(3)

            SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, sensorEvent.values)
            SensorManager
                    .remapCoordinateSystem(rotationMatrixFromVector,
                            SensorManager.AXIS_X, SensorManager.AXIS_Y,
                            updatedRotationMatrix)
            SensorManager.getOrientation(updatedRotationMatrix, orientationValues)


            val quaternion = FloatArray(4)
            SensorManager.getQuaternionFromVector(quaternion, orientationValues)

            if (busStopList.isEmpty() || mLocation == null) {
                return
            }

            busStopList.forEach {

                var busStopLocation = android.location.Location(it.shortName)
                busStopLocation.latitude = it.latitude
                busStopLocation.longitude = it.longitude

                bearing = mLocation!!.bearingTo(busStopLocation)
                azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()

                azimuthRange = Range(bearing - 10, bearing + 10)
                pitchRange = Range(-90.0f, -45.0f)

                var inRange: Boolean = false
                if (azimuthRange.contains(azimuth) && pitchRange.contains(pitch)) {
                    inRange = true
                }
            }
        }

    }

}
