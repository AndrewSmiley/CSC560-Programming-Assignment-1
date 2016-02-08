package csc560.pa1server;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.LinkedList;
import java.util.Queue;

//http://www.java2s.com/Tutorial/Java/0320__Network/TestnonblockingacceptusingServerSocketChannel.htm
public class Server{
    ServerSocket providerSocket;

    ObjectOutputStream out;
    ObjectInputStream in;
    String message;
    Server(){}
    void run()
    {



        try{

            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(7788));
            ssc.configureBlocking(false);
            LinkedList<SocketChannel> connections = new LinkedList<SocketChannel>();

            System.out.println("Waiting for connection");
            while (true) {
                SocketChannel sc = null;
                if (connections.size() == 0) {
                    //if we don't have any queued connections
                    sc = ssc.accept();
                    //if it's null, we want to wait till we get an incoming conneciton
                    if(sc == null){
                        while (sc == null){
                            Thread.sleep(2000);
                            sc= ssc.accept();
                            System.out.println((sc == null) ? "Waiting for connection": "Accepted new incoming connection");
                        }
                    }
                }else{
                    //otherwise grab it off the queue
                    sc = connections.pop();
                    System.out.println("Grabbed connection from Queue");

                }
                boolean gameRunning = true;
                Socket connection = sc.socket();
                while (gameRunning){

                    System.out.println("Connection received from " + connection.getInetAddress().getHostName());
                    //3. get Input and Output streams
                    out = new ObjectOutputStream(connection.getOutputStream());
                    out.flush();
                    in = new ObjectInputStream(connection.getInputStream());
                    sendMessage("Connection successful");
                    //4. The two parts communicate via the input and output streams
                    do{
                        try{
                            //check incoming connections
                            SocketChannel throwAway = ssc.accept();
                            if (throwAway != null){
                                connections.add(throwAway);
                            }
                            Thread.sleep(5000);
                            message = (String)in.readObject();
                            System.out.println("client>" + message);
                            if (message.equals("bye"))
                                sendMessage("bye");
                        }
                        catch(ClassNotFoundException classnot){
                            System.err.println("Data received in unknown format");
                        }
                    }while(!message.equals("bye"));
                    gameRunning = false;
                }

            }
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally{
            //4: Closing connection
            try{
                in.close();
                out.close();
                providerSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
    void sendMessage(String msg)
    {
        try{
            out.writeObject(msg);
            out.flush();
//            System.out.println("server>" + msg);
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    public static void main(String args[])
    {
        Server server = new Server();
        while(true){
            server.run();
        }
    }
}