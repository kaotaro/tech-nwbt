package jp.androidopentextbook.wifi;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private ListView list;
	private ArrayAdapter<String> adapter;
	private Button discover_button;
	private Button con_button;
	private Button status_button;
	private Button setinfo_button;
	private BroadcastReceiver mReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		list = (ListView) findViewById(R.id.listView1);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_single_choice);
		list.setAdapter(adapter);

		discover_button = (Button) findViewById(R.id.apdiscover_button);
		con_button = (Button) findViewById(R.id.con_button);
		setinfo_button = (Button) findViewById(R.id.setinfo_button);
		status_button = (Button) findViewById(R.id.status_button);

		createReceiver();
		setButtons("接続");

		/**
		 * Wi-Fiを有効にしてWi-Fiネットワークをスキャンする
		 */
		discover_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				if (!wm.isWifiEnabled()) {
					Toast.makeText(getApplicationContext(), "WiFiをONにします",
							Toast.LENGTH_SHORT).show();
					wm.setWifiEnabled(true);
				}
				wm.startScan();
				Toast.makeText(getApplicationContext(), "APを探索中",
						Toast.LENGTH_SHORT).show();
				setButtons("接続");
			}
		});
		/**
		 * スキャンしたWi-Fiネットワークから特定のSSIDのWiFi機器にWEPで接続する
		 * 接続中のWi-Fi機器との接続を切断する
		 */
		con_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);

				if (con_button.getText().equals("接続")) {
					int position = list.getCheckedItemPosition();
					if (position < 0 || adapter.getCount() == 0) {
						return;
					}
					String item = (String) list.getItemAtPosition(position);
					if (!item.equals("hoge")) {//　接続したいWi-Fi機器のSSID
						Toast.makeText(getApplicationContext(),
								"指定のSSIDではありません", Toast.LENGTH_SHORT).show();
						return;
					}

					Toast.makeText(getApplicationContext(), item + "に接続します",
							Toast.LENGTH_SHORT).show();
					WifiConfiguration wc = new WifiConfiguration();
					wc.SSID = "\"" + item + "\"";
					wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
					wc.wepKeys[0] = "\"123456\"";//　接続したいWi-Fi機器WEPキー
					wc.wepTxKeyIndex = 0;
					wm.setWifiEnabled(true);
					int id = wm.addNetwork(wc);
					wm.saveConfiguration();
					wm.enableNetwork(id, true);

					setButtons("切断");
					if (mReceiver != null) {
						unregisterReceiver(mReceiver);
						mReceiver = null;
					}
					if (adapter != null) {
						adapter.clear();
					}

				} else {
					disconnectWiFi();
					setButtons("接続");
					createReceiver();
				}
			}
		});
		
		/**
		 * Wi-Fiネットワークの設定情報を取得する
		 */
		setinfo_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
				List<WifiConfiguration> cfgList = wm.getConfiguredNetworks();
				if (cfgList != null) {
					String[] nets = new String[cfgList.size()];
					for (int i = 0; i < cfgList.size(); i++) {
						nets[i] = String.format(
								"[Network ID]\n%4d\n[SSID]\n%s",
								cfgList.get(i).networkId, cfgList.get(i).SSID);
					}
					final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							MainActivity.this);
					alertDialogBuilder.setTitle("設定情報");
					alertDialogBuilder.setItems(nets, null);
					alertDialogBuilder.setCancelable(true);
					alertDialogBuilder.create().show();

				}
			}
		});
		/**
		 * Wi-Fi接続の接続状況を取得する
		 */
		status_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
				WifiInfo info = wm.getConnectionInfo();
				String[] _info = new String[6];
				_info[0] = "SSID : " + info.getSSID();
				int ipAdr = info.getIpAddress();
				_info[1] = String.format("IP Adrress : %02d.%02d.%02d.%02d",
						(ipAdr >> 0) & 0xff, (ipAdr >> 8) & 0xff,
						(ipAdr >> 16) & 0xff, (ipAdr >> 24) & 0xff);
				_info[2] = "MAC : " + info.getMacAddress();
				int level = WifiManager.calculateSignalLevel(info.getRssi(), 5);
				_info[3] = "RSSI : " + info.getRssi() + " LEVEL : " + level;
				_info[4] = "BSSID : " + info.getBSSID();

				int wifiState = wm.getWifiState();
				String status = "none";
				switch (wifiState) {

				case WifiManager.WIFI_STATE_DISABLING:
					status = "WIFI_STATE_DISABLING";
					break;
				case WifiManager.WIFI_STATE_DISABLED:
					status = "WIFI_STATE_DISABLED";
					break;
				case WifiManager.WIFI_STATE_ENABLING:
					status = "WIFI_STATE_ENABLING";
					break;
				case WifiManager.WIFI_STATE_ENABLED:
					status = "WIFI_STATE_ENABLED";
					break;
				case WifiManager.WIFI_STATE_UNKNOWN:
					status = "WIFI_STATE_UNKNOWN";
					break;
				}
				_info[5] = "STATUS : " + status;

				final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						MainActivity.this);
				alertDialogBuilder.setTitle("接続情報");
				alertDialogBuilder.setItems(_info, null);
				alertDialogBuilder.setCancelable(true);
				alertDialogBuilder.create().show();

			}
		});

	}

	/**
	 * 切断
	 */
	public void disconnectWiFi() {
			WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			wm.disconnect();
			wm.setWifiEnabled(false);
			if (adapter != null) {
				adapter.clear();
			}
			if (mReceiver != null) {
				unregisterReceiver(mReceiver);
				mReceiver = null;
			}
		}
	
	/**
	 * スキャン結果のブロードキャストのレシーバ
	 */
	private void createReceiver() {
		mReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
					WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
					List<ScanResult> list = wm.getScanResults();
					for (ScanResult result : list) {
						String ssid = result.SSID.toString();
						adapter.add(ssid);
						adapter.notifyDataSetChanged();
					}
				}
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(mReceiver, filter);
	}

	@Override
	public void onPause() {
		disconnectWiFi();
		super.onStop();
	}

	private void setButtons(String cText) {
		con_button.setText(cText);
	}

}
