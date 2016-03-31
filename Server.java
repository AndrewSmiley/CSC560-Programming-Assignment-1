package csc560.pa1server;

import java.io.*;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

//http://www.java2s.com/Tutorial/Java/0320__Network/TestnonblockingacceptusingServerSocketChannel.htm
public class Server {


    class Counter{
        public Counter() {
            this.wins = 0;
            this.ties = 0;
            this.losses = 0;
        }

        int wins;
        int losses;
        int ties;
        public int getWins() {
            return wins;
        }

        public void setWins(int wins) {
            this.wins = wins;
        }

        public int getLosses() {
            return losses;
        }

        public void setLosses(int losses) {
            this.losses = losses;
        }

        public int getTies() {
            return ties;
        }

        public void setTies(int ties) {
            this.ties = ties;
        }

    }

    void wait(boolean semaphore){
        semaphore = true;
    }
    void signal(boolean semaphore){
        semaphore = false;
    }

    static volatile boolean counterSemaphore = false;
    ServerSocket providerSocket;
    ObjectOutputStream out;
    ObjectInputStream in;
    final int BOARD_ROWS = 3;
    final int BOARD_COLUMNS = 3;
    final int DEFAULTWEIGHT = 0;
    final int CLIENT_ID = 666;
    final int SERVER_ID = 777;
    final int CLIENT_WIN_FLAG = CLIENT_ID;
    final int SERVER_WIN_FLAG = SERVER_ID;
    final int TIE_FLAG = CLIENT_ID + SERVER_ID;
    final int EMPTY_ROW = 123456;

    Server() {
    }

    static volatile LinkedList<SocketChannel> connections = new LinkedList<SocketChannel>();
    Counter counter = new Counter();

    /**
     * Class to represent a space on the board
     */
    private class BoardSpace {
        int row, column, serverWeight, clientWeight, owner, totalScore;

        protected BoardSpace() {
            serverWeight = DEFAULTWEIGHT;
            clientWeight = DEFAULTWEIGHT;
        }

        public BoardSpace(int row, int column) {
            this.row = row;
            this.column = column;
            serverWeight = DEFAULTWEIGHT;
            clientWeight = DEFAULTWEIGHT;
        }

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


        public int getOwner() {
            return owner;
        }

        public void setOwner(int owner) {
            this.owner = owner;
        }


        public int getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(int totalScore) {
            this.totalScore = totalScore;
        }
    }

    public class ServerGame implements Runnable{
        Socket connection;

        public ServerGame(Socket sc) {
            this.connection = sc;
        }

        @Override
        public void run() {


            //start the thread
//            Thread t = new Thread(new ServerThread());
//            t.start();

            PrintWriter outWriter = null;
            try {
                outWriter = new PrintWriter(new BufferedWriter(new FileWriter(Thread.currentThread().getName().replace(" ","")+".txt", true)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
//              System.out.println("Waiting for connection");


                        outWriter.println("Starting new game");
                        boolean gameRunning = true;

                        System.out.println("Connection received from " + connection.getInetAddress().getHostName());
                        Random rand = new Random();
                        int randomNum = rand.nextInt((100 - 1) + 1) + 1;
                        //if it's an even number let the server move first, otherwise let the client go first
                        boolean serverTurn = randomNum % 2 == 0;
                        int[][] board = buildNewBoard();
                        out = new ObjectOutputStream(connection.getOutputStream());
                        out.flush();

                        if (!serverTurn) {
                            sendMessage("NONE");
                        } else {
                            executeServerMove(board, counter, outWriter);
                        }
                        in = new ObjectInputStream(connection.getInputStream());
                        while (gameRunning) {
                            //just each time through check for a new conneciton
//                      acceptNonBlockingConnection(connections, ssc);
                            if (processMove((String) in.readObject(), board, CLIENT_ID, counter, outWriter)) {
                                break;
                            }
//                      acceptNonBlockingConnection(connections, ssc);
                            if (executeServerMove(board,counter, outWriter)) {
                                break;
                            }
//                      acceptNonBlockingConnection(connections, ssc);
                            if (generateGameState(board).size() == 0) {
                                gameRunning = false;
                            }
                        }
                    printStatistics(counter);
                    outWriter.close();
                } catch (Exception ioException) {
                    ioException.printStackTrace();
                    outWriter.close();

                }

        }
    }

    public void spawnNewGame(Socket s) {
        Thread t = new Thread(new ServerGame(s));

        t.start();
    }
    public static void main(String args[]) {
        //we need this part to start the server
        Server s = new Server();
        ServerSocket listener = null;
        try {
            listener = new ServerSocket(7788);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Listening for new connecitons... ");
        int numThreads = 1;
        while (true) {

            Socket throwAway = null;
            try {
                throwAway = listener.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (throwAway != null) {

                //ok so this should be the easy fix.
                //just spawn a new thread each time we get a new connection
                //we can get rid of this lock stuff though.
                s.spawnNewGame(throwAway);
                numThreads++;
//                    connections.add(throwAway);
                System.out.println("Accepted new connection and started new game");
//                    System.out.println("Size of connections after: "+ connections.size());
//                    lock = false;
                //exit CS
            }

    }
    }



    void printStatistics(Counter counter){
        while(counterSemaphore){
            ;
        }
        wait(counterSemaphore);
        System.out.println("\nWins: "+counter.getWins()+"\nLosses: "+counter.getLosses()+"\nTies: "+counter.getTies());
        signal(counterSemaphore);
    }


    ArrayList<BoardSpace> boardArrayToArrayList(int[][] board) {
        ArrayList<BoardSpace> b = new ArrayList<BoardSpace>();
        for (int i = 0; i < BOARD_ROWS; i++) {
            for (int j = 0; j < BOARD_COLUMNS; j++) {
                b.add(new BoardSpace(i, j, board[i][j]));
            }
        }

        return b;
    }

    int determineWinner(int[][] board) {
        ArrayList<BoardSpace> boardSpace = boardArrayToArrayList(board);
        //the rows
        int totalScore = 0;
        for (int i = 0; i < BOARD_COLUMNS * BOARD_ROWS - 1; i = i + BOARD_ROWS) {

            if (i < BOARD_ROWS) {
                //now the columns
                for (int j = i; j < BOARD_ROWS; j++) {
//                    int currentOwner = 0;
                    for (int k = 0; k < BOARD_ROWS; k++) {
//                        currentOwner = k;
                        totalScore = totalScore + boardSpace.get(j + (k * BOARD_ROWS)).getOwner();

                    }
                    if (totalScore == boardSpace.get(i + j).getOwner() * BOARD_ROWS && totalScore != 0) {
                        return boardSpace.get(i + j).getOwner();
                    }
                    totalScore = 0;
                }
                totalScore = 0;


            }

            //now whether we have a winner in the row
            for (int j = 0; j < BOARD_ROWS; j++) {
                totalScore = totalScore + boardSpace.get(i + j).getOwner();
            }

            if (totalScore == boardSpace.get(i).getOwner() * BOARD_ROWS && totalScore != 0) {
                return boardSpace.get(i).getOwner();
            }

            totalScore = 0;


        }
        //here's the diagonal
        totalScore = 0;
        for (int i = 0; i < BOARD_ROWS * 3; i = i + BOARD_ROWS + 1) {
            totalScore = totalScore + boardSpace.get(i).getOwner();
        }

        if (totalScore == boardSpace.get(0).getOwner() * BOARD_ROWS && totalScore != 0) {

            return boardSpace.get(0).getOwner();
        }

        //the other diagonal
        totalScore = 0;
        for (int i = 2; i < BOARD_ROWS * 2 + 1; i = i + BOARD_ROWS - 1) {
            totalScore = totalScore + boardSpace.get(i).getOwner();
        }
        if (totalScore == boardSpace.get(2).getOwner() * BOARD_ROWS && totalScore != 0) {
            return boardSpace.get(2).getOwner();
        }

        return generateGameState(board).size() == 0 ? TIE_FLAG : EMPTY_ROW;
    }

    String getEndGameMessageAction(int winner) {
        switch (winner) {
            case CLIENT_WIN_FLAG:
                return "WIN";
            case SERVER_WIN_FLAG:
                return "LOSS";
            case TIE_FLAG:
                return "TIE";

        }

        return "";
    }

    void sendMessage(String msg) {
        try {
            System.out.println(msg);
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }




    int[][] buildNewBoard() {
        return new int[3][3];
    }

    /**
     * Process a move
     *
     * @param move
     * @param board
     * @param player
     * @return
     */
    boolean processMove(String move, int[][] board, int player, Counter counter, PrintWriter fuck) {
        String[] tmp = move.split(" ");
        try {
            int row = Integer.parseInt(tmp[1]);
            int column = Integer.parseInt(tmp[2]);

            if (board[row][column] == 0) {
                board[row][column] = player;

                if (generateGameState(board).size() == 0 || determineWinner(board) != EMPTY_ROW) {
                    String message = "MOVE " + row + " " + column + " " + getEndGameMessageAction(determineWinner(board));
                    sendMessage(message);
                    fuck.println(message);
                    while(counterSemaphore){
                        ;
                    }
                    wait(counterSemaphore);
                    //this is inefficient but i give NO fucks
                    if(determineWinner(board) == SERVER_ID){
                        counter.setWins(counter.getWins()+1);
                    }else if(determineWinner(board) == CLIENT_ID){
                        counter.setLosses(counter.getLosses()+1);
                    }else{
                        counter.setTies(counter.getTies()+1);
                    }
                    signal(counterSemaphore);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        }

    }

    ArrayList<BoardSpace> generateGameState(int[][] board) {
        //fuck the java 8 standard
        ArrayList<BoardSpace> results = new ArrayList<BoardSpace>();
        for (int i = 0; i < BOARD_ROWS; i++) {
            for (int j = 0; j < BOARD_COLUMNS; j++) {
                if (board[i][j] == 0)
                    results.add(new BoardSpace(i, j));

            }
        }
        return results;

    }

    /**
     * DEtermine a move
     *
     * @param board the current board state
     * @return boolean wheter the game neded nor not
     */
    boolean executeServerMove(int[][] board,Counter counter, PrintWriter fuck) {
        BoardSpace space = minimax(0, SERVER_ID, board);
        System.out.println("Chose move with value " + space.getTotalScore() + " at position " + space.getRow() + " " + space.getColumn());
        board[space.row][space.column] = SERVER_ID;
        if (generateGameState(board).size() == 0 || determineWinner(board) != EMPTY_ROW) {
            String message = "MOVE " + space.row + " " + space.column + " " + getEndGameMessageAction(determineWinner(board));
            sendMessage(message);
            fuck.println(message);
            //so now we enter the CS
            while(counterSemaphore){
                ;
            }
            wait(counterSemaphore);
            if(determineWinner(board) == SERVER_ID){
                counter.setWins(counter.getWins()+1);
            }else if(determineWinner(board) == CLIENT_ID){
                counter.setLosses(counter.getLosses()+1);
            }else{
                counter.setTies(counter.getTies()+1);
            }
            signal(counterSemaphore);
            return true;
        } else {
            sendMessage("MOVE " + space.row + " " + space.column);
            return false;
        }

    }

    /**
     * because I need a way to copy a multi-dimensional array and Arrays.copy doesn't work because FUCK java.
     *
     * @param array
     * @return
     */
    int[][] fuckYouJavaCopyBoard(int[][] array) {
        int[][] tmp = buildNewBoard();
        for (int i = 0; i < BOARD_ROWS; i++) {
            for (int j = 0; j < BOARD_COLUMNS; j++) {
                tmp[i][j] = array[i][j];
            }
        }
        return tmp;
    }


    int getOppositePlayer(int player) {
        if (player == CLIENT_ID) {
            return SERVER_ID;
        } else {
            return CLIENT_ID;
        }
    }

    /**
     * Here's the actual minimax scoring algorithm
     *
     * @param depth  the depth
     * @param player the player
     * @param board  the board state
     * @param space  the space we want to get the minimax score for
     * @return
     */
    int minimaxScore(int depth, int player, int[][] board, BoardSpace space) {

        ArrayList<BoardSpace> possibleMoves = generateGameState(board);
        //base case
        if (possibleMoves.size() == 1) {
            int[][] tmpBoard = fuckYouJavaCopyBoard(board);
            tmpBoard[possibleMoves.get(0).row][possibleMoves.get(0).column] = player;

            int winner = determineWinner(tmpBoard);
            if (winner == player) {
                return depth - 10;

            } else if (winner == getOppositePlayer(player)) {

                return (10 - depth);
            } else {

                return 0;
            }
        }
        depth++;


        ArrayList<BoardSpace> calculatedMoves = new ArrayList<BoardSpace>();

        for (BoardSpace s : possibleMoves) {
            int[][] tmpBoard = fuckYouJavaCopyBoard(board);
            tmpBoard[space.row][space.column] = getOppositePlayer(player);

            if (determineWinner(tmpBoard) == getOppositePlayer(player)) {
                return (10 - depth);
            }
            tmpBoard[space.row][space.column] = player;
            if (determineWinner(tmpBoard) == player) {
                return depth - 10;

            }
            s.setTotalScore(minimaxScore(depth, getOppositePlayer(player), tmpBoard, s));
            calculatedMoves.add(s);

        }
        Collections.sort(calculatedMoves, new Comparator<BoardSpace>() {
            @Override
            public int compare(BoardSpace o1, BoardSpace o2) {

                return new Integer(o2.getTotalScore()).compareTo(new Integer(o1.getTotalScore()));
            }
        });
        if (player == CLIENT_ID) {
            return calculatedMoves.get(calculatedMoves.size() - 1).getTotalScore();
        } else {
            return calculatedMoves.get(0).getTotalScore();
        }

    }

    BoardSpace minimax(int depth, int player, int[][] board) {
        ArrayList<BoardSpace> possibleMoves = generateGameState(board);
        ArrayList<BoardSpace> calculatedMoves = new ArrayList<BoardSpace>();
        for (BoardSpace space : possibleMoves) {
            space.setTotalScore(minimaxScore(depth, player, board, space));
            calculatedMoves.add(space);
        }
        Collections.sort(calculatedMoves, new Comparator<BoardSpace>() {
            @Override
            public int compare(BoardSpace o1, BoardSpace o2) {

                return new Integer(o2.getTotalScore()).compareTo(new Integer(o1.getTotalScore()));
            }
        });

        return calculatedMoves.get(0);
    }


}