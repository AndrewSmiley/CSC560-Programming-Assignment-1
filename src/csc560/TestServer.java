package csc560;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Andrew on 4/9/16.
 */
public class TestServer {

    class T implements Runnable{
        Socket socket;
        int count;
        public  T (Socket s, int count ){
            socket = s;
            this.count = count;

        }
        @Override
        public void run() {
            PrintWriter out = null;
            try {
                while (true) {
                    out = new PrintWriter(socket.getOutputStream(),
                            true);
                    out.println("FUCK YOU FROM SERVER "+count);
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));
                    System.out.println(in.readLine());
                    Thread.sleep(2000);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void spawnNewThread(Socket s, int count){
        new Thread(new T(s, count)).start();
    }

    public static void main(String args[]) {
        TestServer testServer = new TestServer();
        try {
            int clients = 0;
            ServerSocket serverSocket = new ServerSocket(7788);
            while (true) {

                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (socket != null){
                    clients ++;
                    testServer.spawnNewThread(socket, clients);

                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
