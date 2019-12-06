package TCP_concurrentiel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

public class ThreadServ implements Runnable {

	private Socket client;
	private PrintStream writer = null;
	private BufferedReader reader = null;

	public ThreadServ(Socket ClientSock) {
		client = ClientSock;
	}

	// ATTENTION LES PAGES DERREURS DEMANDES UN CHEMIN VERS UN DOSSIER err/ SUIVI DU
	// NOM DE CODE DERREUR .html
	// EXEMPLE err/404.html
	// le dossier res est dsponnible ici https://drive.google.com/drive/folders/1z3l4f-Vvko7zYZcopVXJKK936nFsiT_W?usp=sharing
	// TODO
	// utiliser des regex pour rechercher des elements dans le header // update je comprend pas pourquoi ca ne marche pas :(
	// ya des exceptions à gerer
	public void run() {
		try {
			while (true) {

				String headerRecue = recevoireMsg();
				System.out.println("---------header-----------");
				System.out.println(headerRecue);
				String pagePath = (getPagePath(getHeaderLine(headerRecue, 0)));
				String extension;
				try {
					extension = pagePath.split("[.]")[1];
				} catch (Exception e) {
					extension = "";
				}
				System.out.println("chemin vers fichier : " + pagePath);
				System.out.println("extension : " + extension);
				if (!extension.equalsIgnoreCase("php")) {
					System.out.println("----------HTML----------");

					if (getHeaderLine(headerRecue, 2).equalsIgnoreCase("Connection: Upgrade")) {
						System.out.println("-------WEBSOCKET-------");

						doHandShake(headerRecue);
						client.getOutputStream().write(encode("SALUT JE SUIS LE SERVEUR !"));
						client.getOutputStream().flush();
						printInputStream(client.getInputStream());

					} else {

						System.out.println("--------DE BASE--------");
						byte[] headerEnvoyer = createHeader("200", contentFile(pagePath), fileSize(pagePath));
						sendContent(headerEnvoyer, loadFile(pagePath));

					}

				} else {
					System.out.println("----------PHP----------");

					String phpOutString = Php(headerRecue, pagePath);
					byte[] phpOutput = phpOutString.getBytes();
					byte[] headerEnvoyer = createHeader("200", contentFile(pagePath), phpOutput.length);
					sendContent(headerEnvoyer, phpOutput);
				}

				// client.close();// Connection persistante avec le while(true)
			}
		} catch (NoSuchFileException e) {
			System.err.println("erreur 404");
			erreur("404");
			return;
		} catch (IOException e) {
			System.err.println("erreur 500");
			erreur("500");
		}

		System.out.println("---------------FIN TRAITEMENT---------------");

	}

	private void erreur(String code) { // affiche des pages d'erreurs perso
		String pagePath = "err/" + code + ".html";
		byte[] header;
		try {
			header = createHeader(code, contentFile(pagePath), fileSize(pagePath));
			sendContent(header, loadFile(pagePath));
		} catch (IOException e) {
			System.err.println("erreur dans la methode erreur");
		}

	}

	private byte[] createHeader(String code, String contentType, long size) throws IOException {

		String request = "HTTP/1.1 " + code + "\r\n";
		request += "Host: 127.0.0.1 \r\n";
		request += "Accept-Charset: UTF-8 \r\n";
		request += "Content-Type : " + contentType + "; charset=UTF-8\r\n";
		request += "Content-Length : " + size + "\r\n";
		request += "\r\n";

		return request.getBytes();
	}

	private void sendContent(byte[] header, byte[] content) throws IOException {
		writer = new PrintStream(client.getOutputStream());
		writer.write(header);
		writer.write(content);
		writer.flush();
	}

	private String recevoireMsg() throws IOException {
		return lireBuffer(client.getInputStream());
	}

	private String lireBuffer(InputStream stream) throws IOException {
		String msg = "", ligne;
		reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
		while ((ligne = reader.readLine()) != null) {
			if (ligne.isEmpty())
				break;
			msg += ligne + "\n";
		}

		while (reader.ready())
			msg += (char) reader.read();
		return msg;
	}

	private String getHeaderLine(String header, int ligne) {
		try {
			return header.split("\n")[ligne];
		} catch (Exception e) {
			return "";
		}
	}

	private String getPostValue(String header) {
		String[] headers = header.split("\n");
		String postLine = headers[headers.length - 1];
		String postDonnees = postLine.split("champPost=")[1];

		return postDonnees;
	}

	private String getWebsocketKey(String header) {
		String[] headers = header.split("\n");
		String keyLine = "";
		for (String line : headers) {
			if (line.contains("Sec-WebSocket-Key: ")) {
				keyLine = line;
				break;
			}

		}
		String key = keyLine.split(" ")[1];
		return key;
	}

	private String getHeaderElement(String line, int pos) {
		try {
			return line.split(" ")[pos];
		} catch (Exception e) {
			return "";
		}
	}

	private String getPagePath(String line) {
		try {
			line = line.split(" ")[1].substring(1);
		} catch (Exception e) {
			return "";
		}
		if (line.contains("?"))
			line = line.split("\\?")[0];
		return line;
	}

	private String getGetContent(String line) {
		try {
			line = line.split(" ")[1].substring(1);
		} catch (Exception e) {
			return "";
		}
		if (line.contains("?"))
			line = line.split("\\?")[1];
		else
			line = "";
		return line;
	}

	private byte[] loadFile(String path) throws IOException {
		Path pathFile = Paths.get(path);
		return Files.readAllBytes(pathFile);
	}

	private long fileSize(String path) throws IOException {
		Path pathFile = Paths.get(path);
		return Files.size(pathFile);
	}

	private String contentFile(String path) throws IOException {
		Path pathFile = Paths.get(path);
		return Files.probeContentType(pathFile);
	}

	private String Php(String header, String path) throws IOException {
		String reqMethod = "Request_Method=" + getHeaderElement(getHeaderLine(header, 0), 0);
		String redirectStatut = "Redirect_Status=true";
		String filePath = "SCRIPT_FILENAME=" + path;
		String queryString = "QUERY_STRING=" + getGetContent(getHeaderLine(header, 0));
		String contentLength = "Content_Length=" + fileSize(path);
		String contentType = "Content_type="
				+ ((contentFile(path) != null) ? contentFile(path) : "application/x-www-form-urlencoded");

		String[] env = { reqMethod, redirectStatut, filePath, queryString, contentLength, contentType };

		System.out.println("-----------env-----------");
		for (String str : env) {
			System.out.println(str);
		}

		Process php = Runtime.getRuntime().exec("php-cgi", env);

		OutputStream out = php.getOutputStream();
		if (reqMethod.split("=")[1].contains("POST")) {
			System.out.println("POST : " + getPostValue(header));
			out.write(getPostValue(header).getBytes());
		}
		out.close();
		InputStream in = php.getInputStream();
		BufferedReader phpReader = new BufferedReader(new InputStreamReader(in));
		Boolean html = false;
		String line;
		String resPHP = "";
		while ((line = phpReader.readLine()) != null) {
			if (html)
				resPHP += line + "\n";
			if (line.isEmpty())
				html = true;
		}
		phpReader.close();
		in.close();
		return resPHP;
	}

	private void doHandShake(String header) throws UnsupportedEncodingException {
		Matcher get = Pattern.compile("^GET").matcher(header);
		if (get.find()) { // return true
			System.out.println("WEBSOCKET ----- DEBUT");
			System.out.println(header);
			// Matcher match = Pattern.compile("Sec-WebSocket-Key:
			// (.*)",Pattern.MULTILINE).matcher(header); ne marche pas
			String key = getWebsocketKey(header);
			System.out.println("key : " + key);
			byte[] response = null;
			try {
				response = ("HTTP/1.1 101 Switching Protocols\r\n" + "Connection: Upgrade\r\n"
						+ "Upgrade: websocket\r\n" + "Sec-WebSocket-Accept: "
						+ DatatypeConverter.printBase64Binary(MessageDigest.getInstance("SHA-1")
								.digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
						+ "\r\n\r\n").getBytes("UTF-8");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}

			try {
				System.out.println("reponse");
				System.out.println(new String(response));
				writer = new PrintStream(client.getOutputStream());

				writer.write(response, 0, response.length);
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {

		}

	}

	// websocket --------------------- source :
	// https://stackoverflow.com/questions/43163592/standalone-websocket-server-without-jee-application-server

	private void printInputStream(InputStream inputStream) throws IOException {
		int len = 0;
		byte[] b = new byte[1024];
		// rawIn is a Socket.getInputStream();
		while (true) {
			len = inputStream.read(b);
			if (len != -1) {

				byte rLength = 0;
				int rMaskIndex = 2;
				int rDataStart = 0;
				// b[0] is always text in my case so no need to check;
				byte data = b[1];
				byte op = (byte) 127;
				rLength = (byte) (data & op);

				if (rLength == (byte) 126)
					rMaskIndex = 4;
				if (rLength == (byte) 127)
					rMaskIndex = 10;

				byte[] masks = new byte[4];

				int j = 0;
				int i = 0;
				for (i = rMaskIndex; i < (rMaskIndex + 4); i++) {
					masks[j] = b[i];
					j++;
				}

				rDataStart = rMaskIndex + 4;

				int messLen = len - rDataStart;

				byte[] message = new byte[messLen];

				for (i = rDataStart, j = 0; i < len; i++, j++) {
					message[j] = (byte) (b[i] ^ masks[j % 4]);
				}

				System.out.println(new String(message));

				b = new byte[1024];

			}
		}
	}

	public byte[] encode(String mess) throws IOException {
		byte[] rawData = mess.getBytes();

		int frameCount = 0;
		byte[] frame = new byte[10];

		frame[0] = (byte) 129;

		if (rawData.length <= 125) {
			frame[1] = (byte) rawData.length;
			frameCount = 2;
		} else if (rawData.length >= 126 && rawData.length <= 65535) {
			frame[1] = (byte) 126;
			int len = rawData.length;
			frame[2] = (byte) ((len >> 8) & (byte) 255);
			frame[3] = (byte) (len & (byte) 255);
			frameCount = 4;
		} else {
			frame[1] = (byte) 127;
			int len = rawData.length;
			frame[2] = (byte) ((len >> 56) & (byte) 255);
			frame[3] = (byte) ((len >> 48) & (byte) 255);
			frame[4] = (byte) ((len >> 40) & (byte) 255);
			frame[5] = (byte) ((len >> 32) & (byte) 255);
			frame[6] = (byte) ((len >> 24) & (byte) 255);
			frame[7] = (byte) ((len >> 16) & (byte) 255);
			frame[8] = (byte) ((len >> 8) & (byte) 255);
			frame[9] = (byte) (len & (byte) 255);
			frameCount = 10;
		}

		int bLength = frameCount + rawData.length;

		byte[] reply = new byte[bLength];

		int bLim = 0;
		for (int i = 0; i < frameCount; i++) {
			reply[bLim] = frame[i];
			bLim++;
		}
		for (int i = 0; i < rawData.length; i++) {
			reply[bLim] = rawData[i];
			bLim++;
		}
		return reply;
	}

}
