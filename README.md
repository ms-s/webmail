Webmail

This application implements a simple HTTP server and SMTP client to enable 
sending email through a web interface. It is only intended for use on a LAN, as 
SMTP authentication is not supported.

To compile, execute ./compile.sh. Start the web server by executing ./run.sh. 
Java version >= 5 is required.

- The web server opens a TCP socket and continuously listens on its port, accepting any incoming connections. When a connection is established on the port, the server performs a read() on the socket's input stream, expecting an HTTP request. The server validates incoming requests and returns a 400 Bad Request response if the HTTP is malformed.

- If the incoming request is a valid GET, the server attempts to open the requested file and return it to the user in a 200 OK response. If the user requests a nonexistent file, the server returns a 404 Not Found response.

- If the incoming request is a valid POST, the server decodes the request using the java.net.URLDecoder class.

- In order to support Swedish characters in the email's body, we implement the quoted-printable encoding as presented in RFC2045. As such, we enforce 76-character line limits, encode non-ASCII printable characters as an equals sign followed by their hexadecimal representations, avoid ending lines with tabs and spaces and ensure that meaningful line breaks are properly displayed.

- In order to support Swedish characters in the email's subject, the subject is converted to an RFC2047-compliant format. We assume the use of the ISO-8859-15 encoding, so our encoded words take the form =?ISO-8859-1?Q?Encoded-word?= where the encoded word is quoted-printable encoded.

- If an SMTP server was entered in the web form, the SMTP client attempts to connect to it on port 25. If no server was entered, a DNS MX lookup is performed on the domain of the recipient's email address and the result of the lookup (if any) is used as the SMTP server. Upon connecting, the SMTP client checks for a 220 Service Ready code from the server, indicating successful connection.

- Conforming to RFC2821, the SMTP client sends HELO, MAIL FROM, RCPT TO messages to the SMTP server and checks to ensure that 250 OK was received as a reply for each message. It then sends a DATA message and ensures that a 354 response code is received from the SMTP server. The SMTP client sends the email headers (to, from, subject and date), as well as MIME headers (MIME-Version, Content-Type, Content-Transfer-	Encoding), then completes the message by sending a blank line and a single period. If a 250 OK is received in response, the SMTP client finally sends a QUIT message and disconnects from the SMTP server.

