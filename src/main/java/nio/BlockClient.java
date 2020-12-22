package nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


/**
 * @author yuxiaohui@cmiot.chinamobile.com
 * @Date: 2020-10-10.
 * @Time: 18:11
 */
public class BlockClient {

    public static void main(String[] args) throws IOException {

        // 1. 获取通道
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 6666));

        // 2. 发送一张图片给服务端吧
        FileChannel fileChannel = FileChannel
                .open(Paths.get("E:\\a.png"), StandardOpenOption.READ);

        // 3.要使用NIO，有了Channel，就必然要有Buffer，Buffer是与数据打交道的呢
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        // 4.读取本地文件(图片)，发送到服务器
        while (fileChannel.read(buffer) != -1) {

            // 在读之前都要切换成读模式
            buffer.flip();

            socketChannel.write(buffer);

            // 读完切换成写模式，能让管道继续读取文件的数据
            buffer.clear();
        }
        System.out.println("请求完毕！");
        //必须告诉服务器已经写完了！
        socketChannel.shutdownOutput();
        System.out.println("已经关闭连接，但没有关闭通道！");
//接收服务器的响应
        int len = 0;
        while ((len = socketChannel.read(buffer))!=-1){
            buffer.flip();
            System.out.println(new String(buffer.array(),0,len));
        }
        // 5. 关闭流
        fileChannel.close();
        socketChannel.close();
    }
}
