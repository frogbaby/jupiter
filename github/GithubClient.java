package github;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;





public class GithubClient {
	// URL_TEMPLATE: send request
	private static final String URL_TEMPLATE = "https://jobs.github.com/positions.json?description=%s&lat=%s&long=%s";
	// DEFAULT_KEYWORD: if description is None, use developer as default description
	private static final String DEFAULT_KEYWORD = "developer";
	
	public List<Item> search(double lat, double lon, String keyword) {
		// prepare http request parameter
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		// 转码特殊字符：空格 -> +
		try {
			keyword = URLEncoder.encode(keyword, "UTF-8"); // danping Yan -> danping+Yan
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		String url = String.format(URL_TEMPLATE, keyword, lat, lon);
		
		// send http request
		// 创建http client， 用来send client
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			// 发送request后，得到的response
			CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
			
			// get http response body
			if (response.getStatusLine().getStatusCode() != 200) {
				return new ArrayList<>();
			}
			// get response body
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				//返回长度为0的array
				return new ArrayList<>();

			}
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			StringBuilder responseBody = new StringBuilder();
			String line = null;
			// 一行一行读
			while ((line = reader.readLine()) != null) {
				responseBody.append(line);				
			}
			JSONArray array = new JSONArray(responseBody.toString());
			// get list of item by parsing JsonArray gotten from github api
			return getItemList(array);


		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new ArrayList<>();

		
	}
	
	private List<Item> getItemList(JSONArray array) {
		List<Item> itemList = new ArrayList<>();
		List<String> descriptionList = new ArrayList<>();
		
		for (int i = 0; i < array.length(); i++) {
			String description = getStringFieldOrEmpty(array.getJSONObject(i), "description");
			if (description.equals("") || description.equals("\n")) {
				descriptionList.add(getStringFieldOrEmpty(array.getJSONObject(i), "title"));
			} else {
				descriptionList.add(description);
			}		
		}
		
		String[] strings = descriptionList.toArray(new String[descriptionList.size()]);
		List<List<String>> keywords = MonkeyLearnClient.extractKeywords(strings);



		for (int i = 0; i < array.length(); ++i) {
			JSONObject object = array.getJSONObject(i);
			ItemBuilder builder = new ItemBuilder();
			
			builder.setItemId(getStringFieldOrEmpty(object, "id"));
			builder.setName(getStringFieldOrEmpty(object, "title"));
			builder.setAddress(getStringFieldOrEmpty(object, "location"));
			builder.setUrl(getStringFieldOrEmpty(object, "url"));
			builder.setImageUrl(getStringFieldOrEmpty(object, "company_logo"));
			
			builder.setKeywords(new HashSet<String>(keywords.get(i)));

			
			Item item = builder.build();
			itemList.add(item);


		}
		
		return itemList;

	}
	private String getStringFieldOrEmpty(JSONObject obj, String field) {
		return obj.isNull(field) ? "" : obj.getString(field);
	}


	
	
	
	
	
	

}
