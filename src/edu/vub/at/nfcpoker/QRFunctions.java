package edu.vub.at.nfcpoker;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.graphics.Bitmap;
import android.net.Uri;

public class QRFunctions {

    // Taken from the ZXing source code.
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    
    public static Bitmap encodeBitmap(String contents) throws WriterException {
    	int width = 200;
    	int height = 200;
		BitMatrix result = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, width, height);
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
          int offset = y * width;
          for (int x = 0; x < width; x++) {
            pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
          }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
    
    public static String createJoinUri(String wifiGroupName, String wifiPassword, String ipAddress, boolean isDedicated) {
    	Uri uri = Uri.parse(Constants.INTENT_BASE_URL)
    			     .buildUpon()
    			     .appendQueryParameter(Constants.INTENT_WIFI_NAME, wifiGroupName)
    			     .appendQueryParameter(Constants.INTENT_WIFI_PASSWORD, wifiPassword)
    			     .appendQueryParameter(Constants.INTENT_SERVER_IP, ipAddress)
    			     .appendQueryParameter(Constants.INTENT_IS_DEDICATED, "" + true)
    			     .build();
    	
    	return uri.toString();
	}
}
