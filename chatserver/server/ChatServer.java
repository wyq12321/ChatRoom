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
    public static HashMap<String, Client> onlines;//����

    private static ServerSocket groupChatServer;
    public static Set<Socket> clients;

    static {
        try {
            ss = new ServerSocket(8520);       //��8520�˿�
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
                // ��ͣ�Ĵӿͻ��˽�����Ϣ
                while (true) {
                    // ��ȡ�ӿͻ��˽��յ���catbean��Ϣ
                    ois = new ObjectInputStream(client.getInputStream());//����һ���������������ois����ȡclient����ļ�
                    bean = (ChatBean) ois.readObject();//��ȡ����Ӧ�ñ����л�����Ϣ

                    // ����catbean�У�type������һ������
                    switch (bean.getType()) {
                        // �����߸���
                        case 0: { // ����
                            // ��¼���߿ͻ����û����Ͷ˿���clientbean��
                            Client cbean = new Client();
                            cbean.setName(bean.getName());
                            cbean.setSocket(client);
                            // ��������û�
                            onlines.put(bean.getName(), cbean);
                            // ������������catbean�������͸��ͻ���
                            ChatBean serverBean = new ChatBean();
                            serverBean.setType(0);
                            serverBean.setInfo(bean.getTimer() + "  "
                                    + bean.getName() + "������");
                            // ֪ͨ���пͻ���������
                            HashSet<String> set = new HashSet<>();
                            // �ͻ��ǳ�
                            set.addAll(onlines.keySet());
                            serverBean.setClients(set);
                            sendAll(serverBean);
                            break;
                        }
                        case -1: { // ����
                            // ������������catbean�������͸��ͻ���
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

                            // ��ʣ�µ������û����������뿪��֪ͨ
                            ChatBean serverBean2 = new ChatBean();
                            serverBean2.setInfo(bean.getTimer() + "  "
                                    + bean.getName() + " " + " ������");
                            serverBean2.setType(0);
                            HashSet<String> set = new HashSet<>();
                            set.addAll(onlines.keySet());
                            serverBean2.setClients(set);

                            sendAll(serverBean2);
                            return;
                        }
                        case 1: { // ����

//						 ������������catbean�������͸��ͻ���
                            ChatBean serverBean = new ChatBean();

                            serverBean.setType(1);
                            serverBean.setClients(bean.getClients());
                            serverBean.setInfo(bean.getInfo());
                            serverBean.setName(bean.getName());
                            serverBean.setTimer(bean.getTimer());
                            // ��ѡ�еĿͻ���������
                            sendMessage(serverBean);
                            break;
                        }
                        case 2: { // ��������ļ�
                            // ������������catbean�������͸��ͻ���
                            ChatBean serverBean = new ChatBean();
                            String info = bean.getTimer() + "  " + bean.getName()
                                    + "���㴫���ļ�,�Ƿ���Ҫ����";

                            serverBean.setType(2);//���ñ���������
                            serverBean.setClients(bean.getClients()); // ���Ƿ��͵�Ŀ�ĵ�
                            serverBean.setFileName(bean.getFileName()); // �ļ�����
                            serverBean.setSize(bean.getSize()); // ����ؼ���С
                            serverBean.setName(bean.getName()); // ��Դ
                            serverBean.setTimer(bean.getTimer());//��ʱ��
                            // ��ѡ�еĿͻ���������
                            sendMessage(serverBean);

                            break;
                        }
                        case 3: { // ȷ�������ļ�
                            ChatBean serverBean = new ChatBean();

                            serverBean.setType(3);
                            serverBean.setClients(bean.getClients()); // �ļ���Դ
                            serverBean.setTo(bean.getTo()); // �ļ�Ŀ�ĵ�
                            serverBean.setFileName(bean.getFileName()); // �ļ�����
                            serverBean.setIp(bean.getIp());
                            serverBean.setPort(bean.getPort());
                            serverBean.setName(bean.getName()); // ���յĿͻ�����
                            serverBean.setTimer(bean.getTimer());
                            // ֪ͨ�ļ���Դ�Ŀͻ����Է�ȷ�������ļ�
                            sendMessage(serverBean);
                            break;
                        }
                        case 4: {
                            ChatBean serverBean = new ChatBean();

                            serverBean.setType(4);
                            serverBean.setClients(bean.getClients()); // �ļ���Դ
                            serverBean.setTo(bean.getTo()); // �ļ�Ŀ�ĵ�
                            serverBean.setFileName(bean.getFileName());
                            serverBean.setInfo(bean.getInfo());
                            serverBean.setName(bean.getName());// ���յĿͻ�����
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

        // ��ѡ�е��û���������
        private void sendMessage(ChatBean serverBean) {
            // ����ȡ�����е�values
            Set<String> cbs = onlines.keySet();
            Iterator<String> it = cbs.iterator();
            // ѡ�пͻ�
            HashSet<String> clients = serverBean.getClients();
            while (it.hasNext()) {
                // ���߿ͻ�
                String client = it.next();
                // ѡ�еĿͻ����������ߵģ��ͷ���serverbean
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

        // �����е��û���������
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
                // ��ȡ�ӿͻ��˽��յ���catbean��Ϣ
                try {
                    ois = new ObjectInputStream(client.getInputStream());//����һ���������������ois����ȡclient����ļ�
                    bean = (ChatBean) ois.readObject();//
                    //ת��������Ⱥ���û�
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
