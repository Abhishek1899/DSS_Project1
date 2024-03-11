package org.example.test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import com.google.gson.Gson;

import kotlin.jvm.Synchronized;
import kotlin.time.TimedValue;


import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.RequestLog.Collection;




class WebServletTest {
	public class profilingPerformance{
		public List<Long> latencies;
		
		public profilingPerformance(){
			latencies=new ArrayList<Long>();
		}
		public void addLatency(long latency){
			latencies.add(latency);
		}
		public double getMean(){
			long totalTime=0;
			for(Long latency:latencies){
				totalTime+=latency;
			}
			return totalTime/latencies.size();
		}
		public double getMedian(){
			latencies.sort(Comparator.naturalOrder());
			int length=latencies.size();
			if (length % 2 == 0){
		
				return ((double)latencies.get(length/2) + (double)latencies.get(length/2 -1))/2;
			}
			else{
				
				return (double)latencies.get(length/2);
			}
			
		}
		public long getPercentile(){
			int index=(int) Math.ceil(0.99 * latencies.size());
			long p99=latencies.get(index-1);
			return p99;
		}
	}
	
	public class requestCounter{
		private int origin;
		private int bound;
		private int count;
		public requestCounter(int start,int end){
			this.origin=start;
			this.bound=end;
			this.count=0;
		}
		public int getValue(){
			return count;
		}
		public void decrement(){
			count--;
		}
		public void increment(){
			count++;
		}
		public int getBound(){
			return bound;
		}

	}
	public class requestStats{
		private long wallTime;

		public requestStats(){
			wallTime=0;
		}
		public void setWallTime(long time){
			wallTime+=time;
		}
		public long getWallTime(){
			return wallTime;
		}
		
	}
	public class multithreadedPost{
		private int totalClients;
		private int minConnectionsPerClient;
		private int maxConnectionsPerClient;
		private requestCounter nReq;
		private requestStats stats=new requestStats();
		private boolean createCSV;
		private FileWriter writer;
		private String fileName="post.csv";
		private profilingPerformance pperf;

		public multithreadedPost(int totalClients,int minCon,int maxCon,requestCounter nReq,boolean createCSV){
			this.totalClients=totalClients;
			this.minConnectionsPerClient=minCon;
			this.maxConnectionsPerClient=maxCon;
			this.nReq=nReq;
			this.createCSV=createCSV;
			pperf=new profilingPerformance();
			try{
				if (createCSV){
					writer=new FileWriter(fileName);
					writer.append("Start Time, Request Type, Latency, Response Code\n");
				}
			}catch (IOException e){
				e.printStackTrace();
			}
			
		}
		
		
		public void createMultipleClients() throws Exception{
		
			httpClient[] clients=new httpClient[this.totalClients];
			for(int i=0;i<totalClients;i++){
				clients[i]=new httpClient(i,minConnectionsPerClient,maxConnectionsPerClient,nReq,createCSV,writer,pperf);
			}
			long startTime=System.currentTimeMillis();
			for(httpClient client:clients){
				client.start();
			}
			
			for(httpClient client:clients){
				client.join();
			}
			
			stats.setWallTime(System.currentTimeMillis()-startTime);
			int sum=0;
			for(httpClient client:clients){
				// System.out.println("Client :"+j+" Completed: "+clients[j].totalRequestsExecuted);
				sum+=client.failed.size();
			}
			System.out.println("Failed Threads: "+sum);
			
			//############ Performance Measure ###################
			System.out.printf("Wall Time %.4f sec%n",(float)stats.getWallTime()/1000);
			int r=nReq.getValue();
			System.out.printf("Throughput %.4f r/sec%n",(float)((float)r/stats.getWallTime() * 1000));
			System.out.println("Total Number of Requests "+r);

			//############### Profiling Performance ###############//
			if (createCSV){
			
			//Meand Time
			System.out.println("Mean Time "+pperf.getMean()+" ms");

			//Median Time
			System.out.println("Median Time "+pperf.getMedian());

			//99th Percentile
			System.out.println("99th Percentile "+pperf.getPercentile()+" ms");

			//Min-Max
			System.out.println("Min Response Time "+Collections.min(pperf.latencies)+ " ms");
			System.out.println("Max Response Time "+Collections.max(pperf.latencies)+" ms");
			}
			


		}
	}
	
	public class httpClient extends Thread{
		private PoolingHttpClientConnectionManager httpCallablePool;
		private URI uri;
		private ConnectionKeepAliveStrategy myStrategy;
		private List<Integer> failed;
		private int minCon;
		private int maxCon;
		private int clientIdx;
		private requestCounter nReq;
		public int totalRequestsExecuted;
		private boolean createCSV;
		private FileWriter writer;
		private profilingPerformance pperf;
		
		
		
		
		
		

		public httpClient(int idx,int minCon,int maxCon,requestCounter nReq,boolean createCSV,FileWriter writer,profilingPerformance pperf) throws URISyntaxException{
			httpCallablePool = new PoolingHttpClientConnectionManager();
			httpCallablePool.setMaxTotal(minCon);
			httpCallablePool.setDefaultMaxPerRoute(minCon);// Since we have only one root
			this.nReq=nReq;		
			this.minCon=minCon;
			this.maxCon=maxCon;
			this.writer=writer;
			this.pperf=pperf;
			clientIdx=idx;
			totalRequestsExecuted=0;
			this.createCSV=createCSV;
			failed=new ArrayList<Integer>()	;
			uri=new URIBuilder()
			.setScheme("http")
			.setHost("localhost:8080/skiresorts")
			.setPath("/Skiers")
			.build();

			myStrategy=new
			ConnectionKeepAliveStrategy(){
				@Override
				public long getKeepAliveDuration(HttpResponse response,
				HttpContext context){
					// Time to keep the connection Alive to reuse.
						return 1000;
					
				}
			};

		}
		@Override
		public void run(){
			CloseableHttpClient httpClient=HttpClients.custom()
			.setKeepAliveStrategy(myStrategy)
			.setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy() {
				@Override
				public boolean retryRequest(
					final HttpResponse response,final int executionCount, final HttpContext context
				){
					int statusCode=response.getStatusLine().getStatusCode();
					return (statusCode >= 400 || statusCode >= 500) && executionCount < 5;
				}
				@Override
				public long getRetryInterval(){
					return 0;
				}
			})
			.setConnectionManager(httpCallablePool).build();
			
			
			try{
			HttpThread[] httpPosts=new HttpThread[this.maxCon];
			for(int i=0;i<httpPosts.length;i++){
				httpPosts[i]=new HttpThread(httpClient,uri,createCSV,writer,pperf);
			}
			
			for(HttpThread httpPost:httpPosts){
				httpPost.start();

				synchronized(nReq){
					if (nReq.getValue() < nReq.getBound()){
						nReq.increment();
					}
					else{
						break;
					}
				}
			}
			
			for(int j=0;j<httpPosts.length;j++){
				if (httpPosts[j].getState() == Thread.State.NEW){
					totalRequestsExecuted=j-1;
					break;
				}
				
				httpPosts[j].join();
				
				if (!(httpPosts[j].status >= 200 && httpPosts[j].status < 300)){
					this.failed.add(j);// add thread index to the failed list of client
				}
				else{
					// System.out.println("Client: "+clientIdx+", Thread: "+j+", Done");
				}
				

		}
			httpClient.close();
			
			
			
			
			}catch (Exception e){
				e.printStackTrace();
			}

		
	}
}
	public class HttpThread extends Thread{
		private URI uri;
		private CloseableHttpClient client;
		private Gson gson;
		public Integer status;
		private boolean createCSV;
		private FileWriter writer;
		private profilingPerformance pperf;

		public HttpThread(CloseableHttpClient client,URI uri,boolean createCSV,FileWriter writer,profilingPerformance pperf){
			this.uri=uri;
			this.client=client;
			this.createCSV=createCSV;
			this.writer=writer;
			this.pperf=pperf;
			gson=new Gson();
		}
		
		@Override
		public void run(){
			// Each thread will generate its own data
			SkierLiftRideEvent data=new SkierLiftRideEvent(2022,1);
			try{
			StringEntity body=new StringEntity(gson.toJson(data));
		
			HttpPost request=new HttpPost(this.uri);
			request.setEntity(body);

			long st=System.currentTimeMillis();
			LocalTime lst=LocalTime.now();

			CloseableHttpResponse response=this.client.execute(request);

			long latency=System.currentTimeMillis() - st;
			this.status=response.getStatusLine().getStatusCode();
			if(createCSV){
				synchronized(writer){

					writer.append(String.valueOf(lst)+","+"POST"+","+String.valueOf(latency)+
					","+String.valueOf(this.status)+"\n"
					);
					pperf.addLatency(latency);
				}
				
			}
			
			try{
				HttpEntity entity=response.getEntity();

					if (entity !=null){
						EntityUtils.consume(entity);
					}
				}finally{
					response.close();
				}

				
			}catch(Exception e){
				System.out.println("Thread Failed: "+ e);
				// return 0;
			}
		}

	}
	public class SkierLiftRideEvent{
		private int skierID;
		private int resortID;
		private int liftID;
		private int seasonID;
		private int dayID;
		private int time;

		public SkierLiftRideEvent(int seasonID,int dayID){
			this.seasonID=seasonID;
			this.dayID=dayID;

			Random rand=new Random();
			this.skierID=rand.nextInt(1,100000);
			this.resortID=rand.nextInt(1,10);
			this.liftID=rand.nextInt(1,40);
			this.time=rand.nextInt(1,360);
		}
		
	}


	public class MyException extends Exception{
		public MyException(String message,Throwable err){
			super(message,err);
		}
	}
	ResponseHandler<String> responseHandler=new ResponseHandler<String>(){
		@Override
		public String handleResponse(org.apache.http.HttpResponse response)
				throws ClientProtocolException, IOException {
			int status=response.getStatusLine().getStatusCode();
			if (status >= 200 && status < 300){
				HttpEntity entity=response.getEntity();
				return entity!=null? EntityUtils.toString(entity):null;
			}
			else{
				HttpEntity entity=response.getEntity();
				String message=EntityUtils.toString(entity);
				
				return message+ "Status: "+status;
				
			}
		}
	};
	
	@Test
	void testSkiersGet() throws Exception {
		
		URI uri=new URIBuilder()
		.setScheme("http")
		.setHost("localhost:8080/skiresorts")
		.setPath("/Skiers")
		.build();

		HttpGet request=new HttpGet(uri);
		CloseableHttpClient client=HttpClients.createDefault();
		//--------------- Response Handler------------------
		// long s=System.currentTimeMillis();
		String response=client.execute(request,responseHandler);
		// long e=System.currentTimeMillis();
		// long w=e-s;
		// System.out.printf("%.4f%n",(float)e-s);
		// System.out.printf("%.4f%n",(float)w/1000);
		// float r=5000;
		
		// System.out.printf("%.4f",(float)(r/4408 * 1000));
		
        
	    System.out.println("Get Response "+response);
		
		client.close();

	}
	

	
	@SuppressWarnings("deprecation")
	@Test
	void testSkiersPost() throws Exception {
		requestCounter noRequests=new requestCounter(0,10000);
		multithreadedPost mp=new multithreadedPost(15,5,1000,noRequests,true);
		mp.createMultipleClients();
		
		
	}

	@Test
	void testSkiersPostLatency() throws Exception
	{
		System.out.println("######## Testing Latency #########");
		requestCounter noRequests=new requestCounter(0,500);
		multithreadedPost mp=new multithreadedPost(1,1,502,noRequests,false);
		mp.createMultipleClients();
		float timePerRequest=(float)(mp.stats.getWallTime()/(float)noRequests.getValue());
		System.out.printf("Time for each request %.4f ms%n",timePerRequest);
		System.out.printf("Number of Workers required for %d requests, by Little's Law: %f%n",10000,(10000 * timePerRequest)/1000);
		
		requestCounter noRequests2=new requestCounter(0,10000);
		multithreadedPost mp2=new multithreadedPost(4,4,3000,noRequests2,false);
		mp2.createMultipleClients();
		float timePerRequest2=(float)(mp2.stats.getWallTime()/(float)noRequests2.getValue());
		System.out.printf("Time taken by running %d requests with %d threads %.4f ms%n",noRequests2.getValue(),16,timePerRequest2);

	}
	// @Test
	// void registerPostPerformance() throws Exception{
	// 	requestCounter noRequests=new requestCounter(0,100);
	// 	multithreadedPost mp=new multithreadedPost(5,5,100,noRequests,false);
	// 	mp.createMultipleClients();
	// }


}









