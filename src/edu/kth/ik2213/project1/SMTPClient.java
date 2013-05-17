package edu.kth.ik2213.project1;

import java.io.BufferedReader;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class handles sending email via SMTP
 * 
 * @author Mohit Sethi
 *
 */

public class SMTPClient {

	
	private static final int SMTP_PORT = 25;
	private static final int TIMEOUT = 2000;
	
	BufferedReader reader = null;
	BufferedWriter writer = null;
	Socket socket = null;
	
	public SMTPClient() {
		
	}
	
	// Sends a message and returns the response
	public String sendMessage(String message) {
		String response = "";
		try {
			writer.write(message);
			writer.flush();
		}
		catch(IOException e) {
			System.out.println("Error sending message to server: " + e.getMessage());
		}
		
		try {
			response = reader.readLine();
		}
		catch(IOException e) {
			System.out.println("Error receiving server response: " + e.getMessage());
		}
		
		return response;
	}
	
	public void sendMessageWithoutResponse(String message) {
		try {
			writer.write(message);
			writer.flush();
		}
		catch(IOException e) {
			System.out.println("Error sending message to server: " + e.getMessage());
		}
	}
	
	public void receiveMessage() {
		
	}
	
	/**
	 * Sends an email and returns a status message
	 */
	public String sendMail(EmailMessage message) {
		String to = message.getTo();
		String from = message.getFrom();
		String subject = message.getSubject();
		String server = message.getServer();
		String data = message.getData();
		
		int code;
		
		if(subject.equals("")) {
			subject = "(No Subject)";
			message.setSubject(subject); // For response
		}
		
		// If SMTP server is left blank, use DNS MX lookup
		if(server.equals("")) {
			String domain = getDomainFromAddress(to);
			server = DNSClient.mxLookup(domain);
		
			// Return an error if no MX record exists
			if(server.equals("Unknown")) {
				return "SMTP server not entered, and could not determine SMTP server for recipient's domain";
			}
		}
		
		// Set up socket connection
		try {
			socket = new Socket();
			
			SocketAddress address;
			address = new InetSocketAddress(server, SMTP_PORT);
			
			socket.connect(address, TIMEOUT);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		}
		catch(SocketTimeoutException e) {
			return "Connection to SMTP server timed out";
		}
		catch(IOException e1) {
			return "Connection to SMTP server unsuccessful";
		}
		
		// Check for 220 from server
		String line = "";
		try {
			line = reader.readLine();
		}
		catch(IOException e) {
			close();
			return "Connection to SMTP server unsuccessful";
		}
		
		code = getCode(line);
		if(code != 220) {
			close();
			return "Connection to SMTP server unsuccessful (Error " + Integer.toString(code) + ")";
		}
		
		// Client opens connection to server and server responds with opening message
		String helo = "HELO test.domain\r\n";
		String heloResponse = sendMessage(helo);
		
		code = getCode(heloResponse);
		if(code != 250) {
			close();
			return "Connection to SMTP server unsuccessful (Error " + Integer.toString(code) + ")";
		}
		
		String mailFrom = "MAIL FROM:<" + from + ">\r\n";
		String mailFromResponse = sendMessage(mailFrom);
		
		code = getCode(mailFromResponse);
		if(code != 250) {
			close();
			return "Error sending mail (Error " + Integer.toString(code) + ")";
		}
		
		String rcptTo = "RCPT TO:<" + to + ">\r\n";
		String rcptToResponse = sendMessage(rcptTo);
		
		code = getCode(rcptToResponse);
		if(code != 250) {
			close();
			return "Error sending mail (Error " + Integer.toString(code) + ")";
		}
		
		String dataStr = "DATA\r\n";
		String dataResponse = sendMessage(dataStr);
		
		code = getCode(dataResponse);
		if(code != 354) {
			close();
			return "Error sending mail (Error " + Integer.toString(code) + ")";
		}
		
		String subjectMsg = "Subject: " + toRFC2047(subject) + "\r\n";
		sendMessageWithoutResponse(subjectMsg);
		
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		Date d = new Date();
		String dateMsg = "Date: " + sdf.format(d) + "\r\n";
		message.setDeliveryTime(sdf.format(d));
		sendMessageWithoutResponse(dateMsg);
		
		String toMsg = "To: " + to + "\r\n";
		sendMessageWithoutResponse(toMsg);
		
		String fromMsg = "From: " + from + "\r\n";
		sendMessageWithoutResponse(fromMsg);
		
		String mimeVersion = "MIME-Version: 1.0\r\n";
		sendMessageWithoutResponse(mimeVersion);
		
		String contentType = "Content-Type: text/plain; charset=ISO-8859-15\r\n";
		sendMessageWithoutResponse(contentType);
		
		String cte = "Content-Transfer-Encoding: quoted-printable\r\n";
		sendMessageWithoutResponse(cte);
		
		String blankMsg = "\r\n";
		sendMessageWithoutResponse(blankMsg);
		
		if(data.startsWith(".")) {
			data = data.replaceFirst(".", "..");
		}
		if(data.equals(".")) {
			data = data.replace(".", "..");
		}
		if(data.contains("\n.")) {
			data = data.replace("\n.", "\n..");
		}
		
		String dataMsg = toQuotedPrintable(data) + "\r\n";
		sendMessageWithoutResponse(dataMsg);
		
		String periodMsg = ".\r\n";
		String periodResponse = sendMessage(periodMsg);
		
		code = getCode(periodResponse);
		if(code != 250) {
			close();
			return "Error sending mail (Error " + Integer.toString(code) + ")";
		}
		
		String quit = "QUIT\r\n";
		String quitResponse = sendMessage(quit);
		
		if(getCode(quitResponse) != 221) {
			close();
			return "Error disconnecting from SMTP server (Error " + Integer.toString(code) + ")";
		}
		
		close();
		
		return "Success";
	}
	
	private String getDomainFromAddress(String to) {
		// Get domain name of recipient
		int atIndex = to.indexOf('@');
		return to.substring(atIndex + 1);
	}

	public void close() {
		try {
			reader.close();
			writer.close();
			socket.close();
		}
		catch(IOException e) {
			System.out.println("Error closing: " + e.getMessage());
		}
	}
	
	public void sendMail(final EmailMessage message, final int delay) {
		int seconds=delay*1000;
		Timer t = new javax.swing.Timer(seconds, new ActionListener() {
	          public void actionPerformed(ActionEvent e) {
	           SMTPClient myclient=new SMTPClient();
	           String status = myclient.sendMail(message);
	           WebServer.updateStatus(message.getId(), status);
	           String senderServer = DNSClient.mxLookup(getDomainFromAddress(message.getFrom()));
	           if(senderServer.equals("Unknown")) {
	        	   // We can't send a response because the DNS lookup failed
	           }
	           else {
	        	   EmailMessage reply = new EmailMessage(-1, message.getFrom(), "noreply@ik2213.lab", "Your email: " + message.getSubject(), senderServer, "NotLogged", "The status of your email is: " + status);
	        	   myclient.sendMail(reply);
	           }
	          }
	       });
		t.setRepeats(false);
		t.start();
	}
	
	private int getCode(String message) {
		return Integer.parseInt(message.substring(0, 3));
	}
	
	public String toRFC2047(String message) {
		StringBuffer encoded = new StringBuffer(message.length());
		encoded.append("=?ISO-8859-15?Q?");
		int lineCounter = 16;
		
		for(int i = 0; i < message.length(); i++) {	
			char c = message.charAt(i);
			
			// #1: CRLF
			if(c == '\r') {
				if(i != message.length() - 1) {
					if(message.charAt(i+1) == '\n') {
						// CRLF detected
						i++;
						encoded.append("\r\n");
						lineCounter = 0;
					}
				}
			}
			
			// Printable ASCII chars don't need to be encoded
			else if( ((c >= 33 && c <= 60) || (c >= 62 && c <= 126)) && (c != '?' && c != '=' && c != '_')) {
				encoded.append(c);
				lineCounter++;
			}
			
			// Always encode space/tab characters
			else if((c == 9 || c == 32)) {
				encoded.append('=');
				encoded.append(toHexString(c));
				lineCounter += 3;
			}
			
			// #4: Line breaks in the text body
			else if(c == '\n') {
				encoded.append("\r\n");
			}
			
			else {
				encoded.append('=');
				encoded.append(toHexString(c));
				lineCounter += 3;
			}
			
			// End encoded words at 70 chars (limit is 75)
			if(lineCounter > 69) {
				encoded.append("?=" + "=?ISO-8859-15?Q?");
				lineCounter = 16;
			}
		}
		
		encoded.append("?=");
		
		return encoded.toString();
	}
	
	public String toQuotedPrintable(String message) {
		
		StringBuffer encoded = new StringBuffer(message.length());
		int lineCounter = 0;
		
		for(int i = 0; i < message.length(); i++) {	
			char c = message.charAt(i);
			
			// #1: CRLF
			if(c == '\r') {
				if(i != message.length() - 1) {
					if(message.charAt(i+1) == '\n') {
						// CRLF detected
						i++;
						encoded.append("\r\n");
						lineCounter = 0;
					}
				}
			}
			
			// #2: Printable ASCII chars
			else if((c >= 33 && c <= 60) || (c >= 62 && c <= 126)) {
				encoded.append(c);
				lineCounter++;
			}
			
			// #3: Space/Tab characters
			else if((c == 9 || c == 32)) {
				if(i != message.length() - 1) {
					char nextChar = message.charAt(i+1);
					if(nextChar != '\r' && nextChar != '\n') {
						encoded.append(c);
						lineCounter++;
					}
					else {
						// Need to encode the space/tab
						encoded.append('=');
						encoded.append(toHexString(c));
						lineCounter += 3;
					}
				}
			}
			
			// #4: Line breaks in the text body
			else if(c == '\n') {
				encoded.append("\r\n");
			}
			
			else {
				encoded.append('=');
				encoded.append(toHexString(c));
				lineCounter += 3;
			}
			
			// #5: Max 76 chars per line, but we'll be safe and cut it off at 72
			if(lineCounter > 70) {
				encoded.append("=\r\n");
				lineCounter = 0;
			}
		}
		
		return encoded.toString();
	}
	
	private String toHexString(char c) {
		String hexString = Integer.toHexString((int)c).toUpperCase();
		if(hexString.length()==1) {
			hexString = "0" + hexString;
		}
		return hexString;
	}
}
