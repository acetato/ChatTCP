# ChatTCP
A simple chat server and a client to communicate with the former. Implemented in Java.

## Example

java ChatServer 8000
To run the Server listening in the TCP port 8000.

java ChatClient localhost 8000
To run the Client, connecting it to the server's DNS (eg: localhost) and the TCP port number where the server is listening (8000).

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
