import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientInvitesListenerThread extends Thread {

    /**
     * SocketChannel per gestire gli inviti che supraggiungono mentre il Client e' online
     */
    private SocketChannel invitesSocket;
    /**
     * SocketChannel dell'utente connesso
     */
    private SocketChannel clientSocket;
    /**
     * flag di controllo del ciclo del thread, per consentirgli di interrompere ciclo di ascolto degli
     * inviti, quando l'utente di disconette, e chiudere l' "invitesSocket"
     */
    private boolean userIsOnline;
    /**
     * Classe per gestire invio delle richieste e lettura delle resposte al/dal Server
     */
    private ClientMessageManagement clientMessageManagement;
    /**
     * Classe che contiene variabili di configurazione del Client
     */
    private ClientConfigurationManagement clientConfigurationManagement;
    /**
     * riferimento al chatListener del documento che Client sta Editando
     */
    private ClientChatListener clientChatListenerThread;

    /**
     * Costruttore della classe InvitesHandler
     * @param clientSocket SocketChannel dell'utente connesso
     * @param invitesSocket SocketChannel per gestire le connessioni TCP con il Server, quando invia
     *                      notifiche di invito
     * @param configurationsManagement configurazione del Client
     * @param clientChatListenerThread riferimento al chatListener del documento che Client sta editando
     */
    public ClientInvitesListenerThread(SocketChannel clientSocket, SocketChannel invitesSocket,
              ClientConfigurationManagement configurationsManagement, ClientChatListener clientChatListenerThread) {
        this.userIsOnline = true;
        this.clientSocket = clientSocket;
        this.invitesSocket = invitesSocket;
        this.clientConfigurationManagement = configurationsManagement;
        this.clientChatListenerThread = clientChatListenerThread;
        this.clientMessageManagement = new ClientMessageManagement(this.invitesSocket, configurationsManagement, clientChatListenerThread);
    }

    /**
     * Funzione che cicla per tutto il tempo di connessione del Client al servizio, rimanendo in ascolto di inviti
     * da notificare all'utente connesso
     */
    public void run() {

        //mando msg al Server l'indirizzo del clientSocket, per notificargli che questo channel e' da utilizzare per
        // mandare notifiche al clientSocket
        String hostAndPort = null;
        try {
            hostAndPort = this.clientSocket.getLocalAddress().toString();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        //invio al Server flag OP_INVITE_SOCKET e Client per cui sono invitesSocket
        FunctionOutcome check = this.clientMessageManagement.writeRequest(CommandType.I_AM_INVITE_SOCKET, hostAndPort, "");

        if(check == FunctionOutcome.FAILURE){
            System.out.println("[ERR] >> Impossibile notificare al Server che sono un invitesSocket");
            System.exit(-1);
        }

        //attendo conferma da parte del Server
        check = this.clientMessageManagement.readResponse("");

        if(check == FunctionOutcome.FAILURE){
            System.out.println("[ERR] >> Impossibile leggere la risposta di conferma del Server che sono un" +
                                                                                                    "invitesSocket ");
            System.exit(-1);
        }

        //inizio ciclo di ascolto delle notifiche
        while(this.userIsOnline){
            check = this.clientMessageManagement.readResponse("");

            if(check == FunctionOutcome.FAILURE){
                System.err.println("[ERR] >> Impossibile leggere un nuovo invito");
                System.exit(-1);
            }
        }

        //sono uscita dal ciclo di ascolto degli inviti => utente si e' disconesso
        try {
            this.invitesSocket.close();  //chiudo socket inviti
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Funzione che si occupa di far terminare l'invitesListener, facendo GRACEFUL SHUTDOWN,
     * ossia chiudendo l'inviteListener
     */
    public void interruptClientInvitesListener(){
        this.userIsOnline = false;
    }
}
