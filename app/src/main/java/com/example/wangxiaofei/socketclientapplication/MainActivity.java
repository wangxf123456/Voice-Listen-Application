package com.example.wangxiaofei.socketclientapplication;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.net.wifi.*;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.app.ProgressDialog;

import org.w3c.dom.Text;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.*;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int RECORDER_SAMPLERATE = 4096;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_CHANNELS_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private int PORT = 3333;
    private int CHUNK_SIZE = 2048;

    private Thread listen_thread = null;

    private boolean isListening = false;
    private String ip = "";

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonHandlers();
        enableButtons(false);

    }

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnEnd)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnEnd, isRecording);
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    enableButtons(true);
                    dialog = ProgressDialog.show(MainActivity.this, "wait...", "Registering ip and wait for connection");
                    registerIPAndListen();
                    break;
                }
                case R.id.btnEnd: {
                    enableButtons(false);
                    isListening = false;
                    listen_thread = null;
                    break;
                }
            }
        }
    };

    private void registerIPAndListen() {
        isListening = true;
        listen_thread= new Thread(new Runnable() {
            public void run() {
                register_ip_thread();
            }
        }, "Listen Thread");
        listen_thread.start();
    }

    private void register_ip_thread() {

        HttpClient httpclient = new DefaultHttpClient();
        HttpPut httpPut = new HttpPut("http://52.25.63.79/api/voice");

        try {
            // Add your data
            String ip = getIpAddr();
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("ip", ip));
            httpPut.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httpPut);

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        listen_thread();
    }

    private String getIpAddr() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces
                        .nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                while(nias.hasMoreElements()) {
                    InetAddress ia= (InetAddress) nias.nextElement();
                    if (!ia.isLinkLocalAddress()
                            && !ia.isLoopbackAddress()
                            && ia instanceof Inet4Address) {
                        ip = ia.toString().substring(1);
                        return ip + ":" + (PORT + "");
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void listen_thread() {
        Socket socket = null;

        try {
            ServerSocket server = new ServerSocket(PORT++);
            while (true) {
                System.out.println("Server waiting for connection...");
                socket = server.accept();
                dialog.dismiss();
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;
                int pos = 0;
                InputStream socket_in = socket.getInputStream();
                while ((bytesRead = socket_in.read(buffer, 0, CHUNK_SIZE)) >= 0) {
                    pos += bytesRead;
                    System.out.println(pos + " bytes (" + bytesRead + " bytes read)");
                    PlayViaAudioTrack(buffer);
                }
            }
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number. ");
        }
    }

    private void PlayViaAudioTrack(byte[] byteData) throws IOException{
        // Set and push to audio track..
        int intSize = android.media.AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING);

        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING, intSize, AudioTrack.MODE_STREAM);
        if (at != null) {
            at.play();
            // Write the byte array to the track
            System.out.println("play write length: " + byteData.length);
            at.write(byteData, 0, byteData.length);
            at.stop();
            at.release();
        }

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
