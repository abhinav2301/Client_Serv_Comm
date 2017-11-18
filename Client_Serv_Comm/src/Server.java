import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Set;

public class Server {
    private static final int NEW_CONNECTION_TYPE = 1;
    private static final int REQUEST_COMMUNICATION_TYPE = 2;
    static ServerSocket server;
    static Hashtable<String, SocketAddress> clientList= new Hashtable<>();

    public static void main(String[] args) {
            try{
            System.out.println("Server Starting");
            server = new ServerSocket(8888);
            System.out.println("Server Running.....");

            while(true){
                try {
                    Socket socket = server.accept();
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    if(ois.readInt() == NEW_CONNECTION_TYPE) {
                        String user = ois.readUTF();
                        Server.clientList.put(user, new SocketAddress(socket.getInetAddress().getHostAddress()));
                        System.out.println(user + " Connected.");
                    }
                    else{
                        String s = ois.readUTF();
                        SocketAddress src = clientList.get(s);
                        SocketAddress dst = clientList.get(ois.readUTF());
                        socket.close();
                        System.out.println(s + " Requested." + "\nSender " + src.ip + "\nReceiver" + dst.ip);
                        socket = new Socket(dst.ip, dst.port);
                        System.out.println("Request sent to " + dst.ip + " at port " + String.valueOf(dst.port));
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        oos.writeUTF(s);
                        oos.writeUTF(src.ip);
                        oos.flush();
                        oos.close();
                    }
                    ois.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class SocketAddress{
    int port = 8888;
    String ip;

    SocketAddress(String ip){
        this.ip = ip;
    }

    SocketAddress(int port, String ip){
        this.ip = ip;
        this.port = port;
    }
}
