#include "com_example_apadmin_cameraphoto_Yuv420POperate.h"

#include <string.h>

/**
    * Nv21:
    * YYYYYYYY
    * YYYYYYYY
    * YYYYYYYY
    * YYYYYYYY
    * VUVUVUVU
    * VUVUVUVU
    *
    * Nv12:
    * YYYYYYYY
    * YYYYYYYY
    * YYYYYYYY
    * YYYYYYYY
    * UVUVUVUV
    * UVUVUVUV
    *
    * YUV420P:
    * YYYYYYYY
    * YYYYYYYY
    * YYYYYYYY
    * YYYYYYYY
    * UUUU
    * UUUU
    * VVVV
    * VVVV
*/

//nv21格式转为yuv420P格式
JNIEXPORT void JNICALL Java_com_example_apadmin_cameraphoto_Yuv420POperate_NV21toYUV420P
  (JNIEnv *env, jclass jcls, jbyteArray jnv21bytes, jbyteArray jyuv420p, jint jwidth, jint jheight)
{
    int iSize = jwidth * jheight;
    jsize nv21Len = env->GetArrayLength(jnv21bytes);
    jsize yuv420pLen = env->GetArrayLength(jyuv420p);
    if(nv21Len <= 0 || yuv420pLen <= 0)
        return;
    jbyte* jnv21Data = env->GetByteArrayElements(jnv21bytes, 0);
    jbyte* jyuv420pData = env->GetByteArrayElements(jyuv420p, 0);

    unsigned char* nv21 = (unsigned char*)jnv21Data;
    unsigned char* yuv420p = (unsigned char*)jyuv420pData;
    //拷贝Y分量
    memcpy(yuv420p,nv21,iSize);

    for (int iIndex = 0; iIndex < iSize / 2; iIndex += 2) {
        //V分量
        yuv420p[iSize + iIndex / 2 + iSize / 4] = nv21[iSize + iIndex];
        //U分量
        yuv420p[iSize + iIndex / 2] = nv21[iSize + iIndex + 1];
    }
    env->ReleaseByteArrayElements(jnv21bytes, jnv21Data, 0);
    env->ReleaseByteArrayElements(jyuv420p, jyuv420pData, 0);
    return;
}

//nv21格式转为nv12格式
JNIEXPORT void JNICALL Java_com_example_apadmin_cameraphoto_Yuv420POperate_NV21ToNV12
  (JNIEnv *env, jclass jcls, jbyteArray jnv21, jbyteArray jnv12, jint jwidth, jint jheight)
{
    int iSize = jwidth * jheight;
    jsize nv21Len = env->GetArrayLength(jnv21);
    jsize nv12Len = env->GetArrayLength(jnv12);
    if(nv21Len <= 0 || nv12Len <= 0)
        return;
    jbyte* jnv21Data = env->GetByteArrayElements(jnv21, 0);
    jbyte* jnv12Data = env->GetByteArrayElements(jnv12, 0);

    unsigned char* nv21 = (unsigned char*)jnv21Data;
    unsigned char* nv12 = (unsigned char*)jnv12Data;

    //拷贝Y分量
    memcpy(nv12,nv21,iSize);

    for (int i = 0; i < iSize / 4; i++) {
        nv12[iSize + i * 2] = nv21[iSize + i * 2 + 1]; //U
        nv12[iSize + i * 2 + 1] = nv21[iSize + i * 2]; //V
    }

    env->ReleaseByteArrayElements(jnv21, jnv21Data, 0);
    env->ReleaseByteArrayElements(jnv12, jnv12Data, 0);
    return;
}

//YUV420P图像顺时针旋转90度
JNIEXPORT void JNICALL Java_com_example_apadmin_cameraphoto_Yuv420POperate_YUV420PClockRot90
  (JNIEnv *env, jclass jcls, jbyteArray jdest, jbyteArray jsrc, jint jwidth, jint jheight)
{
    int w = jwidth;
    int h = jheight;
    jsize srcLen = env->GetArrayLength(jsrc);
    jsize destLen = env->GetArrayLength(jdest);
    if(srcLen <= 0 || destLen <= 0)
        return;
    jbyte* jsrcData = env->GetByteArrayElements(jsrc, 0);
    jbyte* jdestData = env->GetByteArrayElements(jdest, 0);

    unsigned char* src = (unsigned char*)jsrcData;
    unsigned char* dest = (unsigned char*)jdestData;

    int nPos = 0;
    //旋转Y
    int k = 0;
    for(int i=0;i<w;i++)
    {
        for(int j = h -1;j >=0;j--)
        {
            dest[k++] = src[j*w + i];
        }
    }
    //旋转U
    nPos = w*h;
    for(int i=0;i<w/2;i++)
    {
        for(int j= h/2-1;j>=0;j--)
        {
            dest[k++] = src[nPos+ j*w/2 +i];
        }
    }

    //旋转V
    nPos = w*h*5/4;
    for(int i=0;i<w/2;i++)
    {
        for(int j= h/2-1;j>=0;j--)
        {
            dest[k++] = src[nPos+ j*w/2 +i];
        }
    }
    env->ReleaseByteArrayElements(jsrc, jsrcData, 0);
    env->ReleaseByteArrayElements(jdest, jdestData, 0);
    return;
}


