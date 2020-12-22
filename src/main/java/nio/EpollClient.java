package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author yuxiaohui@cmiot.chinamobile.com
 * @Date: 2020-12-09.
 * @Time: 17:42
 */
public class EpollClient {
    public static void main(String[] args) {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress("127.0.0.1", 8000));

            ByteBuffer writeBuffer = ByteBuffer.allocate(32);
            ByteBuffer readBuffer = ByteBuffer.allocate(32);

            writeBuffer.put("hello".getBytes());
            writeBuffer.flip();

            while (true) {
                writeBuffer.rewind();
                //请求
                socketChannel.write(writeBuffer);
                readBuffer.clear();
                //响应
                socketChannel.read(readBuffer);
            }
        } catch (IOException e) {
        }
    }
}
