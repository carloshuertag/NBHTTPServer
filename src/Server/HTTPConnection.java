package Server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.StringTokenizer;

/**
 *
 * @author huert
 */
public class HTTPConnection{
    protected SocketChannel socket;
    protected ByteBuffer buffer;
    protected PrintWriter pw;
    protected BufferedOutputStream bos;
    protected BufferedReader br;
    protected String fileName;
                        
    public HTTPConnection(SocketChannel socket) {
        this.socket = socket;
    }
	
    public void processRequest() {
        try {
            buffer = ByteBuffer.allocate(Properties.BUFFER_SIZE);
            buffer.clear();
            int t = socket.read(buffer);
            buffer.flip();
            String request = new String(buffer.array(), 0, t);
            System.out.println("t: "+t);
            if(request == null) {
                htmlResponse(200, "OK", "Empty line, Hello from HTTPServer");
                return;
            }
            System.out.println("\nClient connected from: "+socket.socket().getInetAddress());
            System.out.println("At port: "+socket.socket().getPort());
            System.out.println("Request:\n"+request+"\r\n\r\n");
            StringTokenizer st1= new StringTokenizer(request,"\n");
            String line = st1.nextToken();
            if(line.indexOf("?") == -1) {
                if(line.toUpperCase().startsWith("POST")){
                    String lastToken = request.substring(request.lastIndexOf("\n"));
                    System.out.println(lastToken);
                    paramsResponse(lastToken);
                } else if (line.toUpperCase().startsWith("DELETE")) {
                    getFileName(line);
                    delete();
                } else { //HEAD OR GET WITHPUT PARAMS
                    getFileName(line);
                    boolean get = (line.toUpperCase().startsWith("GET"));
                    if (fileName.compareTo("") == 0) sendFile("index.htm", get);
                    else sendFile(fileName, get);
                }
            } else if(line.toUpperCase().startsWith("GET")) { //GET WITH PARAMS
                StringTokenizer tokens=new StringTokenizer(line,"?");
                String req_a = tokens.nextToken();
                System.out.println("Token1: "+req_a);
                String req = tokens.nextToken();
                System.out.println("Token2: "+req);
                String params = req.substring(0, req.indexOf(" "))+"\n";
                paramsResponse(params);
            } else
                sendStatus(501, "Not Implemented");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private void paramsResponse(String params) throws IOException{
        System.out.println("Params: "+params);
        StringBuffer response= new StringBuffer();
        response.append("HTTP/1.0 200 Okay \n");
        response.append("Date: ").append(new Date()).append(" \n");
        String mimeType = "Content-Type: text/html \n\n";
        response.append(mimeType);
        response.append("<html><head><title>SERVIDOR WEB</title></head>\n");
        response.append("<body bgcolor=\"#AACCFF\"><center><h1><br>Parametros Obtenidos..</br></h1><h3><b>\n");
        response.append(params);
        response.append("</b></h3>\n</center></body></html>\n\n");
        System.out.println("Response: "+response);
        buffer = ByteBuffer.wrap(response.toString().getBytes());
        socket.write(buffer);
        socket.close();
    }
        
    private void delete() throws IOException{
        System.out.println("DELETE");
        File file = new File(fileName);
        if(file.exists())
            if(Files.isWritable(Paths.get(fileName))){
                file.delete();
                System.out.println(fileName + " deleted");
                htmlResponse(200, "OK", "File deleted");
            } else {
                System.out.println(fileName + " not deleted");
                htmlResponse(200, "OK", "File not deleted");
            }
        else
            htmlResponse(404, "Not found", "404 Not Found");
    }
    
    private void htmlResponse(int code, String status, String msg) throws IOException{
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.0 "+code+" "+status+"\r\n");
        sb.append("Date: ").append(new Date()).append(" \n");
        String mimeType = "Content-Type: text/html \n\n";
        sb.append(mimeType);
        sb.append("<html><head><title>Servidor WEB\n");
        sb.append("</title><body bgcolor=\"#AACCFF\"<br>"+msg+"</br>\n");
        sb.append("</body></html>\n");
        buffer = ByteBuffer.wrap(sb.toString().getBytes());
        socket.write(buffer);
        socket.close();
    }

    private void getFileName(String line) {
        int i, f;
        i=line.indexOf("/");
        f=line.indexOf(" ",i);
        fileName=line.substring(i+1,f);
    }
    
    private void sendStatus(int code, String msg) throws IOException{
        String response = "HTTP/1.0 "+code+" "+msg+"\r\n";
        System.out.println(response);
        buffer = ByteBuffer.wrap(response.toString().getBytes());
        socket.write(buffer);
        socket.close();
    }

    private void sendFile(String filePath, boolean get)
        throws IOException{
        int x = 0;
        DataInputStream dis = new DataInputStream(new FileInputStream(filePath));
        byte[] buf = new byte[1024];
        File ff = new File(filePath);			
        long fileSize = ff.length(),cont=0;
        StringBuffer sb = new StringBuffer();
        sb.append("HTTP/1.0 200 ok\r\n").append("Server: HTTPServer/1.0 \r\n");
        sb.append("Date: ").append(new Date()).append(" \r\n");
        sb.append("Content-Type: text/html \r\n");
        sb.append("Content-Length: ").append(fileSize).append(" \r\n\r\n");
        System.out.println(sb);
        buffer = ByteBuffer.wrap(sb.toString().getBytes());
        socket.write(buffer);
        if(get) while(cont<fileSize) {
                x = dis.read(buf);
                buffer = ByteBuffer.wrap(buf, 0, x);
                socket.write(buffer);
                cont += x;
            }
        dis.close();
        socket.close();
    }
}
