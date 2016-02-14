package csc560.pa1server;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.Arrays;

//http://www.java2s.com/Tutorial/Java/0320__Network/TestnonblockingacceptusingServerSocketChannel.htm
public class Server{
    ServerSocket providerSocket;
    ObjectOutputStream out;
    ObjectInputStream in;
    int BOARD_ROWS = 3;
    int BOARD_COLUMNS = 3;
    int DEFAULTWEIGHT=5;
    int CLIENT_ID = 666;
    int SERVER_ID = 777;
    int WIN_FLAG = 6969;
    Server(){}

    private class BoardSpace{
        int row,column, serverWeight, clientWeight;

        protected  BoardSpace() {
            serverWeight = DEFAULTWEIGHT;
            clientWeight = DEFAULTWEIGHT;
        }
        protected BoardSpace(int row, int column){
            this.row = row;
            this.column = column;
            serverWeight = DEFAULTWEIGHT;
            clientWeight = DEFAULTWEIGHT;
        }

//        public BoardSpace(int column, int row, int clientWeight) {
//            this.clientWeight = clientWeight;
//            this.serverWeight = DEFAULTWEIGHT;
//            this.column = column;
//            this.row = row;
//        }
//
//
//        protected BoardSpace(int row, int column, int serverWeight){
//            this.row = row;
//            this.column = column;
//            this.serverWeight = serverWeight;
//            this.clientWeight = DEFAULTWEIGHT;
//        }

        public BoardSpace(int clientWeight, int serverWeight, int column, int row) {
            this.clientWeight = clientWeight;
            this.serverWeight = serverWeight;
            this.column = column;
            this.row = row;
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

        public int getServerWeight() {
            return serverWeight;
        }

        public void setServerWeight(int weight) {
            this.serverWeight = weight;
        }

        public int getClientWeight() {
            return clientWeight;
        }

        public void setClientWeight(int clientWeight) {
            this.clientWeight = clientWeight;
        }
    }

    void run()
    {



        try{

            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(7788));
            ssc.configureBlocking(false);
            LinkedList<SocketChannel> connections = new LinkedList<SocketChannel>();

            System.out.println("Waiting for connection");
            while (true) {
                SocketChannel sc = null;
                if (connections.size() == 0) {
                    //if we don't have any queued connections
                    sc = ssc.accept();
                    //if it's null, we want to wait till we get an incoming conneciton
                    if(sc == null){
                        while (sc == null){
                            Thread.sleep(2000);
                            sc= ssc.accept();
                            System.out.println((sc == null) ? "Waiting for connection": "Accepted new incoming connection");
                        }
                    }
                }else{
                    //otherwise grab it off the queue
                    sc = connections.pop();
                    System.out.println("Grabbed connection from Queue");

                }
                boolean gameRunning = true;
                Socket connection = sc.socket();
                System.out.println("Connection received from " + connection.getInetAddress().getHostName());
                Random rand = new Random();
                int randomNum = rand.nextInt((10 - 1) + 1) + 1;
                //if it's an even number let the server move first, otherwise let the client go first
//                boolean serverTurn= randomNum % 2 == 0;
                boolean serverTurn = false;
                //we want the inverse
                boolean clientTurn = !serverTurn;
                int[][] board  = buildNewBoard();
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();

                if(!serverTurn){
                    out.writeObject("NONE");
                    out.flush();
//                    dos.writeDouble(value);

//                    sendMessage("NONE");
                }else{
                    executeServerMove(board);
                }
                in = new ObjectInputStream(connection.getInputStream());
//                Thread.sleep(50000);
                while (gameRunning){
//                    ssc.configureBlocking(true);
                    //3. get Input and Output streams



                    processMove((String) in.readObject(),board, CLIENT_ID );
                    executeServerMove(board);


//                    sendMessage("Connection successful");


                    //4. The two parts communicate via the input and output streams

                    if (generateGameState(board).size() == 0) {
                        gameRunning = false;
                        sendMessage("WIN");
                    }
//                    String message;
                    //check incoming connections
                    SocketChannel throwAway = ssc.accept();
                    if (throwAway != null){
                        connections.add(throwAway);
                    }
//                    Thread.sleep(5000);
//                    message = readIncomingMessage(in);


                }


            }
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally{
            //4: Closing connection
            try{
                in.close();
                out.close();
                providerSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }

    String readIncomingMessage(DataInputStream in){
        byte[] messageByte = new byte[1000];
        boolean end = false;
        String dataString = "";
        String messageString = "";

        try
        {
//            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            int bytesRead = 0;

            messageByte[0] = in.readByte();
            messageByte[1] = in.readByte();

            int bytesToRead = messageByte[1];

            while(!end)
            {
                bytesRead = in.read(messageByte);
                messageString += new String(messageByte, 0, bytesRead);
                if (messageString.length() == bytesToRead )
                {
                    end = true;
                }
            }
            System.out.println("MESSAGE: " + messageString);
            return dataString;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return messageString;
    }


    void sendMessage(String msg)
    {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    public static void main(String args[])
    {
//        //ok let's test this
//        Server s = new Server();
//        int[][] board = s.buildNewBoard();
////        board[0][0] = s.CLIENT_ID;
////        board[0][2] = s.CLIENT_ID;
//////        board[1][1] = s.SERVER_ID;
////        board[2][0] = s.SERVER_ID;
//        boolean serverTurn = true;
//        Scanner scan= new Scanner(System.in);
//        s.printBoard(board);
//        while (s.generateGameState(board).size() > 0){
//
//            int player = (serverTurn)? s.SERVER_ID:s.CLIENT_ID;
//            if (player == s.CLIENT_ID){
//                System.out.print("Enter the row: ");
//                int row= scan.nextInt();
//                System.out.print("Enter the column: ");
//                int column= scan.nextInt();
//                board[row][column] = s.CLIENT_ID;
//
//            }else {
//                int[][] tmpBoard = s.fuckYouJavaCopyBoard(board);
//                BoardSpace space = s.minimax(player, board);
//                board[space.row][space.column] = player;
//            }
//            serverTurn = !serverTurn;
//            s.printBoard(board);
//
//        }


        Server server = new Server();
        while(true){
            server.run();
        }
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

    int[][] buildNewBoard(){
        return new int[3][3];
    }

    /**
     * Process a move
     * @param move
     * @param board
     * @param player
     * @return
     */
    void processMove(String move, int[][] board, int player){
        String[] tmp=move.split(" ");
        int  row = Integer.parseInt(tmp[1]);
        int column = Integer.parseInt(tmp[2]);
        if(board[row][column] == 0){
            board[row][column] = player;

        }else{
            return;
        }
    }

    LinkedList<BoardSpace> generateGameState(int[][] board){
        //fuck the java 8 standard
        LinkedList<BoardSpace> results = new LinkedList<BoardSpace>();
        for(int i = 0; i < BOARD_ROWS; i++ ){
            for (int j = 0; j < BOARD_COLUMNS; j++){
                if (board[i][j] == 0)
                    results.add(new BoardSpace(i, j));

            }
        }
        return results;
    }
    void executeServerMove(int[][] board){
        BoardSpace space = minimax(SERVER_ID, board);
        board[space.row][space.column] = SERVER_ID;
        sendMessage("MOVE "+space.row+" "+space.column);
    }




    int generateMoveValue(BoardSpace space, int[][] board, int player){
        // so here we want to determine how close we are to 3 in a row
        int totalMoveWeight = 0;
        if(space.row == 1){
            // handle case if it's a middle row
            //check to see if we're working toward a vertical win

            //here's all the ones for the horizontal cases
            //if it's the center column
            if(space.column == 1){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
                //vertical
                tmpWeight = (board[space.row+1][space.column] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-1][space.column] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
                //diagonal
                tmpWeight = (board[space.row+1][space.column+1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-1][space.column-1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //diagonal
                tmpWeight = (board[space.row+1][space.column-1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-1][space.column+1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
            }
//            //if return if the move would give us a win
//            if(totalMoveWeight == DEFAULTWEIGHT*3)
//                return totalMoveWeight;
            //if it's the end column
            if (space.column == 2){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //the vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+1][space.column ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;



            }

            if (space.column == 0){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column + 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //the vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+1][space.column ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
            }




        }


        //bottom row
        if(space.row == 2){

            if (space.column == 0){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column + 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
                //diagonal
                tmpWeight = (board[space.row-1][space.column+1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column+2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
            }

            if(space.column == 1){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column -1 ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
            }

            if(space.column==2){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //diagonal
                tmpWeight = (board[space.row-1][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
            }
        }

        //top row
        if (space.row == 0  ){

            if (space.column == 0){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column + 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
                //diagonal
                tmpWeight = (board[space.row+1][space.column+1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column+2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
            }

            if(space.column == 1){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column -1 ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;
            }

            if(space.column==2){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //diagonal
                tmpWeight = (board[space.row+1][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
            }

        }
        return totalMoveWeight;
    }

    int[][] fuckYouJavaCopyBoard(int[][] array){
        int [][] tmp = buildNewBoard();
        for (int i =0; i < BOARD_ROWS; i++){
            for (int j = 0; j < BOARD_COLUMNS; j++){
                tmp[i][j] = array[i][j];
            }
        }
        return tmp;
    }
    //this is actually our recursive algorithm
    BoardSpace minimax(int PLAYER, int[][] board){
        LinkedList<BoardSpace> possibleMoves = generateGameState(board);
        LinkedList<BoardSpace> calculatedMoves = new LinkedList<BoardSpace>();
        //base case
        if ( possibleMoves.size() == 1){
            return possibleMoves.get(0);
        }
        for(BoardSpace space : possibleMoves){

            int serverWeight = generateMoveValue(space, board, SERVER_ID);
                if (serverWeight == WIN_FLAG){
                    return space;
                }

            int clientWeight = generateMoveValue(space, board, CLIENT_ID);
            //we want to block a win for the client
            if (clientWeight== WIN_FLAG){
                return space;
            }



            int[][] tmpBoard = fuckYouJavaCopyBoard(board);
            tmpBoard[space.row][space.column] = PLAYER;
            BoardSpace s = minimax(PLAYER, tmpBoard);
            //check if after the game plays out, this is the overall best move
            if (s.row == space.row && s.column == space.column){
                calculatedMoves.add(space);
            }else{
                calculatedMoves.add(s);
            }

        }




        return calculatedMoves.get(0);
    }
}