package tdn.stockmatch.data;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;


public class YahooDataDownloader {
	public static final String CONFIG_FILE = "config.cnf";
	
	/*Old host. Doesn't work*/
	//private static final String HOST="ichart.finance.yahoo.com";
	
	
	//const from config
	private static String OUT_DIR="output";
	private static String WRONG_SYMBOL_LIST="wrong_symbol.txt";
	private static String TRACK_SYMBOL_LIST="track_symbol.txt";
	private static String SYMBOL_LIST_FILE="symbol.txt";
	
	/*Date format: yyyymmdd*/
	private static String FROM_DATE="20120924";
	private static String TO_DATE="20130925";
	
	private static String HOST_CRUMB = "C9luNcNjVkK";
	
	/*An example of url: https://query1.finance.yahoo.com/v7/finance/download/AAPL?period1=1497727945&period2=1500319945&interval=1d&events=history&crumb=C9luNcNjVkK
	 * full_url = HOST_SCHEME + HOST_URL + PATH + PARAMETERS
	 * */
	/*Host scheme: http, https*/
	private static String HOST_SCHEME="http";
	private static String HOST_URL="query1.finance.yahoo.com";
	private static String HOST_PATH="/v7/finance/download";
	//time mode: d for daily, w for weekly, m for monthly
	private static String TIME_MODE="d";
	private static String HOST_INTERVAL="1d";
	private static String HOST_EVENT="history";
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		readConfig();
		//read symbol file
		File outdir = new File(OUT_DIR);
		if(!outdir.exists())
			outdir.mkdir();
		
		File f = new File(SYMBOL_LIST_FILE);
		
		try {
			String outFileName = WRONG_SYMBOL_LIST;
			BufferedWriter wrong_symbol_bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outFileName))));
			BufferedWriter symbol_bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(TRACK_SYMBOL_LIST))));
			BufferedReader symbol_br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			String line;
			String symbol;
			//format: symbol \t name
			int cnt = 0;
			
			
			/*Convert from yyyymmdd to unix timestamp*/
			String fromDate_unix_ts = date_to_unix_timestamp(FROM_DATE);
			String toDate_unix_ts = date_to_unix_timestamp(TO_DATE);
			
			/*for each symbol (stock ID) in the symbol list file, download the data for a period specified by FROM_DATE to TO_DATE*/
			while((line = symbol_br.readLine()) != null){
				String[] fields = line.split("\t");
				
				if(fields.length > 0){
					symbol = fields[0];
					System.out.printf("Downloading " + symbol + "...");
					
					/*request data from the host and download */
					long length = downloadSymbolData(symbol, fromDate_unix_ts, toDate_unix_ts);
					if(length <= 0){
						wrong_symbol_bw.write(symbol);
						wrong_symbol_bw.newLine();
					}
					else{
						cnt++;
						symbol_bw.newLine();
						symbol_bw.write("" + cnt + "\t" + symbol + "\t" + length);
					}
					
					System.out.printf("Done.\n");
				}
				
			}//end while
			symbol_br.close();
			wrong_symbol_bw.close();
			symbol_bw.close();
			
			System.out.println("Task Done.");
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	 * Convert from a date string to Unix timestamp
	 * Input: date string in yyyymmdd format
	 * Output: date string in Unix timestamp format
	 * */
	private static String date_to_unix_timestamp(String date_str) {
		Calendar cal = Calendar.getInstance();
		int year = Integer.parseInt(date_str.substring(0, 4));
		int month = Integer.parseInt(date_str.substring(4, 6));
		int day = Integer.parseInt(date_str.substring(6, 8));
		
		cal.set(year, month, day); 	
		
		long unix_time = cal.getTimeInMillis() / 1000L;
		
		return Long.toString(unix_time);
	}
	/**
	 * symbol: Stock symbol, e.g., YHOO
	 * fromDate: from date in yyyymmdd format, Ex: 20130924
	 * toDate: to date in yyyymmdd format
	 * 
	 * return: length of sequence
	 * 
	 * */
	private static long downloadSymbolData(String symbol, String fromDate_unix_ts, String toDate_unix_ts){
		long length = 0;
		
		URI uri;
			
		try {
			String outFileName = OUT_DIR + "/" + symbol + ".txt";
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outFileName))));
			
			/* full_url = HOST_SCHEME + HOST_URL + PATH + PARAMETERS 
			 * Ex: https://query1.finance.yahoo.com/v7/finance/download/AAPL?period1=1497727945&period2=1500319945&interval=1d&events=history&crumb=C9luNcNjVkK
			 */
			String path_with_symbol = HOST_PATH + "/" + symbol;
			
			uri = new URIBuilder()
			.setScheme(HOST_SCHEME)
			.setHost(HOST_URL)
			.setPath(path_with_symbol)
			.setParameter("period1", fromDate_unix_ts)
			.setParameter("period2", toDate_unix_ts)
			.setParameter("interval", HOST_INTERVAL)
			.setParameter("events", HOST_EVENT)
			.setParameter("crumb", HOST_CRUMB)
			.build();
			
			HttpClient client = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(uri);
			/*Send HTTP request to the server*/
			HttpResponse response = client.execute(httpget);
			
			/*The server has responded. Parse the result*/
			int statusCode = response.getStatusLine().getStatusCode();
			if(statusCode != 200){
				System.out.println("Cannot download symbol " + symbol);
				return 0;
			}
			BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			String line;
			StringBuffer result = new StringBuffer();
			//Skip the header
			line = rd.readLine();
			bw.write("Date\tOpen\tHigh\tLow\tClose\tVolume");
			
			String reformatedLine;
			while((line = rd.readLine())!= null){
				reformatedLine = reFormatDataLine(line);
				if(reformatedLine == null){
					System.out.println("There is a error when reading data line\n " +
							"Detail line:" + line);
					return 0;
				}
				bw.newLine();
				bw.write(reformatedLine);
				length++;
			}
			bw.close();
			return length;
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
	}
	
	/*Parse configuration values from the config file*/
	private static void readConfig(){
		File f = new File(CONFIG_FILE);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			String line;
			while((line = br.readLine()) != null){
				if(line.length() == 0 || line.startsWith("//")){
					continue;
				}
				
				
				String[] fields = line.split("=");
				//indexing
				if(fields[0].compareTo("OUT_DIR") == 0){
					OUT_DIR = fields[1];
				}
				else if(fields[0].compareTo("SYMBOL_LIST_FILE") == 0){
					SYMBOL_LIST_FILE = fields[1];
				}
				else if(fields[0].compareTo("WRONG_SYMBOL_LIST") == 0){
					WRONG_SYMBOL_LIST = fields[1];
				}
				else if(fields[0].compareTo("TRACK_SYMBOL_LIST") == 0){
					TRACK_SYMBOL_LIST = fields[1];
				}
				else if(fields[0].compareTo("FROM_DATE") == 0){
					FROM_DATE = fields[1];
				}
				else if(fields[0].compareTo("TO_DATE") == 0){
					TO_DATE = fields[1];
				}
				else if(fields[0].compareTo("TIME_MODE") == 0){
					TIME_MODE = fields[1];
				}
				else if(fields[0].compareTo("HOST_URL") == 0){
					HOST_URL = fields[1];
				}
				else if(fields[0].compareTo("HOST_SCHEME") == 0){
					HOST_SCHEME = fields[1];
				}
				else if(fields[0].compareTo("HOST_CRUMB") == 0){
					HOST_CRUMB = fields[1];
				}
				else if(fields[0].compareTo("HOST_PATH") == 0){
					HOST_PATH = fields[1];
				}
				else if(fields[0].compareTo("HOST_INTERVAL") == 0){
					HOST_INTERVAL = fields[1];
				}
				else if(fields[0].compareTo("HOST_EVENT") == 0){
					HOST_EVENT = fields[1];
				}
			}//end while
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Reformat the input string to our format
	 * input: input string with format
	 * Date,Open,High,Low,Close,Volume,Adj Close
	 * Ex: 2013-09-23,31.03,31.03,30.02,30.26,15728900,30.26
	 * 
	 * Return: our format string
	 * 		time /t		startPrice	/t	highestPrice /t lowestPrice /t endPrice /t transactionCount
	 * Ex: 20130923 	31.03			31.03			30.02			30.26		15728900
	 * */
	private static String reFormatDataLine(String input){
		String line = "";
		
		String[] fields = input.split(",");
		//date
		line = fields[0].replaceAll("-","");
		for(int i = 1; i < 6; i++){
			line += "\t" + fields[i];
		}
		return line;
	}
}
