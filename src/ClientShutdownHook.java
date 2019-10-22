import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientShutdownHook extends Thread{
    /**
     * thread che ascolta inviti
     */
    private ClientInvitesListenerThread invitesListenerThread;
    /**
     * Classe che contiene riferimento al chatListener
     */
    private ClientMessageManagement clientMessageManagement;
    /**
     * Classe che contiene le variabili di configurazione del Client
     */
    private ClientConfigurationManagement configurationsManagement;
    /**
     * SocketChannel del Client
     */
    private SocketChannel clientSocket;

    /**
     * Costruttore della classe ClientShutdownHook
     * @param invitesListenerThread thread che ascolta inviti per farlo terminare
     * @param clientMessageManagement classe che contiene riferimento al chatListener per farlo terminare
     * @param configurationsManagement Classe che contiene le variabili di configurazione del Client per cancellare le
     *                                 cartelle dedicate al Client
     * @param clientSocket SocketChannel del Client per chiuderlo
     */
    public ClientShutdownHook(ClientInvitesListenerThread invitesListenerThread, ClientMessageManagement clientMessageManagement,
                              ClientConfigurationManagement configurationsManagement, SocketChannel clientSocket){
        this.invitesListenerThread = invitesListenerThread;
        this.clientMessageManagement = clientMessageManagement;
        this.configurationsManagement = configurationsManagement;
        this.clientSocket = clientSocket;
    }

    public void run(){
        if(this.clientSocket != null){
            try {

                if(this.invitesListenerThread != null){
                    //interrompo thread inviti
                    this.invitesListenerThread.interruptClientInvitesListener();
                    System.out.println("[Turing] >> Thread inviti e socket-inviti chiusi");
                }

                //interrompo thread inviti (se attivo)
                this.clientMessageManagement.terminateChatListener();
                System.out.println("[Turing] >> Thread chat e socket-chat chiusi");

                //cancello cartella dedicata al Client in "/Turing_downloads/" e in "/Turing_edit_mode/"
                //cancello cartella dedicata al Client in "/Turing_downloads/" e in "/Turing_edit_mode/"
                this.configurationsManagement.deallocateClientConf();
                System.out.println("[Turing] >> Cartelle dedicate al Client cancellate");

                this.clientSocket.close();
                System.out.println("[Turing] >> Client-socket chiuso");

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("[ERR] >> Impossibile chiudere client-socket");
                System.exit(-1);
            }
        }
    }
}
