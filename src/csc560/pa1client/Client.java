package csc560.pa1client;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client{

//    int[][] board = buildNewBoard();
//    GridButtonPanel gridButtonPanel;
//
//    /**
//     * @see http://stackoverflow.com/questions/7702697
//     */

    public static class GridButtonPanel {

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


        Socket requestSocket;
        ObjectOutputStream out;
        ObjectInputStream in;
        String message;
//        Client(){}
        int CLIENT_ID = 666;
        int SERVER_ID = 777;
        static volatile boolean shouldWaitForUserInput = false;
        static final Object lock = new Object();
        int[][] board = buildNewBoard();
        private static final int N = 3;
        private final List<JButton> list = new ArrayList<JButton>();

        void updateButton(int row, int column, int player){
            JButton button = this.getGridButton(row,column);
            if (player==SERVER_ID){
                button.setText("X");
                button.setEnabled(false);
            }
        }
        void run()
        {
            try{
//            GridLayout experimentLayout = new GridLayout(0,2);
                //1. creating a socket to connect to the server
//                gridButtonPanel = new GridButtonPanel();
//                gridButtonPanel.display();
                while (requestSocket == null) {
                    try {
                        requestSocket = new Socket("localhost", 7788);
                    } catch (ConnectException e) {
                        e.printStackTrace();
                    }
                }
//            requestSocket.getInputStream()
                System.out.println("Connected to localhost in port 7788");
                Thread t = Thread.currentThread();
                String name = t.getName();
                System.out.println("Current thread name: " + name);
                Thread.sleep(2000);
                //2. get Input and Output streams
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                //send the initial message
                String initialMessage =readIncomingMessage(in);
                if (initialMessage.equalsIgnoreCase("NONE")){
                    while (shouldWaitForUserInput){
//                    System.out.println("waiting: "+shouldWaitForUserInput);
                    }
                    System.out.println("Get the first user move");


                }else{
                    processMove(initialMessage, board,SERVER_ID);
                    System.out.println("Get the first server move");
                    printBoard(board);
                    System.out.println("Get the second user move");
                    shouldWaitForUserInput = true;
                    while (shouldWaitForUserInput){
//                    System.out.println("waiting: "+shouldWaitForUserInput);
                    }
                    System.out.println("Got the second user move");
                }
                printBoard(board);

                //3: Communicating with the server
                while(true){
                    System.out.println("begin looping");
                    message = readIncomingMessage(in);
                    if (message.contains("WIN") || message.contains("LOSS") || message.contains("TIE")){
                        processEndOfGame(message);
                        break;
                    }
                    System.out.println("Get the server move");
                    processMove(message, board, SERVER_ID);

                    printBoard(board);
                    System.out.println("Get the client move");
                    shouldWaitForUserInput = true;
                    while (shouldWaitForUserInput){
//                        System.out.println("waiting: "+shouldWaitForUserInput);
                    }


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

        }

        void processClientAction(int[][] board, BoardSpace space){
//        BoardSpace space = getUserInput();
            board[space.row][space.column]= CLIENT_ID;
            sendMessage("MOVE "+space.row+" "+space.column);
            shouldWaitForUserInput = false;
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
            updateButton(row, column, player);
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
                System.out.println(msg);
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

        void processEndOfGame(String message){
            printBoard(board);
            if (message.contains("WIN")){
                System.out.println("You win buttfucker");
            }else if (message.contains("LOSS")){
                System.out.println("You lose buttfucker");
            }else if (message.contains("TIE")){
                System.out.println("You tie buttfucker");
            }
            System.exit(0);
        }
        private JButton getGridButton(int r, int c) {
            int index = r * N + c;
            return list.get(index);
        }

        private JButton createGridButton(final int row, final int col) {
            final JButton b = new JButton();

            b.addActionListener(e -> {
                System.out.println("button clicked");
                JButton gb = GridButtonPanel.this.getGridButton(row, col);
                gb.setText("O");
                gb.setEnabled(false);
                System.out.println("We changed the value from ");
                GridButtonPanel.shouldWaitForUserInput = false;
                Thread t = Thread.currentThread();
                String name = t.getName();
                System.out.println("Current thread name: " + name);

                processClientAction(board, new BoardSpace(row, col));
            });
            return b;
        }

        private JPanel createGridPanel() {
            JPanel p = new JPanel(new GridLayout(N, N));
            for (int i = 0; i < N * N; i++) {
                int row = i / N;
                int col = i % N;
                JButton gb = createGridButton(row, col);
                list.add(gb);
                p.add(gb);
            }
            return p;
        }

        private void display() {
            JFrame f = new JFrame("GridButton");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add(createGridPanel());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            this.run();
        }


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
//        Client client = new Client();
        GridButtonPanel gpb = new GridButtonPanel();
        gpb.display();
    }
}