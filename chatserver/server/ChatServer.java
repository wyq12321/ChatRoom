package chat.server;


import chat.function.ChatBean;
import chat.function.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ChatServer {

    private static ServerSocket ss;
    public static HashMap<String, Client> onlines;//链表

    private static ServerSocket groupChatServer;
    public static Set<Socket> clients;

    static {
        try {
            ss = new ServerSocket(8520);       //绑定8520端口
            onlines = new HashMap<>();

            groupChatServer = new ServerSocket(8521);
            clients = new HashSet<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ClientThread extends Thread {
        private Socket client;
        private ChatBean bean;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        public ClientThread(Socket client) {
            this.client = client;
        }

        public void run() {
            try {
                // 不停的从客户端接收信息
                while (true) {
                    // 读取从客户端接收到的catbean信息
                    ois = new ObjectInputStream(client.getInputStream());//定义一个对象操作流对象ois，读取client里的文件
                    bean = (ChatBean) ois.readObject();//读取正常应该被序列化的信息

                    // 分析catbean中，type是那样一种类型
                    switch (bean.getType()) {
                        // 上下线更新
                        case 0: { // 上线
                            // 记录上线客户的用户名和端口在clientbean中
                            Client cbean = new Client();
                            cbean.setName(bean.getName());
                            cbean.setSocket(client);
                            // 添加在线用户
                            onlines.put(bean.getName(), cbean);
                            // 创建服务器的catbean，并发送给客户端
                            ChatBean serverBean = new ChatBean();
                            serverBean.setType(0);
                            serverBean.setInfo(bean.getTimer() + "  "
                                    + bean.getName() + "上线了");
                            // 通知所有客户有人上线
                            HashSet<String> set = new HashSet<>();
                            // 客户昵称
                            set.addAll(onlines.keySet());
                            serverBean.setClients(set);
                            sendAll(serverBean);
                            break;
                        }
                        case -1: { // 下线
                            // 创建服务器的catbean，并发送给客户端
                            ChatBean serverBean = new ChatBean();
                            serverBean.setType(-1);

                            try {
                                oos = new ObjectOutputStream(
                                        client.getOutputStream());
                                oos.writeObject(serverBean);
                                oos.flush();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            onlines.remove(bean.getName());

                            // 向剩下的在线用户发送有人离开的通知
                            ChatBean serverBean2 = new ChatBean();
                            serverBean2.setInfo(bean.getTimer() + "  "
                                    + bean.getName() + " " + " 下线了");
                            serverBean2.setType(0);
                            HashSet<String> set = new HashSet<>();
                            set.addAll(onlines.keySet());
                            serverBean2.setClients(set);

                            sendAll(serverBean2);
                            return;
                        }
                        case 1: { // 聊天

//						 创建服务器的catbean，并发送给客户端
                            ChatBean serverBean = new ChatBean();

                            serverBean.setType(1);
                            serverBean.setClients(bean.getClients());
                            serverBean.setInfo(bean.getInfo());
                            serverBean.setName(bean.getName());
                            serverBean.setTimer(bean.getTimer());
                            // 向选中的客户发送数据
                            sendMessage(serverBean);
                            break;
                        }
                        case 2: { // 请求接受文件
                            // 创建服务器的catbean，并发送给客户端
                            ChatBean serverBean = new ChatBean();
                            String info = bean.getTimer() + "  " + bean.getName()
                                    + "向你传送文件,是否需要接受";

                            serverBean.setType(2);//设置变量的类型
                            serverBean.setClients(bean.getClients()); // 这是发送的目的地
                            serverBean.setFileName(bean.getFileName()); // 文件名称
                            serverBean.setSize(bean.getSize()); // 定义控件大小
                            serverBean.setName(bean.getName()); // 来源
                            serverBean.setTimer(bean.getTimer());//定时器
                            // 向选中的客户发送数据
                            sendMessage(serverBean);

                            break;
                        }
                        case 3: { // 确定接收文件
                            ChatBean serverBean = new ChatBean();

                            serverBean.setType(3);
                            serverBean.setClients(bean.getClients()); // 文件来源
                            serverBean.setTo(bean.getTo()); // 文件目的地
                            serverBean.setFileName(bean.getFileName()); // 文件名称
                            serverBean.setIp(bean.getIp());
                            serverBean.setPort(bean.getPort());
                            serverBean.setName(bean.getName()); // 接收的客户名称
                            serverBean.setTimer(bean.getTimer());
                            // 通知文件来源的客户，对方确定接收文件
                            sendMessage(serverBean);
                            break;
                        }
                        case 4: {
                            ChatBean serverBean = new ChatBean();

                            serverBean.setType(4);
                            serverBean.setClients(bean.getClients()); // 文件来源
                            serverBean.setTo(bean.getTo()); // 文件目的地
                            serverBean.setFileName(bean.getFileName());
                            serverBean.setInfo(bean.getInfo());
                            serverBean.setName(bean.getName());// 接收的客户名称
                            serverBean.setTimer(bean.getTimer());
                            sendMessage(serverBean);

                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                AtomicReference<String> name = new AtomicReference<>();
                onlines.forEach((k, v) -> {
                    if (v.getSocket() == client) {
                        name.set(k);
                    }
                });

                onlines.remove(name.get());

            } finally {
                close();
            }
        }

        // 向选中的用户发送数据
        private void sendMessage(ChatBean serverBean) {
            // 首先取得所有的values
            Set<String> cbs = onlines.keySet();
            Iterator<String> it = cbs.iterator();
            // 选中客户
            HashSet<String> clients = serverBean.getClients();
            while (it.hasNext()) {
                // 在线客户
                String client = it.next();
                // 选中的客户中若是在线的，就发送serverbean
                if (clients.contains(client)) {
                    Socket c = onlines.get(client).getSocket();
                    ObjectOutputStream oos;
                    try {
                        oos = new ObjectOutputStream(c.getOutputStream());
                        oos.writeObject(serverBean);
                        oos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }

        // 向所有的用户发送数据
        public void sendAll(ChatBean serverBean) {
            Collection<Client> clients = onlines.values();
            Iterator<Client> it = clients.iterator();
            ObjectOutputStream oos;
            while (it.hasNext()) {
                Socket c = it.next().getSocket();
                try {
                    oos = new ObjectOutputStream(c.getOutputStream());
                    oos.writeObject(serverBean);
                    oos.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        private void close() {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }


    public void start() {
        try {
            while (true) {
                Socket client = ss.accept();
                new ClientThread(client).start();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    class GroupChatClientThread extends Thread {

        Socket client;
        private ChatBean bean;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        public GroupChatClientThread(Socket client) {
            this.client = client;
            clients.add(client);
            System.out.println(clients);
        }

        @Override
        public void run() {
            while (true) {
                // 读取从客户端接收到的catbean信息
                try {
                    ois = new ObjectInputStream(client.getInputStream());//定义一个对象操作流对象ois，读取client里的文件
                    bean = (ChatBean) ois.readObject();//
                    //转发给所有群聊用户
                    for (Socket c : clients) {
                        oos = new ObjectOutputStream(c.getOutputStream());
                        oos.writeObject(bean);
                        oos.flush();
                    }

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    try {
                        client.close();
                        clients.remove(client);
                        break;
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } finally {

                }

            }
        }
    }

    public void startGroupChat() {
        try {
            while (true) {
                Socket client = groupChatServer.accept();
                new GroupChatClientThread(client).start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Thread(() -> new ChatServer().startGroupChat()).start();
        new ChatServer().start();
    }

}
