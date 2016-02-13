Csc 460/560
Spring 2016
Programming Assignment #1
Simple Client – Server (single thread)
Due Date Feb 11th by 6PM

 This program will implement a simple, single-threaded server. It will handle multiple clients BUT only one client at a time.  The client and server classes are described below.

 The program will play a game of tic-tac-toe against the client.  Each game will have a 50%-50% chance of having the client or the server make the first move. The decision will be randomly decided by the server and the communication protocol will allow the client to easily determine whether it is to make the first move or the second.

  A game can result in either a win for the client, a win for the server, or a tie which will be determined in the obvious manner by the server. 

PROTOCOL for Communication

 Once started, the Server will await a connection from the next client on Port #7788. Upon a successful Socket connection, both the server and client should create the necessary input and output data streams and whatever I/O objects you decide to use for communication via the Socket. 

 The client, after acknowledging the user and indicating that it is attempting to connect to the server, should attempt to connect via a Socket to localhost on port 7788. Once the Socket has been created, the client should block awaiting input via the Socket. 

The Server should randomly decide which player (client or server) will move first. If the client is to move first then the server should send the message

NONE

to the client indicating that it has no move and is awaiting the client’s move.  If the server is randomly selected to move first, then the server should select a move (see below) and communicate the following message

MOVE  row# col#

where row# indicates the row placement of the move (0-2) and col# is an integer (0-2) representing the column number. Row and column numbers will be either 0, 1, or 2. So for example, to place a move into the center square of the board the message would be   MOVE 1 1  and to move in the upper right square would be MOVE 0 2.

 After the initial move of the game, moves will be exchanged using the format of 

MOVE row# col#

messages sent between the client and the server. Only the server should keep track of the number of total moves made.  

Actions for Server

 Upon receiving the client’s move and updating any data structures, the server should increment a local count of moves. The server should determine if the recent client move has resulted in a win for the client. If so, then it should send the message

MOVE 0 0 WIN

To the client and consider the game finished and move on to the next client. If the client’s move does NOT result in a win for the client BUT it was move #9 then the server should reply with the message

MOVE 0 0 TIE

 If neither a client-win nor a tie has occurred, the server should determine its next move.  It should increment the total move counter.

 After the server determines its move (but BEFORE it sends a message to the client!!!) the server should check to see if the move results in a win for the server. (3 consecutive Server squares either horizontal, vertical or diagonal).  If a win is detected, then the server should send the following message and consider the game complete.

MOVE row# col# LOSS

 If the server’s move does not result in a server-win, then the server should check the total move counter and if it is 9 it should send the message

MOVE row# col# TIE

where the row# and col# are the row and column of the final move.

 In short, the server controls the determination of how the game ends. The client is not responsible for anything other than interpreting the server’s messages and possibly interacting with the user to obtain their next move. The client always responds with a MOVE message regardless of anything UNLESS they have received a WIN, LOSS, or TIE message.  In the case of receiving either a WIN, LOSS or TIE message, the client should report the result to the user and cease execution.

  Program notes

I do not care what the interface to the user looks like. It can be text based but be sure the screen looks reasonable and readable and looks like a tic-tac-toe board. You may, of course, use standard Java graphics if you desire. 

Always remember to start your server prior to starting up and clients.

CSC-460 students are allowed to have the server randomly select a legal square to place their move. CSC 560 student’s servers should never lose a game!!!!

We will assume that the Server is ALWAYS playing ‘X’ and the user/client is always playing ‘O’.

I will set up a submission link on blackboard for you to submit a zipped/compressed folder containing the SOURCE code for your client and server.
 




