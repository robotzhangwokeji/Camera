package com.mycamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);

    }


    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            /*当textureView可用时打开摄像头*/
            openCamera(width, height);

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //摄像头打开时激发此方法
            MainActivity.this.cameraDevice = camera;
            //开始预览
            createCameraPreviewSession();

        }

        //摄像头断开连接激发此方法
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            MainActivity.this.cameraDevice = null;

        }

        //打开摄像头错误的时候激发此方法
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            MainActivity.this.cameraDevice = null;
            finish();

        }
    };

    private CameraDevice cameraDevice;

    private AutoFitView textureView;

    private String mCameraId = "0";
    private Size previewSize;
    private CaptureRequest.Builder previewRequestBuilder;
    private ImageReader imageReader;
    private CameraCaptureSession captureSession;
    private CaptureRequest previewRequest;

    private void openCamera(int width, int height) {
        //获取指定摄像头的属性
        setUpCameraOutPuts(width, height);

        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(mCameraId, stateCallBack, null);




        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }
    private void createCameraPreviewSession() {
        try {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(previewSize.getWidth(),previewSize.getHeight());
            Surface surface = new Surface(texture);

        //创建作为预览的 CaptureRequest.Builder

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //将textureView的surface作为 CaptureRequest.build的目标
            previewRequestBuilder.addTarget(new Surface(texture));

            //创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            cameraDevice.createCaptureSession(Arrays.asList(surface,imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    //如果摄像头为null 直接结束方法
                    if (null == cameraDevice) {
                        return;

                    }
                    //当摄像头已经准备好时，开始显示预览
                    captureSession = session;
                    try {
                        //设置自动对焦模式
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //设置自动曝光模式
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        //开始显示相机预览
                        previewRequest = previewRequestBuilder.build();
                        //设置预览时候连续捕获图像数据
                        captureSession.setRepeatingRequest(previewRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }


                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                    Toast.makeText(MainActivity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void setUpCameraOutPuts(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            /*获取摄像头的特性*/
            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(mCameraId);
            /*获取摄像头配置的属性*/
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //获取摄像头支持的最大尺寸
            Size largest = Collections.max(Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),new CompareSizeByArea());
            //创建一个ImageReader对象，用于获取摄像头的图像数据
             imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                //当照片数据可用时激发改方法
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //获取捕捉照片数据
                    Image image = reader.acquireNextImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    //使用io流将照片写入指定文件
                    File file = new File(getExternalCacheDir(), "pic.jpg");
                    buffer.get(bytes);

                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        fileOutputStream.write(bytes);
                        Toast.makeText(MainActivity.this, "保存图片成功", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        image.close();
                    }
                }
            },null);
            //获取最佳的预览尺寸
             previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class), width, height, largest);
            //根据最佳尺寸调整预览组件（textureView）的长宽比
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio)
    {
        //收集摄像头支持的大过预览surface的分辨路
        ArrayList<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        //如果找到多个预览尺寸，获取其中面积最小的
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            Log.e("aaaa", "找不到合适的预览尺寸！！！");
            return choices[0];
        }




    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         textureView = (AutoFitView) findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(mSurfaceTextureListener);

        Button capture = (Button) findViewById(R.id.bt_capture);
        capture.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        captureStillPicture();
    }

    private void captureStillPicture() {
        if (cameraDevice == null) {
            return;
        }
        //创建作为拍照的CapureRequest.build
        try {
           final CaptureRequest.Builder captureRequestBuilder= cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //将imageReader的suface做为CaptureRequest.build的目标
            captureRequestBuilder.addTarget(imageReader.getSurface());
            //设置自动对焦模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //设置自动曝光模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //获取方法
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //根据设备方向计算出照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //捕获静态图像
            int capture = captureSession.capture(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                //拍照完成时激发此方法
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {

                    try {
                        //设置自动对焦模式
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //设置自动曝光模式
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        //设置连续取景模式
                        captureSession.setRepeatingRequest(previewRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }
     //为size 定义一个比较容器
    private static class CompareSizeByArea implements java.util.Comparator< Size> {


        @Override
        public int compare(Size o1, Size o2) {
            //强制转换为long保证不会发生溢出；
            return Long.signum((long)o1.getWidth() * o1.getHeight() - (long)o2.getHeight() * o2.getWidth());

        }
    }
}
