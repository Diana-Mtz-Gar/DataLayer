package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    String TAG = "Mobile Activity";

    Button enviar;
    TextView mMensaje;
    protected Handler myHandler;
    int receivedMessageNumber = 1;
    int sentMessageNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enviar = findViewById(R.id.btnEnviar);
        mMensaje = findViewById(R.id.txtMensaje);

        myHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                messageText(stuff.getString("messageText"));
                return true;
            }
        });

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    public void messageText(String newinfo) {
        if (newinfo.compareTo("") != 0) {
            mMensaje.append("\n" + newinfo);
        }
    }

    public class Receiver extends BroadcastReceiver {
        @Override

        public void onReceive(Context context, Intent intent) {

            String message = "Mensaje recibido del wearable " + receivedMessageNumber++;

            mMensaje.setText(message);
        }
    }

    public void sendClick(View v) {
        String message = "Enviando mensaje... ";
        mMensaje.setText(message);

        new NewThread("/my_path", message).start();
    }

    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("messageText", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);

    }

    class NewThread extends Thread {
        String path;
        String message;

        NewThread(String _path, String _message) {
            path = _path;
            message = _message;
        }

        public void run() {
            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {
                        Integer result = Tasks.await(sendMessageTask);
                        sendmessage("Mensaje enviado a Wearable " + sentMessageNumber++);

                        Log.v(TAG, "NewThread: Message send to: " + node.getDisplayName());

                    } catch (ExecutionException exception) {
                        //TO DO: Handle the exception//
                        sendmessage("NewThread: message failed to: " + node.getDisplayName());
                        Log.e(TAG, "New Task Failed: " + exception);

                    } catch (InterruptedException interruptedException) {
                        //TO DO: Handle the exception//
                        Log.e(TAG, "New Interrupt occurred: " + interruptedException);
                    }
                }
            } catch (ExecutionException exception) {
                //TO DO: Handle the exception//
                sendmessage("Node Task failed: " + exception);
                Log.e(TAG, "Node Task failed: " + exception );

            } catch (InterruptedException interruptedException) {
                //TO DO: Handle the exception//
                Log.e(TAG, "Node Interrupt occurred: " + interruptedException);
            }
        }
    }
}