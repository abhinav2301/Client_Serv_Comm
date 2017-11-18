import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by adarsh on 08/10/2017.
 */

public class ClientComm {
    private JFrame frame;
    private JPanel rootPane;
    private JTextArea textArea;
    private JTextPane messageBox;
    private JButton attachFile;
    private JButton send;
    private JSplitPane spliter;

    private Socket socket;

    private String src, dst;
    private String ip;
    private boolean stat = true;

    private static final int TEXT = 1;
    private static final int FILE = 2;

    ClientComm() {
        try {
            ServerSocket server = new ServerSocket(8888);
            socket = server.accept();
            this.ip = socket.getInetAddress().getHostAddress();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            this.dst = ois.readUTF();
            ois.close();
            socket.close();
            server.close();
            this.src = System.getProperty("user.name");
            initiate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ClientComm(String ip, String dst) {
        try {
            socket = new Socket(ip, 8888);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeUTF(System.getProperty("user.name"));
            oos.flush();
            oos.close();
            this.ip = ip;
            this.src = System.getProperty("user.name");
            this.dst = dst;
            socket.close();
            initiate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        if(args[0].compareToIgnoreCase("sender") == 0)
//            new ClientComm();
//        else
        new ClientComm();
    }

    private void initiate(){

        frame = new JFrame(this.src + " - " + this.dst);
        frame.setSize(400, 500);
        frame.setResizable(false);
        spliter.setDividerLocation(350);
        spliter.setEnabled(false);
        frame.setContentPane(rootPane);

        messageBox.setEditable(false);

        frame.setVisible(true);
        (new Receive()).execute();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        send.setIcon(new ImageIcon("Images/send.png"));
        attachFile.setIcon(new ImageIcon("Images/attach.png"));

        send.addActionListener(e -> {
            (new Send(textArea.getText(), TEXT)).execute();
            textArea.setText(null);
        });

        attachFile.addActionListener(e -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.OPEN_DIALOG)
                    new Send(fileChooser.getSelectedFile().getCanonicalPath(), FILE).execute();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

    }

    private void putText(String name, String data, StyledDocument doc, SimpleAttributeSet sas){
        int len = doc.getLength();
        try {
            doc.insertString(len, name + '\n', null);
            doc.setParagraphAttributes(len + 1, 1, sas, false);

            doc.insertString(len + (name + '\n').length(), data + "\n\n", null);
            doc.setParagraphAttributes(len + name.length() + 1, 1, sas, false);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private class Send extends SwingWorker<Void, Void>{

        private String data;
        private int type;

        public Send(String data, int type){
            this.data = data;
            this.type = type;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                stat = false;
                System.out.println(data);
                socket = new Socket(ip, 8888);
                ObjectOutputStream dos = new ObjectOutputStream(socket.getOutputStream());
                dos.writeInt(this.type);

                if (this.type == TEXT) {
                    dos.writeInt(data.getBytes().length);
                    dos.writeBytes(data);
                    StyledDocument doc = messageBox.getStyledDocument();

                    SimpleAttributeSet sas = new SimpleAttributeSet();
                    StyleConstants.setAlignment(sas, StyleConstants.ALIGN_LEFT);
                    StyleConstants.setForeground(sas, Color.RED);

                    putText(src, data, doc, sas);
                }

                else if (this.type == FILE) {
                    File f = new File(data);
                    FileInputStream fis = new FileInputStream(f);
                    String name = f.getName();
                    dos.writeUTF(name);
                    long size = f.length();
                    dos.writeLong(size);

                    byte[] b = new byte[Math.toIntExact(size)];
                    if (size > 0) {
                        fis.read(b, 0, Math.toIntExact(size));
                        dos.write(b, 0, Math.toIntExact(size));
                    }
                    dos.flush();
                    fis.close();
                }
                dos.close();
                socket.close();
                stat = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private class Receive extends SwingWorker<Void, Void>{

        @Override
        protected Void doInBackground() throws Exception {
            while (true) {
                try {
                    if (stat) {
                        ServerSocket serverSocket = new ServerSocket(8888);
                        socket = serverSocket.accept();
                        ObjectInputStream dis = new ObjectInputStream(socket.getInputStream());

                        int type = dis.readInt();

                        if (type == TEXT) {
                            String data = "";
                            while (dis.available() > 0)
                                data += (dis.readUTF());
                            StyledDocument doc = messageBox.getStyledDocument();

                            SimpleAttributeSet sas = new SimpleAttributeSet();
                            StyleConstants.setAlignment(sas, StyleConstants.ALIGN_RIGHT);
                            StyleConstants.setForeground(sas, Color.BLUE);

                            putText(dst, data, doc, sas);
                        } else if (type == FILE) {
                            String name = dis.readUTF();

                            File f = new File("Downloads/" + name);
                            File dir = new File("Downloads");
                            if (!dir.exists() && !dir.isDirectory())
                                dir.mkdir();
                            f.createNewFile();

                            FileOutputStream fos = new FileOutputStream(f);
                            long size = dis.readLong();
                            byte[] b = new byte[Math.toIntExact(size)];
                            if (size > 0) {
                                dis.readFully(b);
                                fos.write(b, 0, Math.toIntExact(size));
                            }
                            fos.flush();
                            fos.close();

                            int n = JOptionPane.showOptionDialog(frame, "File " + name + " has been received. Click OK to view in Folder.", name, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
                            if (n == JOptionPane.YES_OPTION) {
                                if (System.getProperty("os.name").startsWith("Windows"))
                                    Runtime.getRuntime().exec("explorer.exe /select," + System.getProperty("user.dir") + "Downloads/" + name);
                                else
                                    Runtime.getRuntime().exec("nautilus '" + System.getProperty("user.dir") + "/Downloads/" + "'");
                            }
                            dis.close();
                            socket.close();
                            serverSocket.close();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
