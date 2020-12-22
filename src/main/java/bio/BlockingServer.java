package bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author yuxiaohui@cmiot.chinamobile.com
 * @Date: 2020-10-10.
 * @Time: 18:06
 */
public class BlockingServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        // 为了简单起见，所有的异常信息都往外抛
        int port = 6666;
        // 定义一个ServiceSocket监听在端口8899上
        ServerSocket server = new ServerSocket(port);
        System.out.println("等待与客户端建立连接...");
        while (true) {
            // server尝试接收其他Socket的连接请求，server的accept方法是阻塞式的
            Socket socket = server.accept();
            /**
             * 我们的服务端处理客户端的连接请求是同步进行的， 每次接收到来自客户端的连接请求后，
             * 都要先跟当前的客户端通信完之后才能再处理下一个连接请求。 这在并发比较多的情况下会严重影响程序的性能，
             * 为此，我们可以把它改为如下这种异步处理与客户端通信的方式
             */
            // 每接收到一个Socket就建立一个新的线程来处理它
//            new Thread(new Task(socket)).start();
            System.out.println("just doing slowly...");
            Thread.sleep(40000);
            System.out.println("ok...");
        }
//        server.close();
    }
}
