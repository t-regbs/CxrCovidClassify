package com.timilehinaregbesola.cxrcovidclassify

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image.Plane
import android.media.ImageReader
import android.os.*
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.timilehinaregbesola.cxrcovidclassify.CameraConnectionFragment.ConnectionCallback
import com.timilehinaregbesola.cxrcovidclassify.databinding.ActivityCameraBinding
import com.timilehinaregbesola.cxrcovidclassify.tflite.Classifier.Device
import com.timilehinaregbesola.cxrcovidclassify.utils.ImageUtils
import timber.log.Timber

abstract class CameraActivity :
    AppCompatActivity(),
    ImageReader.OnImageAvailableListener,
    PreviewCallback,
    View.OnClickListener,
    OnItemSelectedListener {
    private lateinit var binding: ActivityCameraBinding

    protected var previewWidth = 0
    protected var previewHeight = 0
    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var useCamera2API = false
    private var isProcessingFrame = false
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null
    protected var luminanceStride = 0
        private set
    private var postInferenceCallback: Runnable? = null
    private var imageConverter: Runnable? = null
    private var sheetBehavior: BottomSheetBehavior<LinearLayout?>? = null
    private var device: Device = Device.CPU
    private var numThreads = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCameraBinding.inflate(layoutInflater)
        Timber.d("onCreate $this")
        super.onCreate(null)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(binding.root)
        if (hasPermission()) {
            setFragment()
        } else {
            requestPermission()
        }
        sheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout.root)
        val vto = binding.bottomSheetLayout.gestureLayout.viewTreeObserver
        vto.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        binding.bottomSheetLayout.gestureLayout.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    } else {
                        binding.bottomSheetLayout.gestureLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                    //                int width = bottomSheetLayout.getMeasuredWidth();
                    val height = binding.bottomSheetLayout.gestureLayout.measuredHeight
                    sheetBehavior!!.peekHeight = height
                }
            })
        sheetBehavior!!.isHideable = false
        sheetBehavior!!.addBottomSheetCallback(
            object : BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            binding.bottomSheetLayout.bottomSheetArrow.setImageResource(R.drawable.ic_arrow_down)
                        }
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            binding.bottomSheetLayout.bottomSheetArrow.setImageResource(R.drawable.ic_arrow_up)
                        }
                        BottomSheetBehavior.STATE_DRAGGING -> {
                        }
                        BottomSheetBehavior.STATE_SETTLING -> binding.bottomSheetLayout.bottomSheetArrow.setImageResource(
                            R.drawable.ic_arrow_up
                        )
                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                            // Do nothing
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        binding.bottomSheetLayout.deviceSpinner.onItemSelectedListener = this
        binding.bottomSheetLayout.plus.setOnClickListener(this)
        binding.bottomSheetLayout.minus.setOnClickListener(this)
        device = Device.valueOf(binding.bottomSheetLayout.deviceSpinner.selectedItem.toString())
        numThreads = binding.bottomSheetLayout.threads.text.toString().trim { it <= ' ' }.toInt()
    }

    protected fun getRgbBytes(): IntArray? {
        imageConverter!!.run()
        return rgbBytes
    }

    protected val luminance: ByteArray?
        get() = yuvBytes[0]

    /** Callback for android.hardware.Camera API  */
    override fun onPreviewFrame(bytes: ByteArray, camera: Camera) {
        if (isProcessingFrame) {
            Timber.w("Dropping frame!")
            return
        }
        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                val previewSize = camera.parameters.previewSize
                previewHeight = previewSize.height
                previewWidth = previewSize.width
                rgbBytes = IntArray(previewWidth * previewHeight)
                onPreviewSizeChosen(Size(previewSize.width, previewSize.height), 90)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception!")
            return
        }
        isProcessingFrame = true
        yuvBytes[0] = bytes
        luminanceStride = previewWidth
        imageConverter = Runnable {
            ImageUtils.convertYUV420SPToARGB8888(
                bytes,
                previewWidth,
                previewHeight,
                rgbBytes!!
            )
        }
        postInferenceCallback = Runnable {
            camera.addCallbackBuffer(bytes)
            isProcessingFrame = false
        }
        processImage()
    }

    /** Callback for Camera2 API  */
    override fun onImageAvailable(reader: ImageReader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }
        if (rgbBytes == null) {
            rgbBytes = IntArray(previewWidth * previewHeight)
        }
        try {
            val image = reader.acquireLatestImage() ?: return
            if (isProcessingFrame) {
                image.close()
                return
            }
            isProcessingFrame = true
            Trace.beginSection("imageAvailable")
            val planes = image.planes
            fillBytes(planes, yuvBytes)
            luminanceStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            imageConverter = Runnable {
                ImageUtils.convertYUV420ToARGB8888(
                    yuvBytes[0]!!,
                    yuvBytes[1]!!,
                    yuvBytes[2]!!,
                    previewWidth,
                    previewHeight,
                    luminanceStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes!!
                )
            }
            postInferenceCallback = Runnable {
                image.close()
                isProcessingFrame = false
            }
            processImage()
        } catch (e: Exception) {
            Timber.e(e, "Exception!")
            Trace.endSection()
            return
        }
        Trace.endSection()
    }

    @Synchronized
    public override fun onStart() {
        Timber.d("onStart $this")
        super.onStart()
    }

    @Synchronized
    public override fun onResume() {
        Timber.d("onResume $this")
        super.onResume()
        handlerThread = HandlerThread("inference")
        handlerThread!!.start()
        handler = Handler(handlerThread!!.looper)
    }

    @Synchronized
    public override fun onPause() {
        Timber.d("onPause $this")
        handlerThread!!.quitSafely()
        try {
            handlerThread!!.join()
            handlerThread = null
            handler = null
        } catch (e: InterruptedException) {
            Timber.e(e, "Exception!")
        }
        super.onPause()
    }

    @Synchronized
    public override fun onStop() {
        Timber.d("onStop $this")
        super.onStop()
    }

    @Synchronized
    public override fun onDestroy() {
        Timber.d("onDestroy $this")
        super.onDestroy()
    }

    @Synchronized
    protected fun runInBackground(r: Runnable?) {
        if (handler != null) {
            handler!!.post(r!!)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment()
            } else {
                requestPermission()
            }
        }
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                    this@CameraActivity,
                    "Camera permission is required for this demo",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
            requestPermissions(arrayOf(PERMISSION_CAMERA), PERMISSIONS_REQUEST)
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    private fun isHardwareLevelSupported(
        characteristics: CameraCharacteristics,
        requiredLevel: Int
    ): Boolean {
        val deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
        return if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            requiredLevel == deviceLevel
        } else requiredLevel <= deviceLevel
        // deviceLevel is not LEGACY, can use numerical sort
    }

    private fun chooseCamera(): String? {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: continue

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                useCamera2API = (
                    facing == CameraCharacteristics.LENS_FACING_EXTERNAL ||
                        isHardwareLevelSupported(
                            characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                        )
                    )
                Timber.i("Camera API lv2?: %s", useCamera2API)
                return cameraId
            }
        } catch (e: CameraAccessException) {
            Timber.e(e, "Not allowed to access camera")
        }
        return null
    }

    protected fun setFragment() {
        val cameraId = chooseCamera()
        val fragment: Fragment
        if (useCamera2API) {
            val camera2Fragment: CameraConnectionFragment = desiredPreviewFrameSize?.let {
                CameraConnectionFragment.newInstance(
                    object : ConnectionCallback {
                        override fun onPreviewSizeChosen(size: Size?, rotation: Int) {
                            previewHeight = size!!.height
                            previewWidth = size.width
                            this@CameraActivity.onPreviewSizeChosen(size, rotation)
                        }
                    },
                    this,
                    it
                )
            }!!
            camera2Fragment.setCamera(cameraId)
            fragment = camera2Fragment
        } else {
            fragment = LegacyCameraConnectionFragment(this, desiredPreviewFrameSize)
        }
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment).commit()
    }

    protected fun fillBytes(planes: Array<Plane>, yuvBytes: Array<ByteArray?>) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                Timber.d("Initializing buffer %d at size %d", i, buffer.capacity())
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer[yuvBytes[i]]
        }
    }

    protected fun readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback!!.run()
        }
    }

    protected val screenOrientation: Int
        protected get() = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }

    @UiThread
    protected fun showResultsInBottomSheet(results: Float) {
        if (results != null) {
            if (results < 0.5) {
                // means prediction was for category corresponding to 0
                binding.bottomSheetLayout.detectedItem.text = "Negative"
                binding.bottomSheetLayout.detectedItemValue.text =
                    String.format("%.2f", 100 * results) + "%"
            } else {
                // means prediction was for category corresponding to 1
                binding.bottomSheetLayout.detectedItem.text = "Positive"
                binding.bottomSheetLayout.detectedItemValue.text =
                    String.format("%.2f", 100 * results) + "%"
            }
//            val recognition: Recognition? = results[0]
//            if (recognition != null) {
//                if (recognition.title != null) binding.bottomSheetLayout.detectedItem.text = recognition.title
//                if (recognition.confidence != null) binding.bottomSheetLayout.detectedItemValue.text =
//                    java.lang.String.format("%.2f", 100 * recognition.confidence)
//                    .toString() + "%"
//            }
//            val recognition1: Recognition? = results[1]
//            if (recognition1 != null) {
//                if (recognition1.title != null) binding.bottomSheetLayout.detectedItem1.text =
//                    recognition1.title
//                if (recognition1.confidence != null) binding.bottomSheetLayout.detectedItem1Value.text =
//                    java.lang.String.format("%.2f", 100 * recognition1.confidence)
//                    .toString() + "%"
//            }
//            val recognition2: Recognition? = results[2]
//            if (recognition2 != null) {
//                if (recognition2.title != null) binding.bottomSheetLayout.detectedItem2.text =
//                    recognition2.title
//                if (recognition2.confidence != null) binding.bottomSheetLayout.detectedItem2Value.text =
//                    java.lang.String.format("%.2f", 100 * recognition2.confidence)
//                    .toString() + "%"
//            }
        }
    }

    protected fun showFrameInfo(frameInfo: String?) {
        binding.bottomSheetLayout.frameInfo.text = frameInfo
    }

    protected fun showCropInfo(cropInfo: String?) {
        binding.bottomSheetLayout.cropInfo.text = cropInfo
    }

    protected fun showCameraResolution(cameraInfo: String?) {
        binding.bottomSheetLayout.viewInfo.text = cameraInfo
    }

    protected fun showRotationInfo(rotation: String?) {
        binding.bottomSheetLayout.rotationInfo.text = rotation
    }

    protected fun showInference(inferenceTime: String?) {
        binding.bottomSheetLayout.inferenceInfo.text = inferenceTime
    }

    protected fun getDevice(): Device {
        return device
    }

    private fun setDevice(device: Device) {
        if (this.device !== device) {
            Timber.d("Updating  device: $device")
            this.device = device
            val threadsEnabled = device === Device.CPU
            binding.bottomSheetLayout.plus.isEnabled = threadsEnabled
            binding.bottomSheetLayout.minus.isEnabled = threadsEnabled
            binding.bottomSheetLayout.threads.text = if (threadsEnabled) numThreads.toString() else "N/A"
            onInferenceConfigurationChanged()
        }
    }

    protected fun getNumThreads(): Int {
        return numThreads
    }

    private fun setNumThreads(numThreads: Int) {
        if (this.numThreads != numThreads) {
            Timber.d("Updating  numThreads: $numThreads")
            this.numThreads = numThreads
            onInferenceConfigurationChanged()
        }
    }

    protected abstract fun processImage()
    protected abstract fun onPreviewSizeChosen(size: Size?, rotation: Int)
    protected abstract val desiredPreviewFrameSize: Size?

    protected abstract fun onInferenceConfigurationChanged()
    override fun onClick(v: View) {
        if (v.id == R.id.plus) {
            val threads = binding.bottomSheetLayout.threads.text.toString().trim { it <= ' ' }
            var numThreads = threads.toInt()
            if (numThreads >= 9) return
            setNumThreads(++numThreads)
            binding.bottomSheetLayout.threads.text = numThreads.toString()
        } else if (v.id == R.id.minus) {
            val threads = binding.bottomSheetLayout.threads.text.toString().trim { it <= ' ' }
            var numThreads = threads.toInt()
            if (numThreads == 1) {
                return
            }
            setNumThreads(--numThreads)
            binding.bottomSheetLayout.threads.text = numThreads.toString()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
        if (parent === binding.bottomSheetLayout.deviceSpinner) {
            setDevice(Device.valueOf(parent.getItemAtPosition(pos).toString()))
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing.
    }

    companion object {
        private const val PERMISSIONS_REQUEST = 1
        private const val PERMISSION_CAMERA = Manifest.permission.CAMERA
        private fun allPermissionsGranted(grantResults: IntArray): Boolean {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }
    }
}
