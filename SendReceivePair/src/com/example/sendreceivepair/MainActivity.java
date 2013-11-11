package com.example.sendreceivepair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	TextView myLabel;
	EditText myTextbox;
	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
	BluetoothDevice mmDevice;
	OutputStream mmOutputStream;
	InputStream mmInputStream;
	Thread workerThread;
	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mmDevice = null;
		Button openButton = (Button)findViewById(R.id.open);
		Button sendButton = (Button)findViewById(R.id.send);
		Button closeButton = (Button)findViewById(R.id.close);
		myLabel = (TextView)findViewById(R.id.label);
		myTextbox = (EditText)findViewById(R.id.entry);

		//Open Button
		openButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					findBT();
					openBT();
				}
				catch (IOException ex) { }
			}
		});

		//Send Button
		sendButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					sendData();
				}
				catch (IOException ex) { }
			}
		});

		//Close button
		closeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					closeBT();
				}
				catch (IOException ex) { }
			}
		});

	}

	void findBT() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null) {
			myLabel.setText("No bluetooth adapter available");
		}

		if(!mBluetoothAdapter.isEnabled()) {
			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetooth, 0);
		}

		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if(pairedDevices.size() > 0) {
			for(BluetoothDevice device : pairedDevices) {
				if(device.getName().startsWith("AMAR")){ 
					//  .getAddress().equals("00:06:66:4B:44:3E"))  {
					//bluetoothDevice.getName().startsWith("AMAR")
					mmDevice = device;
					break;
				}
			}
		}
		myLabel.setText("Bluetooth Device Found");
	}

	void openBT() throws IOException {
		if(mmDevice != null){
			UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
			mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);        
			mmSocket.connect();
			mmOutputStream = mmSocket.getOutputStream();
			mmInputStream = mmSocket.getInputStream();
			beginListenForData();
			myLabel.setText("Bluetooth Opened");
		}
	}
	void beginListenForData() {
		final Handler handler = new Handler(); 
		final byte delimiter = 10; //This is the ASCII code for a newline character

		stopWorker = false;
		readBufferPosition = 0;
		readBuffer = new byte[1024];
		workerThread = new Thread(new Runnable() {
			public void run() {                
				while(!Thread.currentThread().isInterrupted() && !stopWorker) {
					try {
						int bytesAvailable = mmInputStream.available();                        
						if(bytesAvailable > 0) {
							byte[] packetBytes = new byte[bytesAvailable];
							mmInputStream.read(packetBytes);
							for(int i=0;i<bytesAvailable;i++) {
								byte b = packetBytes[i];
								if(b == delimiter) {
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
									final String data = new String(encodedBytes, "US-ASCII");
									readBufferPosition = 0;

									handler.post(new Runnable() {
										public void run() {
											myLabel.setText(data);
										}
									});
								}
								else {
									readBuffer[readBufferPosition++] = b;
								}
							}
						}
					} 
					catch (IOException ex) {
						stopWorker = true;
					}
				}
			}
		});

		workerThread.start();
	}

	void sendData() throws IOException {
		final String msg = myTextbox.getText().toString();
		Log.d("BT", "send to BT: " + msg);
		if (mmOutputStream != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Log.d("BT", "sendText(): Writing message to outputstream: " + msg);
					try {
						mmOutputStream.write(msg.getBytes());
						Log.d("BT", "sendText(): Writing message to outputstream completed.");
					} catch (IOException e) {
						Log.e("BT", "sendText(): Error: " + e);
					}
				}
			}).start();
		}
		myLabel.setText("Data Sent " + msg);
	}

	void closeBT() throws IOException {
		stopWorker = true;
		mmOutputStream.close();
		mmInputStream.close();
		mmSocket.close();
		myLabel.setText("Bluetooth Closed");
	}

	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
