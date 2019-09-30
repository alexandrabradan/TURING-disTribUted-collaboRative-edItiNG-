public class RMIRegistrationHandler {
    private int exit;

        /*private static FunctionOutcome handleRegistration(String username, String password){
        try {
            registrationRMI(username, password);
        } catch (RemoteException | NotBoundException e) {
            System.err.println("[ERR] >> Server offline, impossibile registrare utente");
            closeClientSocket(); //chiudo client-socket e programma
        }
    }*/


    /**
     * Funzione che aziona l'RMI per la registrazione di un nuovo utente
     * @param username nome dell'utente
     * @param password password dell'utente
     * @return SUCCESS se registrazione e' andata a buon fine
     *         FAILURE altrimenti
     */
   /* public static FunctionOutcome registrationRMI(String username, String password){
        int RMIPort = configurationsManagement.getRMIPort();
        Registry registry = LocateRegistry.getRegistry(RMIPort);
        RegistrationInterface stubRMI = (RegistrationInterface) registry.lookup(RegistrationInterface.RMI_NAME);
        int res = stubRMI.registrationHandler(username, password);
        if(res == 0) { //utente  già registrato
            System.err.println("[ERR] >> Username gia' in uso. Per favore scegli un altro username");
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return FunctionOutcome.FAILURE; //per invitare utente e ridigitare comando
        }
        else {  //utente registrato correttamente
            System.out.println("[Turing] >> Utente registrato correttamente");
        }
    }
    */

    /**
     * Funzione chiamata esternamente per interrompere il ciclo di attesa degli inviti live e chiudere il client-socket
     */
    public void interruptListener() {
        exit = 0;
    }
}
