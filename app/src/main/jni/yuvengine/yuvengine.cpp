//
// Created by zhongjihao on 19-8-26.
//


#define LOG_TAG "YuvEngine"

#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <memory.h>
#include "yuvengine.h"
#include "../log.h"


YuvEngine::YuvEngine()
{
    LOGD("%s: constructor",__FUNCTION__);
}


YuvEngine::~YuvEngine()
{
    LOGD("%s: destructor",__FUNCTION__);
}

//YV12 -> I420
void YuvEngine::Yv12ToI420(unsigned char* pYv12, unsigned char* pI420, int width, int height)
{
    if(pYv12 == NULL || pI420 == NULL){
        LOGE("%s: pYv12 is null or pI420 is null",__FUNCTION__);
        return;
    }


    int frameSize = width * height;
    if(frameSize <= 0){
        LOGE("%s: frameSize <= 0",__FUNCTION__);
        return;
    }

    //拷贝Y分量
    memcpy(pI420,pYv12,frameSize);
    //拷贝U分量
    memcpy(pI420+frameSize,pYv12+frameSize*5/4,frameSize/4);
    //拷贝V分量
    memcpy(pI420+frameSize*5/4,pYv12+frameSize,frameSize/4);
}

//I420 -> YV12
void YuvEngine::I420ToYv12(unsigned char* pI420, unsigned char* pYv12, int width, int height)
{
    if(pI420 == NULL || pYv12 == NULL){
        LOGE("%s: pI420 is null or pYv12 is null",__FUNCTION__);
        return;
    }

    int frameSize = width * height;
    if(frameSize <= 0){
        LOGE("%s: frameSize <= 0",__FUNCTION__);
        return;
    }

    //拷贝Y分量
    memcpy(pYv12,pI420,frameSize);
    //拷贝V分量
    memcpy(pYv12+frameSize,pI420+frameSize*5/4,frameSize/4);
    //拷贝U分量
    memcpy(pYv12+frameSize*5/4,pI420+frameSize,frameSize/4);
}

//NV21 -> I420
void YuvEngine::Nv21ToI420(unsigned char* pNv21,unsigned char* pI420,int width,int height)
{
    if(pNv21 == NULL || pI420 == NULL){
        LOGE("%s: pNv21 is null or pI420 is null",__FUNCTION__);
        return;
    }

    int frameSize = width * height;
    if(frameSize <= 0){
        LOGE("%s: frameSize <= 0",__FUNCTION__);
        return;
    }

    int i = 0;
    //拷贝Y分量
    memcpy(pI420,pNv21,frameSize);

    for (i = 0; i < frameSize / 2; i += 2) {
        //U分量
        pI420[frameSize + i/2] = pNv21[frameSize + i + 1];
        //V分量
        pI420[frameSize + i/2 + frameSize / 4] = pNv21[frameSize + i];
    }
}

//I420 -> NV21
void YuvEngine::I420ToNv21(unsigned char* pI420,unsigned char* pNv21,int width,int height)
{
    if(pI420 == NULL || pNv21 == NULL){
        LOGE("%s: pI420 is null or pNv21 is null",__FUNCTION__);
        return;
    }

    int frameSize = width * height;
    if(frameSize <= 0){
        LOGE("%s: frameSize <= 0",__FUNCTION__);
        return;
    }

    int i = 0;
    //拷贝Y分量
    memcpy(pNv21,pI420,frameSize);

    for (i = 0; i < frameSize / 2; i += 2) {
        //V分量
        pNv21[frameSize + i] = pI420[frameSize + frameSize/4 + i/2];
        //U分量
        pNv21[frameSize + i + 1] = pI420[frameSize + i/2];
    }
}

//NV21 -> Yv12
void YuvEngine::Nv21ToYv12(unsigned char* pNv21,unsigned char* pYv12,int width,int height)
{
    if(pNv21 == NULL || pYv12 == NULL){
        LOGE("%s: pNv21 is null or pYv12 is null",__FUNCTION__);
        return;
    }

    int frameSize = width * height;
    if(frameSize <= 0){
        LOGE("%s: frameSize <= 0",__FUNCTION__);
        return;
    }

    int i = 0;
    //拷贝Y分量
    memcpy(pYv12,pNv21,frameSize);

    for (i = 0; i < frameSize / 2; i += 2) {
        //V分量
        pYv12[frameSize + i/2] = pNv21[frameSize + i];//pNv21[frameSize + i + 1];
        //U分量
        pYv12[frameSize + i/2 + frameSize / 4] = pNv21[frameSize + i + 1];
    }
}

//YV12 -> NV21
void YuvEngine::Yv12ToNv21(unsigned char* pYv12,unsigned char* pNv21,int width,int height)
{
    if(pYv12 == NULL || pNv21 == NULL){
        LOGE("%s: pYv12 is null or pNv21 is null",__FUNCTION__);
        return;
    }

    int frameSize = width * height;
    if(frameSize <= 0){
        LOGE("%s: frameSize <= 0",__FUNCTION__);
        return;
    }

    int i = 0;
    //拷贝Y分量
    memcpy(pNv21,pYv12,frameSize);

    for (i = 0; i < frameSize / 2; i += 2) {
        //V分量
        pNv21[frameSize + i] = pYv12[frameSize + i/2];
        //U分量
        pNv21[frameSize + i + 1] = pYv12[frameSize + frameSize/4 + i/2];
    }
}

//NV21 -> NV12
void YuvEngine::Nv21ToNv12(unsigned char* pNv21,unsigned char* pNv12,int width,int height)
{
    if(pNv21 == NULL || pNv12 == NULL){
        LOGE("%s: pNv21 is null or pNv12 is null",__FUNCTION__);
        return;
    }

    int frameSize = width * height;
    if(frameSize <= 0){
        LOGE("%s: frameSize <= 0",__FUNCTION__);
        return;
    }

    //拷贝Y分量
    memcpy(pNv12,pNv21,frameSize);

    int i = 0;
    for (i = 0; i < frameSize / 4; i++) {
        pNv12[frameSize + i * 2] = pNv21[frameSize + i * 2 + 1]; //U
        pNv12[frameSize + i * 2 + 1] = pNv21[frameSize + i * 2]; //V
    }
}

//NV12 -> NV21
void YuvEngine::Nv12ToNv21(unsigned char* pNv12,unsigned char* pNv21,int width,int height)
{
    if(pNv12 == NULL || pNv21 == NULL){
        LOGE("%s: pNv12 is null or pNv21 is null",__FUNCTION__);
        return;
    }

    int frameSize = width * height;
    if(frameSize <= 0){
        LOGE("%s: frameSize <= 0",__FUNCTION__);
        return;
    }

    //拷贝Y分量
    memcpy(pNv21,pNv12,frameSize);

    int i = 0;
    for (i = 0; i < frameSize / 4; i++) {
        pNv21[frameSize + i * 2] = pNv12[frameSize + i * 2 + 1]; //V
        pNv21[frameSize + i * 2 + 1] = pNv12[frameSize + i * 2]; //U
    }
}

/**
 * yuvType yuv类型
 * startX,startY 开始裁剪的坐标位置
 * srcYuv 原始YUV数据
 * srcW,srcH 原始YUV数据的分辨率
 * tarYuv 存储裁剪的数据
 * cutW,cutH 裁剪的分辨率
**/
void YuvEngine::cutCommonYuv(int yuvType, int startX,int startY,unsigned char *srcYuv, int srcW,int srcH,unsigned char *tarYuv,int cutW, int cutH)
{
    if(srcYuv == NULL || tarYuv == NULL)
        return;
    if(srcW < startX + cutW || (srcH < startY + cutH))
        return;

    switch(yuvType){
        case FORMAT_NV21:
        case FORMAT_NV12:{
            int i;
            unsigned char* cutY = tarYuv;
            unsigned char* cutUV = tarYuv + cutW*cutH;

            for(i = 0; i < cutH; i++) {
                //逐行拷贝Y分量
                memcpy(cutY+i*cutW, srcYuv+startX+(i+startY)*srcW, cutW);
            }

            for(i = 0; i < cutH/2; i++) {
                //逐行拷贝UV分量
                memcpy(cutUV+i*cutW, srcYuv+startX+srcW*srcH+(i+startY/2)*srcW, cutW);
            }

#ifdef DUMP_OUTPUT
            char output[256];
            memset(output,0,sizeof(output));
            sprintf(output,"cut_nv21_%dx%d.yuv",cutW,cutH);
            FILE *outPutFp = fopen(output, "w+");
            fwrite(tarYuv, 1, cutW*cutH*3/2, outPutFp);
            fclose(outPutFp);
#endif
            break;
        }
        case FORMAT_I420:{
            int i = 0;
            unsigned char* srcY = srcYuv;
            unsigned char* srcU = srcY + srcW * srcH;
            unsigned char* srcV = srcU + srcW * srcH / 4;

            unsigned char* destY = tarYuv;
            unsigned char* destU = destY + cutW*cutH;
            unsigned char* destV = destU + cutW * cutH / 4;

            //拷贝Y分量
            for (i = 0; i < cutH; i++){ //每次循环一次，扫描一行数据
                memcpy(destY+i*cutW, srcY+startX+(i+startY)*srcW, cutW);
            }

            for (i = 0; i < cutH/2; i++){ //每次循环一次，扫描一行数据
                //拷贝U分量
                memcpy(destU+i*(cutW/2), srcU+startX/2+(i+startY/2)*(srcW/2), cutW/2);
                //拷贝V分量
                memcpy(destV+i*(cutW/2), srcV+startX/2+(i+startY/2)*(srcW/2), cutW/2);
            }
#ifdef DUMP_OUTPUT
            char output[256];
			memset(output,0,sizeof(output));
			sprintf(output,"cut_i420_%dx%d.yuv",cutW,cutH);
			FILE *outPutFp = fopen(output, "w+");
			fwrite(tarYuv, 1, cutW*cutH*3/2, outPutFp);
			fclose(outPutFp);
#endif
            break;
        }
        case FORMAT_YV12:{
            int i = 0;
            unsigned char* srcY = srcYuv;
            unsigned char* srcV = srcY + srcW * srcH;
            unsigned char* srcU = srcV + srcW * srcH / 4;

            unsigned char* destY = tarYuv;
            unsigned char* destV = destY + cutW*cutH;
            unsigned char* destU = destV + cutW * cutH / 4;

            //拷贝Y分量
            for (i = 0; i < cutH; i++){ //每次循环一次，扫描一行数据
                memcpy(destY+i*cutW, srcY+startX+(i+startY)*srcW, cutW);
            }

            for (i = 0; i < cutH/2; i++){ //每次循环一次，扫描一行数据
                //拷贝V分量
                memcpy(destV+i*(cutW/2), srcV+startX/2+(i+startY/2)*(srcW/2), cutW/2);
                //拷贝U分量
                memcpy(destU+i*(cutW/2), srcU+startX/2+(i+startY/2)*(srcW/2), cutW/2);
            }
#ifdef DUMP_OUTPUT
            char output[256];
			memset(output,0,sizeof(output));
			sprintf(output,"cut_yv12_%dx%d.yuv",cutW,cutH);
			FILE *outPutFp = fopen(output, "w+");
			fwrite(tarYuv, 1, cutW*cutH*3/2, outPutFp);
			fclose(outPutFp);
#endif
            break;
        }
        default:
            break;
    }
}


/**
 * yuvType yuv类型
 * dstBuf 目标BUF
 * srcYuv 源yuv
 * srcW  宽
 * srcH 高
 * dirty_Y/dirty_UV 冗余数据
**/
void YuvEngine::getSpecYuvBuffer(int yuvType,unsigned char *dstBuf, unsigned char *srcYuv, int srcW, int srcH,int dirty_Y,int dirty_UV)
{
    int i;
    switch(yuvType){
        case FORMAT_NV21:
        case FORMAT_NV12:{
            int i;

            for(i = 0; i < srcH*srcW; i++) {
                if(srcYuv[i] != dirty_Y) {
                    dstBuf[i] = srcYuv[i];
                }
            }

            unsigned char* dstUV = dstBuf + srcW*srcH;
            unsigned char* srcUV = srcYuv +srcW*srcH;

            for(i = 0; i < srcW*srcH/2; i++) {
                if((srcUV[i] != dirty_UV)) {
                    dstUV[i] = srcUV[i];
                }
            }
#ifdef DUMP_OUTPUT
            char output[256];
			 memset(output,0,sizeof(output));
			 sprintf(output,"nv21_water_buffer_%dx%d.yuv",srcW,srcH);
			 FILE *tarFp = fopen(output, "w+");
			 fwrite(dstBuf, 1, srcW*srcH*3/2, tarFp);
			 fclose(tarFp);
#endif
            break;
        }
        case FORMAT_I420:{
            int i;

            unsigned char* dstY = dstBuf;
            unsigned char* dstU = dstY + srcW*srcH;
            unsigned char* dstV = dstU + srcW*srcH/4;

            unsigned char* srcY = srcYuv;
            unsigned char* srcU = srcY +srcW*srcH;
            unsigned char* srcV = srcU +srcW*srcH/4;

            for(i = 0; i < srcH*srcW; i++) {
                if(srcY[i] != dirty_Y) {
                    dstY[i] = srcY[i];
                }
            }

            for(i = 0; i < srcW*srcH/4; i++) {
                if((srcU[i] != dirty_UV)) {
                    dstU[i] = srcU[i];
                }
            }

            for(i = 0; i < srcW*srcH/4; i++) {
                if((srcV[i] != dirty_UV)) {
                    dstV[i] = srcV[i];
                }
            }
#ifdef DUMP_OUTPUT
            char output[256];
			 memset(output,0,sizeof(output));
			 sprintf(output,"i420_water_buffer_%dx%d.yuv",srcW,srcH);
			 FILE *tarFp = fopen(output, "w+");
			 fwrite(dstBuf, 1, srcW*srcH*3/2, tarFp);
			 fclose(tarFp);
#endif
            break;
        }
        case FORMAT_YV12:{
            int i;

            unsigned char* dstY = dstBuf;
            unsigned char* dstV = dstY + srcW*srcH;
            unsigned char* dstU = dstV+ srcW*srcH/4;

            unsigned char* srcY = srcYuv;
            unsigned char* srcV = srcY +srcW*srcH;
            unsigned char* srcU = srcV +srcW*srcH/4;

            for(i = 0; i < srcH*srcW; i++) {
                if(srcY[i] != dirty_Y) {
                    dstY[i] = srcY[i];
                }
            }

            for(i = 0; i < srcW*srcH/4; i++) {
                if((srcV[i] != dirty_UV)) {
                    dstV[i] = srcV[i];
                }
            }

            for(i = 0; i < srcW*srcH/4; i++) {
                if((srcU[i] != dirty_UV)) {
                    dstU[i] = srcU[i];
                }
            }
#ifdef DUMP_OUTPUT
            char output[256];
			 memset(output,0,sizeof(output));
			 sprintf(output,"yv12_water_buffer_%dx%d.yuv",srcW,srcH);
			 FILE *tarFp = fopen(output, "w+");
			 fwrite(dstBuf, 1, srcW*srcH*3/2, tarFp);
			 fclose(tarFp);
#endif
            break;
        }
        default:
            break;
    }
}

/**
 * yuvType yuv类型
 * startX,startY 需要添加水印的位置
 * waterMarkData 水印YUV数据，可以通过读取水印文件获取
 * waterMarkW,waterMarkH 水印数据的分辨率
 * yuvData 源YUV图像数据
 * yuvW,yuvH 源YUV的分辨率
**/
void YuvEngine::yuvAddWaterMark(int yuvType, int startX, int startY, unsigned char *waterMarkData,
                         int waterMarkW, int waterMarkH, unsigned char *yuvData, int yuvW, int yuvH)
{
    if(waterMarkData == NULL || yuvData == NULL){
        LOGE("%s: waterMarkData is null or yuvData is null",__FUNCTION__);
        return;
    }

    if(yuvW < startX + waterMarkW || (yuvH < startY + waterMarkH)){
        LOGE("%s: startX: %d, startY: %d waterMarkW: %d waterMarkH: %d yuvW: %d yuvH: %d",__FUNCTION__,startX,startY,waterMarkW,waterMarkH,yuvW,yuvH);
        return;
    }

    switch(yuvType) {
        case FORMAT_NV21:
        case FORMAT_NV12:{
            int i = 0;
            int j = 0;
            int k = 0;
            for(i = startY; i < waterMarkH+startY; i++) {
                //逐行拷贝Y分量
                memcpy(yuvData+startX+i*yuvW, waterMarkData+j*waterMarkW, waterMarkW);
                j++;
            }

            for(i = startY/2; i < (waterMarkH+startY)/2; i++) {
                //UV分量高度是Y分量的一半,逐行拷贝UV分量
                memcpy(yuvData+startX+yuvW*yuvH+i*yuvW, waterMarkData+waterMarkW*waterMarkH+k*waterMarkW, waterMarkW);
                k++;
            }

#ifdef DUMP_OUTPUT
            char output[256];
          memset(output,0,sizeof(output));
          sprintf(output,"water_nv21_%dx%d.yuv",yuvW,yuvH);
          FILE *outPutFp = fopen(output, "w+");
          fwrite(yuvData, 1, yuvW*yuvH*3/2, outPutFp);
          fclose(outPutFp);
#endif
            break;
        }
        case FORMAT_I420:{
            int i = 0;
            unsigned char* waterY = waterMarkData;
            unsigned char* waterU = waterY + waterMarkW * waterMarkH;
            unsigned char* waterV = waterU + waterMarkW * waterMarkH / 4;

            unsigned char* destY = yuvData;
            unsigned char* destU = destY + yuvW*yuvH;
            unsigned char* destV = destU + yuvW * yuvH / 4;

            //拷贝Y分量
            for (i = 0; i < waterMarkH; i++){ //每次循环一次，扫描一行数据
                memcpy(destY+startX+(i+startY)*yuvW, waterY+i*waterMarkW, waterMarkW); //y值覆盖
            }

            for (i = 0; i < waterMarkH/2; i++){ //每次循环一次，扫描一行数据
                //拷贝U分量
                memcpy(destU+(i+startY/2)*(yuvW/2)+startX/2, waterU+i*(waterMarkW/2), waterMarkW/2);
                //拷贝V分量
                memcpy(destV+(i+startY/2)*(yuvW/2)+startX/2, waterV+i*(waterMarkW/2), waterMarkW/2);
            }

#ifdef DUMP_OUTPUT
            char output[256];
         memset(output,0,sizeof(output));
         sprintf(output,"water_i420_%dx%d.yuv",yuvW,yuvH);
         FILE *outPutFp = fopen(output, "w+");
         fwrite(yuvData, 1, yuvW*yuvH*3/2, outPutFp);
         fclose(outPutFp);
#endif
            break;
        }
        case FORMAT_YV12:{
            int i = 0;
            unsigned char* waterY = waterMarkData;
            unsigned char* waterV = waterY + waterMarkW * waterMarkH;
            unsigned char* waterU = waterV + waterMarkW * waterMarkH / 4;

            unsigned char* destY = yuvData;
            unsigned char* destV = destY + yuvW*yuvH;
            unsigned char* destU = destV + yuvW * yuvH / 4;

            //拷贝Y分量
            for (i = 0; i < waterMarkH; i++){ //每次循环一次，扫描一行数据
                memcpy(destY+startX+(i+startY)*yuvW, waterY+i*waterMarkW, waterMarkW); //y值覆盖
            }

            for (i = 0; i < waterMarkH/2; i++){ //每次循环一次，扫描一行数据
                //拷贝V分量
                memcpy(destV+(i+startY/2)*(yuvW/2)+startX/2, waterV+i*(waterMarkW/2), waterMarkW/2);
                //拷贝U分量
                memcpy(destU+(i+startY/2)*(yuvW/2)+startX/2, waterU+i*(waterMarkW/2), waterMarkW/2);
            }

#ifdef DUMP_OUTPUT
         char output[256];
         memset(output,0,sizeof(output));
         sprintf(output,"water_yv12_%dx%d.yuv",yuvW,yuvH);
         FILE *outPutFp = fopen(output, "w+");
         fwrite(yuvData, 1, yuvW*yuvH*3/2, outPutFp);
         fclose(outPutFp);
#endif
            break;
        }
        default:
            break;
    }
}

void YuvEngine::Nv21ClockWiseRotate90(unsigned char* pNv21,int srcWidth,int srcHeight, unsigned char* outData,int* outWidth,int* outHeight)
{
    if(pNv21 == NULL || outData == NULL){
        LOGE("%s: pNv21 is null or outData is null",__FUNCTION__);
        return;
    }

    // Rotate the Y luma
    int i =0;
    for(int x =0;x < srcWidth;x++){
        for(int y = srcHeight-1;y >=0;y--){
            outData[i]= pNv21[y*srcWidth+x];
            i++;
        }
    }

    // Rotate the U and V color components
    i = srcWidth*srcHeight*3/2-1;
    for(int x = srcWidth-1;x >0;x=x-2){
        for(int y =0;y < srcHeight/2;y++){
            outData[i]= pNv21[(srcWidth*srcHeight)+(y*srcWidth)+x];
            i--;
            outData[i]= pNv21[(srcWidth*srcHeight)+(y*srcWidth)+(x-1)];
            i--;
        }
    }

    *outWidth = srcHeight;
    *outHeight = srcWidth;
    LOGD("%s: outWidth: %d,outHeight:%d",__FUNCTION__,*outWidth,*outHeight);
}

void YuvEngine::Nv21ClockWiseRotate180(unsigned char* pNv21,int srcWidth,int srcHeight, unsigned char* outData,int* outWidth,int* outHeight)
{
    if(pNv21 == NULL || outData == NULL){
        LOGE("%s: pNv21 is null or outData is null",__FUNCTION__);
        return;
    }

    int i = 0;
    int count = 0;
    for (i = srcWidth * srcHeight - 1; i >= 0; i--) {
        outData[count] = pNv21[i];
        count++;
    }

    i = srcWidth * srcHeight * 3 / 2 - 1;
    for (i = srcWidth * srcHeight * 3 / 2 - 1; i >= srcWidth * srcHeight; i -= 2) {
        outData[count++] = pNv21[i - 1];
        outData[count++] = pNv21[i];
    }

    *outWidth = srcWidth;
    *outHeight = srcHeight;
}

void YuvEngine::Nv21ClockWiseRotate270(unsigned char* pNv21,int srcWidth,int srcHeight, unsigned char* outData,int* outWidth,int* outHeight)
{
    // Rotate the Y luma
    int i = 0;
    for (int x = srcWidth - 1; x >= 0; x--) {
        for (int y = 0; y < srcHeight; y++) {
            outData[i] = pNv21[y * srcWidth + x];
            i++;
        }
    }

    // Rotate the U and V color components
    i = srcWidth * srcHeight;
    for (int x = srcWidth - 1; x > 0; x = x - 2) {
        for (int y = 0; y < srcHeight / 2; y++) {
            outData[i] = pNv21[(srcWidth * srcHeight) + (y * srcWidth) + (x - 1)];
            i++;
            outData[i] = pNv21[(srcWidth * srcHeight) + (y * srcWidth) + x];
            i++;
        }
    }

    *outWidth = srcHeight;
    *outHeight = srcWidth;
}

void YuvEngine::I420ClockWiseRotate90(unsigned char* pI420,int srcWidth,int srcHeight, unsigned char* outData,int* outWidth,int* outHeight)
{
    if(pI420 == NULL || outData == NULL){
        LOGE("%s: pI420 is null or outData is null",__FUNCTION__);
        return;
    }

    int nPos = 0;
    //旋转Y
    int k = 0;
    for(int i=0;i<srcWidth;i++)
    {
        for(int j = srcHeight -1;j >=0;j--)
        {
            outData[k++] = pI420[j*srcWidth + i];
        }
    }
    //旋转U
    nPos = srcWidth*srcHeight;
    for(int i=0;i<srcWidth/2;i++)
    {
        for(int j= srcHeight/2-1;j>=0;j--)
        {
            outData[k++] = pI420[nPos+ j*srcWidth/2 +i];
        }
    }

    //旋转V
    nPos = srcWidth*srcHeight*5/4;
    for(int i=0;i<srcWidth/2;i++)
    {
        for(int j= srcHeight/2-1;j>=0;j--)
        {
            outData[k++] = pI420[nPos+ j*srcWidth/2 +i];
        }
    }

    *outWidth = srcHeight;
    *outHeight = srcWidth;
    LOGD("%s: outWidth: %d,outHeight:%d",__FUNCTION__,*outWidth,*outHeight);
}

void YuvEngine::Nv12ClockWiseRotate90(unsigned char* pNv12,int srcWidth,int srcHeight, unsigned char* outData,int* outWidth,int* outHeight)
{
    if(pNv12 == NULL || outData == NULL){
        LOGE("%s: pNv12 is null or outData is null",__FUNCTION__);
        return;
    }

    // Rotate the Y luma
    int i =0;
    for(int x =0;x < srcWidth;x++){
        for(int y = srcHeight-1;y >=0;y--){
            outData[i]= pNv12[y*srcWidth+x];
            i++;
        }
    }

    // Rotate the U and V color components
    i = srcWidth*srcHeight*3/2-1;
    for(int x = srcWidth-1;x >0;x=x-2){
        for(int y =0;y < srcHeight/2;y++){
            outData[i]= pNv12[(srcWidth*srcHeight)+(y*srcWidth)+x];
            i--;
            outData[i]= pNv12[(srcWidth*srcHeight)+(y*srcWidth)+(x-1)];
            i--;
        }
    }

    *outWidth = srcHeight;
    *outHeight = srcWidth;
    LOGD("%s: outWidth: %d,outHeight:%d",__FUNCTION__,*outWidth,*outHeight);
}

void YuvEngine::Yv12ClockWiseRotate90(unsigned char* pYv12,int srcWidth,int srcHeight, unsigned char* outData,int* outWidth,int* outHeight)
{
    if(pYv12 == NULL || outData == NULL){
        LOGE("%s: pYv12 is null or outData is null",__FUNCTION__);
        return;
    }

    int nPos = 0;
    //旋转Y
    int k = 0;
    for(int i=0;i<srcWidth;i++)
    {
        for(int j = srcHeight -1;j >=0;j--)
        {
            outData[k++] = pYv12[j*srcWidth + i];
        }
    }
    //旋转V
    nPos = srcWidth*srcHeight;
    for(int i=0;i<srcWidth/2;i++)
    {
        for(int j= srcHeight/2-1;j>=0;j--)
        {
            outData[k++] = pYv12[nPos+ j*srcWidth/2 +i];
        }
    }

    //旋转U
    nPos = srcWidth*srcHeight*5/4;
    for(int i=0;i<srcWidth/2;i++)
    {
        for(int j= srcHeight/2-1;j>=0;j--)
        {
            outData[k++] = pYv12[nPos+ j*srcWidth/2 +i];
        }
    }

    *outWidth = srcHeight;
    *outHeight = srcWidth;
    LOGD("%s: outWidth: %d,outHeight:%d",__FUNCTION__,*outWidth,*outHeight);
}

