import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class ChatClient{
    
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    Socket clientSocket=null;
    BufferedReader inFromUser=null; //stream de entrada
    BufferedReader inFromServer=null; //stream de entrada
    OutputStream outToServer=null; //stream de saida
    BufferedReader in=null; //stream de entrada
    
    
    // Método a usar para acrescentar uma string à caixa de texto
    public void printMessage(final String message) {
        chatArea.append(message);
    }
    
    
    // Construtor
    public ChatClient(String server, int port) throws IOException {
        
        // Inicializacao da interface gráfica
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        newMessage(chatBox.getText());
                    } catch (IOException ex) {
                    } finally {
                        chatBox.setText("");
                    }
                }
            });

        
        clientSocket = new Socket(server,port);
        outToServer = clientSocket.getOutputStream();
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //stream de entrada do client
    }
    
    
    // Metodo invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
     
        byte[] bytes = new String(message+"\n").getBytes();
        
        outToServer.write(bytes);
        
        if (message.equals("/bye")) {
            frame.setVisible(false);
            frame.dispose();
            System.exit(0);
        }
    }
    
    // Método principal do objecto
    public void run() throws IOException { //recebe informacao do servidor

        InputStream in = clientSocket.getInputStream();
        int read = -1;
        String str ="";

        while((read = in.read()) != -1) { //lê byte a byte
            char ch = (char) read;
            str+=ch;
	    
            if (ch=='\n') {
                String arr[] = str.split(" ");
                String cmd = arr[0]; //comando
                String output="";
                
                switch(cmd) {
                case "MESSAGE": //MESSAGE nome mensagem
                    String tmp[] = str.split(" ",3);
                    output=tmp[1]+": "+tmp[2].replace("\n",""); //nome: message
                    break;
                case "LEFT": //LEFT nome
                    output=arr[1].replace("\n","") +" saiu da sala"; //nome saiu da sala
                    break;
                case "BYE": //BYE nome
                    output=arr[1].replace("\n","") + " saiu do chat"; //nome saiu
                    break;
                case "JOINED": //JOINED nome
                    output=arr[1].replace("\n","") + " entrou na sala"; //nome entrou na sala
                    break;
                case "NEWNICK": //NEWNICK nome_antigo nome_novo
                    output=arr[1].replace("\n","") +" mudou de nome para " + arr[2].replace("\n",""); //nome mudou para nome2
                    break;
                case "PRIVATE": 
                    String tmp2[] = str.split(" ",3); //PRIVATE de para mensagem
                    output=tmp2[1].replace("\n","") + " (mensagem privada): "+tmp2[2].replace("\n",""); //nome (mensagem privada): mensagem
                    break;
                default:
                    output=str.replace("\n","");
                    break;
                }
                printMessage(output+"\n");
                str="";
            }
        }
    }
    
    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // NAO MODIFICAR
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
    
}
