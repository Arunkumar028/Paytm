package com.example.paytm.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.paytm.model.Checksum;
import com.example.paytm.local.Config;
import com.example.paytm.model.Paytm;
import com.example.paytm.R;
import com.example.paytm.service.Apiservice;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPGService;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;

import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MainActivity extends AppCompatActivity  implements PaytmPaymentTransactionCallback{

    TextView textViewPrice;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewPrice = findViewById(R.id.textViewPrice);
        findViewById(R.id.buttonBuy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //calling the method generateCheckSum() which will generate the paytm checksum for payment
                generateCheckSum();
            }

            private void generateCheckSum() {
              String txnAmount = textViewPrice.getText().toString().trim();

                Gson gson = new GsonBuilder()
                        .setLenient()
                        .create();

              final Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(Apiservice.BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();

                //creating the retrofit api service
                Apiservice apiService = retrofit.create(Apiservice.class);

                //creating paytm object
                //containing all the values required
                final Paytm paytm = new Paytm(
                        Config.M_ID,
                        Config.CHANNEL_ID,
                        txnAmount,
                        Config.WEBSITE,
                        Config.CALLBACK_URL,
                        Config.INDUSTRY_TYPE_ID
                );

                //creating a call object from the apiService
                Call<Checksum> call = apiService.getChecksum(
                        paytm.getmId(),
                        paytm.getOrderId(),
                        paytm.getCustId(),
                        paytm.getChannelId(),
                        paytm.getTxnAmount(),
                        paytm.getWebsite(),
                        paytm.getCallBackUrl(),
                        paytm.getIndustryTypeId()


                );


                call.enqueue(new Callback<Checksum>() {
                    @Override
                    public void onResponse(Call<Checksum> call, Response<Checksum> response) {
                        initializePaytmPayment(response.body().getChecksumHash(), paytm);

                    }

                    @Override
                    public void onFailure(Call<Checksum> call, Throwable t) {
                        if (t instanceof Exception){
                            Toast.makeText(MainActivity.this, "code error"+t, Toast.LENGTH_SHORT).show();
                            System.out.println("Error"+t);

                        }
                        else{
                            Toast.makeText(MainActivity.this, "Internet error", Toast.LENGTH_SHORT).show();
                        }



                    }
                });

            }
        });}

        private void initializePaytmPayment (String checksumHash, Paytm paytm){

            //getting paytm service
           // PaytmPGService Service = PaytmPGService.getService();

            //use this when using for production
            //PaytmPGService Service = PaytmPGService.getStagingService("https://securegw-stage.paytm.in/order/process");

            PaytmPGService Service = PaytmPGService.getStagingService("https://securegw-stage.paytm.in/order/process");
            //creating a hashmap and adding all the values required
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("MID", Config.M_ID);
            paramMap.put("ORDER_ID", paytm.getOrderId());
            paramMap.put("CUST_ID", paytm.getCustId());
            paramMap.put("CHANNEL_ID", paytm.getChannelId());
            paramMap.put("TXN_AMOUNT", paytm.getTxnAmount());
            paramMap.put("WEBSITE", paytm.getWebsite());
            paramMap.put("CALLBACK_URL", paytm.getCallBackUrl());
            paramMap.put("CHECKSUMHASH", checksumHash);
            paramMap.put("INDUSTRY_TYPE_ID", paytm.getIndustryTypeId());

            //creating a paytm order object using the hashmap
            PaytmOrder order = new PaytmOrder((HashMap<String, String>) paramMap);

            //intializing the paytm service
            Service.initialize(order, null);

            //finally starting the payment transaction
            Service.startPaymentTransaction(this, true, true, this);

        }


        //all these overriden method is to detect the payment result accordingly
        @Override
        public void onTransactionResponse(Bundle bundle) {

            Toast.makeText(this, bundle.toString(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void networkNotAvailable() {
            Toast.makeText(this, "Network error", Toast.LENGTH_LONG).show();
        }

        @Override
        public void clientAuthenticationFailed(String s) {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        }

        @Override
        public void someUIErrorOccurred(String s) {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onErrorLoadingWebPage(int i, String s, String s1) {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onBackPressedCancelTransaction() {
            Toast.makeText(this, "Back Pressed", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onTransactionCancel(String s, Bundle bundle) {
            Toast.makeText(this, s + bundle.toString(), Toast.LENGTH_LONG).show();
        }
    }