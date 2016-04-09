package csc560;

import java.io.*;
import java.net.Socket;

/**
 * Created by Andrew on 4/9/16.
 */
public class TestClient {
    int counter = 0;
    public static void main(String args[]) {
        TestClient tc = new TestClient();
        while (true) {
            new Thread() {
                @Override
                public void run() {
                    Socket socket = null;
                    try {
                        socket = new Socket("localhost", 7788);
                        tc.counter++;
                        while (true) {
                            PrintWriter out = new PrintWriter(socket.getOutputStream(),
                                    true);

                            out.println("FUCK YOU FROM CLIENT "+tc.counter);
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

            }.start();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
