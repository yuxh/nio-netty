package bio.threadPool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author yuxiaohui@cmiot.chinamobile.com
 * @Date: 2020-10-09.
 * @Time: 15:10
 */
public class ServerPool {
    private static final int THREAD_POOL_SIZE = 2;

    public static void main(String[] args) throws IOException {
        //服务端在20006端口监听客户端请求的TCP连接
        final ServerSocket server = new ServerSocket(20006);

        //在线程池中一共只有THREAD_POOL_SIZE个线程，
        //最多有THREADPOOLSIZE个线程在accept()方法上阻塞等待连接请求
        for(int i = 0; i< THREAD_POOL_SIZE; i++){
            //匿名内部类，当前线程为匿名线程，还没有为任何客户端连接提供服务
            Thread thread = new Thread(() -> {
                //线程为某连接提供完服务后，循环等待其他的连接请求
                while (true) {
                    try {
                        //等待客户端的连接
                        Socket client = server.accept();
                        System.out.println("与客户端连接成功！");
                        //一旦连接成功，则在该线程中与客户端通信
                        ServerThread.execute(client);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            //先将所有的线程开启
            thread.start();
        }
    }
}

