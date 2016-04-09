package csc560;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by Andrew on 4/9/16.
 */
public class TestClient {
    int counter = 0;
    int serverid = 666;
    int clientid = 777;

    public void playGame() {
        try {


            System.out.println("Client (You) is O, Server is X.\nMoves are zero indexed, i.e. row 1 is row 0, row 2 is row 1, etc. Columns are the same way");
            Socket socket = new Socket("localhost", 7788);
            PrintWriter out = new PrintWriter(socket.getOutputStream(),
                    true);

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
//            String message =
            int[][] board = new int[3][3];
            //get the inital message
            String move = recieveMessage(in);
            //if user makes the first move
            if(move.contains("NONE")){
                move = getUserMove(board);
                sendMessage(out,move);
                //just keep an eye on the board
                processMove(board, move, clientid);
                System.out.println("Client Turn");
                printBoard(board);

            }else{
                //if server makes first move
                processMove(board, move, serverid);
                System.out.println("Server Turn");
                printBoard(board);
                //now we get the user move
                move = getUserMove(board);
                sendMessage(out,move);
                //just keep an eye on the board
                processMove(board, move, clientid);
                System.out.println("Client Turn");
                printBoard(board);

            }

            while(true){
                //get the new move
                move = recieveMessage(in);
                //if the game is over, break
                if(move.toLowerCase().contains("win") || move.toLowerCase().contains("loss") || move.toLowerCase().contains("tie")){
                    processMove(board, move, serverid);
                    System.out.println(move);
                    break;
                }

                //if server makes first move
                processMove(board, move, serverid);
                System.out.println("Server Turn");
                printBoard(board);

                //now we get the user move
                move = getUserMove(board);
                sendMessage(out,move);
                //just keep an eye on the board
                processMove(board, move, clientid);
                System.out.println("Client Turn");
                printBoard(board);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * for the automation testing
     * @param board
     * @return
     */
    public ArrayList<String> getAvailableMoves(int[][] board){
        ArrayList<String> moves = new ArrayList<String>();
        for (int i =0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                if(board[i][j] == 0){
                    moves.add("MOVE "+i+" "+j);
                }
            }
        }
        return moves;
    }

    public void printBoard(int[][] board){
        for(int i = 0; i < 3; i++){
            StringBuilder sb = new StringBuilder();
            for(int j = 0; j < 3; j++){
//                sb.append("|");
//                r = r.concat((board[i][j]== 0) ? "_" : (String) board[i][j]));
                sb.append(String.valueOf((board[i][j]== 0) ? "_" : board[i][j]));
                sb.append(" ");
//                sb.append("|");
            }

            System.out.println(sb.toString().replace("666", "X").replace("777", "O"));
        }
        System.out.println("\n");
    }
    public String getUserMove(int[][] board){
        System.out.println("Your Turn (but it's automated");
//        Scanner reader = new Scanner(System.in);  // Reading from System.in
//        System.out.print("Enter a the row: ");
//        int r = reader.nextInt();
//        System.out.print("Enter a the column: ");
//        int c = reader.nextInt();
        int random = new Random().nextInt(getAvailableMoves(board).size());
        return getAvailableMoves(board).get(random);



//        return "MOVE "+r+" "+c;
    }
    public void sendMessage(PrintWriter out, String message){
        out.println(message);
    }

    public String recieveMessage(BufferedReader bf){
        try {
            return bf.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    void processMove(int[][] board, String move, int user ){
        //get the rows and columns
        String[] tmp = move.split(" ");
        int row = Integer.parseInt(tmp[1]);
        int column = Integer.parseInt(tmp[2]);
        board[row][column] = user;

    }



    public static void main(String args[]) {
        TestClient tc = new TestClient();
        tc.playGame();
//        TestClient tc = new TestClient();
//        while (true) {
//            new Thread() {
//                @Override
//                public void run() {
//                    Socket socket = null;
//                    try {
//                        socket = new Socket("localhost", 7788);
//                        tc.counter++;
//                        while (true) {
//                            PrintWriter out = new PrintWriter(socket.getOutputStream(),
//                                    true);
//
//                            out.println("FUCK YOU FROM CLIENT "+tc.counter);
//                            BufferedReader in = new BufferedReader(new InputStreamReader(
//                                    socket.getInputStream()));
//                            System.out.println(in.readLine());
//                            Thread.sleep(2000);
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//
//            }.start();
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }
}
