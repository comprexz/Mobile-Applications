package com.example.driveawake

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import java.nio.ByteBuffer
import kotlin.math.absoluteValue


class CameraService: Service() {

    private var wm: WindowManager? = null
    private var rootView: View? = null
    private var textureView: TextureView? = null

    private var cameraManager: CameraManager? = null
    private var previewSize: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var captureRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var analyzed = true

    private var withCam = true

    private var width = 480
    private var height = 360

    var eyeClosedCounter = 0

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when(intent?.action) {
            ACTION_START -> start()

            ACTION_START_NO_CAM -> startNoCam()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {

        super.onCreate()
        startForeground()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopCamera()

        if (rootView != null)
            wm?.removeView(rootView)

        sendBroadcast(Intent(ACTION_STOPPED))
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {}

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {}
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            initCam(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }
    private fun imageToByteBuffer(image: Image): ByteBuffer? {
        val crop: Rect = image.getCropRect()
        val width = crop.width()
        val height = crop.height()
        val planes: Array<Image.Plane> = image.getPlanes()
        val rowData = ByteArray(planes[0].getRowStride())
        val bufferSize =
            width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8
        val output: ByteBuffer = ByteBuffer.allocateDirect(bufferSize)
        var channelOffset = 0
        var outputStride = 0
        for (planeIndex in 0..2) {
            if (planeIndex == 0) {
                channelOffset = 0
                outputStride = 1
            } else if (planeIndex == 1) {
                channelOffset = width * height + 1
                outputStride = 2
            } else if (planeIndex == 2) {
                channelOffset = width * height
                outputStride = 2
            }
            val buffer: ByteBuffer = planes[planeIndex].getBuffer()
            val rowStride: Int = planes[planeIndex].getRowStride()
            val pixelStride: Int = planes[planeIndex].getPixelStride()
            val shift = if (planeIndex == 0) 0 else 1
            val widthShifted = width shr shift
            val heightShifted = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until heightShifted) {
                val length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = widthShifted
                    buffer.get(output.array(), channelOffset, length)
                    channelOffset += length
                } else {
                    length = (widthShifted - 1) * pixelStride + 1
                    buffer.get(rowData, 0, length)
                    for (col in 0 until widthShifted) {
                        output.array()[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < heightShifted - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return output
    }

    fun Bitmap.rotate(degree:Int):Bitmap{
        // Initialize a new matrix
        val matrix = Matrix()

        // Rotate the bitmap
        matrix.postRotate(degree.toFloat())

        // Resize the bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(
            this,
            width,
            height,
            true
        )

        // Create and return the rotated bitmap
        return Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(2500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(2500)
        }
    }

    var notAnalyzedCounter = 0
    var alertDialog: AlertDialog? = null
    @RequiresApi(Build.VERSION_CODES.M)
    private val imageListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader?.acquireLatestImage()
        //Log.d(TAG, "Got image: " + image?.width + " x " + image?.height)

        if ( eyeClosedCounter > 1 )
        {
            vibratePhone()
            try {
                val notification: Uri =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val r: Ringtone = RingtoneManager.getRingtone(applicationContext, notification)
                r.play()
                eyeClosedCounter = 0
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            MainActivity.instance?.eyeClosed()
        }

        if(analyzed)
        {
            notAnalyzedCounter = 0
            //Log.d(TAG, "analyzed")
        }else
        {
            notAnalyzedCounter++
            //Log.d(TAG, "not analyzed")
            if ( notAnalyzedCounter == 100 )
            {
                analyzed = true
                notAnalyzedCounter = 0
                //Log.d(TAG, "change to analyzed")
            }
        }

        if ( image != null && analyzed ) {
            analyzed = false
            val yuvBytes = imageToByteBuffer(image)
            val rs: RenderScript = RenderScript.create(this)
            var bitmap =
                Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            val allocationRgb: Allocation = Allocation.createFromBitmap(rs, bitmap)
            val allocationYuv: Allocation =
                Allocation.createSized(rs, Element.U8(rs), yuvBytes!!.array().size)
            allocationYuv.copyFrom(yuvBytes.array())
            val scriptYuvToRgb: ScriptIntrinsicYuvToRGB =
                ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
            scriptYuvToRgb.setInput(allocationYuv)
            scriptYuvToRgb.forEach(allocationRgb)
            allocationRgb.copyTo(bitmap)
            //Log.d(TAG, "Got bitmap: " + bitmap?.width + " x " + bitmap?.height)

            var copy = bitmap.rotate(270)
            if (copy != null) {
                checkEyes(copy)
            }
            if (bitmap != null && !bitmap!!.isRecycled()) {

                bitmap!!.recycle()
                bitmap = null
                allocationYuv.destroy()
                allocationRgb.destroy()
                rs.destroy()
            }
        }
        image?.close()
    }

    private val stateCallback = object : CameraDevice.StateCallback() {

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onOpened(currentCameraDevice: CameraDevice) {
            cameraDevice = currentCameraDevice
            createCaptureSession()
        }

        override fun onDisconnected(currentCameraDevice: CameraDevice) {
            currentCameraDevice.close()
            cameraDevice = null
        }

        override fun onError(currentCameraDevice: CameraDevice, error: Int) {
            currentCameraDevice.close()
            cameraDevice = null
        }
    }

    private fun startNoCam() {

        withCam = false

        initCam(width, height)
    }

    private fun start() {
        withCam = true

        // Draw on other apps
        initOverlay()

        if (textureView!!.isAvailable)
            initCam(textureView!!.width, textureView!!.height)
        else
            textureView!!.surfaceTextureListener = surfaceTextureListener
    }

    private fun initOverlay() {

        val li = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        rootView = li.inflate(R.layout.camera, null)
        textureView = rootView?.findViewById(R.id.texPreview)

        val type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm!!.addView(rootView, params)
    }

    private fun initCam(width: Int, height: Int) {

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var camId: String? = null

        for (id in cameraManager!!.cameraIdList) {
            val characteristics = cameraManager!!.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                camId = id
                break
            }
        }

        previewSize = chooseSupportedSize(camId!!, width, height)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        cameraManager!!.openCamera(camId, stateCallback, null)
    }

    private fun chooseSupportedSize(camId: String, textureViewWidth: Int, textureViewHeight: Int): Size {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Get all supported sizes for TextureView
        val characteristics = manager.getCameraCharacteristics(camId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedSizes = map?.getOutputSizes(SurfaceTexture::class.java)

        // We want to find something near the size of our TextureView
        val texViewArea = textureViewWidth * textureViewHeight
        val texViewAspect = textureViewWidth.toFloat()/textureViewHeight.toFloat()

        val nearestToFurthestSz = supportedSizes?.sortedWith(compareBy(
            // First find something with similar aspect
            {
                val aspect = if (it.width < it.height) it.width.toFloat() / it.height.toFloat()
                else it.height.toFloat()/it.width.toFloat()
                (aspect - texViewAspect).absoluteValue
            },
            // Also try to get similar resolution
            {
                (texViewArea - it.width * it.height).absoluteValue
            }
        ))


        if (nearestToFurthestSz != null) {
            if (nearestToFurthestSz.isNotEmpty())
                return nearestToFurthestSz[0]
        }

        return Size(width, height)
    }

    private fun startForeground() {

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.app_name))
            .setSmallIcon(R.drawable.notification_template_icon_bg)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.app_name))
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createCaptureSession() {
        try {
            // Prepare surfaces we want to use in capture session
            val targetSurfaces = ArrayList<Surface>()

            // Prepare CaptureRequest that can be used with CameraCaptureSession
            val requestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {

                if ( withCam )
                {
                    val texture = textureView!!.surfaceTexture!!
                    texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
                    val previewSurface = Surface(texture)

                    targetSurfaces.add(previewSurface)
                    addTarget(previewSurface)
                }

                // Configure target surface for background processing (ImageReader)
                imageReader = ImageReader.newInstance(
                    previewSize!!.width, previewSize!!.height,
                    ImageFormat.YUV_420_888, 2
                )
                imageReader!!.setOnImageAvailableListener(imageListener, null)

                targetSurfaces.add(imageReader!!.surface)
                addTarget(imageReader!!.surface)

                // Set some additional parameters for the request
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }

            // Prepare CameraCaptureSession
            cameraDevice!!.createCaptureSession(targetSurfaces,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }

                        captureSession = cameraCaptureSession
                        try {
                            // Now we can start capturing
                            captureRequest = requestBuilder!!.build()
                            captureSession!!.setRepeatingRequest(captureRequest!!, captureCallback, null)

                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "createCaptureSession", e)
                        }

                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Log.e(TAG, "createCaptureSession()")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "createCaptureSession", e)
        }
    }

    private fun stopCamera() {
        try {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val TAG = "CameraService"
        val ACTION_START = "com.example.driveawake.action.START"
        val ACTION_START_NO_CAM = "com.example.driveawake.action.START_NO_CAM"
        val ACTION_STOPPED = "com.example.driveawake.action.STOPPED"

        val ONGOING_NOTIFICATION_ID = 6660
        val CHANNEL_ID = "cam_service_channel_id"
        val CHANNEL_NAME = "cam_service_channel_name"

    }

    fun checkEyes( imagBitmap: Bitmap) {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .setMinFaceSize(0.15f)
            //.enableTracking()
            .build()

        var firebaseImage = FirebaseVisionImage.fromBitmap( imagBitmap )
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        val result = detector.detectInImage(firebaseImage).addOnSuccessListener { faces ->
            for (face in faces) {
                analyzed = true
                var leftEyeOpenProb = 1.0f
                var rightEyeOpenProb = 1.0f
                if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    rightEyeOpenProb = face.rightEyeOpenProbability
                }
                if (face.leftEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    leftEyeOpenProb = face.leftEyeOpenProbability
                }

                if( leftEyeOpenProb < 0.3 && rightEyeOpenProb < 0.3 )
                {
                    eyeClosedCounter++
                    Log.d(TAG, "Eyes closed!")
                } else
                {
                    eyeClosedCounter = 0
                }
                //Log.d(TAG, "L: $leftEyeOpenProb R: $rightEyeOpenProb")

            }
        }
            .addOnFailureListener { e ->
                Log.d(TAG, "No face!")
                analyzed = true
            }
    }

}