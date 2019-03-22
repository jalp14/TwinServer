package twins;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Server {

    private final int port;

    private Writer writer;
    private SERVER_PROTOCOL server_state;
    private File dbFile;
    private FileReader fileReader;
    private FileWriter fileWriter;
    private BufferedReader reader;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private String tmpName;
    private String clientInput;
    private String tmpDate;
    private ServerSocket serverSocket;
    private Socket conn;

    /**
     * Initialise a new Twins server. To start the server, call start().
     * @param port the port number on which the server will listen for connections
     */
    public Server(int port) {
        this.port = port;
    }

    // Using enum to represent the different states of the server
    public enum SERVER_PROTOCOL {
        NEW,
        RECEIVE_NAME,
        RECEIVE_DATE,
        RECEIVE_REQ;
    }

    // Types of errors the server can throw
    public static final String ERROR_ONE = "Error 1";
    public static final String ERROR_TWO = "Error 2";
    public static final String ERROR_THREE = "Error 3";
    public static final String ERROR_ZERO = "Error 0";
    /**
     * Start the server.
     * @throws IOException 
     */
    public void start() throws IOException {
        try{
            while (true) {
                if(serverSocket == null) {
                    serverSocket = new ServerSocket(port);
                }
                server_state = SERVER_PROTOCOL.NEW;
                System.out.println("Server listening on port: " + port);
                conn= serverSocket.accept();
                System.out.println("Connected to " + conn.getInetAddress() + ":" + conn.getPort());
                session(conn);
            }
        }catch (SocketException se)
        {
            se.printStackTrace();
        }
    }

    public ArrayList<String> checkName(String name) {
        // create ArrayList to temporarily store all the user's DOB
        dbFile = new File("dB.txt");
        ArrayList<String> tmpList = new ArrayList<>();
        try {
            fileReader = new FileReader(dbFile);
            bufferedReader = new BufferedReader(fileReader);
            // Read data from file line by line
            String line = bufferedReader.readLine();
            while (line != null) {
                // Split data into tokens separated by comma
                String[] tokens = line.split(",");
                // get the name and add it to the arraylist
                tmpName = tokens[0];
                if (tmpName.equals(name)) {
                    tmpDate = tokens[1];
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

    public ArrayList<String> checkTwin(String dob) {
        // create ArrayList to temporarily store all the user's DOB
        dbFile = new File("dB.txt");
        ArrayList<String> tmpList = new ArrayList<>();
        try {
            fileReader = new FileReader(dbFile);
            bufferedReader = new BufferedReader(fileReader);
            // Read data from file line by line
            String line = bufferedReader.readLine();
            while (line != null) {
                // Split data into tokens separated by comma
                String[] tokens = line.split(",");
                // get the date and add it to the arraylist
                String tmpDate = tokens[1];
                if (tmpDate.equals(dob)) {
                    System.out.println(tmpName);
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
        // Assign the file
        dbFile = new File("db.txt");
        // preapre the file writer and set it to append
        fileWriter = new FileWriter(dbFile, true);
        // write the name and the dob to the file
        fileWriter.write(name + "," + dob + "\n");
        // close the file to save chagnes
        fileWriter.close();
    }


    public void deleteUser(String lineToDelete)throws IOException {
            // Assign the file
            File tmpFile = new File("db.txt");
            /*
             Load all the file data into local memory
             Separate each line as different tokens and apply a filter to isolate the line we are looking for
             Remove the line we are looking for
             */
            List<String> fileTokens = Files.lines(tmpFile.toPath()).filter(line -> !(line.contains(lineToDelete))).collect(Collectors.toList());
            // Write data in memory back to the file and save it by using the truncate option
            Files.write(tmpFile.toPath(), fileTokens, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
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
                deleteUser(clientInput);
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

        try {
            String clientDate;
            Date date = new Date();
            String clientRequest;
            ArrayList<String> listOfNames;
            SimpleDateFormat format = new SimpleDateFormat("dd:mm:yyyy");
            writer = new OutputStreamWriter(connection.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            // Checking for name
            clientInput = reader.readLine();
            clientInput.replaceAll(" ", "");
            if (!(clientInput.toLowerCase().equals("hello"))) {
                sendMessage(ERROR_ZERO);
                connection.close();
            }


            sendMessage("What is your name ?");
            server_state = SERVER_PROTOCOL.RECEIVE_NAME;
            clientInput = reader.readLine();
            clientInput.replaceAll(" ", "");
            //String tmpdate = " ";
            // Continue

            // Check if name exists
            boolean nameExists = false;
            listOfNames = checkName(clientInput);
            System.out.println("listOfNames size: " + listOfNames.size());
            for (int i = 0; i < listOfNames.size(); i++) {
                System.out.println("ClientInput: " + clientInput + " ,Name: " + listOfNames.get(i));
                if (clientInput.equals(listOfNames.get(i))) {
                    nameExists = true;
                } else {
                    nameExists = false;
                }
            }
            System.out.println("Name exists: " + nameExists);

            if (nameExists) {
                sendMessage("BEGIN TWIN");
                // check twin func
                listOfNames = checkTwin(tmpDate);
                for (int i = 0; i < listOfNames.size(); i++) {
                    System.out.println(listOfNames.get(i));
                    sendMessage(listOfNames.get(i));
                }
                sendMessage("END TWIN");

                server_state = SERVER_PROTOCOL.RECEIVE_REQ;

                requestOption(connection, listOfNames, tmpDate, reader);


                System.out.println("Closing connection");
                connection.close();
            } else {
                server_state = SERVER_PROTOCOL.RECEIVE_DATE;
                sendMessage("What is your DOB");
                clientDate = reader.readLine();

                SimpleDateFormat fromUser = new SimpleDateFormat("dd:MM:yyyy");
                SimpleDateFormat myFormat = new SimpleDateFormat("dd:MM:yyyy");
                SimpleDateFormat testFormart = new SimpleDateFormat("yyyy");

                try {
                    String reformattedStr = myFormat.format(fromUser.parse(clientDate));
                    String testString = testFormart.format(fromUser.parse(clientDate));
                    if ((Integer.parseInt(testString) > 1899) && (Integer.parseInt(testString) < 2020)) {
                        System.out.println(testString);
                        System.out.println(reformattedStr);
                        tmpDate = reformattedStr;
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
                listOfNames = checkTwin(tmpDate);
                for (int i = 0; i < listOfNames.size(); i++) {
                    System.out.println(listOfNames.get(i));
                    sendMessage(listOfNames.get(i));
                }
                sendMessage("END TWIN");

                server_state = SERVER_PROTOCOL.RECEIVE_REQ;

                requestOption(connection, listOfNames, tmpDate, reader);


                System.out.println("Closing connection");
                connection.close();
            }
            // Checking for date
        }catch (SocketException se)
        {
            server_state = SERVER_PROTOCOL.NEW;
            conn = serverSocket.accept();
            session(conn);
        }
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
        try {
            server.start();
        }catch (SocketException se)
        {
            //server.start();
            System.out.println("Connection disrupted with client.");
            se.printStackTrace();
        }
        System.err.println("Server loop terminated!"); // not supposed to happen
    }
}
