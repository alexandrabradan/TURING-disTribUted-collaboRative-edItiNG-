import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientChatListener extends Thread {
    /**
     * flag di controllo del ciclo del thread, per consentirgli di interrompere ciclo di ascolto degli
     * inviti, quando l'utente di disconette, e chiudere l' "invitesSocket"
     */
    private boolean userIsOnline;
    /**
     * Utente attualemente connesso
     */
    private String currentUser;
    /**
     * coda che contiene i messaggi inviati sulla chat e non ancora letti dall'utente
     */
    private BlockingQueue<String> history;
    /**
     * DatagramSocket utilizzato per la chat del documento identificato dall'indirizzo di multicast
     * sottostante
     */
    private MulticastSocket chatSocket;
    /**
     * indirizzo di multicast utilizzato per la chat
     */
    private InetAddress group;
    /**
     * Classe che contiene le variabili di configurazione del Client
     */
    private ClientConfigurationManagement clientConfigurationManagement;

    public ClientChatListener(String currentUser, String multicastInd, ClientConfigurationManagement clientConfigurationManagement){
        this.userIsOnline = true;
        this.currentUser = currentUser;
        this.history = new LinkedBlockingQueue<>();
        this.clientConfigurationManagement = clientConfigurationManagement;

        try {
            this.group = InetAddress.getByName(multicastInd);
            this.chatSocket = new MulticastSocket(this.clientConfigurationManagement.getMulticastPort());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    /**
     * Funzione che si occuapa di stampare la history di un Client, in seguito all'operazione RECEIVE
     */
    public void printHistory(){
        StringBuilder msgs = new StringBuilder();

        while(!history.isEmpty()){
            String msg = history.remove(); //rimuovo primo elemento dalla History
            //appendo messaggio al mio msg parziale
            msgs.append(msg);
            msgs.append("\n");
        }

        String finalMsgs =  msgs.toString();
        if(finalMsgs.isEmpty()){
            System.out.println("    Non ci sono messagi da mostrare");
        }
        else System.out.println(finalMsgs);
    }

    /**
     * Funzione che attende i messaggi della chat del documento identificato dall'indirizzo di multicast
     */
    public void run(){

        try{
            //ricavo tempo
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String time = sdf.format(cal.getTime());

            //messaggio da inviare in multicast per dire che utente ha iniziato a modificare una sezione
            String welcome = "    |" + time + "| " + "CONNESSIONE DI: " + currentUser;

            //ricavo byte del messaggio specificato
            byte[] bufWelcome = welcome.getBytes();
            //creo DatagramPacket corrispondente
            DatagramPacket packet = new DatagramPacket(bufWelcome, bufWelcome.length, this.group,
                                                        this.clientConfigurationManagement.getMulticastPort());
            //invio sul Socket multicast il messaggio (inserito in un datagramPacket)
            this.chatSocket.send(packet);

            //setto il timeout (tempo di attesa del messaggio) x per non aspettare in eterno arrivo di un pacchetto
            // ed accorgermi eventualmente della richiesta di terminazione del Client
            chatSocket.setSoTimeout(this.clientConfigurationManagement.getConnectionTimeout());

            //welcome msg inviato con successo
            //mi unisco al Gruppo per ricevere i messaggi della chat
            this.chatSocket.joinGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // Ciclo di attesa messaggi chat
        while(this.userIsOnline){
            byte[] buf = new byte[1024];
            DatagramPacket packet;

            try {
                // Inizializzo il pacchetto
                packet = new DatagramPacket(buf, buf.length);
                //acquisisco il pacchetto dal socket Multicast
                chatSocket.receive(packet);
                //recupero messagio ricevuto
                String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
                //aggiungo messaggio alla history
                this.history.add(msg);

            } catch(SocketTimeoutException e) {
                // Ignoro il timeout (mi serve solo per sbloccarmi dalla receive e verificare se devo terminare
                // chatListener oppure il Server e' crashato)
                ;
            } catch (IOException e) {
               // e.printStackTrace();
                break;
            }
        }

        //sono uscita dal ciclo di ascolto della chat => utente si e' disconesso / fine ultima sezione editata del documento
        try {
            this.chatSocket.leaveGroup(group);
            this.chatSocket.close();  //chiudo socket inviti
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Funzione che si occupa di far terminare il chatListener, facendo GRACEFUL SHUTDOWN,
     * ossia chiudendo il chatListener
     */
    public void interruptClientChatListener(){
        this.userIsOnline = false;
    }
}
