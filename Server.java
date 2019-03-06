package twins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final int port;

    private Writer writer;
    private SERVER_PROTOCOL server_state;

    /**
     * Initialise a new Twins server. To start the server, call start().
     * @param port the port number on which the server will listen for connections
     */
    public Server(int port) {
        this.port = port;
    }

    public enum SERVER_PROTOCOL {
        NEW,
        RECEIVE_NAME,
        RECEIVE_DATE,
        RECEIVE_REQ;
    }

    public static final String ERROR_ONE = "Error 1 : Invalid Name";
    public static final String ERROR_TWO = "Error 2 : Invalid Date";
    public static final String ERROR_THREE = "Error 3 : Name in use";
    public static final String ERROR_ZERO = "Error 0 : Welcome to void!";
    /**
     * Start the server.
     * @throws IOException 
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                server_state = SERVER_PROTOCOL.NEW;
                System.out.println("Server listening on port: " + port);
                Socket conn= serverSocket.accept();
                System.out.println("Connected to " + conn.getInetAddress() + ":" + conn.getPort());
                session(conn);
            }
        }
    }

    /**
     * Run a Twins protocol session over an established network connection.
     * @param connection the network connection
     * @throws IOException 
     */
    public void session(Socket connection) throws IOException {
        String clientInput;
        writer = new OutputStreamWriter(connection.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        
        // Checking for name
        clientInput = reader.readLine();
        clientInput.replaceAll(" ","");
        if (!(clientInput.equals("Hello"))) {
            sendMessage(ERROR_ONE);
            connection.close();
        }
        
        sendMessage("What is your name ?");
        server_state = SERVER_PROTOCOL.RECEIVE_NAME;
        clientInput = reader.readLine();
        clientInput.replaceAll(" ","");
        // Continue

        // Checking for date

            

            
        System.out.println("Closing connection");
        connection.close();
    }

    /**
     * Send a newline-terminated message on the output stream to the client.
     * @param msg the message to send, not including the newline
     * @throws IOException 
     */
    private void sendMessage(String msg) throws IOException {
        writer.write(msg);
        writer.write("\n");
        // this flush() is necessary, otherwise ouput is buffered locally and
        // won't be sent to the client until it is too late 
        writer.flush();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        String usage = "Usage: java twins.Server [<port-number>] ";
        if (args.length > 1) {
            throw new Error(usage);
        }
        int port = 8123;
        try {
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
        } catch (NumberFormatException e) {
            throw new Error(usage + "\n" + "<port-number> must be an integer");
        }
        try {
            InetAddress ip = InetAddress.getLocalHost();
            System.out.println("Server host: " + ip.getHostAddress() + " (" + ip.getHostName() + ")");
        } catch (IOException e) {
            System.err.println("could not determine local host name");
        }
        Server server = new Server(port);
        server.start();
        System.err.println("Server loop terminated!"); // not supposed to happen
    }
}
