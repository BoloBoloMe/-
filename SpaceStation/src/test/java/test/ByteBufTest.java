package test;

import com.bolo.downloader.respool.nio.ByteBuffUtils;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

public class ByteBufTest {
    public static void main(String[] args) {
        ByteBuf byteBuf = ByteBuffUtils.copy("hello", Charset.forName("utf8"));
        while (byteBuf.isReadable()){
            System.out.println(byteBuf.readByte());
        }
        System.out.println(byteBuf.toString());
    }
}
