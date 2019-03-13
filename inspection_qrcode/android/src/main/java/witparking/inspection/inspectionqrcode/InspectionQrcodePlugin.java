package witparking.inspection.inspectionqrcode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Base64;

import com.ypy.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import witparking.inspection.inspectionqrcode.zxing.android.CaptureActivity;
import witparking.inspection.inspectionqrcode.zxing.util.CreateErWei;

/**
 * InspectionQrcodePlugin
 */
public class InspectionQrcodePlugin implements MethodCallHandler {

    private static Registrar registrar;
    private Result scanResult;

    private InspectionQrcodePlugin() {
        EventBus.getDefault().register(this);
    }
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {

        InspectionQrcodePlugin.registrar = registrar;

        final MethodChannel channel = new MethodChannel(registrar.messenger(), "inspection_qrcode");
        channel.setMethodCallHandler(new InspectionQrcodePlugin());
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "startScan":
                scanResult = result;
                Intent intent = new Intent(InspectionQrcodePlugin.registrar.activity(), CaptureActivity.class);
                InspectionQrcodePlugin.registrar.activity().startActivity(intent);
                break;
            case "createQRCode":
                String base64Image = "";
                CreateErWei createErWei = new CreateErWei();
                Bitmap bitmap = createErWei.createQRImage((String) call.argument("content"));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                try {
                    baos.flush();
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                byte[] bitmapBytes = baos.toByteArray();
                String res = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
                res = "data:image/png;base64," + res;
                base64Image = res;
                result.success(res);
                break;
            default:
                break;
        }
    }

    /*
    * 扫码返回结果
    * */
    public void onEventMainThread(QRCodeEvent event) {
        if (event.str != null) {
            scanResult.success(event.str);
        }
    }
}
