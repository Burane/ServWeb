package TCP_concurrentiel;

public class Main {

	public static void main(String[] args) {
		Thread server = new Thread(new Serv(8080, "127.0.0.1"));
		server.start();

	}

}