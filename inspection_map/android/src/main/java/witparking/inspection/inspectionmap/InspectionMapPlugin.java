package witparking.inspection.inspectionmap;


import android.Manifest;
import android.support.annotation.NonNull;
import android.util.Log;

import com.baidu.mapapi.map.MapView;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;

/**
 * InspectionMapPlugin
 */
public class InspectionMapPlugin implements MethodCallHandler {

    private Registrar registrar;
    private static EventChannel.EventSink locEventSink;
    private static EventChannel.EventSink hawEventSink;

    private LBSTrace lbsTrace;

    private static BMapViewFactory bMapViewFactory;

    private InspectionMapPlugin(Registrar registrar) {
        this.registrar = registrar;
    };

    /**
     * Plugin registration.
     */
    public static void registerWith(final Registrar registrar) {

        final MethodChannel channel = new MethodChannel(registrar.messenger(), "inspection_map");
        channel.setMethodCallHandler(new InspectionMapPlugin(registrar));

        final EventChannel message_channel = new EventChannel(registrar.messenger(), "map.event.location");
        message_channel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, final EventChannel.EventSink eventSink) {
                InspectionMapPlugin.locEventSink = eventSink;

                LBSLocation lbsLocation = new LBSLocation(registrar.activity());
                lbsLocation.start(new LocationInterface() {
                    @Override
                    public void onLocationUpdate(HashMap res) {
                        eventSink.success(res);
                    }
                });
            }

            @Override
            public void onCancel(Object o) {
                InspectionMapPlugin.locEventSink = null;
            }
        });

        final EventChannel hawEyeMessageChannel = new EventChannel(registrar.messenger(), "map.event.hawk_eye");
        hawEyeMessageChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, final EventChannel.EventSink eventSink) {
                InspectionMapPlugin.hawEventSink = eventSink;
            }

            @Override
            public void onCancel(Object o) {
                InspectionMapPlugin.hawEventSink = null;
            }
        });

        /*
        * Android View
        * */
        bMapViewFactory = new BMapViewFactory(new StandardMessageCodec(), registrar);
        registrar.platformViewRegistry().registerViewFactory("MapView", bMapViewFactory);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {

        switch (call.method) {
            case "startGather":
                startGather(call, result);
                break;
            case "stopGather":
                if (lbsTrace != null) {
                    lbsTrace.stop();
                    lbsTrace = null;
                }
                break;
            case "getUserLocation":
                getUserLocation(result);
                break;
            case "placePin":
                result.notImplemented();
                break;
            default:
                result.notImplemented();
                break;
        }

    }

    /*
     * 开启鹰眼轨迹
     * */
    private void startGather(MethodCall call, final Result result) {
        if (lbsTrace != null) {
            lbsTrace.stop();
            lbsTrace = null;
        }
        lbsTrace = new LBSTrace();
        lbsTrace.init(registrar.activity());
        lbsTrace.start((String) call.argument("entity"), new TraceInterface() {
            @Override
            public void onRes(Map res) {
                Log.e("鹰眼", res.toString());
                if (hawEventSink != null) {
                    hawEventSink.success(res);
                }
            }
        });
        result.success("成功开启鹰眼轨迹");
    }

    /*
    * 获取用户位置信息
    * */
    private void getUserLocation(final Result result) {

        AndPermission
                .with(registrar.activity())
                .requestCode(100)
                .permission(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .callback(new PermissionListener() {
                    @Override
                    public void onSucceed(int requestCode, @NonNull List<String> grantPermissions) {
                        LBSLocation lbsLocation = new LBSLocation(registrar.activity());
                        lbsLocation.onceGet = true;
                        lbsLocation.start(new LocationInterface() {
                            @Override
                            public void onLocationUpdate(HashMap res) {
                                result.success(res);
                            }
                        });
                    }
                    @Override
                    public void onFailed(int requestCode, @NonNull List<String> deniedPermissions) {

                    }
                }).start();

    }

}
