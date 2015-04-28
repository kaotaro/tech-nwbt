package jp.androidopentextbook.bt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

//## 演習5 ## ListViewのアイテムクリックイベントを取得できるようにする
//(5-1)OnItemClickListenerをimplementsする
public class MainActivity extends Activity implements OnItemClickListener {

	// ## 演習1 ## Bluetooth機能を有効にする
	private BluetoothAdapter btAdapter;
	private Button enableButton;
	public static final int REQUEST_BT_ENABLE = 0;

	// ## 演習2 ## Bluetooth対応の外部機器の探索
	private Button discoverButton;
	private List<BluetoothDevice> newDeviceList = new ArrayList<BluetoothDevice>();
	private ListView deviceList;
	private ArrayAdapter<String> adapter;

	// ## 演習3 ## ペアリング済みの外部機器情報取得
	private Button boundedButton;
	private ListView boundedList;
	private ArrayAdapter<String> adapter2;

	// ## 演習4 ## 外部機器から自端末を発見可能にする
	private Button resButton;
	public static final int REQUEST_BT_DISCOVERABLE = 1;


	// (en2-3)外部機器探索中に発見した場合に受け取るACTIONインテント
	// を受け取るためのブロードキャストレシーバー
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {

			// 受け取ったアクションを取り出す
			String action = intent.getAction();

			// ①ACTION_FOUND：デバイスが発見された場合
			// BluetoothDeviceオブジェクトを取り出してListに格納する
			
			// ②ACTION_DISCOVERY_FINISHED：デバイス検索が終了した場合
			// デバイス探索をキャンセルして終了通知をToastで表示する
			// Listに格納してあるBluetoothDeviceオブジェクトからデバイスの名前を取り出して
			// ListViewにセットしてあるアダプタに追加し、リスト表示を更新する
			// レシーバーをシステムから登録解除する
			
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				
				// BluetoothDeviceオブジェクトを取り出してListに格納する
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				newDeviceList.add(device);

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				
				// デバイス探索をキャンセルして終了通知をToastで表示する
				btAdapter.cancelDiscovery();
				Toast.makeText(getApplicationContext(), "外部機器を探索開始終了しました",
						Toast.LENGTH_SHORT).show();

				// Listに格納してあるBluetoothDeviceオブジェクトからデバイスの名前を取り出して
				// ListViewにセットしてあるアダプタに追加し、リスト表示を更新する
				adapter.clear();
				if (newDeviceList.size() == 0) {
					adapter.add("デバイスがありません");
				}
				for (BluetoothDevice device : newDeviceList) {
					String name = device.getName();
					Log.i("MainActivity", "デバイス名 = " + name);
					adapter.add(name);
				}
				adapter.notifyDataSetChanged();

				// レシーバーをシステムから登録解除する
				if (mReceiver != null) {
					unregisterReceiver(mReceiver);
					mReceiver = null;
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ## 演習1 ## Bluetooth機能を有効にする
		initEnableButton();

		// ## 演習2 ## Bluetooth対応の外部機器の探索
		// 検索して発見された外部機器の名前を表示するListViewとアダプタの準備
		deviceList = (ListView) findViewById(R.id.device_list);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		deviceList.setAdapter(adapter);
		
		initDiscoverButton();

		// (en2-4)外部機器探索状態のACTIONインテントを受け取るためのIntentFilter生成
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);

		// (en2-5)IntentFilterとレシーバーをシステムに登録する
		registerReceiver(mReceiver, filter);

		// ## 演習3 ## ペアリング済みの外部機器情報取得
		// ペアリング済みのの外部機器情報取得した結果（ペアリング済みの端末名）を
		// 表示するListViewとアダプタの準備
		boundedList = (ListView) findViewById(R.id.bounded_list);
		adapter2 = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		boundedList.setAdapter(adapter2);
		
		initBoundedButton();


		// ## 演習4 ## 外部機器から自端末を発見可能にする
		initResButton();

		// ## 演習5 ## 外部機器探索結果のリストから端末名を選ぶと端末情報を表示する
		// OnItemClickListenerをimplementsしてListViewにセット
		deviceList.setOnItemClickListener(this);

	}

	/**
	 * ## 演習1 ## Bluetooth機能を有効にする
	 */
	private void initEnableButton() {
		enableButton = (Button) findViewById(R.id.enable_button);
		enableButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				// (en1-1)BluetoothAdapterのインスタンスを取り出す
				btAdapter = BluetoothAdapter.getDefaultAdapter();

				// (en1-2)取り出したインスタンスの内容によって処理を分ける
				// ①Bluetooth未サポートの場合はその旨をToastで表示
				if (btAdapter == null) {
					Toast.makeText(getApplicationContext(),
							"この端末はBluetoothがサポートされていません。", Toast.LENGTH_SHORT)
							.show();
					return;
				}

				// ②Bluetoothが既に有効な場合はその旨をToastで表示
				// ③Bluetoothが無効な場合は有効にする
				if (btAdapter.isEnabled()) {
					Toast.makeText(getApplicationContext(),
							"Bluetoothは既に有効になっています。", Toast.LENGTH_SHORT)
							.show();
				} else {
					// Bluetooth機能を有効にするためのアクションインテントを発行
					Intent intent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_ENABLE);
					// onActivityResultメソッドで結果を受けとるようにする
					startActivityForResult(intent, REQUEST_BT_ENABLE);
				}
				return;
			}
		});
	}
	
	// ## 演習1 ## Bluetooth機能を有効にする
	// (en1-3)onActivityResultコールバックメソッドの実装
	@Override
	protected void onActivityResult(int reqestCode, int resultCode, Intent data) {

		// (en1-2)-③の応答処理
		if (reqestCode == REQUEST_BT_ENABLE) {
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(getApplicationContext(), "Bluetoothを有効にしました。",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), "Bluetooth有効に失敗しました。",
						Toast.LENGTH_SHORT).show();
			}
			return;
		}
		// ## 演習4 ## 外部機器から自端末を発見可能にする
		// (en4-2)の応答処理
		if (reqestCode == REQUEST_BT_DISCOVERABLE) {
			if (resultCode == Activity.RESULT_CANCELED) {
				Toast.makeText(getApplicationContext(), "端末は探索可能な状態ではありません",
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(),
						resultCode + "秒間外部機器から探索可能状態です", Toast.LENGTH_SHORT)
						.show();

			}
		}
	}

	/**
	 * ## 演習2 ## Bluetooth対応の外部機器の探索
	 */
	private void initDiscoverButton() {
		discoverButton = (Button) findViewById(R.id.discover_button);
		discoverButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				// (en2-1)BluetoothAdapterを取得し、もう既に探索中だったら一度キャンセルする
				if (btAdapter == null) {
					btAdapter = BluetoothAdapter.getDefaultAdapter();
				}
				if (btAdapter.isDiscovering()) {
					btAdapter.cancelDiscovery();
				}

				// (en2-2)外部機器探索開始
				btAdapter.startDiscovery();
			}
		});
	}

	/**
	 * ## 演習3 ## ペアリング済みの外部機器情報取得
	 */
	private void initBoundedButton() {
		boundedButton = (Button) findViewById(R.id.bounded_button);
		boundedButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				if (btAdapter == null) {
					btAdapter = BluetoothAdapter.getDefaultAdapter();
				}

				// (en3-1) ペアリング済みの端末情報一覧を取得する
				Set<BluetoothDevice> boundedDevices = btAdapter
						.getBondedDevices();

				// (en3-2) リスト表示するためにアダプタにセットする
				adapter2.clear();
				if (boundedDevices.size() == 0) {
					adapter2.add("ペアリング済み端末なし");
				}
				for (BluetoothDevice device : boundedDevices) {
					adapter2.add(device.getName());
				}
				adapter2.notifyDataSetChanged();
			}
		});
	}

	/**
	 *  ## 演習4 ## 外部機器から自端末を発見可能にする
	 */
	private void initResButton() {
		resButton = (Button) findViewById(R.id.res_button);
		resButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				if (btAdapter == null) {
					btAdapter = BluetoothAdapter.getDefaultAdapter();
				}

				if (btAdapter.isEnabled()) {
					// (en4-1) 外部機器から自端末を発見可能にするためのアクションインテントを発行
					Intent intent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
					
					// (en4-2) onActivityResultメソッドで結果を受けとるようにする
					startActivityForResult(intent, REQUEST_BT_DISCOVERABLE);
				
				} else {
					// Bluetoothが無効の場合はその旨をToastで通知
					Toast.makeText(getApplicationContext(),
							"Bluetoothが有効になっていません", Toast.LENGTH_SHORT).show();
					return;
				}
			}
		});
	}

	// ## 演習5 ## 外部機器探索結果のリストから端末名を選ぶと端末情報を表示する
	// (5-2)OnItemClickListenerのonItemClickコールバックメソッドをオーバーライドする
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		
		// (5-3)クリックされた端末名の端末情報(端末名とMACアドレス)をトーストに表示する
		ListView listView = (ListView) parent;
		String deviceName = (String) listView.getAdapter().getItem(position);
		for (BluetoothDevice device : newDeviceList) {
			if (device.getName().equals(deviceName)) {
				Toast.makeText(getApplicationContext(),
						"端末名 = " + device.getName() + 
						" \nMAC ADDRESS = " + device.getAddress(), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	}
}
