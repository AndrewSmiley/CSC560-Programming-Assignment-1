package csc560.pa1server;
import com.sun.tools.doclets.formats.html.SourceToHTMLConverter;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Server {

    public static void main(String[] args) {
        // write your code here
        System.out.println("it started");
        while (true) {
            ServerSocket server;
            int reply;
            Socket sock;
            ObjectInputStream inputStream = null;
            ObjectOutputStream outputStream = null;
            try{
                server = new ServerSocket(7788);
                sock = server.accept();

                outputStream = new ObjectOutputStream(sock.getOutputStream());
                outputStream.flush();
                inputStream = new ObjectInputStream(sock.getInputStream());
                outputStream.writeObject("Connection Successful");
                outputStream.flush();
                String message = "";
                do{
                    try{
                        message = (String)inputStream.readObject();
                        System.out.println("client>" + message);
                        Scanner keyboard = new Scanner(System.in);
                        //try to write a message to the client
                        System.out.println("Enter message to send client: ");
                        outputStream.writeObject(keyboard.next());
                        outputStream.flush();
                        if (message.equals("bye"))
                            outputStream.writeObject("bye");
                            outputStream.flush();
                    }
                    catch(Exception exception){
                        exception.printStackTrace();
                    }
                }while(!message.equals("bye"));

    //            new PrintWriter(outputStream, true).println();
    //            new BufferedReader(inputStream.).readLine();
    //              System.out.println(inputStream.);
    //            outputStream.writeBytes("You suck, message received");



            } catch (IOException e) {
                System.out.println("IOException on socket listen: " + e);
            }finally {

                try {
                    inputStream.close();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
