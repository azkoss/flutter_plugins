package witparking.inspection.inspectionprinter.ZKC;

/*
* 深圳市智谷联软件技术有限公司
* 5804
* 热敏打印机
* */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.smartdevicesdk.btprinter.BluetoothService;
import com.smartdevicesdk.btprinter.ICoallBack;
import com.smartdevicesdk.btprinter.PrintService;

import java.util.Set;

import witparking.inspection.inspectionprinter.WPPrinter;

public class ZKC extends WPPrinter {

    private PrintService printService;
    private BluetoothAdapter bluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    private Context _context;

    public ZKC(Context context) {

        this._context = context;

        try {
            printService = new PrintService(context);
            //设置字体
            printService.write(PrintCommand.set_FontStyle(0, 0, 0, 0, 0));
            // 查询已配对的设备
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            pairedDevices = bluetoothAdapter.getBondedDevices();
        } catch (Exception e) {
            Log.e("", e.getMessage());
        }

    }

    public void connect(final ZKCConnectionInterface zkcConnectionInterface) {

        //设备已经连接
        if (printService.isConnected()) {
            printService.write(PrintCommand.set_Buzzer(2, 1));
            zkcConnectionInterface.onConnect(true, "");
            return;
        }

        // 手机上没有配对设备
        try {
            if (pairedDevices == null) {
                zkcConnectionInterface.onConnect(false, "请确认蓝牙是否开启");
                // 查询已配对的设备
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                pairedDevices = bluetoothAdapter.getBondedDevices();
                return;
            }else if (pairedDevices.size() <= 0) {
                zkcConnectionInterface.onConnect(false, "暂无配对设备，请打开手机设置，配对连接蓝牙设备");
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                pairedDevices = bluetoothAdapter.getBondedDevices();
                return;
            }
        } catch (Exception e) {
            zkcConnectionInterface.onConnect(false, "请确认蓝牙是否开启");
            return;
        }


        // 手机上存在蓝牙配对设备
        for (BluetoothDevice device : pairedDevices) {
            Intent intent = new Intent();
            intent.putExtra("device_address", device.getAddress());
            printService.connectDevice(intent, true);
        }

        printService.setOnPrinterStatus(new ICoallBack() {
            @Override
            public void onPrinterStatus(int s, String text) {
                switch (s) {
                    case BluetoothService.STATE_CONNECTED:
                        // 连接成功蜂鸣器发声 可以打印
                        printService.write(PrintCommand.set_Buzzer(2, 1));
                        zkcConnectionInterface.onConnect(true, "");
                        break;
                    case BluetoothService.STATE_CONNECTING:
                        // 正在连接
                        break;
                    case BluetoothService.STATE_LISTEN:
                    case BluetoothService.STATE_NONE:
                        // 未连接
                        break;
                    case BluetoothService.READ_DATA:
                        // 打印机返回数据
                        break;
                }
            }
        });
    }

    /*
    * 检测打印机连接状态
    * */
    public void isConnected(ZKCConnectionInterface zkcConnectionInterface) {
        if (printService.isConnected()) {
            printService.write(PrintCommand.set_Buzzer(2, 1));
            zkcConnectionInterface.onConnect(true, "");
        }
        zkcConnectionInterface.onConnect(false, "未连接");
    }

    /*
    * 检测打印机状态
    * 检测蓝牙连接状态
    * */
    private boolean checkPrinterState () {
        return false;
    }

    /*
    * 打印文本
    * */
    public void printText(String text, ZKCPrintInterface callback) {

        if (text.indexOf("centered:") == 0) {
            printService.write(PrintCommand.set_Align(1));
            text = text.split("centered:")[1];
            printService.printText(text);
            return;
        }
        printService.write(PrintCommand.set_Align(0));
        printService.printText(text);
    }

    /*
    * 打印空白行
    * */
    public void printBlankLine(ZKCPrintInterface callback) {
        printService.printText("\n");
    }

    /*
    * 打印图片
    * */
    public void printImage(String base64Image, ZKCPrintInterface callback) {
        printService.write(PrintCommand.set_Align(1));
        Bitmap bitmap = null;
        byte[] bitmapArray;
        bitmapArray = Base64.decode(base64Image, Base64.DEFAULT);
        bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
        printService.printRasterImage(bitmap);
    }

    /*
    * 打印二维码
    * */
    public void printQRCode(String content, ZKCPrintInterface callback) {
        printService.printQrCode(content);
    }
}
