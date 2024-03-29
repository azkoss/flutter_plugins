package witparking.inspection.inspectionprinter;

import android.util.Log;

import com.landicorp.android.eptapi.exception.RequestException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import witparking.inspection.inspectionprinter.LIANDI.LIANDI;
import witparking.inspection.inspectionprinter.UBOXUN.UBOXUN;
import witparking.inspection.inspectionprinter.UBOXUN.UBOXUNPrintInterface;
import witparking.inspection.inspectionprinter.ZKC.ZKC;
import witparking.inspection.inspectionprinter.ZKC.ZKCConnectionInterface;
import witparking.inspection.inspectionprinter.ZKC.ZKCPrintInterface;


/** InspectionPrinterPlugin */
public class InspectionPrinterPlugin implements MethodCallHandler {

  private WPPrinter wpPrinter;

  private InspectionPrinterPlugin(Registrar registrar) {

    if ("UBX".equals(android.os.Build.MANUFACTURER)) {
      wpPrinter = new UBOXUN(registrar.activity());
    }else if ("LANDI".equals(android.os.Build.MANUFACTURER)) {
      wpPrinter = new LIANDI(registrar.activity());
    } else {
      wpPrinter = new ZKC(registrar.activity());
    }

  }

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {

    final MethodChannel channel = new MethodChannel(registrar.messenger(), "inspection_printer");
    channel.setMethodCallHandler(new InspectionPrinterPlugin(registrar));

  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {

    switch (call.method) {
      case "print":
        print(call, result);
        break;
      case "getBLEPrinterState":
        getBLEPrinterState(result);
        break;
      case "connectToBLEPrinter":
        connectToBLEPrinter(result);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  /*
  * 打印内容
  * */
  private void print(MethodCall call, Result result) {

    final ArrayList list = call.argument("list");

    if (list == null || list.size() <= 0) return;

    // 检测设备
    if ("LANDI".equals(android.os.Build.MANUFACTURER)) {//联迪POS
      printForLIANDI(list);
    } else if("UBX".equals(android.os.Build.MANUFACTURER)) {//优博讯POS机
      printForUBX(list);
    }else{
      //普通手机 选择蓝牙打印方式
      printForZKC(list, result);
    }

  }

  /*
  * ZKC
  * 蓝牙打印机
  * 打印
  * */
  private void printForZKC(final ArrayList list, final Result result) {

    final ZKC zkc = (ZKC)wpPrinter;

    new Thread(new Runnable() {
      @Override
      public void run() {

        zkc.connect(new ZKCConnectionInterface() {
          @Override
          public void onConnect(boolean success, String msg) {
            if (success) {
              for (Object item : list) {

                String value = (String) item;

                // base64图片
                if (value.indexOf("pictureStream") == 0) {
                  try {
                    String base64Image = value.split("pictureStream")[1];
                    zkc.printImage(base64Image, null);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }else if (value.equals("splitline")) {
                  zkc.printText("--------------------------------", null);
                } else {
                  value += '\n';
                  zkc.printText(value, new ZKCPrintInterface() {
                    @Override
                    public void onError(String message) {

                    }
                  });
                }

                try {
                  Thread.sleep(100);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
              // 出纸
              zkc.printText("\n\n\n", null);
            } else
            {
              HashMap map = new HashMap();
              map.put("success", false);
              map.put("msg", msg);
              result.success(map);
            }
          }
        });

      }
    }).start();

  }

  /*
  * UBX
  * 优博讯POS机打印
  * */
  private void printForUBX(final ArrayList list) {

    UBOXUN uboxun = (UBOXUN)wpPrinter;

    for (Object item : list) {

      String value = (String) item;

      // base64图片
      if (value.indexOf("pictureStream") == 0) {
        String base64Image = value.split("pictureStream")[1];
        try {
          uboxun.printImage(base64Image, null);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }else if (value.equals("splitline")) {
        uboxun.printText("--------------------------------------------", null);
      } else {
        if (value.isEmpty()) value = "\n";
        uboxun.printText(value, new UBOXUNPrintInterface() {
          @Override
          public void onError(String message) {

          }
        });
      }

    }

    uboxun.printText("\n\n\n\n", null);
  }

  /*
  * LIANDI
  * 联迪POS打印机
  * */
  private void printForLIANDI(final ArrayList list) {

    LIANDI liandi = (LIANDI)wpPrinter;

    try {
      liandi.list = list;
      liandi.progress.start();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /*
  * 蓝牙打印机连接状态
  * */
  private void getBLEPrinterState(final Result result) {

    if ("UBX".equals(android.os.Build.MANUFACTURER)) {
      HashMap map = new HashMap();
      map.put("success", false);
      map.put("msg", "设备自带打印设备，无需连接");
      result.success(map);
    }else if ("LANDI".equals(android.os.Build.MANUFACTURER)) {
      HashMap map = new HashMap();
      map.put("success", false);
      map.put("msg", "设备自带打印设备，无需连接");
      result.success(map);
    } else {
      ZKC zkc = (ZKC)wpPrinter;
      zkc.isConnected(new ZKCConnectionInterface() {
        @Override
        public void onConnect(boolean success, String msg) {
          if (success) {
            HashMap map = new HashMap();
            map.put("success", true);
            map.put("msg", "连接成功");
            result.success(map);
          }else
          {
            HashMap map = new HashMap();
            map.put("success", false);
            map.put("msg", msg);
            result.success(map);
          }
        }
      });
    }
  }

  /*
  * 连接蓝牙打印机
  * */
  private void connectToBLEPrinter(final Result result) {

    if ("UBX".equals(android.os.Build.MANUFACTURER)) {
      HashMap map = new HashMap();
      map.put("success", false);
      map.put("msg", "设备自带打印设备，无需连接");
      result.success(map);
    }else if ("LANDI".equals(android.os.Build.MANUFACTURER)) {
      HashMap map = new HashMap();
      map.put("success", false);
      map.put("msg", "设备自带打印设备，无需连接");
      result.success(map);
    } else {
      ZKC zkc = (ZKC)wpPrinter;
      zkc.connect(new ZKCConnectionInterface() {
        @Override
        public void onConnect(boolean success, String msg) {
          if (success) {
            HashMap map = new HashMap();
            map.put("success", true);
            map.put("msg", "连接成功");
            result.success(map);
          }else
          {
            HashMap map = new HashMap();
            map.put("success", false);
            map.put("msg", msg);
            result.success(map);
          }
        }
      });
    }

  }
}
