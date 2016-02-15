package csc560.pa1client;

import sun.misc.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client{
    Socket requestSocket;
    ObjectOutputStream out;
    ObjectInputStream in;
    String message;
    Client(){}
    int CLIENT_ID = 666;
    int SERVER_ID = 777;

    private class BoardSpace{
        int row;
        int column;

        public BoardSpace(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public BoardSpace() {
            this.row = 0;
            this.column = 0;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getColumn() {
            return column;
        }

        public void setColumn(int column) {
            this.column = column;
        }
    }
    void run()
    {
        try{
            GridLayout experimentLayout = new GridLayout(0,2);
            //1. creating a socket to connect to the server

            while (requestSocket == null) {
                try {
                    requestSocket = new Socket("localhost", 7788);
                } catch (ConnectException e) {
                    e.printStackTrace();
                }
            }
//            requestSocket.getInputStream()
            System.out.println("Connected to localhost in port 7788");
            Thread.sleep(2000);
            //2. get Input and Output streams
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
            int[][] board = buildNewBoard();
            //send the initial message
            String initialMessage =readIncomingMessage(in);
            if (initialMessage.equalsIgnoreCase("NONE")){
                processClientAction(board);

            }else{
                processMove(initialMessage, board,SERVER_ID);
                printBoard(board);
                processClientAction(board);
            }
            printBoard(board);

            //3: Communicating with the server
            while(true){

                    message = readIncomingMessage(in);
                if (message.contains("WIN") || message.contains("LOSS") || message.contains("TIE")){break;}

                    processMove(message, board, SERVER_ID);
                    printBoard(board);
                    processClientAction(board);
//                    System.out.println("server>" + message);
//                  /  sendMessage("Hi my server");
//                    message = "bye";
//                    sendMessage(message);

            }
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
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
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }

    String readIncomingMessage(ObjectInputStream in){
        try {
            return (String) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return "";
//        BufferedReader bf = new BufferedReader(new InputStreamReader(in));
//        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
//        String line;
//        try {
//            while((line = bf.readLine()) != null)
//            {
//                System.out.println("echo: " + line);
//
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        StringBuilder sb=new StringBuilder();
//        BufferedReader br = new BufferedReader(new InputStreamReader(in));
//        String read;
//
//        try {
//            while((read=br.readLine()) != null) {
//                //System.out.println(read);
//                sb.append(read);
//            }
//
//
//        br.close();
//        return sb.toString();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return "";
//        try
//        {
////            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
//            int bytesRead = 0;
//            in.re
//            messageByte[0] = in.readByte();
//            messageByte[1] = in.readByte();
//
//            int bytesToRead = messageByte[1];
//
//            while(!end)
//            {
//                bytesRead = in.read(messageByte);
//                messageString += new String(messageByte, 0, bytesRead);
//                if (messageString.length() == bytesToRead )
//                {
//                    end = true;
//                }
//            }
//            System.out.println("MESSAGE: " + messageString);
//            return dataString;
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//return"";

//        return messageString;
    }

    void processClientAction(int[][] board){
        BoardSpace space = getUserInput();
        board[space.row][space.column]= CLIENT_ID;
        sendMessage("MOVE "+space.row+" "+space.column);
    }
    int[][] buildNewBoard(){
        return new int[3][3];
    }
    void printBoard(int[][] board){
        for (int i =0; i <3;i++){
            String row = "";
            for(int j = 0;j < 3; j++){
                if (board[i][j] == 0){
                    row = row+"| |";
                }else if (board[i][j] == CLIENT_ID){
                    row = row+"|O|";
                }else if (board[i][j] == SERVER_ID){
                    row = row+"|X|";
                }

            }
            System.out.println(row+"\n\n");
        }
        System.out.println("\n\n");
    }

    /**
     * Process a move
     * @param move
     * @param board
     * @param player
     * @return
     */
    void processMove(String move, int[][] board, int player){
        System.out.println(move);

        String[] tmp=move.split(" ");
        int  row = Integer.parseInt(tmp[1]);
        int column = Integer.parseInt(tmp[2]);
        if(board[row][column] == 0){
            board[row][column] = player;

        }else{
            return;
        }
    }
    BoardSpace getUserInput(){
                Scanner scan= new Scanner(System.in);
                        System.out.print("Enter the row: ");
                int row= scan.nextInt();
                System.out.print("Enter the column: ");
                int column= scan.nextInt();
        return new BoardSpace(row, column);

    }
    void sendMessage(String msg)
    {

        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
//            Writer writer = new BufferedWriter(out, "UTF-8");
//            out.writeObject(msg);
//            out.flush();
//            System.out.println("client>" + msg);

    }
    public static void main(String args[])
    {
//        JFrame.setDefaultLookAndFeelDecorated(true)
//// int i = 3;
//        int j = 4;
//        JPanel[][] panelHolder = new JPanel[i][j];
//
//        for(int m = 0; m < i; m++) {
//            for(int n = 0; n < j; n++) {
//                panelHolder[m][n] = new JPanel();
//                add(panelHolder[m][n]);
//            }
//        };
//        JFrame frame = new JFrame("GridLayout Test");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        fra

//        frame.setLayout(new GridLayout(3, 3));
//        frame.add(new JButton("Button 1"));
//        frame.add(new JButton("Button 2"));
//        frame.add(new JButton("Button 3"));
//        frame.add(new JButton("Button 4"));
//        frame.add(new JButton("Button 5"));
//        frame.add(new JButton("Button 6"));
//        frame.add(new JButton("Button 7"));
//        frame.add(new JButton("Button 8"));
//        frame.pack();
//        frame.setVisible(true);

        Client client = new Client();
        client.run();
    }
}