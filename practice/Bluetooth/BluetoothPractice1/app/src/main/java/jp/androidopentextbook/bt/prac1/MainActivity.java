package jp.androidopentextbook.bt.prac1;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

//## 演習5 ## ListViewのアイテムクリックイベントを取得できるようにする
// TODO (5-1)OnItemClickListenerをimplementsして
public class MainActivity extends Activity {

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

	// TODO (en2-3)外部機器探索中に発見した場合に受け取るACTIONインテント
	// を受け取るためのブロードキャストレシーバー
	
	
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

		// TODO (en2-4)外部機器探索状態のACTIONインテントを受け取るためのIntentFilter生成

		// TODO (en2-5)IntentFilterとレシーバーをシステムに登録する

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

		// TODO ## 演習5 ## 外部機器探索結果のリストから端末名を選ぶと端末情報を表示する
		// OnItemClickListenerをimplementsしてListViewにセット

	}

	/**
	 * ## 演習1 ## Bluetooth機能を有効にする
	 */
	private void initEnableButton() {
		enableButton = (Button) findViewById(R.id.enable_button);
		enableButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				// TODO (en1-1)BluetoothAdapterのインスタンスを取り出す
				
				// (en1-2)取り出したインスタンスの内容によって処理を分ける
				// TODO ①Bluetooth未サポートの場合はその旨をToastで表示
				
				
				// TODO ②Bluetoothが既に有効な場合はその旨をToastで表示
				// TODO ③Bluetoothが無効な場合は有効にする
				
				
				return;
			}
		});
	}

	/**
	 * ## 演習2 ## Bluetooth対応の外部機器の探索
	 */
	private void initDiscoverButton() {
		discoverButton = (Button) findViewById(R.id.discover_button);
		discoverButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// TODO (en2-1)BluetoothAdapterを取得し、もう既に探索中だったら一度キャンセルする
				
				// TODO (en2-2)外部機器探索開始
				
				
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
				// TODO (en3-1) ペアリング済みの端末情報一覧を取得する
				
				// TODO (en3-2) リスト表示するためにアダプタにセットする
				
			}
		});
	}

	/**
	 * ## 演習4 ## 外部機器から自端末を発見可能にする
	 */
	private void initResButton() {
		resButton = (Button) findViewById(R.id.res_button);
		resButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				if (btAdapter == null) {
					btAdapter = BluetoothAdapter.getDefaultAdapter();
				}

				if (btAdapter.isEnabled()) {
					// TODO (en4-1) 外部機器から自端末を発見可能にするためのアクションインテントを発行
					
					
					// TODO (en4-2) onActivityResultメソッドで結果を受けとるようにする
			
				
				} else {
					// Bluetoothが無効の場合はその旨をToastで通知
					Toast.makeText(getApplicationContext(),
							"Bluetoothが有効になっていません", Toast.LENGTH_SHORT).show();
					return;
				}
				

			}
		});
	}
	
	// ## 演習1 ## Bluetooth機能を有効にする
	// TODO (en1-3)onActivityResultコールバックメソッドの実装
	// TODO onActivityResultコールバックメソッド内で(en1-2)-③の応答処理
	// ## 演習4 ## 外部機器から自端末を発見可能にする
	// TODO onActivityResultコールバックメソッド内で(en4-2)の応答処理
	
	
	
	
	// ## 演習5 ## 外部機器探索結果のリストから端末名を選ぶと端末情報を表示する
	// TODO (5-2)OnItemClickListenerのonItemClickコールバックメソッドをオーバーライドする
	// TODO (5-3)クリックされた端末名の端末情報(端末名とMACアドレス)をトーストに表示する
	
}
