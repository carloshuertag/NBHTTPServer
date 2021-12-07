package Server;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 *
 * @author huert
 */
public class HTTPServer{
    
    private final int port;
    protected ServerSocket serverSocket = null;

    public HTTPServer(int port) {
        this.port = port;
    }

    public void server() {
        try {
            Selector selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            server.socket().bind(new InetSocketAddress("localhost", port));
            server.register(selector, SelectionKey.OP_ACCEPT);
            int x;
            Iterator<SelectionKey> iterator;
            SelectionKey key;
            SocketChannel channel, client;
            HTTPConnection connection;
            for(;;){
                if((x = selector.select()) == 0) continue;
                iterator = selector.selectedKeys().iterator();
                while(iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    if(key.isAcceptable()){
                        client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        continue;
                    }
                    if(key.isReadable()){
                        channel = (SocketChannel) key.channel();
                        connection = new HTTPConnection(channel);
                        connection.processRequest();
                    }
                }
            }
        } catch(Exception ex) {
            System.err.println("Exception: "+ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    public static void main(String args[]) {
        HTTPServer httpServer = new HTTPServer(Properties.PORT);
        httpServer.server();
    }
    
}
