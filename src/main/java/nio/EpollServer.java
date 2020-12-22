package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author yuxiaohui@cmiot.chinamobile.com
 * @Date: 2020-12-09.
 * @Time: 17:32
 */

public class EpollServer {
    public static void main(String[] args) {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress("127.0.0.1", 8000));
            ssc.configureBlocking(false);

            Selector selector = Selector.open();
            // 注册 channel，并且指定感兴趣的事件是 Accept
            ssc.register(selector, SelectionKey.OP_ACCEPT);

            ByteBuffer readBuff = ByteBuffer.allocate(1024);
            ByteBuffer writeBuff = ByteBuffer.allocate(128);
            writeBuff.put("received".getBytes());
            writeBuff.flip();

            while (true) {
                int nReady = selector.select();
                //得到所有可用channel的集合。
                Set<SelectionKey> keys = selector.selectedKeys();
                System.out.println("keys number:"+keys.size());
                Iterator<SelectionKey> it = keys.iterator();

                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    System.out.println("keys number2:"+keys.size());
                    it.remove();
                    System.out.println("keys number3:"+keys.size());
                    if (key.isAcceptable()) {
                        // 创建新的连接，并且把连接注册到selector上，而且，
                        // 声明这个channel只对读操作感兴趣。
                        SocketChannel socketChannel = ssc.accept();
                        socketChannel.configureBlocking(false);
                        //接受新的连接，然后把这个新的连接也注册到selector中去。
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        System.out.println("keys number4:"+keys.size());
                    } else if (key.isReadable()) {
                        System.out.println("keys number5:"+keys.size());
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        readBuff.clear();
                        socketChannel.read(readBuff);

                        readBuff.flip();
                        System.out.println("received : " + new String(readBuff.array()));
                        key.interestOps(SelectionKey.OP_WRITE);
                        System.out.println("keys number6:"+keys.size());
                    } else if (key.isWritable()) {
                        System.out.println("keys number7:"+keys.size());
                        writeBuff.rewind();
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        socketChannel.write(writeBuff);
                        key.interestOps(SelectionKey.OP_READ);
                        System.out.println("keys number8:"+keys.size());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
