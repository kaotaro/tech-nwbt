package jp.androidopentextbook.bt.prac2;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MessagingActivity extends ActionBarActivity {

	private static final String TAG = "MessagingActivity";

	private BTMessagingThreads mMessagingThreads;
	
	// BTMessagingThreadsのハンドラから送られるメッセージ種別
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	
	// BTMessagingThreadsのハンドラから送られるキーの名前
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// DeviceSelectActivityから戻ってきたときのリクエストコード
	public static final int REQUEST_DEVICE_SELECT = 100;

	// 画面のUI部品
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;
	private TextView status;

	// 接続済みのデバイス名
	private String mConnectedDeviceName;
	// メッセージ表示用のListViewのアダプタ
	private ArrayAdapter<String> mConversationArrayAdapter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_messaging);
	}

	@Override
	public void onStart() {
		super.onStart();

		if (mMessagingThreads == null) {
			// 画面の初期化
			// 接続状態を表示するTextView
			status = (TextView) findViewById(R.id.connect_status);
			// 接続された端末とやりとりをする文字列データを表示するListViewとアダプタ
			mConversationArrayAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1);
			mConversationView = (ListView) findViewById(R.id.in_list);
			mConversationView.setAdapter(mConversationArrayAdapter);

			// 接続先に送る文字列を入力するEditText
			mOutEditText = (EditText) findViewById(R.id.edit_text_out);

			// データ送信ボタン
			mSendButton = (Button) findViewById(R.id.button_send);
			mSendButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					String message = mOutEditText.getText().toString();
					sendMessage(message);
				}
			});

			// BTMessagingThreadsクラスのオブジェクト生成
			mMessagingThreads = new BTMessagingThreads(this, mHandler);

		}

	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (mMessagingThreads != null) {
			// 未接続状態のときのみBTMessagingThreadsのstartメソッドを呼び出す
			if (mMessagingThreads.getState() == BTMessagingThreads.STATE_NONE) {
				// AcceptThreadをスタートさせる
				mMessagingThreads.start();
			}
		}
	}

	private void sendMessage(String message) {
		// 未接続の場合は、相手先端末に文字列は送らない
		if (mMessagingThreads.getState() != BTMessagingThreads.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}
		// 入力文字がある場合、MessagingThreadsのwriteメソッドに文字列からバイトを取得して送る
		if (message.length() > 0) {
			byte[] send = message.getBytes();
			// ConnectedThreadのwriteメソッドに送る
			mMessagingThreads.write(send);
			mOutEditText.setText("");
		}
	}

	// MessagingThreadsから通知される情報を受け取るハンドラ
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BTMessagingThreads.STATE_CONNECTED:
					status.setText(mConnectedDeviceName
							+ getResources().getText(R.string.connected_to,
									"none"));
					mConversationArrayAdapter.clear();
					break;
				case BTMessagingThreads.STATE_CONNECTING:
					status.setText(getResources().getText(R.string.connecting, "none"));
				
					break;
				case BTMessagingThreads.STATE_LISTEN:
				case BTMessagingThreads.STATE_NONE:
					status.setText(getResources().getText(R.string.not_connected, "none"));
					break;
				}
				break;
			case MESSAGE_WRITE: // 自端末のメッセージ
				byte[] writeBuf = (byte[]) msg.obj;
				String writeMessage = new String(writeBuf);
				mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ: // 相手端末から送られてきたメッセージ
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);
				mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
						+ readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// 接続済みデバイス名を保持
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.device_select:
			// DeviceSelectActivityに遷移してデバイス探索、選択などを行う
			Intent intent = new Intent(this, DeviceSelectActivity.class);
			startActivityForResult(intent, REQUEST_DEVICE_SELECT);
			return true;
		default:
			return false;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_DEVICE_SELECT: //DeviceSelectActivityから戻った場合
			if (resultCode == Activity.RESULT_OK) {
		        // 選択した機器のMACアドレスを取得する
		        String address = data.getExtras().getString("device_adress");
		        // BluetoothDeviceオブジェクトの取得
		        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		        BluetoothDevice device = btAdapter.getRemoteDevice(address);
		        // 選択した機器への接続実行
		        mMessagingThreads.connect(device);
			}
			break;
		}
	}

    
	@Override
	public void onDestroy() {
		super.onDestroy();
		// MessagingThreadsのすべてのスレッドをstopする
		if (mMessagingThreads != null) {
			mMessagingThreads.stop();
		}
	}
    
}
