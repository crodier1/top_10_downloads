package crodier.top10downloader;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ListView listApps;
    private String feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int feedLimit = 10;
    private int lastFeed = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listApps = (ListView) findViewById(R.id.xmlListView);

        downLoadUrl(String.format(feedUrl, feedLimit));

    }

    //called when it is time to create options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feeds_menu, menu);

        if (feedLimit == 10){
            menu.findItem(R.id.mnu10).setChecked(true);
        } else {
            menu.findItem(R.id.mnu25).setChecked(true);
        }


        return true;


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        int newFeed = 0;


        switch (id){
            case R.id.mnuFree:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                newFeed = 0;
                break;

            case  R.id.mnuPaid:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                newFeed = 1;
                break;

            case R.id.mnuSongs:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                newFeed = 2;
                break;

            case R.id.mnuRefresh:
                downLoadUrl(String.format(feedUrl, feedLimit));
                Log.d(TAG, "onOptionsItemSelected: page refreshed");
                break;

            case R.id.mnu10:
            case R.id.mnu25:
                if (!item.isChecked()){
                    item.setChecked(true);
                    feedLimit = 35 - feedLimit;

                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle() + " setting fee to " + feedLimit);
                } else {
                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle() + "feedLimit unchanged");
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        
        if (!item.isChecked()){

            downLoadUrl(String.format(feedUrl, feedLimit));
        }

        if (newFeed != lastFeed){

            downLoadUrl(String.format(feedUrl, feedLimit));

            lastFeed = newFeed;
        } else {

            Log.d(TAG, "onOptionsItemSelected: feed not changed");
        }

        return true;
    }

    private void downLoadUrl(String feedUrl){
        Log.d(TAG, "downLoadUrl: staring Asynctask");
        DownloadData downloadData = new DownloadData();
        downloadData.execute(feedUrl);
        Log.d(TAG, "downLoadUrl: done");

    }



    //3 params let us know what type of class
    //1st param is the url
    //2nd is for the progress bar. not using progress bar
    //3rd is type of data we want back
    private class DownloadData extends AsyncTask<String, Void, String>{
        private static final String TAG = "DownloadData";
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
//            Log.d(TAG, "onPostExecute: param is " + s);
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse(s);

            //old adapter
//            ArrayAdapter<FeedEntry> arrayAdapter = new ArrayAdapter<FeedEntry>(
//                    MainActivity.this,
//                    R.layout.list_item,
//                    parseApplications.getApplications()
//            );
//
//            listApps.setAdapter(arrayAdapter);

            FeedAdapter<FeedEntry> feedAdapter = new FeedAdapter<>(MainActivity.this, R.layout.list_record, parseApplications.getApplications());

            listApps.setAdapter(feedAdapter);
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground: starts with " + strings[0]);
            String rssFeed = downloadXML(strings[0]);
            if (rssFeed == null){
                Log.e(TAG, "doInBackground: Error downloading");
            }
            return rssFeed;

        }

        private String downloadXML(String urlPath){
            StringBuilder xmlResult = new StringBuilder();

            try {
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.d(TAG,"downloadXML: The response code was " + response);
                //non chained together request
//                InputStream inputStream = connection.getInputStream();
//                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//                //reads xml
//                BufferedReader reader = new BufferedReader(inputStreamReader);

                //chained together request. does same as above
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                int charsRead;
                char[] inputBuffer = new char[500];
                while (true){
                    charsRead = reader.read(inputBuffer);
                    if (charsRead < 0){
                        break;
                    }

                    if (charsRead > 0){
                        xmlResult.append(String.copyValueOf(inputBuffer, 0, charsRead));
                    }
                }
                reader.close();
                return xmlResult.toString();
            } catch (MalformedURLException e){
                Log.e(TAG, "downloadXML: Invalid URL " + e.getMessage());
            } catch (IOException e){
                Log.e(TAG, "downloadXML: IO Exception reading data: " + e.getMessage());
            } catch (SecurityException e){
                Log.e(TAG, "downloadXML: security exception. Needs permission? " + e.getMessage());
//                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("feedLimit", feedLimit);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        feedLimit = savedInstanceState.getInt("feedLimit");
    }
}
