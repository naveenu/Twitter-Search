package com.trackthebird.twittersearch;
/*	By Naveenu	*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

public class TSMainActivity extends Activity {
	private Context	 context				=   null;
	private ListView twitterListView		=	null;
	private Button 	 searchItemButton		=	null;
	private String 	 mConsumerKey 			= 	null;
	private String 	 mConsumerSecret 		=	null;
	private ProgressDialog showProgressDlg	=	null;
	final static String LOG_TAG 			= "TWITTER SEARCH";
	
	/*	Initialize	*/
	private void init(){
		context				= 	this;
		searchItemButton	=	(Button) findViewById(R.id.searchItemButton);	
		searchItemButton.setOnClickListener(searchButtonHandler);
		twitterListView		=	(ListView) findViewById(R.id.twitterListView);
		// Get and set consumer keys
		mConsumerKey		=	getConsumerkey("CONSUMER_KEY");    // Add Consumer Key in Manifest file
		mConsumerSecret		=	getConsumerkey("CONSUMER_SECRET"); // Add Consumer Secret in Manifest file
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tsmain);
		// Initilize
		init();
	}

	/*	Search button click listener handler	*/
	OnClickListener	searchButtonHandler		=	new OnClickListener(){
		@Override
		public void onClick(View v) {
			EditText etSearchField							=		(EditText) findViewById(R.id.searchItemEditText);
			String	searchText								=		etSearchField.getText().toString();
			if(searchText.length() > 0){
					InputMethodManager inputMethodManager 	= 		(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(etSearchField.getWindowToken(), 0);
					getTwitterSearchItems(searchText);
			}
			else{
	    		Toast.makeText(getBaseContext(),getResources().getString(R.string.enter_twitter_search_item),Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	// Get ConsumerKeys from Manifest files
	private String getConsumerkey(String type) {
		String keys = null;
		try {
			ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			keys 					= (String)appInfo.metaData.get(type);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return keys;
	}
	
	// Get Twitter search items
	private void getTwitterSearchItems(String searchText){
		if(isNetworkAvailable()){
			ShowPopUp();
			new GetTwitterSearchItemAsyncTask().execute(searchText);
		}
		else{
    		Toast.makeText(getBaseContext(),getResources().getString(R.string.internet_not_available),Toast.LENGTH_SHORT).show();
		}
	}
	
	/*	Async Task class, connects to Twitter and gets the Text search details 	*/
	private class GetTwitterSearchItemAsyncTask extends AsyncTask<String, Void, String>{
		String twitterSearchURL		=	getResources().getString(R.string.twitter_search_url);
		
		//Background task
		@Override
		protected String doInBackground(String... params) {
			if(params.length > 0){
				try {
					String encodedURL	= URLEncoder.encode(params[0].toString(), "UTF-8");
					return getTwitterStream(twitterSearchURL + encodedURL);
					} catch (UnsupportedEncodingException ex) {
					} catch (IllegalStateException ex1) {
				}
			}
			return null;
		}
		
		// Converts JSON results into a Twitter object (ie. Array list of Tweets)
		@Override
		protected void onPostExecute(String result) {
			TSSearches searchesResults 	= jsonToSearches(result);
			if(searchesResults != null){
				String[] messageArray 		= 	new String[searchesResults.size()];
				CustomListAdapter adapter	=	new CustomListAdapter(context, searchesResults, messageArray);
				twitterListView=(ListView)findViewById(R.id.twitterListView);
				twitterListView.setAdapter(adapter);
			}
			else{
				Toast.makeText(getBaseContext(),getResources().getString(R.string.unknow_error),Toast.LENGTH_SHORT).show();
			}
			DismissPopUp();
		}
	}
	
	// Converts a string of JSON data into a SearchResults object
	private TSSearches jsonToSearches(String result) {
		TSSearches tSSearches = null;
		if (result != null && result.length() > 0) {
			try {
				Gson gson = new Gson();
				// Brings entire search object
				TSSearchResults searchresults = gson.fromJson(result, TSSearchResults.class);
				tSSearches = searchresults.getStatuses();
			} catch (IllegalStateException ex) {
			}
		}
		return tSSearches;
	}
	
	// Connects and gets search results
	private String getTwitterStream(String url) {
		String results 			= null;
		String twitterTokenURL	= getResources().getString(R.string.twitter_token_url);
		// Encode consumer key and secret
		try {
			// URL encode the consumer key and secret
			String urlConsumerApiKey 	= URLEncoder.encode(mConsumerKey,	"UTF-8");
			String urlConsumerApiSecret = URLEncoder.encode(mConsumerSecret,"UTF-8");

			// Concatenate the encoded consumer key, a colon character, and the encoded consumer secret
			String combined = urlConsumerApiKey + ":" + urlConsumerApiSecret;

			// Base64 encode the string
			String base64Encoded = Base64.encodeToString(combined.getBytes(), Base64.NO_WRAP);

			// Obtains a bearer token
			HttpPost httpPost = new HttpPost(twitterTokenURL);
			httpPost.setHeader("Authorization", "Basic " + base64Encoded);
			httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			httpPost.setEntity(new StringEntity("grant_type=client_credentials"));
			String rawAuthorization = getResponseBody(httpPost);
			TSAuthentication auth = jsonToAuthenticated(rawAuthorization);
			// Applications should verify that the value associated with the
			// token_type key of the returned object is bearer
			if (auth != null && auth.token_type.equals("bearer")) {
				HttpGet httpGet = new HttpGet(url);
				httpGet.setHeader("Authorization", "Bearer " + auth.access_token);
				httpGet.setHeader("Content-Type", "application/json");
				results = getResponseBody(httpGet);
				}
			} catch (UnsupportedEncodingException ex) {
		} catch (IllegalStateException ex1) {
		}
		return results;
	}
	
	// Get response body
	private String getResponseBody(HttpRequestBase request) {
		StringBuilder sb = new StringBuilder();
		try {
			DefaultHttpClient httpClient	= new DefaultHttpClient(new BasicHttpParams());
			HttpResponse response 			= httpClient.execute(request);
			int statusCode 					= response.getStatusLine().getStatusCode();
			String reason 					= response.getStatusLine().getReasonPhrase();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream inputStream = entity.getContent();
				BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
				String line = null;
				while ((line = bReader.readLine()) != null) {
					sb.append(line);
				}
			} else {
				sb.append(reason);
			}
		} catch (UnsupportedEncodingException ex) {
		} catch (ClientProtocolException ex1) {
		} catch (IOException ex2) {
		}
		return sb.toString();
	}
	
	// Convert a JSON authentication object into an Authenticated object
	private TSAuthentication jsonToAuthenticated(String rawAuthorization) {
		TSAuthentication auth = null;
		if (rawAuthorization != null && rawAuthorization.length() > 0) {
			try {
				Gson gson = new Gson();
				auth 	  = gson.fromJson(rawAuthorization, TSAuthentication.class);
			} catch (IllegalStateException ex) {
			}
		}
		return auth;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tsmain, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		return super.onOptionsItemSelected(item);
	}

	/*	Custom adapter which dosplay Twitter profile image as well as message	*/
	public class CustomListAdapter extends ArrayAdapter<String> {
		 private final Activity context;
		 private TSSearches tSSearches;		 
		 public CustomListAdapter(Context context, TSSearches tSSearches, String[] itemname) {
		 super(context, R.layout.twitter_list_item, itemname);		 
		 this.context=(Activity) context;
		 this.tSSearches	=	tSSearches;
		 }
		 		 
		 public View getView(int position,View view,ViewGroup parent) {
			 LayoutInflater inflater	=	context.getLayoutInflater();
			 View rowDisplayView		=	inflater.inflate(R.layout.twitter_list_item, null,true);
			 TextView tvTwitterMessage 	= 	(TextView) rowDisplayView.findViewById(R.id.tvTwitterMessage);
			 ImageView ivProfileIcon 	= 	(ImageView) rowDisplayView.findViewById(R.id.ivTwitterProfileIcon);
			 tvTwitterMessage.setText(tSSearches.get(position).getText());
			 String url = tSSearches.get(position).getUser().getProfileImageUrl();
			 Picasso.with(context)
			    .load(url)
			    .resize(50, 50)
			    .placeholder(R.drawable.ic_owner)
			    .error(R.drawable.ic_owner)
			    .into(ivProfileIcon);
			 return rowDisplayView;
		 };
	}
	
	/*	Checks whether Network is available or not	*/
	public boolean isNetworkAvailable(){
		try{
			ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		    // if no network is available networkInfo will be null
		    // otherwise check if we are connected
		    if (networkInfo != null && networkInfo.isConnected()) {
		        return true;
		    }
		} catch (Exception e){
			e.printStackTrace();
		}
		 return false;
	}
	
	// Show popup message while downloading search items
	protected void ShowPopUp(){
        this.runOnUiThread(new Runnable() {
            public void run() {
        		showProgressDlg = new ProgressDialog(context);
        		showProgressDlg.setTitle(getResources().getString(R.string.twitter_text_search_title));
        		showProgressDlg.setMessage(getResources().getString(R.string.please_wait));
        		showProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        		showProgressDlg.setIcon(R.drawable.ic_owner);
        		showProgressDlg.setCancelable(true);
        		showProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.stop), new DialogInterface.OnClickListener() {
        		    @Override
        		    public void onClick(DialogInterface dialog, int which) {
        		    	try{
        			        dialog.cancel();
        			        dialog= null;
        			        showProgressDlg.dismiss();
        		    	}
        		    	catch(Exception e){
        		    	}
        		    }
        		});
        		showProgressDlg.show();
            }
        });
	}
	// Dismiss popup
	protected void DismissPopUp(){
		if(showProgressDlg != null && showProgressDlg.isShowing()){
			showProgressDlg.dismiss();
		}
	}
	
}
