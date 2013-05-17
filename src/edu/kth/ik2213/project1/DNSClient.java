package edu.kth.ik2213.project1;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * Performs DNS Lookups
 * 
 * @author Mohit Sethi
 */
public class DNSClient {
	
	public DNSClient() {
		
	}
	
	public static String mxLookup(String domain) {
		String address = "";
		NamingEnumeration<?> values = null;
		
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		
		try {
				DirContext ictx = new InitialDirContext(env);
				Attributes a = ictx.getAttributes(domain, new String[] { "MX" });
				NamingEnumeration<?> all = a.getAll();
				
				if(all.hasMore()) {
					Attribute attr = (Attribute)all.next();
					values = attr.getAll(); 
					if(values.hasMore()) {
						address = (String)values.next();
						// Strip leading zero
						address = address.substring(2);
						// Stip trailing period
						address = address.substring(0, address.length() - 1);
					}
			}
		}
		catch(NamingException ne) {
			return "Unknown";
		}
		
		String ip = "Unknown";
		
		if(!address.equals("")) {
			try {
				ip = InetAddress.getByName(address).getHostAddress();
			}
			catch(UnknownHostException e) {
				// Do nothing - just return Unknown
			}
		}
		
		return ip;
	}
}
