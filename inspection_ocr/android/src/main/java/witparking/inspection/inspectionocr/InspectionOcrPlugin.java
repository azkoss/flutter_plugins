package witparking.inspection.inspectionocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.ypy.eventbus.EventBus;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * InspectionOcrPlugin
 */
public class InspectionOcrPlugin implements MethodCallHandler {

    private static Registrar registrar;
    private SpiteUtil spiteUtil;
    private Result recognitionResult;

    private InspectionOcrPlugin() {
        EventBus.getDefault().register(this);
        spiteUtil = new SpiteUtil(InspectionOcrPlugin.registrar.activity());
        spiteUtil.init();
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {

        InspectionOcrPlugin.registrar = registrar;

        final MethodChannel channel = new MethodChannel(registrar.messenger(), "inspection_ocr");
        channel.setMethodCallHandler(new InspectionOcrPlugin());
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "recognitionTheplate":
                recognitionResult = result;
                spiteUtil.spite(true);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    /*
    * 车牌识别结果
    * */
    public void onEventMainThread(SpiteBackEvent event) {

        String base64Image = "";
        File dir = new File(event.path);
        if (dir.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(event.path);
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
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("number", event.number);
        map.put("color", event.color);
        map.put("path", event.path);
        map.put("base64Image", base64Image);
        recognitionResult.success(map);
    }
}