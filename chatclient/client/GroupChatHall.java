package chat.client;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import chat.function.ChatBean;


class GroupChatHall extends JFrame {

    private Socket clientSocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;


    public static void main(String[] args) throws IOException {
        new GroupChatHall("", new Socket("localhost", 8521)).setVisible(true);
    }


    /**
     *
     */
    private static final long serialVersionUID = -3874945774254624025L;
    GroupChatHall u;
    private static TextArea textShow;
    private static TextArea textSend;
    private static Button send;
    private static Button clear;
    private String userName;


    public GroupChatHall(String userName, Socket socket) throws HeadlessException, UnknownHostException {
        this.userName = userName;
        clientSocket = socket;
        init();
        centerPanel();
        southPanel();
        this.event();
        this.setIconImage(Toolkit.getDefaultToolkit().createImage("qq.png"));

        // 离开
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    clientSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

    }


    public void init() {
        setTitle("聊天大厅");
        this.setSize(500, 600);
        this.setVisible(true);
        this.setLocation(800, 200);
        new receive("接收").start();
    }

    class receive extends Thread {
        public receive(String name) {
            super(name);
        }

        
        public void run() {
            while (true) {
                try {
                    ois = new ObjectInputStream(clientSocket.getInputStream());
                    final ChatBean bean = (ChatBean) ois.readObject();
                    String message = bean.getName() + ":" + getCurrentTime() + "\r\n" + bean.getInfo()
                            + "\r\n";
                    textShow.append(message);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    try {
                        clientSocket.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    break;
                }

                new Thread(() -> {
                    try {/**/
                        sound("music\\清新.mid");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }


        }
    }


    public String getCurrentTime() {
        Date date = new Date();
        SimpleDateFormat fomat = new SimpleDateFormat("YYYY/MM/dd:hh:mm:ss");
        return fomat.format(date);
    }

    public void send() {
        try {
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ChatBean chatBean = new ChatBean();
            chatBean.setName(userName);
            chatBean.setInfo(textSend.getText().substring(4));

            oos.writeObject(chatBean);
            oos.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    public void southPanel() throws UnknownHostException {
        Panel south = new Panel();
        send = new Button("发送");
        clear = new Button("清屏");
        send.setBackground(Color.YELLOW);
        clear.setBackground(Color.PINK);
        south.add(send);
        south.add(clear);
        this.add(south, BorderLayout.SOUTH);
    }

    public void centerPanel() {
        JPanel center = new JPanel();
        textShow = new TextArea();
        textSend = new TextArea(5, 1);
        center.setLayout(new BorderLayout());
        center.add(textShow, BorderLayout.CENTER);
        center.add(textSend, BorderLayout.SOUTH);
        this.add(center, BorderLayout.CENTER);
        textShow.setEditable(false);
        textShow.setBackground(new Color(209, 224, 239));
        textShow.setFont(new Font("", Font.PLAIN, 20));
        textSend.setFont(new Font("", Font.PLAIN, 20));
        textSend.setBackground(Color.LIGHT_GRAY);
        textSend.setText("请输入:");
        center.updateUI();
    }


    public void sound(String url) throws Exception {
        // 创建音乐文件输入流对象
        InputStream in = new FileInputStream(url);
        // 创建音频流对象
        final AudioStream audioStream = new AudioStream(in);
        new Thread(() -> AudioPlayer.player.start(audioStream)).start();
        Thread.sleep(3000);
        // 停止声音播放
        AudioPlayer.player.stop(audioStream);
    }

    public void event() {

        send.addActionListener(e -> {
            if (textSend.getText().equals("请输入:")) {
                JOptionPane.showMessageDialog(u, "输入内容不能为空", "温馨提示", JOptionPane.DEFAULT_OPTION);
            } else {
                send();
                textSend.setText("请输入:");
            }
        });

        clear.addActionListener(e -> {
            textShow.setText(null);
            textSend.setText("请输入:");
            send.setEnabled(true);
        });

        // 设置快捷键
        textSend.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    send();
                    textSend.setText("请输入:");
                    textSend.setCaretPosition(4);

                }
            }
        });

    }


}

