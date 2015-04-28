package jp.androidopentextbook.bt.prac2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class BTMessagingThreads {

	private static final String TAG = "BTMessagingThreads";

	private static final UUID SPP_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState; //現在の接続状態

	// 現在の接続状態を示す定数
	public static final int STATE_NONE = 0; // 未接続
	public static final int STATE_LISTEN = 1; // 接続要求待ち
	public static final int STATE_CONNECTING = 2; // 接続要求発行（処理中）
	public static final int STATE_CONNECTED = 3; // リモートデバイスに接続済み

	public BTMessagingThreads(Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}

	/**
	 * 最新の接続状態の保持
	 */
	private synchronized void setState(int state) {
		Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;
		// 最新の接続状態をUIスレッドに更新
		mHandler.obtainMessage(MessagingActivity.MESSAGE_STATE_CHANGE, state,
				-1).sendToTarget();
	}

	/**
	 * 最新の接続状態取得
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * AcceptThreadをスタートさせる
	 * 自端末をサーバーとして接続要求待ち状態にしておく
	 */
	public synchronized void start() {
		Log.d(TAG, "start");

		// 接続処理用スレッドはキャンセル
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// 接続済みでデータ送受信用のスレッドはキャンセル
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		setState(STATE_LISTEN);

		// BluetoothServerSocketによる接続要求待ちスレッドを開始
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}

	}

	/**
	 * ConnectThreadを開始するためのメソッド
	 * （リモートデバイスとの接続処理を行うスレッド）
	 * 	 * @param device
	 *            接続要求を発行する機器（接続先）のBluetoothDeviceオブジェクト
	 */
	public synchronized void connect(BluetoothDevice device) {
		Log.d(TAG, "connect to: " + device);

		// 接続処理中である場合、その処理を行っているConnectThreadスレッドをキャンセル
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// 現在接続済のスレッドがある場合はキャンセル
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// 指定された機器に接続するConnectThreadを開始
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * ConnectedThreadを開始するためのメソッド
	 * （リモートデバイスとデータの送受信を行うスレッド）
	 * @param socket
	 *            接続が確立されたBluetoothSocketオブジェクト
	 * @param device
	 *            接続済みの機器のBluetoothDeviceオブジェクト
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		Log.d(TAG, "connected");

		// 接続が完了したConnectThreadをキャンセルして終了させる
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// 既に動いているConnectedThreadがあればキャンセル
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// 接続が確立したのでAcceptThreadはキャンセルで終了させる
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
	

		// ConnectedThreadをスタートしてデータの送受信を行う
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// UIスレッドに接続済みのデバイス名を送る
		Message msg = mHandler
				.obtainMessage(MessagingActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(MessagingActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}

	/**
	 * すべてのスレッドの停止
	 */
	public synchronized void stop() {
		Log.d(TAG, "stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		setState(STATE_NONE);
	}

	/**
	 * ConnectedThreadに対してUIスレッドから受け取った文字列（ユーザが入力した文字）
	 * を非同期に書き込む
	 * @param out
	 *            書き込むデータ（byte）
	 * @see jp.androidopentextbook.bt.prac2.BTMessagingThreads.ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		ConnectedThread r;
		// ConnectedThreadのテンポラリーオブジェクトをロックする
		synchronized (this) {
			if (mState != STATE_CONNECTED){
				return;	
			}
			r = mConnectedThread;
		}
		// データの書き込み実行
		r.write(out);
	}

	/**
	 * 接続エラー時の処理
	 */
	private void connectionFailed() {
		// UIスレッドにエラーメッセージを送る
		Message msg = mHandler.obtainMessage(MessagingActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MessagingActivity.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		// BTMessagingThreadsを再スタートして接続要求待ち状態（サーバーモード）にする
		BTMessagingThreads.this.start();
	}

	/**
	 * 接続がLOSTした場合の処理
	 */
	private void connectionLost() {
		// UIスレッドにメッセージを送る
		Message msg = mHandler.obtainMessage(MessagingActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MessagingActivity.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		// BTMessagingThreadsを再スタートして接続要求待ち状態（サーバーモード）にする
		BTMessagingThreads.this.start();
	}

	/**
	 * 接続要求待ち用（サーバーモード）のスレッド
	 * 接続が確率された場合は、このスレッドは終了
	 */
	private class AcceptThread extends Thread {

		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// BluetoothServerSocketを生成
			try {
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(
						"BTMessaging", SPP_UUID);
			} catch (IOException e) {
				Log.e(TAG, "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			Log.d(TAG, "BEGIN mAcceptThread" + this);
			setName("AcceptThread");

			BluetoothSocket socket = null;

			// 未接続の間はずっとサーバーモードで待機
			while (mState != STATE_CONNECTED) {
				try {
					// クライアントから接続要求があり正常に処理ができた場合のみ
					// 戻り値が返ってくる
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "accept() failed", e);
					break;
				}

				// 直前の接続状態により、この先の処理がかわる
				if (socket != null) {
					synchronized (BTMessagingThreads.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// 接続要求待ち状態または接続処理中の場合は
							// ConnectedThreadを開始する
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// 未接続の場合、または、既に接続済みの場合は状態として正しくないので
							// BluetoothSocketは閉じて再び接続要求待ち状態でいる
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Could not close unwanted socket", e);
							}
							break;
						}
					}
				}
			}
			Log.i(TAG, "END mAcceptThread");

		}
		
		/**
		 * AcceptThreadスレッドの停止
		 */
		public void cancel() {
			Log.d(TAG, "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of server failed", e);
			}
		}
	}

	/**
	 * 接続処理を行うためのスレッド
	 * 自端末はクライアントとして、外部端末（サーバー）に接続要求を発行する
	 * 
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// 指定のBluetoothDeviceオブジェクト（接続したいデバイス）で
			// SPP接続のためのBluetoothSocketを取得する
			try {
				tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);

			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// 外部機器探索機能は停止させる
			mAdapter.cancelDiscovery();

			// BluetoothSocketに接続する
			try {
				// 接続成功の場合のみconnectメソッドから戻ってくる
				// エラーの場合はExceptionが投げられる
				mmSocket.connect();
			} catch (IOException e) {
				// 接続エラーの場合はソケットを閉じる
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() "
							+ " socket during connection failure", e2);
				}
				// 接続エラー時の処理
				connectionFailed();
				return;
			}

			// 接続が完了したのでConnectThreadを初期化する
			synchronized (BTMessagingThreads.this) {
				mConnectThread = null;
			}

			// ConnectedThreadを開始
			connected(mmSocket, mmDevice);
		}

		/**
		 * ConnectThreadスレッドの停止
		 */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect " + " socket failed", e);
			}
		}
	}

	/**
	 * 接続済みのリモートデバイスとデータの送受信を行うスレッド
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// BluetoothSocketからInputStreamとOutputStreamsを取得
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;

			// 接続中の間、接続先のデバイスから送られてくるデータの受信待ち
			while (true) {
				try {
					// InputStreamからデータを読み出す
					bytes = mmInStream.read(buffer);

					// UIスレッドにバイトデータのまま送る
					mHandler.obtainMessage(MessagingActivity.MESSAGE_READ,
							bytes, -1, buffer).sendToTarget();
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					//接続がLostした場合の処理
					connectionLost();
					// BTMessagingThreadsを再スタートして接続要求待ち状態（サーバーモード）にする
					BTMessagingThreads.this.start();
					break;
				}
			}
		}

		/**
		 * OutStreamにデータを書き込む（接続先デバイスにデータを送信）
		 * 
		 * @param buffer
		 *            書き込む文字列のバイトデータ
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);

				// 書き込んだデータをUIスレッドに戻して共有する
				mHandler.obtainMessage(MessagingActivity.MESSAGE_WRITE, -1, -1,
						buffer).sendToTarget();
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		/**
		 * ConnectedThreadスレッドの停止
		 */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
