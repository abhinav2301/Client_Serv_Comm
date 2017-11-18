import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Client {
    private static final int NEW_CONNECTION_TYPE = 1;
    private static final int REQUEST_COMMUNICATION_TYPE = 2;

    private static Socket s;
    private static ServerSocket server;
    private static ObjectOutputStream dos;

    public static void main(String[] args){
        String serverIP = "192.168.43.42";
        try{
            s = new Socket(serverIP, 8888);
            dos = new ObjectOutputStream(s.getOutputStream());
            dos.writeInt(NEW_CONNECTION_TYPE);
            dos.writeUTF(System.getProperty("user.name"));
        	dos.flush();
            dos.close();
            s.close();

            JFrame frame = new JFrame(System.getProperty("user.name"));
            frame.setSize(500, 100);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            server = new ServerSocket(8888);

            JButton button = new JButton("Start Chat");
            JTextField textField = new JTextField();
            textField.setBounds(10, 10, 250, 60);
            button.setBounds(270, 10, 150, 60);
            button.addActionListener(e -> {
                try {
                    server.close();
                    s = new Socket(serverIP, 8888);
                    dos = new ObjectOutputStream(s.getOutputStream());
                    dos.writeInt(REQUEST_COMMUNICATION_TYPE);
                    dos.writeUTF(System.getProperty("user.name"));
                    dos.writeUTF(textField.getText());
                    dos.flush();
                    dos.close();
                    s.close();
                    if(s.isClosed() && server.isClosed())
                        System.out.println("Client Sockets Closed");

                    frame.setVisible(false);
                    frame.dispose();
                    new ClientComm();
                    System.out.println("Request Processed.");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });
            frame.setLayout(null);
            frame.add(textField);
            frame.add(button);
            frame.setVisible(true);

            s = server.accept();
            System.out.println("Request Received");
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
            String src = ois.readUTF();
            String ip = ois.readUTF();
            ois.close();
            s.close();
            server.close();
            if(s.isClosed() && server.isClosed())
                System.out.println("Client Sockets Closed");
            frame.setVisible(false);
            frame.dispose();
            new ClientComm(ip, src);
        }catch (Exception e){
            if(e.getMessage().compareToIgnoreCase("socket closed") == 0)
                System.out.println("request Sent");
        }
    }
}
