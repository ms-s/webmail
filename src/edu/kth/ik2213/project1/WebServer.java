package edu.kth.ik2213.project1;

import java.io.BufferedReader;
import edu.kth.ik2213.project1.SMTPClient;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * WebServer.java
 * 
 * HTTP Server
 * 
 * @author Mohit Sethi
 */

public class WebServer {
	
	public static ArrayList<EmailMessage> messages;
	public static BufferedReader reader;
	public static BufferedWriter writer;
	public static Socket socket;
	public static int messageId;
	
	public static void updateStatus(int id, String status) {
		messages.get(id).setStatus(status);
	}
	
	public static void sendFail(String message) throws IOException{
		String httpResponse = "";
		httpResponse += "HTTP/1.1 301 Moved Permanently\r\n";
		httpResponse += "Location: /failure.html\r\n";
		File f = new File("../html/" + "failure.html");
		FileWriter fwriter=new FileWriter(f);
		fwriter.write("<html><head><title>Delivery Failure</title><body>Delivery Failure: " + message + "<br /><a href=\"form.html\">Back</a></body></html>");
		fwriter.flush();
		fwriter.close();
		httpResponse += "Content-Type: text/html;charset=iso-8859-15\r\n";
		httpResponse += "\r\n";
		sendResponse(httpResponse);
	}
	
	public static void sendResponse(String response) {
		try {
			writer.write(response);
			writer.flush();
			writer.close();
		} 
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void processRequest(String request) {
		BufferedReader fileReader = null;
		String httpResponse = "";
		String filename = "";
		
		try {
			StringTokenizer tokenizer = new StringTokenizer(request);
			String requestType = "";
			if(tokenizer.hasMoreTokens()) {
				requestType = tokenizer.nextToken();
			}
			else {
				sendmalformedhttp();
				return;
			}
			
			if(requestType.equals("GET")) {
				if(!tokenizer.hasMoreTokens()) {
					filename = "/";
				}
				else {
					filename = tokenizer.nextToken();
				}
				
				if(filename.equals("/")) {
					filename += "form.html";
				}
				
				File f = new File("../html/" + filename.substring(1));
				
				if(f.exists()) {
					httpResponse += "HTTP/1.1 200 OK\r\n";
					httpResponse += "Content-Type: text/html;charset=utf-8\r\n";
					httpResponse += "\r\n";
					
					// Update status page
					if(filename.equals("/status.html")) {
						String statusEntry = "";
						   statusEntry += "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" /><title>Status Page</title></head><body>";
						   statusEntry += "<a href=\"form.html\">Back</a> <a href=\"status.html\">Refresh</a><br />";
						   statusEntry += "<table border=\"1\" empty-cells=\"show\"><tr><td>To</td><td>From</td><td>Subject</td><td>Status</td><td>Submitted Time</td><td>Delivered Time</td></tr>";
						   
						   for(int i = 0; i < messages.size(); i++) {
							   boolean pending = false;
							   
							   statusEntry+="<tr>";
							   statusEntry += "<td>";
							   statusEntry += messages.get(i).getTo() + "</td> ";
							   statusEntry += "<td>";
							   statusEntry += messages.get(i).getFrom() + "</td> ";
							   statusEntry += "<td>";
							   statusEntry += messages.get(i).getSubject() + "</td> ";
							   statusEntry += "<td>";
							   String status = messages.get(i).getStatus();
							   if(status.equals("Pending"))
								   pending = true;
							   statusEntry += status + "</td> ";
							   statusEntry += "<td>";
							   statusEntry += messages.get(i).getSubmitTime() + "</td> ";
							   statusEntry += "<td>";
							   if(pending)
								   statusEntry += "Pending</td>";
							   else{ 
								   if(messages.get(i).getDeliveryTime()==null) {
									   messages.get(i).setDeliveryTime("Failed</td>");
								   }
								   
								   statusEntry += messages.get(i).getDeliveryTime() + "</td>\n";
							   }
							   statusEntry+="</tr>";
						   }
						   statusEntry+="</table></body></html>";
						   File f2=new File("../html/" + "status.html");
						   FileWriter fwriter=new FileWriter(f2);
						   fwriter.write(statusEntry);
						   fwriter.flush();
						   fwriter.close();
					}
					
					String filecontents="";
					fileReader = new BufferedReader(new FileReader(f));
					String line = "";
					while((line = fileReader.readLine()) != null) {
						filecontents += line;
					}
					httpResponse += filecontents + "\r\n";
				}
				else {
					httpResponse += "HTTP/1.1 404 Not Found\r\n";
					httpResponse += "Content-Type: text/html;charset=iso-8859-15\r\n";
					httpResponse += "\r\n";
					httpResponse += "<html><body>Page not found (Error 404)</html></body>\r\n";
				}
			}
			else if(requestType.equals("POST")) {
			   String line="";
			   boolean lengthFound = false;
			   int length = 0;
			   
			   // Pass through the headers
			   try {
				   line = reader.readLine();
				   while(line != null && !line.equals("")){
					   line = reader.readLine();
					   if(line.contains("Content-Length")) {
						   lengthFound = true;
						   try {
							   length = Integer.parseInt(line.substring(line.indexOf(" ")+1));
						   }
						   catch(NumberFormatException e) {
							   length = -1;
						   }
					   }
				   }
			   }
			   catch(InterruptedIOException e) {
				   length = -1;
			   }
			   
			   if(!lengthFound || length < 0) {
				   // Invalid request
				   httpResponse += "HTTP/1.1 400 Bad Request\r\n";
				   httpResponse += "Content-Type: text/html;charset=iso-8859-15\r\n";
				   httpResponse += "\r\n";
				   httpResponse += "<html><body>Bad Request (Error 400)</body></html>\r\n";
			   }
			   else {
				   char content[]=new char[length];
				   reader.read(content);
				   String data=new String(content);
				   data=URLDecoder.decode(data, "ISO-8859-15");
				   
				   String from="";
				   String to="";
				   String smtpserver="";
				   String subject="";
				   String message="";
				   String delay="";
				   
				   if(!data.startsWith("from=")) {
				   	   sendmalformedhttp();
				   	   return;
				   }
				   
				   int fromIndex = data.indexOf("from=");
				   int ampIndex = data.indexOf("&");
				   if(fromIndex == -1 || ampIndex == -1) {
					   sendmalformedhttp();
				   	   return;
				   }
				   
				   from=data.substring(fromIndex+5, ampIndex);   
				   
				   if(data.indexOf("&") == -1) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   data=data.substring(data.indexOf("&")+1,data.length());
				   
				   if(!data.startsWith("to=")) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   if(data.indexOf("to=")==-1 || data.indexOf("&")==-1) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   to=data.substring(data.indexOf("to=")+3, data.indexOf("&"));
				   
				   if(data.indexOf("&") == -1) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   data=data.substring(data.indexOf("&")+1,data.length());
				   
				   if(!data.startsWith("subject=")) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   if(data.indexOf("subject=")==-1 || data.indexOf("&")==-1) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   subject=data.substring(data.indexOf("subject=")+8, data.indexOf("&"));
				   
				   if(data.indexOf("&") == -1) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   data=data.substring(data.indexOf("&")+1,data.length());
				   
				   if(!data.startsWith("smtpserver=")) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   if(data.indexOf("smtpserver=")==-1 || data.indexOf("&")==-1) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   smtpserver=data.substring(data.indexOf("smtpserver=")+11, data.indexOf("&"));
				   
				   if(data.indexOf("&") == -1) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   data=data.substring(data.indexOf("&")+1,data.length());
				   
				   if(!data.startsWith("message=")) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   if(data.indexOf("message=")==-1 || data.indexOf("&")==-1){
					   sendmalformedhttp();
					   return;
				   }
				   
				   message=data.substring(data.indexOf("message=")+8, data.indexOf("&"));
				   
				   if(data.indexOf("&") == -1){
					   sendmalformedhttp();
					   return;
				   }
				   
				   data=data.substring(data.indexOf("&")+1,data.length());
				   
				   if(!data.startsWith("delay=")) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   if(data.indexOf("delay=")==-1 ) {
					   sendmalformedhttp();
					   return;
				   }
				   
				   delay=data.substring(data.indexOf("delay=")+6, data.length());
				   
				   if(data.indexOf("&")!=-1) {
					   sendmalformedhttp();
					   return;		  
				   }
				   
				   SMTPClient mailclient=new SMTPClient();
				   String mailStatus;
				   int d = 0;
				   
				   if(!delay.equals("")) {
					   try {
						   d = Integer.parseInt(delay);
					   }
					   catch(NumberFormatException nfe) {
						   // Ignore invalid delays - just send right away
					   }
				   }
				   
				   // Validate to and from addresses
					if(to.equals("") || from.equals("")) {
						sendFail("Both TO and FROM addresses must be specified");
						return;
					}
					
					// One and only one '@' symbol
					if(!to.contains("@") || to.indexOf("@") != to.lastIndexOf("@") || !to.contains(".")){
						sendFail("Invalid TO address");
						return;
					}
					
					// One and only one '@' symbol
					if(!from.contains("@") || from.indexOf("@") != from.lastIndexOf("@") || !to.contains(".")){
						sendFail("Invalid FROM address");
						return;
					}
					
				   if(d > 0) {
					   EmailMessage m = new EmailMessage(messageId, to, from, subject, smtpserver, "Pending", message);
					   messages.add(m);
					   messageId++;
					   mailclient.sendMail(m, Integer.parseInt(delay));
					   
					   httpResponse += "HTTP/1.1 301 Moved Permanently\r\n";
					   httpResponse += "Location: /status.html\r\n";
					   httpResponse += "Content-Type: text/html;charset=iso-8859-15\r\n";
					   httpResponse += "\r\n";
				   }
				   else {
					   EmailMessage m = new EmailMessage(messageId, to, from, subject, smtpserver, "Pending", message);
					   messages.add(m);
					   messageId++;
					   mailStatus = mailclient.sendMail(m);
					   updateStatus(m.getId(), mailStatus);
					   
					   httpResponse += "HTTP/1.1 301 Moved Permanently\r\n";
					   if(mailStatus.equals("Success")) {
						   httpResponse += "Location: /success.html\r\n";
					   }
					   else {
						   httpResponse += "Location: /failure.html\r\n";
						   File f = new File("../html/" + "failure.html");
						   FileWriter fwriter=new FileWriter(f);
						   fwriter.write("<html><head><title>Delivery Failure</title><body>Delivery Failure: " + mailStatus + "<br /><a href=\"form.html\">Back</a></body></html>");
						   fwriter.flush();
						   fwriter.close();
					   }
					   httpResponse += "Content-Type: text/html;charset=iso-8859-15\r\n";
					   httpResponse += "\r\n";
				   }
			   }
			}
			else {
				// Invalid request
				httpResponse += "HTTP/1.1 400 Bad Request\r\n";
				httpResponse += "Content-Type: text/html;charset=iso-8859-15\r\n";
				httpResponse += "\r\n";
				httpResponse += "<html><body>Bad Request (Error 400)</body></html>\r\n";
			}
			
			sendResponse(httpResponse);
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	
	private static void sendmalformedhttp() {
		// Returning Malformed Status message
		String malformedHttpResponse="";
		malformedHttpResponse += "HTTP/1.1 400 Bad Request\r\n";
		malformedHttpResponse += "Content-Type: text/html;charset=iso-8859-15\r\n";
		malformedHttpResponse += "\r\n";
		malformedHttpResponse += "<html><body>Bad Request (Error 400)</body></html>\r\n";
		sendResponse(malformedHttpResponse);
	}

	public WebServer(int port) {
		messages = new ArrayList<EmailMessage>();
		
		try {
			
			ServerSocket server = new ServerSocket(port);
			
			for(;;) {
				socket = server.accept();
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				socket.setSoTimeout(1000); // Time out read()s after 1 second
				processRequest(reader.readLine());		
			}
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	public void stop() {
		try {
			socket.close();
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void main(String[] args) { 
		new WebServer(8080);
	}
}
