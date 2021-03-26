package bike.hackboy.bronco;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Set;

public class CbyDiscovery extends Fragment {
	final ArrayList<BluetoothDevice> matchingDevices = new ArrayList<>();
	protected RecyclerView recyclerViewDevices;
	protected DeviceListAdapter deviceListAdapter;

	private final Handler loaderThreadHandler = new Handler(Looper.getMainLooper());
	private final Runnable hideLoader = () ->
		requireView().findViewById(R.id.loader).setVisibility(View.INVISIBLE);

	private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String event = intent.getStringExtra("event");
			//Log.d("event", event);

			if ("on-discovered".equals(event)) {
				NavHostFragment.findNavController(CbyDiscovery.this)
					.navigate(R.id.action_CbyDiscovery_to_Dashboard);
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();

		LocalBroadcastManager.getInstance(requireContext())
			.registerReceiver(messageReceiver, new IntentFilter(BuildConfig.APPLICATION_ID));
	}

	@Override
	public void onPause() {
		super.onPause();

		LocalBroadcastManager.getInstance(requireContext())
			.unregisterReceiver(messageReceiver);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView =  inflater.inflate(R.layout.discovery, container, false);
		recyclerViewDevices = rootView.findViewById(R.id.items_list);

		((MainActivity) requireActivity()).getSupportActionBar().setTitle(R.string.app_name);

		return rootView;
	}

	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		deviceListAdapter = new DeviceListAdapter(requireContext(), matchingDevices);
		deviceListAdapter.setOnDeviceClickListener(new DeviceListAdapter.onDeviceClickListener() {
			@Override
			public void onClick(String mac) {
				connect(mac);
			}
		});

		recyclerViewDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerViewDevices.setAdapter(deviceListAdapter);
		recyclerViewDevices.setItemAnimator(new DefaultItemAnimator());

		view.findViewById(R.id.button_connect).setOnClickListener(view1 -> listDevices());
		listDevices();
	}

	public void listDevices() {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		requireView().findViewById(R.id.bluetooth_off).setVisibility(View.INVISIBLE);
		requireView().findViewById(R.id.no_devices).setVisibility(View.INVISIBLE);
		requireView().findViewById(R.id.items_list).setVisibility(View.INVISIBLE);

		matchingDevices.clear();

		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			requireView().findViewById(R.id.bluetooth_off).setVisibility(View.VISIBLE);
			return;
		}

		showPlaceboLoader();
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

		for(BluetoothDevice device : pairedDevices) {
			if(device.getName().equals("COWBOY")) {
				matchingDevices.add(device);
			}
		}

		if (matchingDevices.size() < 1) {
			requireView().findViewById(R.id.no_devices).setVisibility(View.VISIBLE);
			return;
		}

		requireView().findViewById(R.id.items_list).setVisibility(View.VISIBLE);
		deviceListAdapter.notifyDataSetChanged();
		//Log.d("devices", matchingDevices.toString());
	}

	public void connect(String mac) {
		//Log.d("connect", mac);

		Intent intent = new Intent(BuildConfig.APPLICATION_ID);
		intent.putExtra("event", "connect");
		intent.putExtra("mac", mac);
		LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
	}

	// listing is instant but let's give some fake feedback to the user so
	// they don't think nothing happened if the list stays the same
	public void showPlaceboLoader() {
		loaderThreadHandler.removeCallbacks(hideLoader);

		requireView().findViewById(R.id.loader).setVisibility(View.VISIBLE);
		loaderThreadHandler.postDelayed(hideLoader, (int) (Math.random()*400 + 400));
	}
}