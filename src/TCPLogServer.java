import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Objects;

//denna klass hanterar tcp med logg server
public class TCPLogServer {
    private final int portESP = 1234;
    private final int portVisualServer = 4321;

    private ArrayList<ClientHandler> clients;
    private VisualServer visualServer;
    private Logging logging;

    public TCPLogServer(){
        logging = new Logging("log.txt");
        clients = new ArrayList<>();
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


                for(int i = 0; i < 10; i++){
                    String log = "new:" + i + ":" + i;
                    send(log);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String log = "move:5:1:8";
                send(log);
                log = "move:4:2:8";
                send(log);
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

                } catch (Exception e) {}
            }
        }
    }

    private class ClientHandler extends Thread{
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;

        public ClientHandler(Socket socket){
            try{
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());System.out.println("esp oos created");
                objectInputStream = new ObjectInputStream(socket.getInputStream());System.out.println("esp oos created");
                clients.add(this);
                System.out.println("new esp");
            }catch(IOException ignored){}

        }
        public void run(){
            while (true){
                try {
                    Object object = objectInputStream.readObject();
                    if(object instanceof String string){
                        visualServer.send(string); System.out.println("Received (sent to visual): " + string);
                        //logging.addLog(string);
                    }
                } catch (IOException | ClassNotFoundException e){}
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
