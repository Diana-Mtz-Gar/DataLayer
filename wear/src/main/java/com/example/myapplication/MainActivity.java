package com.example.myapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity {

    private final static String TAG = "Wear MainActivity";

    private TextView mMensaje;
    private Button envio;
    int receivedMessageNumber = 1;
    int sentMessageNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMensaje =  findViewById(R.id.txtMensaje2);
        envio =  findViewById(R.id.btnEnvio);

        envio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String onClickMessage = "Mensaje enviado a celular " + sentMessageNumber++;
                mMensaje.setText(onClickMessage);

                String datapath = "/my_path";
                new SendMessage(datapath, onClickMessage).start();

            }
        });

        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);

    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String onMessageReceived = "Mensaje recibido del celular " + receivedMessageNumber++;
            mMensaje.setText(onMessageReceived);
        }
    }

    class SendMessage extends Thread {
        String path;
        String message;

        SendMessage(String _path, String _message){
            path = _path;
            message = _message;
        }

        public void run() {
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

            try{
                List<Node> nodes = Tasks.await(nodeListTask);

                for (Node node : nodes){
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this)
                                    .sendMessage(node.getId(), path, message.getBytes());

                    try {
                        Integer result = Tasks.await(sendMessageTask);
                        Log.v(TAG, "SendMessage: message send to: " + node.getDisplayName());
                    } catch (ExecutionException exception){
                        Log.e(TAG,"Task failed: " + exception);
                    } catch (InterruptedException interruptedException) {
                        Log.e(TAG, "Interrupt occurred: " + interruptedException);
                    }
                }
            } catch (ExecutionException exception){
                Log.e(TAG,"Task failed: " + exception);
            } catch (InterruptedException iException){
                Log.e(TAG, "Interrupt occurred: " + iException);
            }
        } // end run
    } // end class SendMessage
}