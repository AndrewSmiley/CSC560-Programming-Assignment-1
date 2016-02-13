package csc560.pa1server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

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
    Server(){}

    private class BoardSpace{
        int row,column, weight;

        protected  BoardSpace() {
            weight  = DEFAULTWEIGHT;
        }
        protected BoardSpace(int row, int column){
            this.row = row;
            this.column = column;
        }

        protected BoardSpace(int row, int column, int weight){
            this.row = row;
            this.column = column;
            this.weight = weight;
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

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
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
                boolean serverTurn= randomNum % 2 == 0;
                //we want the inverse
                boolean clientTurn = !serverTurn;
                int[][] board  = buildNewBoard();

                while (gameRunning){
                    //3. get Input and Output streams
                    out = new ObjectOutputStream(connection.getOutputStream());
                    out.flush();
                    in = new ObjectInputStream(connection.getInputStream());

                    if (clientTurn){
                        processMove((String)in.readObject(),board, CLIENT_ID );
                        executeServerMove(board);
                    }

                    sendMessage("Connection successful");


                    //4. The two parts communicate via the input and output streams

                        try{
//                            gameRunning = false;
                            String message;
                            //check incoming connections
                            SocketChannel throwAway = ssc.accept();
                            if (throwAway != null){
                                connections.add(throwAway);
                            }
                            Thread.sleep(5000);
                            message = (String)in.readObject();
                            System.out.println("client>" + message);
                            if (message.equals("bye"))
                                sendMessage("bye");
                        }
                        catch(ClassNotFoundException classnot){
                            System.err.println("Data received in unknown format");
                        }
//                    }while(!message.equals("bye"));

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
    void sendMessage(String msg)
    {
        try{
            out.writeObject(msg);
            out.flush();
//            System.out.println("server>" + msg);
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    public static void main(String args[])
    {
        Server server = new Server();
        while(true){
            server.run();
        }
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
            return;        }
    }

    LinkedList<BoardSpace> generateGameState(int[][] board){
        //fuck the java 8 standard
        LinkedList<BoardSpace> results = new LinkedList<BoardSpace>();
        for(int i = 0; i < BOARD_ROWS; i++ ){
            for (int j = 0; j < BOARD_COLUMNS; j++){
                if (board[i][j] == 0);
                results.add(new BoardSpace(i, j));

            }
        }
        return results;
    }
    void executeServerMove(int[][] board){

    }




    int generateMoveValue(BoardSpace space, int[][] board, int player){
        // so here we want to determine how close we are to 3 in a row
        int totalMoveWeight = DEFAULTWEIGHT;
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
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
                //vertical
                tmpWeight = (board[space.row+1][space.column] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-1][space.column] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
                //diagonal
                tmpWeight = (board[space.row+1][space.column+1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-1][space.column-1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //diagonal
                tmpWeight = (board[space.row+1][space.column-1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-1][space.column+1] == player)? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
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
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //the vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
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
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //the vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+1][space.column ] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
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
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
                //diagonal
                tmpWeight = (board[space.row-1][space.column+1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column+2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
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
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
            }

            if(space.column==2){
                int tmpWeight = DEFAULTWEIGHT;
                //horizontal
                tmpWeight = (board[space.row][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row][space.column + 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row-1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //diagonal
                tmpWeight = (board[space.row-1][space.column - 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row-2][space.column - 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
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
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
                //diagonal
                tmpWeight = (board[space.row+1][space.column+1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column+2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
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
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
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
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //vertical
                tmpWeight = (board[space.row+1][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;

                //diagonal
                tmpWeight = (board[space.row+1][space.column + 1] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;
                tmpWeight = (board[space.row+2][space.column + 2] == player) ? tmpWeight += DEFAULTWEIGHT : tmpWeight;

                //if return if the move would give us a win
                if(tmpWeight == DEFAULTWEIGHT*3)
                    return tmpWeight;
                else
                    totalMoveWeight += tmpWeight;
                    tmpWeight = DEFAULTWEIGHT;
            }

        }
        return totalMoveWeight;
    }

    //this is actually our recursive algorithm
    BoardSpace minimax(int PLAYER, int[][] board){
        LinkedList<BoardSpace> possibleMoves = generateGameState(board);
        for(BoardSpace space : possibleMoves){
            
        }


        return new BoardSpace();
    }
}