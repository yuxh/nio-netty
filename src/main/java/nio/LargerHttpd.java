package nio;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;

/**
 *  accept new client socket connections, wrap them in an instance
 * of HttpdConnection, and then watch the client’s status with a Selector.
 */
public class LargerHttpd {
    Selector clientSelector;

    public void run(int port, int threads) throws IOException {
        clientSelector = Selector.open();
        //类似建立普通的ServerSocket，except that we must
        //first create an InetSocketAddress object to hold the local loopback address and port
        //combination of our server socket and then explicitly bind our socket to that address
        //with the ServerSocketChannel bind() method. We also configure the server socket
        //to nonblocking mode and register it with our main Selector so that we can select for
        //client connections in the same loop that we use to select for client read and write
        //readiness.
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        ssc.socket().bind(sa);
        ssc.register(clientSelector, SelectionKey.OP_ACCEPT);

        Executor executor = Executors.newFixedThreadPool(threads);

        while (true) {
            try {
                while (clientSelector.select(100) == 0) ;
                Set<SelectionKey> readySet = clientSelector.selectedKeys();
                for (Iterator<SelectionKey> it = readySet.iterator();it.hasNext(); ) {
                    final SelectionKey key = it.next();
                    it.remove();
                    if (key.isAcceptable()) {
                        acceptClient(ssc);
                    } else {
                        //取消所有监听
//                         It’s important that we change the interest set to zero to clear it before
//                        the next loop; otherwise, we’d be in a race to see whether the thread pool performed its
//                        maximum work before we detected another ready condition. Setting the interest ops to
//                        0 and resetting it in the HttpdConnection object upon completion ensures that only
//                        one thread is handling a given client at a time.

                        //有个误区，如果key的OP_READ已经存在，取消监听，isReadable仍然是true（因为针对的是readyOps）
                        key.interestOps(0);
//一旦检测到客户端准备发送或接收数据，就递交一个 Runnable task给Executor。这个task根据就绪的操作，在对应的客户端上调用 read() 或 write()。
                        executor.execute(new Runnable() {
                            public void run() {
                                try {
                                    handleClient(key);
                                } catch (IOException e) {
                                    System.out.println(e);
                                }
                            }
                        });
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    void acceptClient(ServerSocketChannel ssc) throws IOException {
        SocketChannel clientSocket = ssc.accept();
        System.out.println("accept success");
        clientSocket.configureBlocking(false);
        SelectionKey key = clientSocket.register(clientSelector,
                SelectionKey.OP_READ);
        //FIXME 是否意味着 一个socket 对应 一个key 对应一个channel 对应一个HttpdConnection？
        HttpdConnection client = new HttpdConnection(clientSocket);
        key.attach(client);
    }

    void handleClient(SelectionKey key) throws IOException {
        System.out.println("handleClient...");
        HttpdConnection client = (HttpdConnection) key.attachment();
        if (key.isReadable()) {
            client.read(key);
        } else {
            client.write(key);
        }
        clientSelector.wakeup();
    }

    public static void main(String argv[]) throws IOException {
        //new LargerHttpd().run( Integer.parseInt(argv[0]), 3/*threads*/ );
        new LargerHttpd().run(1235, 3/*threads*/);
    }
}
/**
 encapsulates a socket and handles the conversation with the client
 */
class HttpdConnection {
    static Charset charset = Charset.forName("8859_1");
    static Pattern httpGetPattern = Pattern.compile("(?s)GET /?(\\S*).*");
    SocketChannel clientSocket;
    ByteBuffer buff = ByteBuffer.allocateDirect(64 * 1024);
    String request;
    String response;
    FileChannel file;
    int filePosition;

    HttpdConnection(SocketChannel clientSocket) {
        this.clientSocket = clientSocket;
    }
//Each read gets as much data as available and checks
//to see whether we’ve reached the end of a line (a \n newline character).
    void read(SelectionKey key) throws IOException {
        if (request == null && (clientSocket.read(buff) == -1
                || buff.get(buff.position() - 1) == '\n'))
            processRequest(key);
        else//On each incomplete call to read(), we set the interest ops  of our key back to OP_READ
            key.interestOps(SelectionKey.OP_READ);
    }

    void processRequest(SelectionKey key) {
        buff.flip();
        request = charset.decode(buff).toString();
        Matcher get = httpGetPattern.matcher(request);
        if (get.matches()) {
            request = get.group(1);
            if (request.endsWith("/") || request.equals(""))
                request = request + "index.html";
            System.out.println("Request: " + request);
            try {
                file = new FileInputStream(request).getChannel();
            } catch (FileNotFoundException e) {
                response = "404 Object Not Found";
            }
        } else
            response = "400 Bad Request";

        if (response != null) {
            buff.clear();
            charset.newEncoder().encode(
                    CharBuffer.wrap(response), buff, true);
            buff.flip();
        }
        //完成了读和处理请求，准备发送响应
        key.interestOps(SelectionKey.OP_WRITE);
    }

    void write(SelectionKey key) throws IOException {
        if (response != null) {
            clientSocket.write(buff);
            if (buff.remaining() == 0)
                response = null;
        } else if (file != null) {
            int remaining = (int) file.size() - filePosition;
            long sent = file.transferTo(filePosition, remaining,
                    clientSocket);
            if (sent >= remaining || remaining <= 0) {
                file.close();
                file = null;
            } else
                filePosition += sent;
        }
        //When we’re done, we close the client
        //socket and cancel our key, which causes it to be removed from the Selector’s key set
        //during the next select operation (discarding our HttpdConnection object with it).
        if (response == null && file == null) {
            clientSocket.close();
            key.cancel();
        } else
            key.interestOps(SelectionKey.OP_WRITE);
    }
}
