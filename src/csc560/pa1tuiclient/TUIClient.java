package csc560.pa1tuiclient;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by pridemai on 4/2/16.
 */
public class TUIClient {
    public int CLIENT_ID=666;
    public int SERVER_ID=777;

    public ArrayList<String> getAvailableMoves(int[][] board){
        ArrayList<String> moves = new ArrayList<String>();
        for (int i =0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                if(board[i][j] == 0){
                    moves.add(""+i+""+j);
                }
            }
        }
        return moves;
    }

    public String getUserMove(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.println("Enter a the row: ");
        int r = reader.nextInt();
        System.out.println("Enter a the column: ");
        int c = reader.nextInt();



        return ""+r+""+c;
    }

    public void sendMessage(String sendMessage, Socket socket){
        //Send the message to the server
        OutputStream os = null;
        try {
            os = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);
        try {
            bw.write(sendMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Message sent to the server : "+sendMessage);
    }

    public String recieveMessage(Socket socket){
        //Get the return message from the server
        InputStream is = null;
        try {
            is = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringBuffer buffer = new StringBuffer();
        try {
            while(br.ready()){
                char[] c = new char[] { 1024 };
                br.read(c);
                buffer.append(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String message = buffer.toString();
        System.out.println("Message received from the server : " +message);
        return message;

    }

    public static void main(String args[]) {
        TUIClient tc = new TUIClient();


        Socket socket = null;
        try
        {
            String host = "localhost";
            int port = 7788;
            System.out.println("Connecting.");
            InetAddress address = InetAddress.getByName(host);
            socket = new Socket(address, port);
            System.out.println("Connected.");

            int[][] board = new int[3][3];
            boolean gameOn = true;
            String message = tc.recieveMessage(socket);
            //we make the first move
            if (message.contains("NONE")){
                String move = tc.getUserMove();
                tc.sendMessage("MOVE "+move.substring(0,1)+" "+move.substring(1,2), socket);
                board[Integer.parseInt(move.substring(0,1))][Integer.parseInt(move.substring(1,2))] = tc.CLIENT_ID;

            }else{
//                String move = tc.recieveMessage(socket);
                String[] tmp = message.split(" ");
                int row = Integer.parseInt(tmp[1]);
                int column = Integer.parseInt(tmp[2]);
                board[row][column] = tc.SERVER_ID;
                String move = tc.getUserMove();
                tc.sendMessage("MOVE "+move.substring(0,1)+" "+move.substring(1,2), socket);
                board[Integer.parseInt(move.substring(0,1))][Integer.parseInt(move.substring(1,2))] = tc.CLIENT_ID;

            }
            while (gameOn){
                String move = tc.recieveMessage(socket);
                while(move.equalsIgnoreCase("")){
                    Thread.sleep(1000);
                    move = tc.recieveMessage(socket);
                }
                if(message.contains("WIN")){
                    break;
                }
                //we make the first move

//                String move = tc.recieveMessage(socket);
                String[] tmp = move.split(" ");
                int row = Integer.parseInt(tmp[1]);
                int column = Integer.parseInt(tmp[2]);
                board[row][column] = tc.SERVER_ID;

                move = tc.getUserMove();
                tc.sendMessage("MOVE "+move.substring(0,1)+" "+move.substring(1,2), socket);
                board[Integer.parseInt(move.substring(0,1))][Integer.parseInt(move.substring(1,2))] = tc.CLIENT_ID;





            }




        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            //Closing the socket
            try
            {
                socket.close();
                System.out.println("Closed.");
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
