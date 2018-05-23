package com.lbh.lesson.gles.lesson2;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;

import com.lbh.lesson.gles.util.CoordinateUtil;
import com.lbh.lesson.gles.util.ImageUtil;
import com.lbh.lesson.gles.util.OpenGLUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by linbinghe on 2018/3/18.
 */

public class TextureRender implements GLSurfaceView.Renderer {

    private static final String VERTEX_SHADER =
            "//设置的顶点坐标数据\n" +
                    "attribute vec4 vPosition;" +
                    "//设置的纹理坐标数据\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    "//输出到片元着色器的纹理坐标数据\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "void main() {" +
                    "//设置最终坐标\n" +
                    "  gl_Position = vPosition;" +
                    "  textureCoordinate = inputTextureCoordinate.xy;\n" +
                    "}";
    private static final String FRAGMENT_SHADER =
            "//设置float类型默认精度，顶点着色器默认highp，片元着色器需要用户声明\n" +
                    "precision mediump float;" +
                    "//从顶点着色器传入的纹理坐标数据\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "//用户传入的纹理数据\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "void main() {" +
                    "//该片元最终颜色值\n" +
                    "  gl_FragColor = texture2D(inputImageTexture, textureCoordinate);" +
                    "}";
    private static final String GREY_FRAGMENT_SHADER =
            "//设置float类型默认精度，顶点着色器默认highp，片元着色器需要用户声明\n" +
                    "precision mediump float;" +
                    "//从顶点着色器传入的纹理坐标数据\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "//用户传入的纹理数据\n" +
                    "uniform sampler2D inputImageTexture;\n" +
                    "void main() {" +
                    "//该片元最终颜色值\n" +
                    "  vec4 color = texture2D(inputImageTexture, textureCoordinate);" +
                    "  float result = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;" +
                    "  gl_FragColor = vec4(result, result, result, 1.0);" +
                    "}";

    private int mProgramId; //GL程序对象句柄
    private int mPositionId; //顶点位置句柄
    private int mTexture; //纹理句柄
    private int mInputTextureCoordinate; //纹理坐标句柄

    private int[] mFrameBuffers; //FBO句柄
    private int[] mFrameBufferTextures; //FBO对应纹理句柄

    private int[] mTextureId = new int[]{OpenGLUtil.NO_TEXTURE}; //用于绘制的纹理ID

    private FloatBuffer mVertexBuffer; //顶点坐标数据
    private FloatBuffer mTextureBuffer; //纹理坐标数据

    private Context mContext;
    private int mVPWidth, mVPHeight;

    public TextureRender(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        //编译着色器并链接顶点与片元着色器生成OpenGL程序句柄
//        mProgramId = OpenGLUtil.loadProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        //生成灰度图
        mProgramId = OpenGLUtil.loadProgram(VERTEX_SHADER, GREY_FRAGMENT_SHADER);
        //通过OpenGL程序句柄查找获取顶点着色器中的顶点坐标句柄
        mPositionId = GLES20.glGetAttribLocation(mProgramId, "vPosition");
        //通过OpenGL程序句柄查找获取顶点着色器中的纹理坐标句柄
        mInputTextureCoordinate = GLES20.glGetAttribLocation(mProgramId, "inputTextureCoordinate");
        //通过OpenGL程序句柄查找获取片元着色器中的纹理句柄
        mTexture = GLES20.glGetUniformLocation(mProgramId, "inputImageTexture");

        //获取顶点数据
        mVertexBuffer = CoordinateUtil.getVertexCoord();
        //获取与顶点数据顺序相同的纹理坐标数据
//        mTextureBuffer = CoordinateUtil.getTextureCoord();
        //获取与顶点数据顺序垂直翻转的纹理坐标数据
        mTextureBuffer = CoordinateUtil.getTextureVerticalFlipCoord();

        //加载纹理
        loadTexture();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mVPWidth = width;
        mVPHeight = height;

        processViewPortChanged();
    }

    protected void processViewPortChanged() {
        mFrameBuffers = new int[1];
        mFrameBufferTextures = new int[1];
        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mVPWidth,
                mVPHeight, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);


        GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("lbh", "status != GL_FRAMEBUFFER_COMPLETE: " + status);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        //这里网上很多博客说是设置背景色，其实更严格来说是通过所设置的颜色来清空颜色缓冲区，改变背景色只是其作用之一
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);//白色不透明
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //告知OpenGL所要使用的Program
        GLES20.glUseProgram(mProgramId);

        //启用指向渲染图形所处的顶点数据的句柄
        GLES20.glEnableVertexAttribArray(mPositionId);
        //绑定渲染图形所处的顶点数据
        GLES20.glVertexAttribPointer(mPositionId, CoordinateUtil.PER_COORDINATE_SIZE,
                GLES20.GL_FLOAT, false,
                CoordinateUtil.COORDINATE_STRIDE, mVertexBuffer);

        //启用指向纹理坐标数据的句柄
        GLES20.glEnableVertexAttribArray(mInputTextureCoordinate);
        //绑定纹理坐标数据
        GLES20.glVertexAttribPointer(mInputTextureCoordinate, CoordinateUtil.PER_COORDINATE_SIZE,
                GLES20.GL_FLOAT, false, CoordinateUtil.COORDINATE_STRIDE, mTextureBuffer);

        //将纹理对象设置到0号单元上
        GLES20.glUniform1i(mTexture, 0);
        //激活GL_TEXTURE0，该纹理单元默认激活
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //绑定纹理数据到GL_TEXTURE0纹理单元上
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);

        //使用GL_TRIANGLE_FAN方式绘制纹理
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, CoordinateUtil.VERTEX_COUNT);

        //禁用顶点数据对象
        GLES20.glDisableVertexAttribArray(mPositionId);
        //禁用纹理坐标数据对象
        GLES20.glDisableVertexAttribArray(mInputTextureCoordinate);
        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

//        getBitmap();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);


        //告知OpenGL所要使用的Program
        GLES20.glUseProgram(mProgramId);

        //启用指向渲染图形所处的顶点数据的句柄
        GLES20.glEnableVertexAttribArray(mPositionId);
        //绑定渲染图形所处的顶点数据
        GLES20.glVertexAttribPointer(mPositionId, CoordinateUtil.PER_COORDINATE_SIZE,
                GLES20.GL_FLOAT, false,
                CoordinateUtil.COORDINATE_STRIDE, mVertexBuffer);

        //启用指向纹理坐标数据的句柄
        GLES20.glEnableVertexAttribArray(mInputTextureCoordinate);
        //绑定纹理坐标数据
        GLES20.glVertexAttribPointer(mInputTextureCoordinate, CoordinateUtil.PER_COORDINATE_SIZE,
                GLES20.GL_FLOAT, false, CoordinateUtil.COORDINATE_STRIDE, mTextureBuffer);

        //将纹理对象设置到0号单元上
        GLES20.glUniform1i(mTexture, 0);
        //激活GL_TEXTURE0，该纹理单元默认激活
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //绑定纹理数据到GL_TEXTURE0纹理单元上
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);

        //使用GL_TRIANGLE_FAN方式绘制纹理
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, CoordinateUtil.VERTEX_COUNT);

        //禁用顶点数据对象
        GLES20.glDisableVertexAttribArray(mPositionId);
        //禁用纹理坐标数据对象
        GLES20.glDisableVertexAttribArray(mInputTextureCoordinate);
        //解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

//        getBitmap();
    }

    public void getBitmap() {
        final int[] pixelMirroredArray = new int[mVPWidth * mVPHeight];

        IntBuffer pixelBuffer = IntBuffer.allocate(mVPWidth * mVPHeight);
        GLES20.glReadPixels(0, 0, mVPWidth, mVPHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                pixelBuffer);
        int[] pixelArray = pixelBuffer.array();
        for (int i = 0; i < mVPHeight; i++) {
            for (int j = 0; j < mVPWidth; j++) {
                pixelMirroredArray[(mVPHeight - i - 1) * mVPWidth + j] =
                        pixelArray[i * mVPWidth + j];
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(mVPWidth, mVPHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(pixelMirroredArray));

        try {
            bitmap2File(bitmap, "/sdcard/PhotonLib/cache/" + System.currentTimeMillis() + ".png", 100);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void checkAndMakeDirs(String path) {
        File dstFile = new File(path);
        if (dstFile != null) {
            if (dstFile.isDirectory()) {
                if (!dstFile.exists()) {
                    dstFile.mkdirs();
                }
            } else {
                if (dstFile.getParentFile() != null && !dstFile.getParentFile().exists()) {
                    dstFile.getParentFile().mkdirs();
                }
            }
        }
    }

    public static void bitmap2File(Bitmap bitmap, String path, int quality) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        checkAndMakeDirs(path);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, quality, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTexture() {
        //生成原始纹理图片
        Bitmap texture = getTextureBitmap();
        if (mTextureId[0] == OpenGLUtil.NO_TEXTURE) {
            //若尚未创建过纹理对象
            //创建一个纹理对象，存放于mTextureId数组内
            GLES20.glGenTextures(1, mTextureId, 0);
            //将该纹理绑定到GL_TEXTURE_2D目标，代表这是一个二维纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
            //设置其放大缩小滤波方式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            //设置其WRAP方式
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            //将图片绑定到当前glBindTexture的纹理对象上
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
        } else {
            //若创建过纹理对象，则重复利用
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
            //更新已存在的纹理对象中的纹理数据
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, texture);
        }
        //绑定完纹理后可直接回收图片资源
        texture.recycle();
    }

    private Bitmap getTextureBitmap() {
        return ImageUtil.getBitmapFromAssets(mContext, "cat.png");
    }

    public void destroy() {
        if (mTextureId[0] != OpenGLUtil.NO_TEXTURE) {
            GLES20.glDeleteTextures(1, mTextureId, 0);
        }
        GLES20.glDeleteProgram(mProgramId);
    }
}
