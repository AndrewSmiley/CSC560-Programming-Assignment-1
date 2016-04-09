package csc560;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Andrew on 4/9/16.
 */
public class TestServer {
    int serverid = 666;
    int clientid = 777;
    int tieflag = 6969;
    int nowinnerflag=9999;
    volatile int wins = 0;
    volatile int losses = 0;
    volatile int ties = 0;
    boolean semaphore = false;
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

                    out = new PrintWriter(socket.getOutputStream(),
                            true);

                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));

                int[][] board = new int[3][3];
                Random rand = new Random();
                int randomNum = rand.nextInt((100 - 1) + 1) + 1;

                if(randomNum % 2 == 0){
                    sendMessage(out, "NONE");

                }else{
                    //generate the random server move
                    int random = new Random().nextInt(getAvailableMoves(board).size());
                    sendMessage(out, getAvailableMoves(board).get(random));
                    processMove(board, getAvailableMoves(board).get(random), serverid);

                }

                while(true){
                    //ok so now get the response from the client
                    String move = recieveMessage(in);
                    //process it
                    processMove(board, move, clientid);
                    //check if we have a winner
                    int winlosstienowin = determineWinner(board);
                    if (winlosstienowin != nowinnerflag){
                        if(winlosstienowin == serverid){
                            sendMessage(out, move+" LOSS");
                            while (semaphore){
                                ;
                            }
                            //aquire
                            semaphore = true;
                            losses ++;
                            semaphore = false;
                        }
                        if(winlosstienowin == clientid){
                            sendMessage(out, move+" WIN");
                            while (semaphore){
                                ;
                            }
                            //aquire
                            semaphore = true;
                            wins++;
                            semaphore = false;
                        }
                        if(winlosstienowin == tieflag){
                            sendMessage(out, move+" TIE");
                            while (semaphore){
                                ;
                            }
                            //aquire
                            semaphore = true;
                            ties ++;
                            semaphore = false;
                        }
                        printStats();
                        break;
                    }

                    //do the server work
                    int random = new Random().nextInt(getAvailableMoves(board).size());
                    sendMessage(out, getAvailableMoves(board).get(random));
                    processMove(board, getAvailableMoves(board).get(random), serverid);
                    winlosstienowin = determineWinner(board);
                    if (winlosstienowin != nowinnerflag){
                        if(winlosstienowin == serverid){
                            sendMessage(out, move+" LOSS");
                            while (semaphore){
                                ;
                            }
                            //aquire
                            semaphore = true;
                            losses ++;
                            semaphore = false;
                        }
                        if(winlosstienowin == clientid){
                            sendMessage(out, move+" WIN");
                            while (semaphore){
                                ;
                            }
                            //aquire
                            semaphore = true;
                            wins++;
                            semaphore = false;
                        }
                        if(winlosstienowin == tieflag){
                            sendMessage(out, move+" TIE");
                            while (semaphore){
                                ;
                            }
                            //aquire
                            semaphore = true;
                            ties ++;
                            semaphore = false;
                        }
                        break;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void printStats(){
        while(semaphore){
            ;
        }
        //aquire lock
        semaphore = true;
        System.out.println("Wins: "+wins);
        System.out.println("Losses: "+losses);
        System.out.println("Ties: "+ties);
        semaphore = false;

    }
    public int determineWinner(int[][] board){
        //check to see if the rows are the same
        if(board[0][0] == board[0][1] && board[0][1] == board[0][2]){
            if (board[0][0] != 0) {
                return board[0][0];
            }
        }
        if(board[1][0] == board[1][1] && board[1][1] == board[1][2]){
            if (board[1][0] != 0) {
                return board[1][0];
            }
        }
        if(board[2][0] == board[2][1] && board[2][1] == board[2][2]){
            if (board[2][0]!=0) {
                return board[2][0];
            }
        }

        //now check the columns
        if(board[0][0] == board[1][0] && board[1][0] == board[2][0]){
            if (board[0][0] != 0) {
                return board[0][0];
            }
        }

        if(board[0][1] == board[1][1] && board[1][1] == board[2][1]){
            if (board[0][1]!=0) {
                return board[0][1];
            }
        }
        if(board[0][2] == board[1][2] && board[1][2] == board[2][2]){
            if (board[0][2]!=0) {
                return board[0][2];
            }
        }

        //now check the diagonals
        if(board[0][0] == board[1][1] && board[1][1] == board[2][2]){
            if (board[0][0] != 0) {
                return board[0][0];
            }
        }

            if(board[0][2] == board[1][1] && board[1][1] == board[2][0]){
                if (board[0][2]!=0) {
                    return board[0][2];
                }
        }

        if(getAvailableMoves(board).size() == 0){
            return tieflag;
        }

        return nowinnerflag;
    }
    void processMove(int[][] board, String move, int user ){
        //get the rows and columns
        String[] tmp = move.split(" ");
        int row = Integer.parseInt(tmp[1]);
        int column = Integer.parseInt(tmp[2]);
        board[row][column] = user;

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
