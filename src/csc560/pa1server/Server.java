package csc560.pa1server;

import java.io.*;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Random;

//http://www.java2s.com/Tutorial/Java/0320__Network/TestnonblockingacceptusingServerSocketChannel.htm
public class Server{
    ServerSocket providerSocket;
    ObjectOutputStream out;
    ObjectInputStream in;
    final int BOARD_ROWS = 3;
    final int BOARD_COLUMNS = 3;
    final int DEFAULTWEIGHT=0;
    final int CLIENT_ID = 666;
    final int SERVER_ID = 777;
    final int CLIENT_WIN_FLAG=CLIENT_ID;
    final int SERVER_WIN_FLAG=SERVER_ID;
    final int TIE_FLAG = CLIENT_ID+SERVER_ID;
    final int MINIMAX_WIN_FLAG = 6969;
    final int EMPTY_ROW = 123456;
    Server(){}

    private class BoardSpace{
        int row,column, serverWeight, clientWeight,owner, totalScore;

        protected  BoardSpace() {
            serverWeight = DEFAULTWEIGHT;
            clientWeight = DEFAULTWEIGHT;
        }
        public BoardSpace(int row, int column){
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

        public BoardSpace(int row, int column, int owner) {
            this.row = row;
            this.column = column;
            this.owner = owner;
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

        public int getOwner() {
            return owner;
        }

        public void setOwner(int owner) {
            this.owner = owner;
        }

        public int getAbsolutePostion(){
            return (row*3)+column;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(int totalScore) {
            this.totalScore = totalScore;
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
                int randomNum = rand.nextInt((100 - 1) + 1) + 1;
                //if it's an even number let the server move first, otherwise let the client go first
//                boolean serverTurn= randomNum % 2 == 0;
                boolean serverTurn = true;
                //we want the inverse
                boolean clientTurn = !serverTurn;
                int[][] board  = buildNewBoard();
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();

                if(!serverTurn){
                    sendMessage("NONE");
//                    out.writeObject("NONE");
//                    out.flush();
                }else{
                    executeServerMove(board);
                }
                in = new ObjectInputStream(connection.getInputStream());
                while (gameRunning){
                    SocketChannel throwAway = ssc.accept();
                    if (throwAway != null){
                        connections.add(throwAway);
                    }
//                    ssc.configureBlocking(true);
                    //3. get Input and Output streams



                    if (processMove((String) in.readObject(),board, CLIENT_ID )){
                        break;
                    }
                    if(executeServerMove(board)){
                        break;
                    }


//                    sendMessage("Connection successful");


                    //4. The two parts communicate via the input and output streams

                    if (generateGameState(board).size() == 0) {
                        gameRunning = false;
//                        sendMessage("MOVEWIN");
                    }
//                    String message;
                    //check incoming connections

//                    Thread.sleep(5000);
//                    message = readIncomingMessage(in);


                }


            }
        }
        catch(Exception ioException){
            ioException.printStackTrace();

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

    ArrayList<BoardSpace> boardArrayToArrayList(int[][] board){
        ArrayList<BoardSpace> b = new ArrayList<BoardSpace>();
        for(int i = 0; i < BOARD_ROWS; i++){
            for(int j = 0; j < BOARD_COLUMNS; j++) {
                b.add(new BoardSpace(i, j, board[i][j]));
            }
        }

        return b;
    }

    int determineWinner(int[][] board){
        ArrayList<BoardSpace> boardSpace = boardArrayToArrayList(board);
//        if(generateGameState(board).size() != 0){
//            return EMPTY_ROW;
//        }
        //the rows
        int totalScore = 0;
        for(int i = 0; i < BOARD_COLUMNS*BOARD_ROWS-1; i = i+BOARD_ROWS){

            if (i < BOARD_ROWS){
                //now the columns
                for(int j = i; j < BOARD_ROWS; j++){

                    for(int k = 0; k < BOARD_ROWS; k++){
                        totalScore = totalScore + boardSpace.get(k+(k*BOARD_ROWS)).getOwner();

                    }
                    if (totalScore == boardSpace.get(i+j).getOwner()*BOARD_ROWS &&  totalScore != 0){
                        return boardSpace.get(i+j).getOwner();
                    }
                }
                totalScore=0;


            }

            //now whether we have a winner in the row
            for(int j = 0; j < BOARD_ROWS; j++){
                System.out.println("Getting cell value for row at : "+(j+i));
                totalScore  = totalScore+boardSpace.get(i+j).getOwner();

            }

            if (totalScore == boardSpace.get(i).getOwner()*BOARD_ROWS && totalScore != 0){
                return boardSpace.get(i).getOwner();
            }

            totalScore = 0;


        }
        //here's the diagonal
        totalScore = 0;
        for(int i = 0; i < BOARD_ROWS*3; i = i +BOARD_ROWS+1){
            System.out.println("Getting cell value for diagonal at : "+(i));
            totalScore = totalScore+boardSpace.get(i).getOwner();
        }

        if (totalScore == boardSpace.get(0).getOwner()*BOARD_ROWS && totalScore != 0){

            return boardSpace.get(0).getOwner();
        }

        //the other diagonal
        totalScore = 0;
        for(int i = 2; i < BOARD_ROWS*2+1; i = i +BOARD_ROWS-1){
            System.out.println("Getting cell value for diagonal at : "+(i));
            totalScore = totalScore+boardSpace.get(i).getOwner();
        }
        if (totalScore == boardSpace.get(2).getOwner()*BOARD_ROWS && totalScore != 0){
            return boardSpace.get(2).getOwner();
        }



//        for(int[] row : board){
//
//        }


        return generateGameState(board).size() == 0 ? TIE_FLAG: EMPTY_ROW;
    }
//    int determineWinner(int[][] board){
//        ArrayList<BoardSpace> boardSpace = boardArrayToArrayList(board);
////        if(generateGameState(board).size() != 0){
////            return EMPTY_ROW;
////        }
//        //the rows
//        int totalScore = 0;
//        for(int i = 0; i < BOARD_COLUMNS*BOARD_ROWS-1; i = i+BOARD_ROWS){
//
//            if (i < BOARD_ROWS){
//                //now the columns
//                for(int j = i; j < BOARD_ROWS; j++){
//
//                    for(int k = 0; k < BOARD_ROWS; k++){
//                        totalScore = totalScore + boardSpace.get(k+(k*BOARD_ROWS)).getOwner();
//
//                    }
//                    if (totalScore == boardSpace.get(i+j).getOwner()*BOARD_ROWS &&  totalScore != 0){
//                        return boardSpace.get(i+j).getOwner();
//                    }
//                }
//                totalScore=0;
//
//
//            }
//
//            //now whether we have a winner in the row
//            for(int j = 0; j < BOARD_ROWS; j++){
//                System.out.println("Getting cell value for row at : "+(j+i));
//                totalScore  = totalScore+boardSpace.get(i+j).getOwner();
//
//            }
//
//            if (totalScore == boardSpace.get(i).getOwner()*BOARD_ROWS && totalScore != 0){
//                return boardSpace.get(i).getOwner();
//            }
//
//            totalScore = 0;
//
//
//        }
//        //here's the diagonal
//        totalScore = 0;
//        for(int i = 0; i < BOARD_ROWS; i = i +BOARD_ROWS+1){
//            System.out.println("Getting cell value for diagonal at : "+(i));
//            totalScore = totalScore+boardSpace.get(i).getOwner();
//        }
//
//        if (totalScore == boardSpace.get(0).getOwner()*BOARD_ROWS && totalScore != 0){
//
//            return boardSpace.get(0).getOwner();
//        }
//
//        //the other diagonal
//        totalScore = 0;
//        for(int i = 2; i < BOARD_ROWS; i = i +BOARD_ROWS-1){
//            System.out.println("Getting cell value for diagonal at : "+(i));
//            totalScore = totalScore+boardSpace.get(i).getOwner();
//        }
//        if (totalScore == boardSpace.get(2).getOwner()*BOARD_ROWS && totalScore != 0){
//            return boardSpace.get(0).getOwner();
//        }
//
//
//
////        for(int[] row : board){
////
////        }
//
//
//        return generateGameState(board).size() == 0? TIE_FLAG: EMPTY_ROW;
//    }

    String getEndGameMessageAction(int winner){
        switch (winner){
            case CLIENT_WIN_FLAG:
                return "WIN";
            case SERVER_WIN_FLAG:
                return "LOSS";
            case TIE_FLAG:
                return "TIE";

        }

        return "";
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
            System.out.println(msg);
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    BoardSpace test(int row, int col){
        return new BoardSpace(row,col);
    }
    public static void main(String args[])
    {
//        //ok let's test this
//        Server s = new Server();
//        int[][] board = s.buildNewBoard();
////        board[0][0] = s.SERVER_ID;
//        board[0][2] = s.CLIENT_ID;
//        board[1][1] = s.CLIENT_ID;
////        BoardSpace bo
////        board[2][0] = s.CLIENT_ID;
//        System.out.println(s.generateMoveValue(s.test(2,0), board, s.CLIENT_ID));
//        System.out.println(s.determineWinner(board));

//        board[2][0] = s.SERVER_ID;
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
    boolean processMove(String move, int[][] board, int player){
        String[] tmp=move.split(" ");
        int  row = Integer.parseInt(tmp[1]);
        int column = Integer.parseInt(tmp[2]);

        if(board[row][column] == 0){
            board[row][column] = player;

            if (generateGameState(board).size() == 0|| determineWinner(board) != EMPTY_ROW ){
                sendMessage("MOVE "+row+" "+column+" "+getEndGameMessageAction(determineWinner(board)));
                return true;
            }else{
                return false;
            }


        }
        else{
            return false;
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

//    boolean executeServerMove(int[][] board){
//        BoardSpace serverSpace = minimax(SERVER_ID, board);
//        BoardSpace clientSpace = minimax(CLIENT_ID, board);
//        BoardSpace space;
//        if(serverSpace.getServerWeight() > clientSpace.getClientWeight()){
//            space = serverSpace;
//        }else if(serverSpace.getServerWeight() < clientSpace.getClientWeight()){
//            space = clientSpace;
//        }else if(serverSpace.getServerWeight() == clientSpace.getClientWeight()){
//            space = clientSpace;
//        }else{
//            space = serverSpace;
//        }
//        board[space.row][space.column] = SERVER_ID;
//        if(generateGameState(board).size() == 0 || determineWinner(board) != EMPTY_ROW ){
//            sendMessage("MOVE "+space.row+" "+space.column+" "+getEndGameMessageAction(determineWinner(board)));
//            return true;
//        }else{
//            sendMessage("MOVE "+space.row+" "+space.column);
//            return false;
//        }
//
//    }
    boolean executeServerMove(int[][] board){
        BoardSpace space = minimax(SERVER_ID, board);
        board[space.row][space.column] = SERVER_ID;
        if(generateGameState(board).size() == 0 || determineWinner(board) != EMPTY_ROW ){
            sendMessage("MOVE "+space.row+" "+space.column+" "+getEndGameMessageAction(determineWinner(board)));
            return true;
        }else{
            sendMessage("MOVE "+space.row+" "+space.column);
            return false;
        }

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
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;
                //vertical
                tmpWeight = (board[space.row+1][space.column] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-1][space.column] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;
                //diagonal
                tmpWeight = (board[space.row+1][space.column+1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-1][space.column-1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //diagonal
                tmpWeight = (board[space.row+1][space.column-1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-1][space.column+1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
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
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //the vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+1][space.column ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
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
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //the vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+1][space.column ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
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
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;
                //diagonal
                tmpWeight = (board[space.row-1][space.column+1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column+2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
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
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
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
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //diagonal
                tmpWeight = (board[space.row-1][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
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
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;
                //diagonal
                tmpWeight = (board[space.row+1][space.column+1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column+2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
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
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
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
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;

                //diagonal
                tmpWeight = (board[space.row+1][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return MINIMAX_WIN_FLAG;
                else
                    totalMoveWeight += tmpWeight;
                tmpWeight = DEFAULTWEIGHT;
            }

        }
        return totalMoveWeight;
    }

//    int generateMoveValue(BoardSpace space, int[][] board, int player){
//        // so here we want to determine how close we are to 3 in a row
//        int totalMoveWeight = 0;
//        if(space.row == 1){
//            // handle case if it's a middle row
//            //check to see if we're working toward a vertical win
//
//            //here's all the ones for the horizontal cases
//            //if it's the center column
//            if(space.column == 1){
//                int tmpWeight = DEFAULTWEIGHT;
//                //horizontal
//                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//                //vertical
//                tmpWeight = (board[space.row+1][space.column] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row-1][space.column] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//                //diagonal
//                tmpWeight = (board[space.row+1][space.column+1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row-1][space.column-1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //diagonal
//                tmpWeight = (board[space.row+1][space.column-1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row-1][space.column+1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//            }
////            //if return if the move would give us a win
////            if(totalMoveWeight == DEFAULTWEIGHT*3)
////                return totalMoveWeight;
//            //if it's the end column
//            if (space.column == 2){
//                int tmpWeight = DEFAULTWEIGHT;
//                //horizontal
//                tmpWeight = (board[space.row][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //the vertical
//                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row+1][space.column ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//
//
//            }
//
//            if (space.column == 0){
//                int tmpWeight = DEFAULTWEIGHT;
//                //horizontal
//                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row][space.column + 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //the vertical
//                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row+1][space.column ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//            }
//
//
//
//
//        }
//
//
//        //bottom row
//        if(space.row == 2){
//
//            if (space.column == 0){
//                int tmpWeight = DEFAULTWEIGHT;
//                //horizontal
//                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row][space.column + 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //vertical
//                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//                //diagonal
//                tmpWeight = (board[space.row-1][space.column+1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row-2][space.column+2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//            }
//
//            if(space.column == 1){
//                int tmpWeight = DEFAULTWEIGHT;
//                //horizontal
//                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row][space.column -1 ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //vertical
//                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//            }
//
//            if(space.column==2){
//                int tmpWeight = DEFAULTWEIGHT;
//                //horizontal
//                tmpWeight = (board[space.row][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //vertical
//                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //diagonal
//                tmpWeight = (board[space.row-1][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row-2][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//            }
//        }
//
//        //top row
//        if (space.row == 0  ){
//
//            if (space.column == 0){
//                int tmpWeight = DEFAULTWEIGHT;
//                //horizontal
//                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row][space.column + 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //vertical
//                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//                //diagonal
//                tmpWeight = (board[space.row+1][space.column+1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row+2][space.column+2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//            }
//
//            if(space.column == 1){
//                int tmpWeight = DEFAULTWEIGHT;
//                //horizontal
//                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row][space.column -1 ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //vertical
//                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//            }
//
//            if(space.column==2){
//                int tmpWeight = DEFAULTWEIGHT;
//                //horizontal
//                tmpWeight = (board[space.row][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //vertical
//                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//
//                //diagonal
//                tmpWeight = (board[space.row+1][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//                tmpWeight = (board[space.row+2][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
//
//                //if return if the move would give us a win
//                if(tmpWeight == DEFAULTWEIGHT*3)
//                    return MINIMAX_WIN_FLAG;
//                else
//                    totalMoveWeight += tmpWeight;
//                tmpWeight = DEFAULTWEIGHT;
//            }
//
//        }
//        return totalMoveWeight;
//    }

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
    int getOppositePlayer(int player){
        if (player == CLIENT_ID){
            return SERVER_ID;
        }else{
            return CLIENT_ID;
        }
    }
    BoardSpace minimaxRecursive(int player, int[][] board){
        LinkedList<BoardSpace> possibleMoves = generateGameState(board);
        LinkedList<BoardSpace> calculatedMoves = new LinkedList<BoardSpace>();
        if(possibleMoves.size() == 1){
            if(player == CLIENT_ID){
                possibleMoves.get(0).setClientWeight(generateMoveValue(possibleMoves.get(0), board, player));
            }else{
                possibleMoves.get(0).setServerWeight(generateMoveValue(possibleMoves.get(0), board, player));
            }

            return possibleMoves.get(0);
        }
        for (BoardSpace space : possibleMoves){
            int score = generateMoveValue(space,board,player);
            int[][] tmpBoard = fuckYouJavaCopyBoard(board);
            tmpBoard[space.row][space.column] = player;
            BoardSpace s = minimaxRecursive(getOppositePlayer(player), tmpBoard);
            if(player == CLIENT_ID){
                s.setClientWeight(score);
            }else{
                s.setServerWeight(score);
            }
            if (score==MINIMAX_WIN_FLAG){
                return s;
            }

        }
        return new BoardSpace();
    }
    BoardSpace minimax(int player, int[][] board){
        LinkedList<BoardSpace> possibleMoves = generateGameState(board);
        LinkedList<BoardSpace> calculatedMoves = new LinkedList<BoardSpace>();
      for (BoardSpace space : possibleMoves){
            calculatedMoves.add(minimaxRecursive(player, board));
      }


        LinkedList<BoardSpace> clientMoves = calculatedMoves;
        LinkedList<BoardSpace> serverMoves= calculatedMoves;

        serverMoves.sort(new Comparator<BoardSpace>() {
                             @Override
                             public int compare(BoardSpace o1, BoardSpace o2) {
                                 return new Integer(o1.getServerWeight()).compareTo(new Integer(o2.getServerWeight()));
                             }
                         });
        clientMoves.sort(new Comparator<BoardSpace>() {
            @Override
            public int compare(BoardSpace o1, BoardSpace o2) {
                return new Integer(o1.getClientWeight()).compareTo(new Integer(o2.getClientWeight()));
            }
        });
        if(clientMoves.get(0).getClientWeight() > MINIMAX_WIN_FLAG){
            return clientMoves.get(0);
        }else{
            return serverMoves.get(0);
        }
//        if(clientMoves.get(0).getClientWeight() >= serverMoves.get(0).getServerWeight()){
//            return clientMoves.get(0);
//        }else{
//            return serverMoves.get(0);
//        }

//        //base case
//        if ( possibleMoves.size() == 1){
//            return possibleMoves.get(0);
//        }
//        for(int i = 0; i < possibleMoves.size(); i++) {
//            BoardSpace space = possibleMoves.get(i);
//
//            int clientWeight = generateMoveValue(space, board, CLIENT_ID);
//            //we want to block a win for the client
////            if (clientWeight== MINIMAX_WIN_FLAG){
////                return space;
////            }
//
//            int serverWeight = generateMoveValue(space, board, SERVER_ID);
////            if (serverWeight == MINIMAX_WIN_FLAG){
////                return space;
////            }
//
//            space.setServerWeight(space.getServerWeight() + serverWeight);
//
//            space.setClientWeight(space.getClientWeight() + clientWeight);
//            calculatedMoves.add(space);
//        }


//            int[][] tmpBoard = fuckYouJavaCopyBoard(board);
//            tmpBoard[space.row][space.column] = PLAYER;
////            BoardSpace s =
//            BoardSpace sSpace = minimax(CLIENT_ID, tmpBoard);
//            BoardSpace cSpace = minimax(SERVER_ID, tmpBoard);
////            s.setServerWeight(s.getServerWeight()+serverWeight);
////            s.setClientWeight(s.getClientWeight()+clientWeight);
//
//            //check if after the game plays out, this is the overall best move
////            if (s.row == space.row && s.column == space.column){
////                calculatedMoves.add(space);
////            }else{
////                calculatedMoves.add(s);
////            }
//
//        }
//
//                if (calculatedMoves.size() > 1) {
//            calculatedMoves.sort(new Comparator<BoardSpace>() {
//                @Override
//                public int compare(BoardSpace o1, BoardSpace o2) {
//                        if(o1.getClientWeight() > o2.getClientWeight()){
//                            return -1;
//                        }else if (o1.getClientWeight() < o2.getClientWeight()){
//                            return 1;
//                        }else{
//                            return 0;
////                            if(o1.getServerWeight() > o2.getServerWeight()){
////                                return -1;
////                            }else if(o1.getServerWeight() < o2.getServerWeight()){
////                                return 1;
////                            }else{
////                                return 0;
////                            }
//
////                            return 0;
//                        }
////                        if(o1.getServerWeight() > o1.getClientWeight() && o1.getServerWeight() > o2.getClientWeight()){
////                            return -1;
////                        }else if (o2.getServerWeight() > o2.getClientWeight() && o2.getServerWeight() > o1.getClientWeight()){
////                            return -1;
////                        }else if (o1.getServerWeight() < o2.getClientWeight() && o1.getServerWeight() < o1.getClientWeight()){
////                            return -1;
////                        }else if (o2.getServerWeight() > o2.getClientWeight() && o2.getServerWeight() > o1.getClientWeight()){
////                            return -1;
////                        }
////                        return 0;
//
//////                    int serverWeightResult  = new Integer(o1.getServerWeight()).compareTo(new Integer((o1.getClientWeight())));
//////                    if (serverWeightResult != 0){
//////                        return serverWeightResult;
//////                    }else{
////                        return new Integer(o2.getClientWeight()).compareTo(new Integer(o2.getClientWeight()));
//////                    }
//
//
//                }
//            });
//        }
////        return calculatedMoves.get(calculatedMoves.size()-1);
//        if (calculatedMoves.size() >= 2) {
////            if ()
//
//            if(calculatedMoves.get(0).getServerWeight() == calculatedMoves.get(1).getServerWeight() && calculatedMoves.get(0).getClientWeight() == calculatedMoves.get(0).getClientWeight()){
//                return calculatedMoves.get(1);
//            }else if(calculatedMoves.get(0).getClientWeight() <= calculatedMoves.get(1).getClientWeight()){
//                return calculatedMoves.get(1);
//            }else if(calculatedMoves.get(0).getClientWeight() >= calculatedMoves.get(1).getClientWeight()){
//                return  calculatedMoves.get(0);
//
//            }else if(calculatedMoves.get(0).getClientWeight() <= calculatedMoves.get(1).getServerWeight()){
//                return calculatedMoves.get(1);
//            }else if(calculatedMoves.get(0).getServerWeight() >= calculatedMoves.get(1).getClientWeight()){
//                return calculatedMoves.get(1);
//            }else{
//                return calculatedMoves.get(0);
//            }
//        }else{
//            return calculatedMoves.get(0);
//        }

    }

    boolean isTwoInARow(BoardSpace space, int[][] board, int player){
        if(space.column == BOARD_ROWS-1){
            if (board[space.row][space.column-1] == player){
                return true;
            }
        }

        if(space.column == BOARD_ROWS -2){
            if(board[space.row][space.column-1] == player || board[space.row][space.column+1] == player){
                return true;
            }
        }
        if(space.column == 0){
            if (board[space.row][space.column+1] == player){
                return true;
            }
        }


        return false;
    }
    //this is actually our recursive algorithm
//    BoardSpace minimax(int PLAYER, int[][] board){
//        LinkedList<BoardSpace> possibleMoves = generateGameState(board);
//        LinkedList<BoardSpace> calculatedMoves = new LinkedList<BoardSpace>();
//        //base case
//        if ( possibleMoves.size() == 1){
//            return possibleMoves.get(0);
//        }
//        for(BoardSpace space : possibleMoves){
//
//            int clientWeight = generateMoveValue(space, board, CLIENT_ID);
//            //we want to block a win for the client
//            if (clientWeight== MINIMAX_WIN_FLAG){
//                space.setClientWeight(clientWeight);
//                return space;
//            }
//
//
//            int serverWeight = generateMoveValue(space, board, SERVER_ID);
//            if (serverWeight == MINIMAX_WIN_FLAG){
//                space.setServerWeight(serverWeight);
//                return space;
//            }
//
//
//
//
//
//            int[][] tmpBoard = fuckYouJavaCopyBoard(board);
//            tmpBoard[space.row][space.column] = PLAYER;
//            BoardSpace s = minimax(PLAYER, tmpBoard);
//            //check if after the game plays out, this is the overall best move
//            if(s.getServerWeight() > s.getClientWeight() && space.getServerWeight() > space.getClientWeight()){
//                if (s.getServerWeight() >= space.getServerWeight()){
//                    calculatedMoves.add(s);
//                }else{
//                    calculatedMoves.add(space);
//                }
//            }else if(s.getServerWeight() <= s.getClientWeight() && space.getServerWeight() <= space.getClientWeight()){
//                if (s.getServerWeight() >= space.getServerWeight()){
//                    calculatedMoves.add(s);
//                }else{
//                    calculatedMoves.add(space);
//                }
//            }else{
//                calculatedMoves.add(space);
//            }
////            if (s.row == space.row && s.column == space.column){
////                calculatedMoves.add(space);
////            }else{
////                calculatedMoves.add(s);
////            }
//
//        }
//
//        if (calculatedMoves.size() > 1) {
//            calculatedMoves.sort(new Comparator<BoardSpace>() {
//                @Override
//                public int compare(BoardSpace o1, BoardSpace o2) {
//                    int serverWeightResult  = new Integer(o1.getServerWeight()).compareTo(new Integer(o2.getServerWeight()));
//                    if (serverWeightResult != 0){
//                        return serverWeightResult;
//                    }else{
//                        return new Integer(o1.getClientWeight()).compareTo(new Integer(o2.getClientWeight()));
//                    }
//
//
//                }
//            });
//        }
//
//
//        return calculatedMoves.get(0);
//    }
}