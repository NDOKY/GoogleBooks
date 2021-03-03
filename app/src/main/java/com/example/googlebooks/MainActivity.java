package com.example.googlebooks;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Console;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private ProgressBar mLoadingProgress;
    private RecyclerView rvBooks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLoadingProgress = findViewById(R.id.pb_loading);//ProgressBar
        rvBooks = findViewById(R.id.rv_books);
        //Recuperation de l'intent et de l'url pour la recherche
        Intent intent = getIntent();
        URL query = (URL) intent.getSerializableExtra("Query");
        LinearLayoutManager booksLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rvBooks.setLayoutManager(booksLayoutManager);
        URL bookUrl;

        //Construction et execution de la requete contenant l'url
        try {
            if (query == null /*|| query.toString().isEmpty()*/){
                bookUrl = ApiUtil.buildUrl("cooking");
            }
            else{
                bookUrl = new URL(query.toString());
            }
            new BooksQueryTask().execute(bookUrl);


        } catch (Exception e) {
            Log.d("error", Objects.requireNonNull(e.getMessage()));
        }


    }

    //Creation des differentes options du menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_list_menu,menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        ArrayList<String> recentList = SpUtil.getQueryList(getApplicationContext());
        int itemNum = recentList.size();
        //MenuItem recentMenu;
        for (int i = 0; i<itemNum; i++){
            menu.add(Menu.NONE, i, Menu.NONE, recentList.get(i));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            //Ouverture de l'activité SearchActivity
            case R.id.action_advanced_search:
                Intent intent = new Intent(this, SearchActivity.class);
                startActivity(intent);
                return true;
            default:
                int position = item.getItemId() + 1;
                String preferenceName = SpUtil.QUERY + position;
                String query = SpUtil.getPreferenceString(getApplicationContext(), preferenceName);
                String[] prefParams = query.split("\\,");
                String[] queryParams =  new String[4];

                for (int i= 0; i<prefParams.length; i++){
                    queryParams[i] = prefParams[i];
                }
                URL bookUrl = ApiUtil.buildUrl(
                        (queryParams[0]==null)?"":queryParams[0],
                        (queryParams[1]==null)?"":queryParams[1],
                        (queryParams[2]==null)?"":queryParams[2],
                        (queryParams[3]==null)?"":queryParams[3]
                );
                new BooksQueryTask().execute(bookUrl);
                return super.onOptionsItemSelected(item);
        }
    }

    //Execution de la requete apres construction de l'url
    @Override
    public boolean onQueryTextSubmit(String query) {
        try {
            URL bookUrl = ApiUtil.buildUrl(query);
            new BooksQueryTask().execute(bookUrl);
        }catch (Exception ex){
            Log.d("Error", Objects.requireNonNull(ex.getMessage()));
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public class BooksQueryTask extends AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... urls) {
            URL searchUrl = urls[0];
            String result = "null";
            try{
                result = ApiUtil.getJson(searchUrl);
            }
            catch (IOException e){
                Log.d("Error", e.getMessage());
            }
            return result;
        }

        //Liaison du recycleview et des données ainsi que rendu visible
        @Override
        protected void onPostExecute(String result) {
            TextView tvError = findViewById(R.id.tvError);
            if(result == null){
                rvBooks.setVisibility(View.INVISIBLE);
                tvError.setVisibility(View.VISIBLE);
            }
            else{
                rvBooks.setVisibility(View.VISIBLE);
                tvError.setVisibility(View.INVISIBLE);
                mLoadingProgress.setVisibility(View.INVISIBLE);
                ArrayList<Book> books = ApiUtil.getBooksFromJson(result);
                BooksAdapter adapter = new BooksAdapter(books);
                rvBooks.setAdapter(adapter);
            }

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingProgress.setVisibility(View.VISIBLE);
        }
    }
}