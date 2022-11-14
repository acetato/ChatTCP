# ChatTCP
Chat server and a client implemented in Java.

## Example

<b> java ChatServer 8000 </b> <br>
To run the Server listening on port 8000.

<b>java ChatClient localhost 8000 </b> </br>
To run the Client, connecting it to the server's DNS (eg: localhost) and the TCP port number where the server is listening on (8000).

## Commands

<b> /nick name </b> <br>
Chooses a new nickname

<b>/join room </b> <br>
Enters the chatroom

<b>/leave </b> <br>
Leaves the chatroom

<b>/bye </b> <br>
Leaves the application

<b>/priv name message </b> <br>
Sends a private message to name
