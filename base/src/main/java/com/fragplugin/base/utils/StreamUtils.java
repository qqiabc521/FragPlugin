package com.fragplugin.base.utils;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by silvercc on 16/2/23.
 */
public class StreamUtils {
    /**
     * 流转字符串，建议后面放到流工具类中
     *
     * @param is
     * @return
     */
    public static String readFromStreamToString(InputStream is) {
        if(is == null){
            throw new IllegalArgumentException("is shouldn't be null!");
        }
        String result = "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            result = baos.toString();
            is.close();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 流转File，建议后面放到流工具类中
     *
     * @param inStream
     * @param filePath
     * @return
     */
    public static File readFromStreamToFile(InputStream inStream, String filePath) {
        if(inStream == null){
            throw new IllegalArgumentException("is shouldn't be null!");
        }
        if(TextUtils.isEmpty(filePath)){
            throw new IllegalArgumentException("filePath shouldn't be null!");
        }
        File result = new File(filePath);
        try {
            OutputStream os = new FileOutputStream(filePath);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = inStream.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            inStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] readFromStreamToByte(InputStream inStream){
        byte[] result = null;
        try {
            result = new byte[inStream.available()];
            inStream.read(result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(inStream != null){
                try {
                    inStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
    }
}
