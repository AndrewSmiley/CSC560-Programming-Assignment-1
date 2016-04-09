package csc560.pa1tuiserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by pridemai on 4/2/16.
 */
public class TUIServer {
    public int CLIENT_ID=666;
    public int SERVER_ID=777;

    class TUIServerGame implements Runnable{
        Socket socket;
        public TUIServerGame(){
            this.socket = null;
        }
        public TUIServerGame(Socket socket){
            this.socket = socket;
        }


        @Override
        public void run() {
            //create our new board
            int[][] board = new int[3][3];
            //create our stuff to determine who goes first, client or server
            Random rand = new Random();
            int randomNum = rand.nextInt((100 - 1) + 1) + 1;
            //if it's an even number let the server move first, otherwise let the client go first
            boolean serverTurn = randomNum % 2 == 0;
            if (serverTurn){
                ArrayList<String> rowColList = getAvailableMoves(board);
                String rowCol = rowColList.get(rand.nextInt((rowColList.size() - 1) + 1) + 1);
                sendMessage("MOVE "+rowCol.substring(0,1)+" "+rowCol.substring(1,2),this.socket);
                board[Integer.parseInt(rowCol.substring(0,1))][Integer.parseInt(rowCol.substring(1,2))] = SERVER_ID;
            }else{
                sendMessage("NONE",this.socket);
            }
            boolean gameOn = true;
            while (gameOn){
                //so now we do the work for the client move
                ArrayList<String> rowColList = getAvailableMoves(board);
                //if the game is over
                if(rowColList.size() == 0){
                    //fuck it i don't care
                    sendMessage("MOVE 0 0 WIN", this.socket);
                }

                //get the messag back from the client and update board
                String move = receiveMessage(this.socket);
                while(move.equalsIgnoreCase("")){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    move = receiveMessage(this.socket);
                }

                String[] tmp = move.split(" ");
                int row = Integer.parseInt(tmp[1]);
                int column = Integer.parseInt(tmp[2]);
                board[row][column] = CLIENT_ID;
                //so now we do the work for the server move
                rowColList = getAvailableMoves(board);
                //if the game is over
                if(rowColList.size() == 0){
                    //fuck it i don't care
                    sendMessage("MOVE 0 0 WIN", this.socket);
                }
                String rowCol = rowColList.get(rand.nextInt((rowColList.size() - 1) + 1) + 1);
                sendMessage("MOVE "+rowCol.substring(0,1)+" "+rowCol.substring(1,2),this.socket);
                board[Integer.parseInt(rowCol.substring(0,1))][Integer.parseInt(rowCol.substring(1,2))] = SERVER_ID;
            }

//
//            sendMessage("test from server", this.socket);
//            receiveMessage(this.socket);
        }

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


        public void sendMessage(String msg, Socket socket){
//               //Send the message to the client
            OutputStream os = null;
            try {
                os = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osw);
            try {
                bw.write(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Message sent to the client : "+msg);
        }

        public String receiveMessage(Socket socket){
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

            System.out.println("Message received from the client : " +message);
            return message;
        }

    }


    public void spawnNewThread(Socket socket){
        new Thread(new TUIServerGame(socket)).start();
    }

    public static void main(String args[]) {
        //fuck you java
        TUIServer tuiServer = new TUIServer();


//        String sendMessage="FUCK FROM SERVER";
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(7788);

        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (true) {
                Socket socket = listener.accept();
                tuiServer.spawnNewThread(socket);


            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                listener.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
