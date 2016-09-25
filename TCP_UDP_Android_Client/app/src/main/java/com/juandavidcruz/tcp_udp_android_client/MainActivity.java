package com.juandavidcruz.tcp_udp_android_client;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    public int threadQuantity = 2;



    public String connectionMode = "TCP";

    private Socket socket;

    public String serverIP = "NONE";
    public String serverPort = "NONE";

    public Boolean run = false;

    byte[] send_data = new byte[1024];

    public Location mLastLocation;

    public  GoogleApiClient mGoogleApiClient;


    public TextView resultsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        resultsTextView = (TextView)findViewById(R.id.textViewResults);

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                RadioButton selected = (RadioButton)findViewById(checkedId);
                connectionMode = selected.getText().toString();

            }
        });
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (run == false) {
                    EditText textIP = (EditText)findViewById(R.id.textIP);
                    EditText textPort = (EditText)findViewById(R.id.textPuerto);

                    serverIP = textIP.getText().toString();
                    serverPort = textPort.getText().toString();

                    Snackbar.make(view, "Conectando a " + textIP.getText() + ":" + textPort.getText(), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    run = true;
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_pause));

                    for (int i = 0; i < threadQuantity; i++) {
                        ClientThread newClient = new ClientThread();
                        newClient.setId(i);
                        new Thread(newClient).start();
                    }

                } else {
                    resultsTextView.append("Preparado el cierre de la conexión \n");
                    run = false;
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_play));

                }





            }
        });


        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },
                    100 );
        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                //mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
                //mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
            }
        }
    }

    public String getDeviceLocation() {
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },
                    100 );
            return "Error";
        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                String location = String.valueOf(mLastLocation.getLatitude()) + "," + String.valueOf(mLastLocation.getLongitude());
                return location;
            } else {
                return "Not Known last location";
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Connection Suspended", "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.w("Connection Failed", "Connecting to Google Play Services failed. Result=" + result);

    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

    public void sendLocationToPrintWriter(final PrintWriter pOut, final int pThreadID ) {


            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {

                    if (run == true) {
                        final String deviceLocation = getDeviceLocation();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultsTextView.append("Enviando Localización: "+ deviceLocation +" (Thread ID: "+ pThreadID +")"+"\n");
                            }
                        });

                        pOut.println(deviceLocation);
                        pOut.println(pThreadID+"");
                    } else {

                        pOut.println("Bye");
                        try {
                            socket.close();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resultsTextView.append("Conexión TCP Cerrada (Thread ID: "+ pThreadID +")"+"\n");
                                }
                            });
                            timer.cancel();
                        } catch (final IOException e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resultsTextView.append("Error al cerrar conexión: " + e.getMessage() + " (Thread ID: "+ pThreadID +")"+"\n");
                                }
                            });
                            timer.cancel();
                            e.printStackTrace();
                        }
                    }
                }

            },0,1000);//Update text every second


    }

    public void sendLocationToUDPSocket(final DatagramSocket pSocket, final InetAddress pAddress, final int pPort, final int pThreadID) {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                if (run == true) {
                    try {

                        final String str = getDeviceLocation() + "|" + pThreadID;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultsTextView.append("Enviando Localización: "+ str +" (Thread ID: "+ pThreadID +")"+"\n");
                            }
                        });

                        send_data = str.getBytes();
                        DatagramPacket send_packet = new DatagramPacket(send_data, str.length(), pAddress, pPort);
                        pSocket.send(send_packet);
                    } catch (final IOException e){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultsTextView.append("Error al enviar Localización: "+ e.getMessage() +" (Thread ID: "+ pThreadID +")"+"\n");
                            }
                        });

                        e.printStackTrace();
                    }
                } else {
                    pSocket.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resultsTextView.append("Conexión UDP Cerrada (Thread ID: "+ pThreadID +")"+"\n");
                        }
                    });
                    timer.cancel();
                }
            }

        },0,1000);//Update text every second


    }


    class ClientThread implements Runnable {

        public int id;

        public void setId(int pID) {
            id = pID;
        }

        @Override
        public void run() {

            if (connectionMode.equals("UDP")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultsTextView.setText("");
                        resultsTextView.append("Creando conexión UDP al Servidor: " + serverIP+":"+serverPort + " (Thread ID: "+ id +")"+"\n");
                    }
                });

                try {
                    DatagramSocket client_socket = new DatagramSocket(Integer.parseInt(serverPort));
                    InetAddress IPAddress = InetAddress.getByName(serverIP);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resultsTextView.append("Preparando envio de localización del dispositivo" + " (Thread ID: "+ id +")"+"\n");
                        }
                    });

                    sendLocationToUDPSocket(client_socket, IPAddress, Integer.parseInt(serverPort), id);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else if (connectionMode.equals("TCP")) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        resultsTextView.setText("");
                        resultsTextView.append("Creando conexión TCP al Servidor: " + serverIP+":"+serverPort + " (Thread ID: "+ id +")"+"\n");


                    }
                });


                try {

                    InetAddress serverAddr = InetAddress.getByName(serverIP);

                    socket = new Socket(serverAddr, Integer.parseInt(serverPort));

                    try {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultsTextView.append("Enviando Hello al servidor (Thread ID: "+ id +")"+"\n");
                            }
                        });


                        PrintWriter out = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(socket.getOutputStream())),
                                true);
                        BufferedReader inFromServer =
                                new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out.println("Hello");
                        String answer = inFromServer.readLine();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultsTextView.append("Recibiendo HelloBack del Servidor (Thread ID: "+ id +")"+"\n");
                            }
                        });

                        if (answer.equalsIgnoreCase("HelloBack")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resultsTextView.append("Preparando envio de localización del dispositivo (Thread ID: "+ id +")"+"\n");
                                }
                            });

                           sendLocationToPrintWriter(out, id);

                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resultsTextView.append("No se obtuvo HelloBack, cerrando conexión (Thread ID: "+ id +")"+"\n");
                                }
                            });

                            socket.close();
                        }

                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                } catch (UnknownHostException e1) {

                    e1.printStackTrace();

                } catch (IOException e1) {

                    e1.printStackTrace();

                }
            }



        }

    }


}
