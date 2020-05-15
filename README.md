# AndroidPicCompress

具体使用方法和分析可参考博文[【Android中图片压缩方案详解】](https://www.jianshu.com/p/0b4854aae105)

图片的展示可以说在我们任何一个应用中都避免不了，可是大量的图片就会出现很多的问题，比如加载大图片或者多图时的OOM问题，可以移步到[Android高效加载大图、多图避免程序OOM](http://www.jianshu.com/p/da754f9fad51).还有一个问题就是图片的上传下载问题，往往我们都喜欢图片既清楚又占的内存小，也就是尽可能少的耗费我们的流量，这就是我今天所要讲述的问题：图片的压缩方案的详解。

## 1、质量压缩法

设置bitmap options属性，降低图片的质量，像素不会减少
第一个参数为需要压缩的bitmap图片对象，第二个参数为压缩后图片保存的位置
设置options 属性0-100，来实现压缩。

```
private Bitmap compressImage(Bitmap image) {  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中  
        int options = 100;  
        while ( baos.toByteArray().length / 1024>100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩         
            baos.reset();//重置baos即清空baos  
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中  
            options -= 10;//每次都减少10  
        }  
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中  
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片  
        return bitmap;  
    }  
```
质量压缩不会减少图片的像素。它是在保持像素不变的前提下改变图片的位深及透明度等，来达到压缩图片的目的。进过它压缩的图片文件大小会有改变，但是导入成bitmap后占得内存是不变的。因为要保持像素不变，所以它就无法无限压缩，到达一个值之后就不会继续变小了。显然这个方法并不适用于缩略图，其实也不适用于想通过压缩图片减少内存的适用，仅仅适用于想在保证图片质量的同时减少文件大小的情况而已。

## 2、采样率压缩法

```
private Bitmap getimage(String srcPath) {  
        BitmapFactory.Options newOpts = new BitmapFactory.Options();  
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了  
        newOpts.inJustDecodeBounds = true;  
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath,newOpts);//此时返回bm为空 
        newOpts.inJustDecodeBounds = false;  
        int w = newOpts.outWidth;  
        int h = newOpts.outHeight;  
        //现在主流手机比较多是1280*720分辨率，所以高和宽我们设置为  
        float hh = 1280f;//这里设置高度为1280f  
        float ww = 720f;//这里设置宽度为720f 
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可  
        int be = 1;//be=1表示不缩放  
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放  
            be = (int) (newOpts.outWidth / ww);  
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放  
            be = (int) (newOpts.outHeight / hh);  
        }  
        if (be <= 0)  
            be = 1;  
        newOpts.inSampleSize = be;//设置缩放比例  
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了  
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);  
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩  
    }  
```

这个方法的好处是大大的缩小了内存的使用，在读存储器上的图片时，如果不需要高清的效果，可以先只读取图片的边，通过宽和高设定好取样率后再加载图片，这样就不会过多的占用内存。

## 3、缩放法

通过缩放图片像素来减少图片占用内存大小。

 + **方式一**
```
public static void compressBitmapToFile(Bitmap bmp, File file){
    // 尺寸压缩倍数,值越大，图片尺寸越小
    int ratio = 2;
    // 压缩Bitmap到对应尺寸
    Bitmap result = Bitmap.createBitmap(bmp.getWidth() / ratio, bmp.getHeight() / ratio, Config.ARGB_8888);
    Canvas canvas = new Canvas(result);
    Rect rect = new Rect(0, 0, bmp.getWidth() / ratio, bmp.getHeight() / ratio);
    canvas.drawBitmap(bmp, null, rect, null);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // 把压缩后的数据存放到baos中
    result.compress(Bitmap.CompressFormat.JPEG, 100 ,baos);
    try {  
        FileOutputStream fos = new FileOutputStream(file);  
        fos.write(baos.toByteArray());  
        fos.flush();  
        fos.close();  
    } catch (Exception e) {  
        e.printStackTrace();  
    } 
}
```

 + **方式二**
 
```
ByteArrayOutputStream out = new ByteArrayOutputStream();  
image.compress(Bitmap.CompressFormat.JPEG, 85, out);  
float zoom = (float)Math.sqrt(size * 1024 / (float)out.toByteArray().length);  
  
 Matrix matrix = new Matrix();  
matrix.setScale(zoom, zoom);  
  
 Bitmap result = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);  
  
 out.reset();  
result.compress(Bitmap.CompressFormat.JPEG, 85, out);  
while(out.toByteArray().length > size * 1024){  
    System.out.println(out.toByteArray().length);  
    matrix.setScale(0.9f, 0.9f);  
    result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, true);  
    out.reset();  
    result.compress(Bitmap.CompressFormat.JPEG, 85, out);  
}
```

缩放法其实很简单，设定好matrix，在createBitmap就可以了。但是我们并不知道缩放比例，而是要求了图片的最终大小。直接用大小的比例来做的话肯定是有问题的，用大小比例的开方来做会比较接近，但是还是有差距。但是只要再做一下微调应该就可以了，微调的话就是修改过的图片大小比最终大小还大的话，就进行0.8的压缩再比较，循环直到大小合适。这样就能得到合适大小的图片，而且也能比较保证质量。

## 4、JNI调用libjpeg库压缩

JNI静态调用 `bitherlibjni.c` 中的方法来实现压缩`Java_net_bither_util_NativeUtil_compressBitmap`
net_bither_util为包名，NativeUtil为类名，compressBitmap为native方法名，我们只需要调用saveBitmap()方法就可以，bmp 需要压缩的Bitmap对象, quality压缩质量0-100, fileName 压缩后要保存的文件地址, optimize 是否采用哈弗曼表数据计算 品质相差5-10倍。

```
jstring Java_net_bither_util_NativeUtil_compressBitmap(JNIEnv* env,
		jobject thiz, jobject bitmapcolor, int w, int h, int quality,
		jbyteArray fileNameStr, jboolean optimize) {

	AndroidBitmapInfo infocolor;
	BYTE* pixelscolor;
	int ret;
	BYTE * data;
	BYTE *tmpdata;
	char * fileName = jstrinTostring(env, fileNameStr);
	if ((ret = AndroidBitmap_getInfo(env, bitmapcolor, &infocolor)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return (*env)->NewStringUTF(env, "0");;
	}
	if ((ret = AndroidBitmap_lockPixels(env, bitmapcolor, &pixelscolor)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
	}

	BYTE r, g, b;
	data = NULL;
	data = malloc(w * h * 3);
	tmpdata = data;
	int j = 0, i = 0;
	int color;
	for (i = 0; i < h; i++) {
		for (j = 0; j < w; j++) {
			color = *((int *) pixelscolor);
			r = ((color & 0x00FF0000) >> 16);
			g = ((color & 0x0000FF00) >> 8);
			b = color & 0x000000FF;
			*data = b;
			*(data + 1) = g;
			*(data + 2) = r;
			data = data + 3;
			pixelscolor += 4;

		}

	}
	AndroidBitmap_unlockPixels(env, bitmapcolor);
	int resultCode= generateJPEG(tmpdata, w, h, quality, fileName, optimize);
	free(tmpdata);
	if(resultCode==0){
		jstring result=(*env)->NewStringUTF(env, error);
		error=NULL;
		return result;
	}
	return (*env)->NewStringUTF(env, "1"); //success
}
```

## 5、质量压缩+采样率压缩+JNI调用libjpeg库压缩结合使用

首先通过尺寸压缩，压缩到手机常用的一个分辨率(1280*960 微信好像是压缩到这个分辨率),然后我们要把图片压缩到一定大小以内(比如说200k)，然后通过循环进行质量压缩来计算options需要设置为多少，最后调用JNI压缩。效果如下图：
![1.png](https://upload-images.jianshu.io/upload_images/3678546-597fdb06c17dba8b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

+ 计算缩放比

```
   /**
	 * 计算缩放比
	 * @param bitWidth 当前图片宽度
	 * @param bitHeight 当前图片高度
	 * @return int 缩放比
	 */
	public static int getRatioSize(int bitWidth, int bitHeight) {
		// 图片最大分辨率
		int imageHeight = 1280;
		int imageWidth = 960;
		// 缩放比
		int ratio = 1;
		// 缩放比,由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
		if (bitWidth > bitHeight && bitWidth > imageWidth) {
			// 如果图片宽度比高度大,以宽度为基准
			ratio = bitWidth / imageWidth;
		} else if (bitWidth < bitHeight && bitHeight > imageHeight) {
			// 如果图片高度比宽度大，以高度为基准
			ratio = bitHeight / imageHeight;
		}
		// 最小比率为1
		if (ratio <= 0)
			ratio = 1;
		return ratio;
	}
```

+ 质量压缩+JNI压缩

```
/**
	 * @Description: 通过JNI图片压缩把Bitmap保存到指定目录
	 * @param curFilePath
	 *            当前图片文件地址
	 * @param targetFilePath
	 *            要保存的图片文件地址
	 */
	public static void compressBitmap(String curFilePath, String targetFilePath) {
		// 最大图片大小 500KB
		int maxSize = 500;
		//根据地址获取bitmap
		Bitmap result = getBitmapFromFile(curFilePath);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
		int quality = 100;
		result.compress(Bitmap.CompressFormat.JPEG, quality, baos);
		// 循环判断如果压缩后图片是否大于500kb,大于继续压缩
		while (baos.toByteArray().length / 1024 > maxSize) {
			// 重置baos即清空baos
			baos.reset();
			// 每次都减少10
			quality -= 10;
			// 这里压缩quality，把压缩后的数据存放到baos中
			result.compress(Bitmap.CompressFormat.JPEG, quality, baos);
		}
		// JNI保存图片到SD卡 这个关键
		NativeUtil.saveBitmap(result, quality, targetFilePath, true);
		// 释放Bitmap
		if (!result.isRecycled()) {
			result.recycle();
		}
	}
```

+ JNI图片压缩工具类

```
package net.bither.util;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
/**
 * JNI图片压缩工具类
 *
 * @Description TODO
 * @Package net.bither.util
 * @Class NativeUtil
 */
public class NativeUtil {

	private static int DEFAULT_QUALITY = 95;

	/**
	 * @Description: JNI基本压缩
	 * @param bit
	 *            bitmap对象
	 * @param fileName
	 *            指定保存目录名
	 * @param optimize
	 *            是否采用哈弗曼表数据计算 品质相差5-10倍
	 */
	public static void compressBitmap(Bitmap bit, String fileName, boolean optimize) {
		saveBitmap(bit, DEFAULT_QUALITY, fileName, optimize);
	}

	/**
	 * @Description: 通过JNI图片压缩把Bitmap保存到指定目录
	 * @param image
	 *            bitmap对象
	 * @param filePath
	 *            要保存的指定目录
	 */
	public static void compressBitmap(Bitmap image, String filePath) {
		// 最大图片大小 150KB
		int maxSize = 150;
		// 获取尺寸压缩倍数
		int ratio = NativeUtil.getRatioSize(image.getWidth(),image.getHeight());
		// 压缩Bitmap到对应尺寸
		Bitmap result = Bitmap.createBitmap(image.getWidth() / ratio,image.getHeight() / ratio, Config.ARGB_8888);
		Canvas canvas = new Canvas(result);
		Rect rect = new Rect(0, 0, image.getWidth() / ratio, image.getHeight() / ratio);
		canvas.drawBitmap(image,null,rect,null);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
		int options = 100;
		result.compress(Bitmap.CompressFormat.JPEG, options, baos);
		// 循环判断如果压缩后图片是否大于100kb,大于继续压缩
		while (baos.toByteArray().length / 1024 > maxSize) {
			// 重置baos即清空baos
			baos.reset();
			// 每次都减少10
			options -= 10;
			// 这里压缩options%，把压缩后的数据存放到baos中
			result.compress(Bitmap.CompressFormat.JPEG, options, baos);
		}
		// JNI保存图片到SD卡 这个关键
		NativeUtil.saveBitmap(result, options, filePath, true);
		// 释放Bitmap
		if (!result.isRecycled()) {
			result.recycle();
		}
	}

	/**
	 * @Description: 通过JNI图片压缩把Bitmap保存到指定目录
	 * @param curFilePath
	 *            当前图片文件地址
	 * @param targetFilePath
	 *            要保存的图片文件地址
	 */
	public static void compressBitmap(String curFilePath, String targetFilePath) {
		// 最大图片大小 500KB
		int maxSize = 500;
		//根据地址获取bitmap
		Bitmap result = getBitmapFromFile(curFilePath);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
		int quality = 100;
		result.compress(Bitmap.CompressFormat.JPEG, quality, baos);
		// 循环判断如果压缩后图片是否大于500kb,大于继续压缩
		while (baos.toByteArray().length / 1024 > maxSize) {
			// 重置baos即清空baos
			baos.reset();
			// 每次都减少10
			quality -= 10;
			// 这里压缩quality，把压缩后的数据存放到baos中
			result.compress(Bitmap.CompressFormat.JPEG, quality, baos);
		}
		// JNI保存图片到SD卡 这个关键
		NativeUtil.saveBitmap(result, quality, targetFilePath, true);
		// 释放Bitmap
		if (!result.isRecycled()) {
			result.recycle();
		}

	}

	/**
	 * 计算缩放比
	 * @param bitWidth 当前图片宽度
	 * @param bitHeight 当前图片高度
	 * @return int 缩放比
	 */
	public static int getRatioSize(int bitWidth, int bitHeight) {
		// 图片最大分辨率
		int imageHeight = 1280;
		int imageWidth = 960;
		// 缩放比
		int ratio = 1;
		// 缩放比,由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
		if (bitWidth > bitHeight && bitWidth > imageWidth) {
			// 如果图片宽度比高度大,以宽度为基准
			ratio = bitWidth / imageWidth;
		} else if (bitWidth < bitHeight && bitHeight > imageHeight) {
			// 如果图片高度比宽度大，以高度为基准
			ratio = bitHeight / imageHeight;
		}
		// 最小比率为1
		if (ratio <= 0)
			ratio = 1;
		return ratio;
	}

	/**
	 * 通过文件路径读获取Bitmap防止OOM以及解决图片旋转问题
	 * @param filePath
	 * @return
	 */
	public static Bitmap getBitmapFromFile(String filePath){
		BitmapFactory.Options newOpts = new BitmapFactory.Options();
		newOpts.inJustDecodeBounds = true;//只读边,不读内容  
		BitmapFactory.decodeFile(filePath, newOpts);
		int w = newOpts.outWidth;
		int h = newOpts.outHeight;
		// 获取尺寸压缩倍数
		newOpts.inSampleSize = NativeUtil.getRatioSize(w,h);
		newOpts.inJustDecodeBounds = false;//读取所有内容
		newOpts.inDither = false;
		newOpts.inPurgeable=true;
		newOpts.inInputShareable=true;
		newOpts.inTempStorage = new byte[32 * 1024];
		Bitmap bitmap = null;
		File file = new File(filePath);
		FileInputStream fs = null;
		try {
			fs = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			if(fs!=null){
				bitmap = BitmapFactory.decodeFileDescriptor(fs.getFD(),null,newOpts);
				//旋转图片
				int photoDegree = readPictureDegree(filePath);
				if(photoDegree != 0){
					Matrix matrix = new Matrix();
					matrix.postRotate(photoDegree);
					// 创建新的图片
					bitmap = Bitmap.createBitmap(bitmap, 0, 0,
							bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if(fs!=null) {
				try {
					fs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bitmap;
	}

	/**
	 *
	 * 读取图片属性：旋转的角度
	 * @param path 图片绝对路径
	 * @return degree旋转的角度
	 */

	public static int readPictureDegree(String path) {
		int degree = 0;
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			int orientation = exifInterface.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return degree;
	}

	/**
	 * 调用native方法
	 * @Description:函数描述
	 * @param bit
	 * @param quality
	 * @param fileName
	 * @param optimize
	 */
	private static void saveBitmap(Bitmap bit, int quality, String fileName, boolean optimize) {
		compressBitmap(bit, bit.getWidth(), bit.getHeight(), quality, fileName.getBytes(), optimize);
	}

	/**
	 * 调用底层 bitherlibjni.c中的方法
	 * @Description:函数描述
	 * @param bit
	 * @param w
	 * @param h
	 * @param quality
	 * @param fileNameBytes
	 * @param optimize
	 * @return
	 */
	private static native String compressBitmap(Bitmap bit, int w, int h, int quality, byte[] fileNameBytes,
												boolean optimize);
	/**
	 * 加载lib下两个so文件
	 */
	static {
		System.loadLibrary("jpegbither");
		System.loadLibrary("bitherjni");
	}
 }
```

## 图片压缩处理中可能遇到的问题：

+ 请求系统相册有三个Action

注意：图库（缩略图）   和  图片（原图）

 + `ACTION_OPEN_DOCUMENT `   仅限4.4或以上使用  默认打开原图
 
从图片获取到的uri 格式为：`content://com.android.providers.media.documents/document/image%666>>>`

 + `ACTION_GET_CONTENT `      4.4以下默认打开缩略图  。 以上打开文件管理器 供选择，选择图库打开为缩略图页面，选择图片打开为原图浏览。
 
从图库获取到的uri格式为：`content://media/external/images/media/666666`

 + `ACTION_PICK  `                    都可用，打开默认是缩略图界面，还需要进一步点开查看。
 
参考代码：
```
public void pickFromGallery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
                    REQUEST_PICK_IMAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_KITKAT_PICK_IMAGE);
        }
    }
```

+ 根据URI获取对应的文件路径

在我们从图库中选择图片后回调给我们的`data.getData()`可能是URI,我们平时对文件的操作基本上都是基于路径然后进行各种操作与转换，如今我们需要将URI对应的文件路径找出来，具体参考代码如下：

```
public static String getPathByUri(Context context, Uri data){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return getPathByUri4BeforeKitkat(context, data);
        }else {
            return getPathByUri4AfterKitkat(context, data);
        }
    }
    //4.4以前通过Uri获取路径：data是Uri，filename是一个String的字符串，用来保存路径
    public static String getPathByUri4BeforeKitkat(Context context, Uri data) {
        String filename=null;
        if (data.getScheme().toString().compareTo("content") == 0) {
            Cursor cursor = context.getContentResolver().query(data, new String[] { "_data" }, null, null, null);
            if (cursor.moveToFirst()) {
                filename = cursor.getString(0);
            }
        } else if (data.getScheme().toString().compareTo("file") == 0) {// file:///开头的uri
            filename = data.toString().replace("file://", "");// 替换file://
            if (!filename.startsWith("/mnt")) {// 加上"/mnt"头
                filename += "/mnt";
            }
        }
        return filename;
    }
    //4.4以后根据Uri获取路径：
    @SuppressLint("NewApi")
    public static String getPathByUri4AfterKitkat(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {// ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {// DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {// MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {// MediaStore
            // (and
            // general)
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {// File
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
```
