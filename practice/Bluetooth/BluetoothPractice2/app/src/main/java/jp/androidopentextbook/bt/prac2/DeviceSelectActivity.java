package jp.androidopentextbook.bt.prac2;

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

public class DeviceSelectActivity extends Activity implements
		OnItemClickListener {
	
	private static final String TAG = "DeviceSelectActivity";
	
	private BluetoothAdapter btAdapter;
	private Button enableButton;
	private Button discoverButton;
	private Button boundedButton;
	private Button resButton;

	private List<BluetoothDevice> newDeviceList = new ArrayList<BluetoothDevice>();
	private ListView deviceList;
	private ArrayAdapter<String> adapter;

	private ListView boundedList;
	private Set<BluetoothDevice> boundedDevices;
	private ArrayAdapter<String> adapter2;

	public static final int REQUEST_BT_ENABLE = 0;
	public static final int REQUEST_BT_DISCOVERABLE = 1;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				newDeviceList.add(device);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				btAdapter.cancelDiscovery();
				Toast.makeText(getApplicationContext(), "外部機器を探索開始終了しました",
						Toast.LENGTH_SHORT).show();
				adapter.clear();
				if (newDeviceList.size() == 0) {
					adapter.add("デバイスがありません");
				}
				for (BluetoothDevice device : newDeviceList) {
					String name = device.getName();
					Log.i(TAG, "デバイス名 = " + name);
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

		deviceList = (ListView) findViewById(R.id.device_list);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		deviceList.setAdapter(adapter);
		deviceList.setOnItemClickListener(this);
		

		boundedList = (ListView) findViewById(R.id.bounded_list);
		adapter2 = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		boundedList.setAdapter(adapter2);
		boundedList.setOnItemClickListener(this);

		initEnableButton();
		initDiscoverButton();
		initBoundedButton();
		initResButton();

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (btAdapter != null) {
			btAdapter.cancelDiscovery();
		}
	}

	private void initEnableButton() {
		enableButton = (Button) findViewById(R.id.enable_button);
		enableButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				btAdapter = BluetoothAdapter.getDefaultAdapter();
				if (btAdapter == null) {
					Toast.makeText(getApplicationContext(),
							"この端末はBluetoothがサポートされていません。", Toast.LENGTH_SHORT)
							.show();
					return;
				}
				if (btAdapter.isEnabled()) {
					Toast.makeText(getApplicationContext(),
							"Bluetoothは既に有効になっています。", Toast.LENGTH_SHORT)
							.show();
				} else {
					Intent intent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(intent, REQUEST_BT_ENABLE);
				}
				return;
			}
		});
	}

	private void initDiscoverButton() {
		discoverButton = (Button) findViewById(R.id.discover_button);
		discoverButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (btAdapter == null) {
					btAdapter = BluetoothAdapter.getDefaultAdapter();
				}
				if (btAdapter.isDiscovering()) {
					btAdapter.cancelDiscovery();
				}
				btAdapter.startDiscovery();
			}
		});
	}

	private void initBoundedButton() {
		boundedButton = (Button) findViewById(R.id.bounded_button);
		boundedButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (btAdapter == null) {
					btAdapter = BluetoothAdapter.getDefaultAdapter();
				}
				boundedDevices = btAdapter.getBondedDevices();

				if (boundedDevices == null) {
					return;
				}

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

	private void initResButton() {
		resButton = (Button) findViewById(R.id.res_button);
		resButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (btAdapter == null) {
					btAdapter = BluetoothAdapter.getDefaultAdapter();
				}
				if (btAdapter.isEnabled()) {
					Intent intent = new Intent(
							BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

					startActivityForResult(intent, REQUEST_BT_DISCOVERABLE);
				} else {
					Toast.makeText(getApplicationContext(),
							"Bluetoothが有効になっていません", Toast.LENGTH_SHORT).show();
					return;
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int reqestCode, int resultCode, Intent data) {
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		ListView listView = (ListView) parent;
		String deviceName = (String) listView.getAdapter().getItem(position);
		if (listView.getId() == R.id.device_list) {
			for (BluetoothDevice device : newDeviceList) {
				if (device.getName().equals(deviceName)) {
					Toast.makeText(
							getApplicationContext(),
							"端末名 = " + device.getName() + " \nMAC ADDRESS = "
									+ device.getAddress(), Toast.LENGTH_SHORT)
							.show();

					Intent intent = new Intent();
					intent.putExtra("device_adress", device.getAddress());
					setResult(Activity.RESULT_OK, intent);
					finish();

					break;
				}
			}
		} else if (listView.getId() == R.id.bounded_list) {
			for (BluetoothDevice device : boundedDevices) {
				if (device.getName().equals(deviceName)) {
					Toast.makeText(
							getApplicationContext(),
							"端末名 = " + device.getName() + " \nMAC ADDRESS = "
									+ device.getAddress(), Toast.LENGTH_SHORT)
							.show();

					Intent intent = new Intent();
					intent.putExtra("device_adress", device.getAddress());
					setResult(Activity.RESULT_OK, intent);
					finish();

					break;
				}
			}
		}
	}
}
