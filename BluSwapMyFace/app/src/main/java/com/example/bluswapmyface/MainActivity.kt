package com.example.bluswapmyface

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.divyanshu.draw.widget.DrawView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private var imageUri1: Uri? = null
    private var imageUri2: Uri? = null

    private var imageBitmap1: Bitmap? = null
    private var imageBitmap2: Bitmap? = null

    private var faceBitmap1: Bitmap? = null
    private var faceBitmap2: Bitmap? = null

    private var face1Bound: Rect? = null
    private var face2Bound: Rect? = null

    private var mydrawView1: DrawView? = null
    private var mydrawView2: DrawView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mydrawView1 = findViewById(R.id.draw_view1)
        mydrawView2 = findViewById(R.id.draw_view2)

        getRuntimePermissions()
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }
        savedInstanceState?.let {
            imageUri1 = it.getParcelable(KEY_IMAGE_URI1)
            imageUri2 = it.getParcelable(KEY_IMAGE_URI2)
        }
        tryReloadAndDisplayImage1()
        tryReloadAndDisplayImage2()
    }
    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = this.packageManager.getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }


    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) { putParcelable(KEY_IMAGE_URI1, imageUri1)
        }
        with(outState) { putParcelable(KEY_IMAGE_URI2, imageUri2)
        }
    }

    fun findFace1( imagBitmap: Bitmap ) {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

        var firebaseImage = FirebaseVisionImage.fromBitmap( imagBitmap )
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        val result = detector.detectInImage(firebaseImage).addOnSuccessListener { faces ->
            for (face in faces) {
                face1Bound = face.boundingBox
                if (face1Bound != null) {
                    faceBitmap1 = Bitmap.createBitmap(
                        imagBitmap,
                        face1Bound!!.left, face1Bound!!.top, face1Bound!!.width(), face1Bound!!.height()
                    )
                    Toast.makeText(applicationContext,"Face1 detected",Toast.LENGTH_SHORT).show()
                }

                val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                // nose available):
                val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                leftEar?.let {
                    val leftEarPos = leftEar.position
                }

                // If classification was enabled:
                if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    val smileProb = face.smilingProbability
                }
                var leftEyeOpenProb = 1.0f
                var rightEyeOpenProb = 1.0f
                if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    rightEyeOpenProb = face.rightEyeOpenProbability
                }
                if (face.leftEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    leftEyeOpenProb = face.leftEyeOpenProbability
                }

                if( leftEyeOpenProb < 0.5 || rightEyeOpenProb < 0.5 )
                {
                    Toast.makeText(applicationContext,"Open your eyes!!!",Toast.LENGTH_SHORT).show()
                }

                // If face tracking was enabled:
                if (face.trackingId != FirebaseVisionFace.INVALID_ID) {
                    val id = face.trackingId
                }
            }
        }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext,"No face detected",Toast.LENGTH_SHORT).show()
            }
    }

    fun findFace2( imagBitmap: Bitmap ) {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        var firebaseImage = FirebaseVisionImage.fromBitmap( imagBitmap )
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        val result = detector.detectInImage(firebaseImage).addOnSuccessListener { faces ->
            for (face in faces) {
                face2Bound = face.boundingBox
                if (face2Bound != null) {
                    faceBitmap2 = Bitmap.createBitmap(
                        imagBitmap,
                        face2Bound!!.left, face2Bound!!.top, face2Bound!!.width(), face2Bound!!.height()
                    )
                    Toast.makeText(applicationContext,"Face2 detected",Toast.LENGTH_SHORT).show()
                }

                val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                // nose available):
                val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                leftEar?.let {
                    val leftEarPos = leftEar.position
                }

                // If classification was enabled:
                if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    val smileProb = face.smilingProbability
                }
                var leftEyeOpenProb = 1.0f
                var rightEyeOpenProb = 1.0f
                if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    rightEyeOpenProb = face.rightEyeOpenProbability
                }
                if (face.leftEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                    leftEyeOpenProb = face.leftEyeOpenProbability
                }

                if( leftEyeOpenProb < 0.5 || rightEyeOpenProb < 0.5 )
                {
                    Toast.makeText(applicationContext,"Open your eyes!!!",Toast.LENGTH_SHORT).show()
                }

                // If face tracking was enabled:
                if (face.trackingId != FirebaseVisionFace.INVALID_ID) {
                    val id = face.trackingId
                }
            }

        }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext,"No face detected",Toast.LENGTH_SHORT).show()
            }
    }

    fun startCameraIntentForResult1(view: View) {
        imageUri1 = null
        imageView1?.setImageBitmap(null)

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture1")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            imageUri1 = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri1)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    fun startCameraIntentForResult2(view: View) {
        imageUri2 = null
        imageView2?.setImageBitmap(null)

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture2")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            imageUri2 = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri2)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    fun startChooseImageIntentForResult1(view: View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE1)
    }

    fun startChooseImageIntentForResult2(view: View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            tryReloadAndDisplayImage1()
            tryReloadAndDisplayImage2()
        } else if (requestCode == REQUEST_CHOOSE_IMAGE1 && resultCode == Activity.RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri1 = data!!.data
            tryReloadAndDisplayImage1()
        } else if (requestCode == REQUEST_CHOOSE_IMAGE2 && resultCode == Activity.RESULT_OK) {
            imageUri2 = data!!.data
            tryReloadAndDisplayImage2()
        }
    }

    private fun tryReloadAndDisplayImage1() {
        try {
            if (imageUri1 == null) {
                return
            }
            val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri1)
            } else {
                val source = ImageDecoder.createSource(contentResolver, imageUri1!!)
                ImageDecoder.decodeBitmap(source)
            }
            findFace1( imageBitmap )
            //imageView1?.setImageBitmap(imageBitmap)
            imageBitmap1 = imageBitmap
            mydrawView1?.background= BitmapDrawable(resources, imageBitmap);
        } catch (e: IOException) {
        }
    }
    private fun tryReloadAndDisplayImage2() {
        try {
            if (imageUri2 == null) {
                return
            }
            val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri2)
            } else {
                val source = ImageDecoder.createSource(contentResolver, imageUri2!!)
                ImageDecoder.decodeBitmap(source)
            }
            findFace2( imageBitmap )

            //imageView2?.setImageBitmap(imageBitmap)
            imageBitmap2 = imageBitmap
            mydrawView2?.background= BitmapDrawable(resources, imageBitmap);
        } catch (e: IOException) {
        }
    }

    fun swapFaces(view: View)
    {
        //imageBitmap1?.let { findFace1(it) }
        //imageBitmap2?.let { findFace2(it) }
        if( imageBitmap1 != null && imageBitmap2 != null && faceBitmap1!= null && faceBitmap2!=null ) {
            var swap1 = Bitmap.createBitmap(
                imageBitmap1!!.getWidth(),
                imageBitmap1!!.getHeight(),
                imageBitmap1!!.getConfig()
            )

            var canvas1 = Canvas(swap1)

            canvas1.drawBitmap(imageBitmap1!!, Matrix(), null )
            canvas1.drawBitmap(faceBitmap2!!, face1Bound!!.left.toFloat(), face1Bound!!.top.toFloat(), null )

            var swap2 = Bitmap.createBitmap(
                imageBitmap2!!.getWidth(),
                imageBitmap2!!.getHeight(),
                imageBitmap2!!.getConfig()
            )

            var canvas2 = Canvas(swap2)
            canvas2.drawBitmap(imageBitmap2!!, Matrix(), null )
            canvas2.drawBitmap(faceBitmap1!!, face2Bound!!.left.toFloat(), face2Bound!!.top.toFloat(), null )

            mydrawView1?.background= BitmapDrawable(resources, swap1);
            mydrawView2?.background= BitmapDrawable(resources, swap2);
        }

        //mydrawView2?.background= BitmapDrawable(resources, imageBitmap1);
    }

    fun undoSwap(view: View)
    {
        mydrawView1?.background= BitmapDrawable(resources, imageBitmap1);
        mydrawView2?.background= BitmapDrawable(resources, imageBitmap2);
    }

    fun blurImage1( view: View ) {
        try {
            if (imageUri1 == null) {
                return
            }
            val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri1)
            } else {
                val source = ImageDecoder.createSource(contentResolver, imageUri1!!)
                ImageDecoder.decodeBitmap(source)
            }
            var imageBitmapblur = bitmapBlur( imageBitmap, 0.2f, 32)
            //imageView1?.setImageBitmap(imageBitmapblur)
            mydrawView1?.background= BitmapDrawable(resources, imageBitmapblur);
        } catch (e: IOException) {
        }
    }
    fun blurImage2( view: View ) {
        try {
            if (imageUri2 == null) {
                return
            }
            val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri2)
            } else {
                val source = ImageDecoder.createSource(contentResolver, imageUri2!!)
                ImageDecoder.decodeBitmap(source)
            }
            var imageBitmapblur = bitmapBlur( imageBitmap, 0.2f, 32)
            //imageView2?.setImageBitmap(imageBitmapblur)
            mydrawView2?.background= BitmapDrawable(resources, imageBitmapblur);
        } catch (e: IOException) {
        }
    }
    fun clearBlur1( view: View )
    {
        if( mydrawView1?.mPaths != null ) {
            mydrawView1?.mPaths?.clear()
        }
        if ( imageBitmap1 != null )
        {
            mydrawView1?.background= BitmapDrawable(resources, imageBitmap1);
        }
    }
    fun clearBlur2( view: View )
    {
        if( mydrawView2?.mPaths != null ) {
            mydrawView2?.mPaths?.clear()
        }
        if ( imageBitmap2 != null )
        {
            mydrawView2?.background= BitmapDrawable(resources, imageBitmap2);
        }
    }


    fun bitmapBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        var sentBitmap = sentBitmap
        val width = Math.round(sentBitmap.width * scale)
        val height = Math.round(sentBitmap.height * scale)
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
        val bitmap = sentBitmap.copy(sentBitmap.config, true)
        if (radius < 1) {
            return null
        }
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yi = 0
        yw = yi
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
// Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    companion object {
        private const val KEY_IMAGE_URI1 = "edu.uw.eep523.bluswapmyface.KEY_IMAGE_URI1"
        private const val KEY_IMAGE_URI2 = "edu.uw.eep523.bluswapmyface.KEY_IMAGE_URI2"
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_CHOOSE_IMAGE1 = 1002
        private const val REQUEST_CHOOSE_IMAGE2 = 1003
        private const val PERMISSION_REQUESTS = 1
    }

}
