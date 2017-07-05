import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

enum State
{
    INIT,
    OUTSIDE,
    INSIDE;
}

class User
{
    private Socket socket=null;
    private String name=null;
    private State state=null;
    private Room room=null;
   
    User(Socket s) {
        this.socket= s;
        this.state=State.INIT;
    }

    public State getState() {
        return this.state;
    }
    
    public void addSocket(Socket socket) {
        this.socket = socket;
    }

    public void setName(String str) {
        this.name=str;
        if (this.state==State.INIT)
            this.state=State.OUTSIDE;
    }

    public void setRoom(Room room) {
        this.room=room;
	this.state=State.INSIDE;
    }
    
    public Socket getSocket() {
        return this.socket;
    }
    
    public String getName() {
        return name;
    }
    
    public Room getRoom() {
        return this.room;
    }

    public void leaveRoom() {
        getRoom().removeUser(this);
        this.room=null;
        this.state=State.OUTSIDE;
    }
    
    public void sendMessage(String str) throws IOException {
        if (getName()==null || getRoom()==null)
            sendStatusToClient("ERROR");
        else
            getRoom().sendMessage(this, str);
    }
    
    
    public void sendStatusToClient(String str) throws IOException{
        byte[] message = new String(str+"\n").getBytes();
        ByteBuffer buf = ByteBuffer.wrap(message);
        while(buf.hasRemaining())
            getSocket().getChannel().write(buf);
    }
    
    public void sendStatusToOthers(String str) throws IOException {
        if (getRoom()!=null)
            getRoom().sendStatusToOthers(this, str);
    }
    
    public void sendPrivateMessage(User u, String str) throws IOException{

        if (this.getRoom()!=null && (u.getRoom() == this.getRoom())) {
            byte[] message = new String("PRIVATE " + this.getName() + " " + str+"\n").getBytes();
            ByteBuffer buf = ByteBuffer.wrap(message);
            
            while(buf.hasRemaining()) //para o destinatario
                u.getSocket().getChannel().write(buf);//PRIVATE user message
            buf.flip();
            
            while(buf.hasRemaining()) // e para este utilizador
                this.getSocket().getChannel().write(buf);//PRIVATE user message
            
            sendStatusToClient("OK");
        }
        else
            sendStatusToClient("ERROR");
    }
    
    public void disconnect() throws IOException{
        getSocket().getChannel().close();
    }
}

class Room
{
    String name=null;
    ArrayList<User> usersConnected=null;
    
    Room(String name) {
        this.name=name;
        usersConnected=new ArrayList<User>();
    }
    
    public String getName() {
        return name;
    }
    
    public void addUser(User user) {
        if (!usersConnected.contains(user))
            usersConnected.add(user);
    }
    
    public void removeUser(User user) {
        usersConnected.remove(user);
    }
    
    public boolean isEmpty() {
        return usersConnected.isEmpty();
    }
    
    public void sendMessage(User from, String str) throws IOException {
        
        str+="\n";
        byte[] message = new String("MESSAGE " + from.getName() + " ").getBytes();
        byte[] content = str.getBytes();
        
        ByteBuffer buf = ByteBuffer.wrap(message);
        ByteBuffer contentBuf = ByteBuffer.wrap(content);
        
        for (User u : usersConnected) {
            u.getSocket().getChannel().write(buf);
            u.getSocket().getChannel().write(contentBuf);
            buf.flip();
            contentBuf.flip();
        }
    }
    
    public void sendStatusToOthers(User from, String str) throws IOException {
        
        byte[] message = new String(str+"\n").getBytes();

        for (User u : usersConnected) {
            ByteBuffer buf = ByteBuffer.wrap(message);
            
            if (u!=from) {
                while(buf.hasRemaining())
                    u.getSocket().getChannel().write(buf);
                buf.flip();
            }
        }
    }
}


public class ChatServer
{
    static private final ByteBuffer buffer = ByteBuffer.allocate(16384);
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();
    static private ArrayList<User> users=null;
    static private ArrayList<Room> chatRooms=null;
    
    static public void main( String args[] ) throws Exception {
        users = new ArrayList<User>();
        chatRooms = new ArrayList<Room>();
        int port = Integer.parseInt( args[0] );
        
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking( false );
            
            ServerSocket ss = ssc.socket();
            InetSocketAddress isa = new InetSocketAddress(port);
            ss.bind( isa );
            
            Selector selector = Selector.open();             
            ssc.register(selector,SelectionKey.OP_ACCEPT);   
            System.out.println( "Listening on port "+port );
            
            while (true) {
                int num = selector.select();
                
                if (num == 0) {
                    continue;
                }
                
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                        Socket s = ss.accept();
                        System.out.println( "Got connection from "+s);
                        User newUser = new User(s); //cria um novo utilizador associado a esta socket.
                        users.add(newUser);
                        SocketChannel sc = s.getChannel();
                        sc.configureBlocking( false );
                        sc.register(selector, SelectionKey.OP_READ);
                    }
                    else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        
                        SocketChannel sc = null;
                        
                        try {
                            sc = (SocketChannel)key.channel();
                            
                            boolean ok = processInput(sc);
                            
                            if (!ok) {
                                
                                key.cancel();
                                
                                Socket s = null;
                                try {
                                    s = sc.socket();
                                    User u = fetchUser(sc);
                                    users.remove(u);
                                    System.out.println( "Closing connection to "+s );
                                    s.close();
                                } catch( IOException ie ) {
                                    System.err.println( "Error closing socket "+s+": "+ie );
                                }
                            }
                            
                        } catch( IOException ie ) {
                            key.cancel();
                            
                            try {
                                sc.close();
                            } catch( IOException ie2 ) { System.out.println( ie2 ); }
                            
                            User u = fetchUser(sc);
                            users.remove(u);
                            System.out.println( "Closed "+sc );
                        }
                    }
                }
                keys.clear();
            }
        } catch( IOException ie ) {
            System.err.println( ie );
        }
    }
    
    static private String[] getInput(SocketChannel sc) throws IOException {
        
        boolean stop=false;
        String str="";
        String[] lines = null;
        
        while (!stop) { //espera pela new line
            buffer.clear(); 
            sc.read(buffer);
            buffer.flip();
            str+= decoder.decode(buffer).toString();
            byte[] arr = str.getBytes();
            
            if (arr.length<1)
                stop=true;
            else {
                for (int i=1;i<10;i++) {
                    if ((arr.length-i)<=0) {
                        stop=true;
                        break;
                    }
                    if (arr[arr.length-i]==10){
                        stop=true;
                        break;
                    }
                }
            }
        }
        
        lines = str.split("\\r?\\n");
        return lines;
    }

    static private boolean processInput(SocketChannel sc) throws IOException {
        
        User u = fetchUser(sc);
        
        boolean stop=false;
        String[] lines = getInput(sc);

        if (lines!=null && u!=null) {
            
            for (String line : lines) {
                if (line.length()>1) {
                    char ch1=line.charAt(0);
                    char ch2=line.charAt(1);
                    if ((ch1=='/')&&(ch2!='/'))
                        processCommand(line,u);
                    else {
                        u.sendMessage(line);
                    }
                }
            }
        }
        else
            u.sendStatusToClient("ERROR");

        if (buffer.limit()==0) {         
            return false;
        }
        return true;
    }
    
    static private void processCommand(String str, User u) throws IOException {
        String[] args = str.split("\\s+");
        String cmd = args[0];
        
        switch(cmd) {
        case "/nick":
            if (args.length==2) {
                setUsername(args[1],u);
                break;
            }
        case "/join":
            if (args.length==2){
                joinRoom(args[1],u);
                break;
            }
            
        case "/priv" :
            if (args.length==3) {
                User to = fetchUser(args[1]);
                if (to!=null && u!=to) {
                    u.sendPrivateMessage(to, args[2]);
                    break;
                }
            }            
        case "/leave":
            if (args.length==1){
                leavesRoom(u,false);
                break;
            }
            
        case "/bye":
            if (args.length==1) {
                u.sendStatusToClient("BYE");
                u.sendStatusToOthers("LEFT " + u.getName());
                u.disconnect();
                users.remove(u);
                break;
            }
        default:
            u.sendStatusToClient("ERROR");
            break;
        }
    }
    
    static private void joinRoom(String roomName, User user) throws IOException{
	
	if (user.getState() == State.INIT) {
	    user.sendStatusToClient("ERROR");
	    return;
	}
	    
        if (user.getState() == State.INSIDE){ //verificar se o utilizador ja estava dentro duma sala de chat
            if (!roomName.equals(user.getRoom().getName()))
                leavesRoom(user,true); //true se a chamada para leavesRoom partir deste metodo (joinRoom)
            else
                user.sendStatusToClient("ERROR");
        }
        
        for (Room room : chatRooms) { //verifica se sala ja existe
            if (room.getName().equals(roomName)){ 
                user.setRoom(room);
                room.addUser(user);
                user.sendStatusToOthers("JOINED " + user.getName());
                user.sendStatusToClient("OK");
                return;
            }
        }
        
        Room room = new Room(roomName); //cria uma nova sala
        chatRooms.add(room);
        user.setRoom(room);
        room.addUser(user);
        user.sendStatusToClient("OK");
    }        
    
    static private void setUsername(String nickName, User u) throws IOException{
	
        if (!isTaken(nickName)) {
            String oldNick = u.getName();
            u.setName(nickName);
            u.sendStatusToClient("OK");
            u.sendStatusToOthers("NEWNICK " + oldNick + " " + u.getName());
        }
        else {
            u.sendStatusToClient("ERROR");
        }
    }
    
    static private boolean isTaken(String name) {
        for (User user : users) {
            if (user.getName()!=null) {
                if (user.getName().equals(name))
                    return true;
            }
        }
        return false;
    }
    
    static private void leavesRoom(User user, boolean calledFromJoin) throws IOException{
        
        if (user.getState()==State.INSIDE) {
            if (!calledFromJoin)
                user.sendStatusToClient("OK");
            user.sendStatusToOthers("LEFT " + user.getName());
            
            Room room = user.getRoom();
            user.leaveRoom(); //user abandona a sala
        }
        else {
            user.sendStatusToClient("ERROR");
        }
    }

    static private User fetchUser(SocketChannel sc) {
        for (User u : users) {
            if (u.getSocket() == sc.socket())
                return u;
        }
        return null;
    }

    static private User fetchUser(String str) {
        for (User u : users) {
            if (u.getName().equals(str))
                return u;
        }
        return null;
    }
}
