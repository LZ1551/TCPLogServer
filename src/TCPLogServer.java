import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime; // Import the LocalDateTime class
import java.time.format.DateTimeFormatter; // Import the DateTimeFormatter class

//denna klass hanterar tcp med logg server
public class TCPLogServer {
    private final int portESP = 1234;
    private final int portVisualServer = 4321;

    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    //private ArrayList<ClientHandler> clients;
    private VisualServer visualServer;
    private Logging logging;

    private Buffer buffer;

    public TCPLogServer(){
        buffer = new Buffer();
        logging = new Logging("log.txt");
        //clients = new ArrayList<>();
        new ConnectionListener(portVisualServer,"visual").start();
        new ConnectionListener(portESP,"esp").start();
        try {System.out.println("ip address: " + InetAddress.getLocalHost().getHostAddress());} catch (UnknownHostException e) {e.printStackTrace();}
    }

    private class ConnectionListener extends Thread{
        private ServerSocket serverSocket;
        private String connectionType;

        public ConnectionListener(int port, String connectionType) {
            try {
                this.connectionType = connectionType;
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            while (true){
                try{
                    if(connectionType.equals("esp")){
                        Socket socket = serverSocket.accept();System.out.println("log server: esp socket accept");
                        new ClientHandler(socket).start();
                    } else if(connectionType.equals("visual")){
                        Socket socket = serverSocket.accept();System.out.println("log server: visual server socket accept");
                        new VisualServer(socket).start();
                    }
                } catch (Exception e) {e.printStackTrace();}
            }
        }
    }

    private class VisualServer extends Thread {
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;

        public VisualServer(Socket socket) {
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream()); System.out.println("visual oos created");
                objectInputStream = new ObjectInputStream(socket.getInputStream()); System.out.println("visual ois created");
                visualServer = this; System.out.println("visual server connected");

                //*/
            } catch (IOException ignored) {}
        }

        public void send(String string){
            try{
                objectOutputStream.writeObject(string);
                objectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while (true) {
                try {
                    Object object = buffer.get();
                    objectOutputStream.writeObject(object);
                    objectOutputStream.flush();
                } catch (Exception e) {}
            }
        }
    }

    private class ClientHandler extends Thread {
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;

        private BufferedReader bufferedReader;
        private Socket socket;
        String macAddress;

        public ClientHandler(Socket socket) {
            try {
                //objectOutputStream = new ObjectOutputStream(socket.getOutputStream());System.out.println("esp oos created");
                //objectInputStream = new ObjectInputStream(socket.getInputStream());System.out.println("esp oos created");
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.socket = socket;

                String handshake = bufferedReader.readLine();
                if (handshake == null || handshake.trim().isEmpty()) {
                    System.out.println("handshake not succesful");
                    return;
                }

                macAddress = handshake.trim();

                clients.put(macAddress, this);
                buffer.put(new Message(macAddress, "new"));
                System.out.println("new esp: " + macAddress);
            } catch (IOException ignored) {
            }

        }

        public void run() {
            //instans av logging klassen som skriver till textfil
            //flyttad utanför loopen för att saker ska vara smoother (så)
            Logging logg = new Logging("log.txt");
            //för formaterting av tid och datum
            //utflyttad, så de inte händer flera gånger i onödan. (så)
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");


            try {
                String inDataString;
                while ((inDataString = bufferedReader.readLine()) != null) {

                    //formaterat tid och datum
                    String formattedDate = LocalDateTime.now().format(myFormatObj);
                    //Object object = objectInputStream.readObject();
                    //String string = bufferedReader.readLine();
                    buffer.put(new Message(macAddress, inDataString));

                    //hur det skrivs till text filen
                    String loggingTextString = formattedDate + ", " + macAddress + ", " + inDataString;
                    System.out.println(loggingTextString);
                    //lägger till och sparas i textfilen
                    logg.addLog(loggingTextString);
                    System.out.println(macAddress + ": Received (sent to visual): " + inDataString);


                }
            } catch (IOException e) {
                System.out.println("connection  error at " + macAddress);
            } finally {

                clients.remove(macAddress);
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
        }
    }







    /*public static void main(String[] args) throws Exception {
        //skapa LogServer och ange loggfil
        Logging logging = new Logging("log.txt");

        //starta TCP-server på port 12345
        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("Server started on port 12345");

        InetAddress inetAddress = InetAddress.getLocalHost();
        String inetAddressIP = inetAddress.getHostAddress();
        System.out.println("IP Address: " + inetAddressIP);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket.getInetAddress());

            //starta en ny tråd för varje klient
            new Thread(() -> handleClient(clientSocket, logging)).start();
        }
    }

    private static void handleClient(Socket socket, Logging logging) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //här loggas varje mottaget meddelande
                System.out.println("Received: " + line);
                logging.addLog(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}
