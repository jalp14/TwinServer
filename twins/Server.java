package twins;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Server {

    private final int port;

    private Writer writer;
    private SERVER_PROTOCOL server_state;
    private File dbFile;
    private FileReader fileReader;
    private FileWriter fileWriter;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String tmpName;

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

    public static final String ERROR_ONE = "Error 1";
    public static final String ERROR_TWO = "Error 2";
    public static final String ERROR_THREE = "Error 3";
    public static final String ERROR_ZERO = "Error 0";
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

    public ArrayList<String> checkTwin(String dob) {
        ArrayList<String> tmpList = new ArrayList<>();
        try {
            fileReader = new FileReader(dbFile);
            bufferedReader = new BufferedReader(fileReader);
            String line = bufferedReader.readLine();
            while (line != null) {
                String[] tokens = line.split(",");
                String tmpDate = tokens[1];
                if (tmpDate.equals(dob)) {
                    tmpName = tokens[0];
                    tmpList.add(tmpName);
                }
                line = bufferedReader.readLine();
            }
            return tmpList;
        } catch (IOException e) {
            System.out.println("Error reading from file");
        }
        return tmpList;
    }

    public void storeUserDetails(String name, String dob) throws IOException {
        dbFile = new File("db.txt");
        fileWriter = new FileWriter(dbFile, true);
        fileWriter.write(name + "," + dob + "\n");
        fileWriter.close();
    }

    public void requestOption(Socket connection, ArrayList<String> listOfNames, String tmpdate, BufferedReader reader) throws IOException
    {
        String clientRequest = reader.readLine();

        switch (clientRequest) {
            case "Quit" :
                connection.close();
                break;
            case "Refresh" :
                listOfNames = checkTwin(tmpdate);
                for (int i = 0; i < listOfNames.size(); i++) {
                    System.out.println(listOfNames.get(i));
                    sendMessage(listOfNames.get(i));
                }
                requestOption(connection, listOfNames, tmpdate, reader);
                break;
            case "Delete me" :
                sendMessage("Fk you no deleting here");
                break;
            default :
                break;
        }
    }

    /**
     * Run a Twins protocol session over an established network connection.
     * @param connection the network connection
     * @throws IOException 
     */
    public void session(Socket connection) throws IOException {
        String clientInput;
        String clientDate;
        Date date = new Date();
        String clientRequest;
        ArrayList<String> listOfNames;
        SimpleDateFormat format = new SimpleDateFormat("dd:mm:yyyy");
        writer = new OutputStreamWriter(connection.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        
        // Checking for name
        clientInput = reader.readLine();
        clientInput.replaceAll(" ","");
        if (!(clientInput.equals("Hello"))) {
            sendMessage(ERROR_ZERO);
            connection.close();
        }
        
        sendMessage("What is your name ?");
        server_state = SERVER_PROTOCOL.RECEIVE_NAME;
        clientInput = reader.readLine();
        clientInput.replaceAll(" ","");
        String tmpdate = " ";
        // Continue

        // Checking for date
        server_state = SERVER_PROTOCOL.RECEIVE_DATE;
        sendMessage("What is your DOB");
        clientDate = reader.readLine();

        SimpleDateFormat fromUser = new SimpleDateFormat("dd:MM:yyyy");
        SimpleDateFormat myFormat = new SimpleDateFormat("dd:MM:yyyy");
        SimpleDateFormat testFormart = new SimpleDateFormat("yyyy");

        try {
            String reformattedStr = myFormat.format(fromUser.parse(clientDate));
            String testString = testFormart.format(fromUser.parse(clientDate));
            if ((Integer.parseInt(testString) > 1850) && (Integer.parseInt(testString) < 2020)) {
                System.out.println(testString);
                System.out.println(reformattedStr);
                tmpdate = reformattedStr;
                storeUserDetails(clientInput, reformattedStr);
            } else {
                sendMessage(ERROR_TWO);
            }
        } catch (ParseException e) {
            sendMessage(ERROR_TWO);
            e.printStackTrace();
        }

        sendMessage("BEGIN TWIN");
        // check twin func
         listOfNames = checkTwin(tmpdate);
         for (int i = 0; i < listOfNames.size(); i++) {
             System.out.println(listOfNames.get(i));
             sendMessage(listOfNames.get(i));
         }
        sendMessage("END TWIN");

         server_state = SERVER_PROTOCOL.RECEIVE_REQ;

         requestOption(connection, listOfNames, tmpdate, reader);
         

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
