package com.datatrixsoft.printerdriver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;


public class HsBluetoothPrintDriver implements Contants {
    private static final int STATE_NONE = 0;
    private static final int STATE_LISTEN = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int STATE_CONNECTED = 3;
    private static HsBluetoothPrintDriver mBluetoothPrintDriver;
    private final String TAG = "HsBluetoothPrintDriver";
    private final boolean D = true;
    private final String NAME = "HsBluetoothPrintDriver";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState = 0;

    private HsBluetoothPrintDriver() {
    }

    public static HsBluetoothPrintDriver getInstance() {
        if (mBluetoothPrintDriver == null) {
            mBluetoothPrintDriver = new HsBluetoothPrintDriver();
        }

        return mBluetoothPrintDriver;
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    private void sendMessageToMainThread(int flag) {
        if (this.mHandler != null) {
            Message message = this.mHandler.obtainMessage();
            Bundle data = new Bundle();
            data.putInt("flag", flag);
            message.setData(data);
            this.mHandler.sendMessage(message);
        }

    }

    private void sendMessageToMainThread(int flag, int state) {
        if (this.mHandler != null) {
            Message message = this.mHandler.obtainMessage();
            Bundle data = new Bundle();
            data.putInt("flag", flag);
            data.putInt("state", state);
            message.setData(data);
            this.mHandler.sendMessage(message);
        }

    }

    public synchronized int getState() {
        return this.mState;
    }

    private synchronized void setState(int state) {
        Log.d("HsBluetoothPrintDriver", "setState() " + this.mState + " -> " + state);
        this.mState = state;
        switch(this.mState) {
            case 0:
                this.sendMessageToMainThread(32, 16);
            case 1:
            case 2:
            default:
                break;
            case 3:
                this.sendMessageToMainThread(32, 17);
        }

    }

    public synchronized void start() {
        Log.d("HsBluetoothPrintDriver", "start");
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        if (this.mAcceptThread == null) {
            this.mAcceptThread = new AcceptThread();
            this.mAcceptThread.start();
        }

        this.setState(1);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d("HsBluetoothPrintDriver", "connect to: " + device);
        if (this.mState == 2 && this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        this.mConnectThread = new ConnectThread(device);
        this.mConnectThread.start();
        this.setState(2);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d("HsBluetoothPrintDriver", "connected");
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        if (this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }

        this.mConnectedThread = new ConnectedThread(socket);
        this.mConnectedThread.start();
        this.sendMessageToMainThread(34);
        this.setState(3);
    }

    public synchronized void stop() {
        Log.d("HsBluetoothPrintDriver", "stop");
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if (this.mConnectedThread != null) {
            this.mConnectedThread.cancel();
            this.mConnectedThread = null;
        }

        if (this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }

        this.setState(0);
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized(this) {
            if (this.mState != 3) {
                return;
            }

            r = this.mConnectedThread;
        }

        r.write(out);
    }

    public void write2(byte[] out) throws IOException {
        ConnectedThread r;
        synchronized(this) {
            if (this.mState != 3) {
                return;
            }

            r = this.mConnectedThread;
        }

        for(int i = 0; i < out.length; ++i) {
            r.mmOutStream.write(out[i]);
        }

    }

    public void BT_Write(String dataString) {
        byte[] data = null;
        if (this.mState == 3) {
            ConnectedThread r = this.mConnectedThread;

            try {
                data = dataString.getBytes("GBK");
            } catch (UnsupportedEncodingException var5) {
                var5.printStackTrace();
            }

            r.write(data);
        }
    }

    public void BT_Write(String dataString, boolean bGBK) {
        byte[] data = null;
        if (this.mState == 3) {
            ConnectedThread r = this.mConnectedThread;
            if (bGBK) {
                try {
                    data = dataString.getBytes("GBK");
                } catch (UnsupportedEncodingException var6) {
                }
            } else {
                data = dataString.getBytes();
            }

            r.write(data);
        }
    }

    public void BT_Write(byte[] out) {
        if (this.mState == 3) {
            ConnectedThread r = this.mConnectedThread;
            r.write(out);
        }
    }

    public void BT_Write(byte[] out, int dataLen) {
        if (this.mState == 3) {
            ConnectedThread r = this.mConnectedThread;
            r.write(out, dataLen);
        }
    }

    public boolean IsNoConnection() {
        return this.mState != 3;
    }

    public boolean InitPrinter() {
        byte[] combyte = new byte[]{27, 64};
        if (this.mState != 3) {
            return false;
        } else {
            this.BT_Write(combyte);
            return true;
        }
    }

    public void WakeUpPritner() {
        byte[] b = new byte[3];

        try {
            this.BT_Write(b);
            Thread.sleep(100L);
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    public void SetDefaultSetting() {
        this.BT_Write(new byte[]{27, 33, 0});
    }

    public void Begin() {
        this.WakeUpPritner();
        this.InitPrinter();
    }

    public void LF() {
        byte[] cmd = new byte[]{13};
        this.BT_Write(cmd);
    }

    public void CR() {
        byte[] cmd = new byte[]{10};
        this.BT_Write(cmd);
    }

    public void SelftestPrint() {
        byte[] cmd = new byte[]{18, 84};
        this.BT_Write(cmd, 2);
    }

    public void Beep(byte times, byte time) {
        byte[] cmd = new byte[]{27, 66, times, time};
        this.BT_Write(cmd, 4);
    }

    public void StatusInquiry() {
        byte[] cmd = new byte[]{16, 4, -2};
        this.BT_Write(cmd, 3);
        byte[] cmd1 = new byte[]{16, 4, -1};
        this.BT_Write(cmd1, 3);
    }

    public void SetRightSpacing(byte Distance) {
        byte[] cmd = new byte[]{27, 32, Distance};
        this.BT_Write(cmd);
    }

    public void SetAbsolutePrintPosition(byte nL, byte nH) {
        byte[] cmd = new byte[]{27, 36, nL, nH};
        this.BT_Write(cmd);
    }

    public void SetRelativePrintPosition(byte nL, byte nH) {
        byte[] cmd = new byte[]{27, 92, nL, nH};
        this.BT_Write(cmd);
    }

    public void SetDefaultLineSpacing() {
        byte[] cmd = new byte[]{27, 50};
        this.BT_Write(cmd);
    }

    public void SetLineSpacing(byte LineSpacing) {
        byte[] cmd = new byte[]{27, 51, LineSpacing};
        this.BT_Write(cmd);
    }

    public void SetLeftStartSpacing(byte nL, byte nH) {
        byte[] cmd = new byte[]{29, 76, nL, nH};
        this.BT_Write(cmd);
    }

    public void SetAreaWidth(byte nL, byte nH) {
        byte[] cmd = new byte[]{29, 87, nL, nH};
        this.BT_Write(cmd);
    }

    public void SetCharacterPrintMode(byte CharacterPrintMode) {
        byte[] cmd = new byte[]{27, 33, CharacterPrintMode};
        this.BT_Write(cmd);
    }

    public void SetUnderline(byte UnderlineEn) {
        byte[] cmd = new byte[]{27, 45, UnderlineEn};
        this.BT_Write(cmd);
    }

    public void SetBold(byte BoldEn) {
        byte[] cmd = new byte[]{27, 69, BoldEn};
        this.BT_Write(cmd);
    }

    public void SetCharacterFont(byte Font) {
        byte[] cmd = new byte[]{27, 77, Font};
        this.BT_Write(cmd);
    }

    public void SetRotate(byte RotateEn) {
        byte[] cmd = new byte[]{27, 86, RotateEn};
        this.BT_Write(cmd);
    }

    public void SetAlignMode(byte AlignMode) {
        byte[] cmd = new byte[]{27, 97, AlignMode};
        this.BT_Write(cmd);
    }

    public void SetInvertPrint(byte InvertModeEn) {
        byte[] cmd = new byte[]{27, 123, InvertModeEn};
        this.BT_Write(cmd);
    }

    public void SetFontEnlarge(byte FontEnlarge) {
        byte[] cmd = new byte[]{29, 33, FontEnlarge};
        this.BT_Write(cmd);
    }

    public void SetBlackReversePrint(byte BlackReverseEn) {
        byte[] cmd = new byte[]{29, 66, BlackReverseEn};
        this.BT_Write(cmd);
    }

    public void SetChineseCharacterMode(byte ChineseCharacterMode) {
        byte[] cmd = new byte[]{28, 33, ChineseCharacterMode};
        this.BT_Write(cmd);
    }

    public void SelChineseCodepage() {
        byte[] cmd = new byte[]{28, 38};
        this.BT_Write(cmd);
    }

    public void CancelChineseCodepage() {
        byte[] cmd = new byte[]{28, 46};
        this.BT_Write(cmd);
    }

    public void SetChineseUnderline(byte ChineseUnderlineEn) {
        byte[] cmd = new byte[]{28, 45, ChineseUnderlineEn};
        this.BT_Write(cmd);
    }

    public void OpenDrawer(byte DrawerNumber, byte PulseStartTime, byte PulseEndTime) {
        byte[] cmd = new byte[]{27, 112, DrawerNumber, PulseStartTime, PulseEndTime};
        this.BT_Write(cmd);
    }

    public void CutPaper() {
        byte[] cmd = new byte[]{27, 105};
        this.BT_Write(cmd);
    }

    public void PartialCutPaper() {
        byte[] cmd = new byte[]{27, 109};
        this.BT_Write(cmd);
    }

    public void FeedAndCutPaper(byte CutMode) {
        byte[] cmd = new byte[]{29, 86, CutMode};
        this.BT_Write(cmd);
    }

    public void FeedAndCutPaper(byte CutMode, byte FeedDistance) {
        byte[] cmd = new byte[]{29, 86, CutMode, FeedDistance};
        this.BT_Write(cmd);
    }

    public void AddQRCodePrint() {
        byte[] cmd = new byte[]{29, 40, 107, 3, 0, 49, 67, 3, 29, 40, 107, 3, 0, 49, 69, 51, 29, 40, 107, 83, 0, 49, 80, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 29, 40, 107, 3, 0, 49, 81, 48, 29, 40, 107, 4, 0, 49, 65, 49, 0};
        this.BT_Write(cmd);
    }

/*    public void AddCodePrint(BarcodeType CodeType, String data) {
        switch($SWITCH_TABLE$com$mocoo$hang$rtprinter$driver$BarcodeType()[CodeType.ordinal()]) {
            case 1:
                this.UPCA(data);
                break;
            case 2:
                this.UPCE(data);
                break;
            case 3:
                this.EAN13(data);
                break;
            case 4:
                this.EAN8(data);
                break;
            case 5:
                this.CODE39(data);
                break;
            case 6:
                this.ITF(data);
                break;
            case 7:
                this.CODEBAR(data);
                break;
            case 8:
                this.CODE93(data);
                break;
            case 9:
                this.Code128_B(data);
                break;
            case 10:
                this.CODE_QR_CODE(data);
        }

    }*/

    public void UPCA(String data) {
        int m = 0;
        int num = data.length();
        int mIndex = 0;
        byte[] cmd = new byte[1024];
        int var7 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var7++] = 107;
        cmd[var7++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if (data.charAt(i) > '9' || data.charAt(i) < '0') {
                return;
            }
        }

        if (num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var7++] = (byte)data.charAt(i);
            }

            this.BT_Write(cmd);
        }
    }

    public void UPCE(String data) {
        int m = 1;
        int num = data.length();
        int mIndex = 0;
        byte[] cmd = new byte[1024];
        int var7 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var7++] = 107;
        cmd[var7++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if (data.charAt(i) > '9' || data.charAt(i) < '0') {
                return;
            }
        }

        if (num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var7++] = (byte)data.charAt(i);
            }

            this.BT_Write(cmd);
        }
    }

    public void EAN13(String data) {
        int m = 2;
        int num = data.length();
        int mIndex = 0;
        byte[] cmd = new byte[1024];
        int var7 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var7++] = 107;
        cmd[var7++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if (data.charAt(i) > '9' || data.charAt(i) < '0') {
                return;
            }
        }

        if (num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var7++] = (byte)data.charAt(i);
            }

            this.BT_Write(cmd);
        }
    }

    public void EAN8(String data) {
        int m = 3;
        int num = data.length();
        int mIndex = 0;
        byte[] cmd = new byte[1024];
        int var7 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var7++] = 107;
        cmd[var7++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if (data.charAt(i) > '9' || data.charAt(i) < '0') {
                return;
            }
        }

        if (num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var7++] = (byte)data.charAt(i);
            }

            this.BT_Write(cmd);
        }
    }

    public void CODE39(String data) {
        int m = 4;
        int num = data.length();
        int mIndex = 0;
        byte[] cmd = new byte[1024];
        int var7 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var7++] = 107;
        cmd[var7++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if (data.charAt(i) > 127 || data.charAt(i) < ' ') {
                return;
            }
        }

        if (num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var7++] = (byte)data.charAt(i);
            }

            this.BT_Write(cmd);
        }
    }

    public void ITF(String data) {
        int m = 5;
        int num = data.length();
        int mIndex = 0;
        byte[] cmd = new byte[1024];
        int var7 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var7++] = 107;
        cmd[var7++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if (data.charAt(i) > '9' || data.charAt(i) < '0') {
                return;
            }
        }

        if (num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var7++] = (byte)data.charAt(i);
            }

            this.BT_Write(cmd);
        }
    }

    public void CODEBAR(String data) {
        int m = 6;
        int num = data.length();
        int mIndex = 0;
        byte[] cmd = new byte[1024];
        int var7 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var7++] = 107;
        cmd[var7++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if (data.charAt(i) > 127 || data.charAt(i) < ' ') {
                return;
            }
        }

        if (num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var7++] = (byte)data.charAt(i);
            }

            this.BT_Write(cmd);
        }
    }

    public void CODE93(String data) {
        int m = 7;
        int num = data.length();
        int mIndex = 0;
        byte[] cmd = new byte[1024];
        int var7 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var7++] = 107;
        cmd[var7++] = (byte)m;

        int i;
        for(i = 0; i < num; ++i) {
            if (data.charAt(i) > 127 || data.charAt(i) < ' ') {
                return;
            }
        }

        if (num <= 30) {
            for(i = 0; i < num; ++i) {
                cmd[var7++] = (byte)data.charAt(i);
            }

            this.BT_Write(cmd);
        }
    }

    public void Code128_B(String data) {
        int m = 73;
        int num = data.length();
        int transNum = 0;
        int mIndex = 0;
        byte[] cmd = new byte[1024];
        int var11 = mIndex + 1;
        cmd[mIndex] = 29;
        cmd[var11++] = 107;
        cmd[var11++] = (byte)m;
        int Code128C = var11++;
        cmd[var11++] = 123;
        cmd[var11++] = 66;

        int checkcodeID;
        for(checkcodeID = 0; checkcodeID < num; ++checkcodeID) {
            if (data.charAt(checkcodeID) > 127 || data.charAt(checkcodeID) < ' ') {
                return;
            }
        }

        if (num <= 30) {
            for(checkcodeID = 0; checkcodeID < num; ++checkcodeID) {
                cmd[var11++] = (byte)data.charAt(checkcodeID);
                if (data.charAt(checkcodeID) == '{') {
                    cmd[var11++] = (byte)data.charAt(checkcodeID);
                    ++transNum;
                }
            }

            checkcodeID = 104;
            int n = 1;

            for(int i = 0; i < num; ++i) {
                checkcodeID += n++ * (data.charAt(i) - 32);
            }

            checkcodeID %= 103;
            if (checkcodeID >= 0 && checkcodeID <= 95) {
                cmd[var11++] = (byte)(checkcodeID + 32);
                cmd[Code128C] = (byte)(num + 3 + transNum);
            } else if (checkcodeID == 96) {
                cmd[var11++] = 123;
                cmd[var11++] = 51;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if (checkcodeID == 97) {
                cmd[var11++] = 123;
                cmd[var11++] = 50;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if (checkcodeID == 98) {
                cmd[var11++] = 123;
                cmd[var11++] = 83;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if (checkcodeID == 99) {
                cmd[var11++] = 123;
                cmd[var11++] = 67;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if (checkcodeID == 100) {
                cmd[var11++] = 123;
                cmd[var11++] = 52;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if (checkcodeID == 101) {
                cmd[var11++] = 123;
                cmd[var11++] = 65;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            } else if (checkcodeID == 102) {
                cmd[var11++] = 123;
                cmd[var11++] = 49;
                cmd[Code128C] = (byte)(num + 4 + transNum);
            }

            this.BT_Write(cmd);
        }
    }


    public void printString(String str) {
        try {
            this.BT_Write(str.getBytes("GBK"));
            this.BT_Write(new byte[]{10});
        } catch (IOException var3) {
            var3.printStackTrace();
        }

    }

    public void printParameterSet(byte[] buf) {
        this.BT_Write(buf);
    }

    public void printByteData(byte[] buf) {
        this.BT_Write(buf);
        this.BT_Write(new byte[]{10});
    }

    public void printImage(Bitmap bitmap,int reqWidth) {
        Bitmap newBm = BitmapConvertUtil.decodeSampledBitmapFromBitmap(bitmap, reqWidth);
        byte xL = (byte)(((newBm.getWidth() - 1) / 8 + 1) % 256);
        byte xH = (byte)(((newBm.getWidth() - 1) / 8 + 1) / 256);
        byte yL = (byte)(newBm.getHeight() % 256);
        byte yH = (byte)(newBm.getHeight() / 256);
        Log.d("HsBluetoothPrintDriver", "xL = " + xL);
        Log.d("HsBluetoothPrintDriver", "xH = " + xH);
        Log.d("HsBluetoothPrintDriver", "yL = " + yL);
        Log.d("HsBluetoothPrintDriver", "yH = " + yH);
        byte[] pixels = BitmapConvertUtil.convert(newBm);
        this.BT_Write(new byte[]{29, 118, 48, 0, xL, xH, yL, yH});
        this.BT_Write(pixels);
        this.BT_Write(new byte[]{10});
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = HsBluetoothPrintDriver.this.mAdapter.listenUsingRfcommWithServiceRecord("HsBluetoothPrintDriver", HsBluetoothPrintDriver.this.MY_UUID);
            } catch (IOException var4) {
                Log.e("HsBluetoothPrintDriver", "listen() failed", var4);
            }

            this.mmServerSocket = tmp;
        }

        public void run() {
            Log.d("HsBluetoothPrintDriver", "BEGIN mAcceptThread" + this);
            this.setName("AcceptThread");
            BluetoothSocket socket = null;

            while(HsBluetoothPrintDriver.this.mState != 3) {
                try {
                    socket = this.mmServerSocket.accept();
                } catch (IOException var6) {
                    Log.e("HsBluetoothPrintDriver", "accept() failed", var6);
                    break;
                }

                if (socket != null) {
                    synchronized(HsBluetoothPrintDriver.this) {
                        switch(HsBluetoothPrintDriver.this.mState) {
                            case 0:
                            case 3:
                                try {
                                    socket.close();
                                } catch (IOException var4) {
                                    Log.e("HsBluetoothPrintDriver", "Could not close unwanted socket", var4);
                                }
                                break;
                            case 1:
                            case 2:
                                HsBluetoothPrintDriver.this.connected(socket, socket.getRemoteDevice());
                        }
                    }
                }
            }

            Log.i("HsBluetoothPrintDriver", "END mAcceptThread");
        }

        public void cancel() {
            Log.d("HsBluetoothPrintDriver", "cancel " + this);

            try {
                this.mmServerSocket.close();
            } catch (IOException var2) {
                Log.e("HsBluetoothPrintDriver", "close() of server failed", var2);
            }

        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(HsBluetoothPrintDriver.this.MY_UUID);
            } catch (IOException var5) {
                Log.e("HsBluetoothPrintDriver", "create() failed", var5);
            }

            this.mmSocket = tmp;
        }

        public void run() {
            Log.i("HsBluetoothPrintDriver", "BEGIN mConnectThread");
            this.setName("ConnectThread");
            HsBluetoothPrintDriver.this.mAdapter.cancelDiscovery();

            try {
                this.mmSocket.connect();
            } catch (IOException var5) {
                HsBluetoothPrintDriver.this.sendMessageToMainThread(33);

                try {
                    this.mmSocket.close();
                } catch (IOException var3) {
                    Log.e("HsBluetoothPrintDriver", "unable to close() socket during connection failure", var3);
                }

                HsBluetoothPrintDriver.this.start();
                return;
            }

            synchronized(HsBluetoothPrintDriver.this) {
                HsBluetoothPrintDriver.this.mConnectThread = null;
            }

            HsBluetoothPrintDriver.this.connected(this.mmSocket, this.mmDevice);
        }

        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException var2) {
                Log.e("HsBluetoothPrintDriver", "close() of connect socket failed", var2);
            }

        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d("HsBluetoothPrintDriver", "create ConnectedThread");
            this.mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException var6) {
                Log.e("HsBluetoothPrintDriver", "temp sockets not created", var6);
            }

            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;
        }

        public void run() {
            Log.i("HsBluetoothPrintDriver", "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];

            while(true) {
                try {
                    while(true) {
                        if (this.mmInStream.available() != 0) {
                            for(int i = 0; i < 3; ++i) {
                                buffer[i] = (byte)this.mmInStream.read();
                            }
                        }
                    }
                } catch (IOException var3) {
                    Log.e("HsBluetoothPrintDriver", "disconnected", var3);
                    return;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                this.mmOutStream.write(buffer);
            } catch (IOException var3) {
                Log.e("HsBluetoothPrintDriver", "Exception during write", var3);
            }

        }

        public void write(byte[] buffer, int dataLen) {
            try {
                for(int i = 0; i < dataLen; ++i) {
                    this.mmOutStream.write(buffer[i]);
                }
            } catch (IOException var4) {
                Log.e("HsBluetoothPrintDriver", "Exception during write", var4);
            }

        }

        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException var2) {
                Log.e("HsBluetoothPrintDriver", "close() of connect socket failed", var2);
            }

        }
    }
}

