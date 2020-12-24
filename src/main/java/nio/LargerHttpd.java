package nio;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;

public class LargerHttpd {
    Selector clientSelector;

    public void run(int port, int threads) throws IOException {
        clientSelector = Selector.open();
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

    void read(SelectionKey key) throws IOException {
        if (request == null && (clientSocket.read(buff) == -1
                || buff.get(buff.position() - 1) == '\n'))
            processRequest(key);
        else
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
        if (response == null && file == null) {
            clientSocket.close();
            key.cancel();
        } else
            key.interestOps(SelectionKey.OP_WRITE);
    }
}
