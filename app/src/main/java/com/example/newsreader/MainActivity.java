package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles =new ArrayList<>();
    ArrayList<String> content =new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDB = this.openOrCreateDatabase("news", MODE_PRIVATE, null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS news (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");

        DownloadTask task = new DownloadTask();
        try {
//            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        ListView listViewTitles = findViewById(R.id.listViewTitles);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
        listViewTitles.setAdapter(arrayAdapter);

        listViewTitles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent =new Intent(getApplicationContext(), NewsActivity.class);
                intent.putExtra("content", content.get(position));
                startActivity(intent);
            }
        });
        updateListView();
    }

    public void updateListView() {
        Cursor c = articlesDB.rawQuery("SELECT * FROM news", null);

        int titleIndex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");

        if (c.moveToFirst()) {
            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());

//            c.close();
            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            StringBuilder result = new StringBuilder();
            URL url;
            HttpURLConnection urlConnection;
//            InputStream inputStream;
//            InputStreamReader inputStreamReader;
//            int data;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();

                while (data != -1) {
                    char current = (char) data;
                    result.append(current);
                    data = inputStreamReader.read();
                }

                JSONArray jsonArray = new JSONArray(String.valueOf(result));
                int numberOfItems = 20;

                if (jsonArray.length() < 20) {
                    numberOfItems = jsonArray.length();
                }

                articlesDB.execSQL("DELETE FROM news");

                for (int i=0; i<numberOfItems; i++) {
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    data = inputStreamReader.read();
                    StringBuilder articleInfo = new StringBuilder();

                    while (data != -1) {
                        char current = (char) data;
                        articleInfo.append(current);
                        data = inputStreamReader.read();
                    }
//                    Log.i("Article Info", articleInfo.toString());
                    JSONObject jsonObject = new JSONObject(String.valueOf(articleInfo));
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

//                        Log.i("TitleAndUrl", articleTitle + " " + articleUrl);

                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);

                        data = inputStreamReader.read();
                        StringBuilder articleContent = new StringBuilder();

                        while (data != -1) {
                            char current = (char) data;
                            articleContent.append(current);
                            data = inputStreamReader.read();
                        }
                        Log.i("HTML", String.valueOf(articleContent));
                        String sql = "INSERT INTO news (articleId, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, String.valueOf(articleContent));

                        statement.execute();
                    }
                }

                Log.i("URL Content", String.valueOf(result));
                return String.valueOf(result);

//
//                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }
}