package app.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * @author guille
 */
public class SendPost {
	public static String send(String url, String text, Integer rt, Integer fav, String date) {

		URL obj = null;
		try {
			obj = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) obj.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//add reuqest header
		try {
			con.setRequestMethod("POST");
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
		//con.setRequestProperty("User-Agent", USER_AGENT);
		//con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String urlParameters = "text=" + text + "&rt=" + rt + "&fav=" + fav + "&date=" + date;

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = null;
		int responseCode = 0;

		try {
			wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			responseCode = con.getResponseCode();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = null;
		String inputLine;
		StringBuffer response = new StringBuffer();
		try {
			in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return response.toString();

	}

}
