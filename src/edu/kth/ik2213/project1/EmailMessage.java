package edu.kth.ik2213.project1;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An email message
 * 
 * @author Mohit Sethi
 *
 */
public class EmailMessage {

	private String to;
	private String from;
	private String status;
	private String subject;
	private String server;
	private String data;
	private int id;
	private String submitTime;
	private String deliveryTime;
	
	public EmailMessage(int id, String to, String from, String subject, String server, String status, String data) {
		this.to = to;
		this.from = from;
		this.subject = subject;
		this.status = status;
		this.id = id;
		this.server = server;
		this.setData(data);
		
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		Date d = new Date();
		this.submitTime = sdf.format(d);
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return status;
	}
	
	public void setFrom(String from) {
		this.from = from;
	}

	public String getFrom() {
		return from;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getTo() {
		return to;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getSubject() {
		return subject;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getServer() {
		return server;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}

	public void setSubmitTime(String submitTime) {
		this.submitTime = submitTime;
	}

	public String getSubmitTime() {
		return submitTime;
	}

	public void setDeliveryTime(String deliveryTime) {
		this.deliveryTime = deliveryTime;
	}

	public String getDeliveryTime() {
		return deliveryTime;
	}
}
