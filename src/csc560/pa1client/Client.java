package csc560.pa1client;
import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 * Created by Andrew on 2/2/16.
 */
public class Client {



    public static void main(String[] args) {
        // write your code here
        ServerSocket server;
        int reply;
        Socket sock;
        ObjectInputStream inputStream = null;
        ObjectOutputStream outputStream = null;
        boolean gameFinished = false;
        String message = "";
        try{

                sock = new Socket("localhost", 7788);
                do{

                    outputStream = new ObjectOutputStream(sock.getOutputStream());
                    outputStream.flush();
                    inputStream = new ObjectInputStream(sock.getInputStream());
//                    outputStream.writeObject("Connection Successful");
//                    outputStream.flush();
////                    outputStream.writeBytes("Fuck you!");
//                    System.out.println(inputStream.readByte());
                    try{
                        message = (String)inputStream.readObject();
                        System.out.println("server>" + message);
                        Scanner keyboard = new Scanner(System.in);
                        //try to write a message to the client
                        System.out.println("Enter message to send server: ");
                        outputStream.writeObject(keyboard.next());
//                        outputStream.writeObject("Test client");
                        outputStream.flush();
                        if (message.equals("bye"))
                            outputStream.writeObject("bye");
                        outputStream.flush();
                    }
                    catch(ClassNotFoundException classnot){
                        System.err.println("Data received in unknown format");
                    }
                }while(!message.equalsIgnoreCase("bye"));


        } catch (IOException e) {
            System.out.println("IOException on socket listen: " + e);
        }
    }

}
