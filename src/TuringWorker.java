import javax.swing.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public class TuringWorker implements Runnable{
    /**
     * classe che contiene variabili di configurazione del Server
     */
    private ServerConfigurationsManagement configurationsManagement;
    /**
     * classe che contiene strutture dati del Server
     */
    private ServerDataStructures dataStructures;
    /**
     * classe per gestire files e directories
     */
    private FileManagement fileManagement;
    /**
     * Classe che contiene metodi per leggere richieste e scrivere risposte di esito ai Clients
     */
    private ServerMessageManagement serverMessageManagement;
    /**
     * Classe che contiene metodi per gestire e soddisfare richieste/tasks
     */
    private TuringTask turingTask;
    /**
     * SocketChannel del Client di cui bisogna leggere richiesta
     */
    private SocketChannel client;
    /**
     * richiesta coorente letta dal SocketChannel
     */
    private CommandType currentCommand;
    /**
     * eventuale argomento 1 della richiesta
     */
    private String currentArg1;
    /**
     * eventuale argomento 2 della richiesta
     */
    private String currentArg2;

    /**
     * Costruttore della classe TuringWorker
     * @param configurationsManagement  classe che contiene variabili di configurazione del Server
     * @param dataStructures  classe che contiene strutture dati del Server
     * @param client SocketChannel del Client di cui bisogna leggere richiesta
     */
    public TuringWorker(ServerConfigurationsManagement configurationsManagement, ServerDataStructures dataStructures,
                                                                                                SocketChannel client){
        this.configurationsManagement = configurationsManagement;
        this.dataStructures = dataStructures;
        this.fileManagement = new FileManagement();
        this.serverMessageManagement = new ServerMessageManagement(client);
        this.turingTask = new TuringTask(configurationsManagement, dataStructures, this.serverMessageManagement, client);
        this.client = client;

        this.currentCommand = CommandType.HELP;
        this.currentArg1 = "";
        this.currentArg2 = "";
    }

    //*********************************VERIFICA ARGOMENTI DELLA RICHIESTA*********************************************//

    /**
     * Funzione che si occupa di resettare le variabili di condizione della classe
     */
    private void setDefaultVariablesValues(){
        this.currentCommand = CommandType.HELP;
        this.currentArg1 = "";
        this.currentArg2 = "";
    }

    /**
     * Funzione che verifica se l'username/password/documento passato come argomento rientra nel range stabilito dal file
     * di configurazione o meno
     * @param argument username/password/documento da verificare
     * @return SUCCESS se username/password/documento e' lecito
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkMinNumCharactersArg(String argument){
        int minNumCharactersArg = this.configurationsManagement.getMinNumCharactersArg();
        if(argument.length() < minNumCharactersArg)
            return FunctionOutcome.FAILURE;  //argomento inferiore num. minimo caratteri consentito
        else
            return FunctionOutcome.SUCCESS;  //argomento non inferiore num. minimo caratteri consentito
    }

    /**
     * Funzione che verifica se l'username/password/documento passato come argomento rientra nel range stabilito dal file
     * di configurazione o meno
     * @param argument username/password/documento da verificare
     * @return SUCCESS se username/password/documento e' lecito
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkMaxNumCharactersArg(String argument){
        int maxNumCharactersArg = this.configurationsManagement.getMaxNumCharactersArg();
        if(argument.length() > maxNumCharactersArg)
            return FunctionOutcome.FAILURE;  //argomento supera num. caratteri consentito
        else
            return FunctionOutcome.SUCCESS;  //argomento non supera num.caratteri consentito
    }


    /**
     * Funzione che verifica se il numero di sezioni di un documento che si vuole creare, non supera il valore
     * massimo consentito dal file di configurazione
     * @param numSections numero di sezioni da verificare
     * @return SUCCESS se il numero di sezioni e' lecito
     *         FAILURE altrimenti
     */
    private  FunctionOutcome checkMaxNumSectionsPerDocument(int numSections){
        int maxNumSectionsPerDocument = this.configurationsManagement.getMaxNumSectionsPerDocument();
        if(numSections > maxNumSectionsPerDocument)
            return FunctionOutcome.FAILURE;  //numero sezioni supera valore consentito
        else
            return FunctionOutcome.SUCCESS;  //numero sezioni non supera valore consentito
    }

    /**
     * Funzione che si occupa di  verificare se gli argomenti della richiesta fatta dal Client sono leciti, piu' precisamente:
     * 1. verifica che username/password/nome_documento non superino il num. massimo di caratteri consentito
     * 2. verifica che username/password non abbiano un num. di caratteri inferiore a quello consentito
     * 3. verifica che username non contenga caratteri speciali => username viene utilizzato per creare cartelle (file
     * speciali che non ammettono caratteri speciali)
     * 4. verifica se il numero di sezione passato come argomento rientra nel range stabilito (0, MAX_NUM_SEC]
     * Inoltre, la funzione si occupa di inviare un messaggio di errore immediato se un argomento non supera i controlli,
     * altrimenti invoca la funzione che soddisfa tale richiesta.
     * @return SUCCESS se l'invio della risposta al Client e' andato a buon fine (non ci sono stati problemi I/O con
     *                  suo SocketChannel e lo posso reinserire nel Selector)
     *         FAILURE altrimenti
     */
    private FunctionOutcome satisfyRequest(){
        switch(this.currentCommand){
            case LOGIN:{
                //provo a soddisfare la richiesta del Client e gli invio esito
                return this.turingTask.loginTask(this.currentArg1, this.currentArg2);
            }
            case LOGOUT:{
                //provo a soddisfare la richiesta del Client e gli invio esito
                return this.turingTask.logoutTask();
            }
            case CREATE:{
                //verifico se il nomde del documento supera il numero massimo consentito
                FunctionOutcome check = checkMaxNumCharactersArg(this.currentArg1);
                if(check == FunctionOutcome.SUCCESS){
                    //verifico che il numero delle sezioni non superi il valore massimo consentito
                    check = checkMaxNumSectionsPerDocument(Integer.parseInt(this.currentArg2));
                    if(check == FunctionOutcome.SUCCESS){

                        //provo a soddisfare la richiesta del Client e gli invio esito
                        return this.turingTask.createTask(this.currentArg1, Integer.parseInt(this.currentArg2));
                    }
                    else{
                        return this.serverMessageManagement.writeResponse(ServerResponse.OP_SECTION_EXCEED_LIMIT, "");
                    }
                }
                else {
                    return this.serverMessageManagement.writeResponse(ServerResponse.OP_DOCUMENT_TOO_LONG, "");
                }
            }
            case SHARE:{
                //provo a soddisfare la richiesta del Client e gli invio esito
                return this.turingTask.shareTask(this.currentArg1, this.currentArg2);
            }
            case SHOW_DOCUMENT:{
                //provo a soddisfare la richiesta del Client e gli invio esito
                return this.turingTask.showDocumentTask(this.currentArg1);
            }
            case SHOW_SECTION:{
                //provo a soddisfare la richiesta del Client e gli invio esito
               return this.turingTask.showSectionTask(this.currentArg1, Integer.parseInt(this.currentArg2));
            }
            case LIST:{
                //provo a soddisfare la richiesta del Client e gli invio esito
                return this.turingTask.listTask();
            }
            case EDIT:{
                //provo a soddisfare la richiesta del Client e gli invio esito
                return this.turingTask.editTask(this.currentArg1, Integer.parseInt(this.currentArg2));
            }
            case END_EDIT:{
                //provo a soddisfare la richiesta del Client e gli invio esito
                return this.turingTask.endEditTask(this.currentArg1, Integer.parseInt(this.currentArg2));
            }
            case SEND:{
                //provo a soddisfare la richiesta del Client e gli invio esito
                return this.turingTask.sendTask(this.currentArg1);
            }
            case RECEIVE:{
                //provo a soddisfare la richiesta del Client e gli invio esito
                return this.turingTask.receiveTask();
            }
            default:{
                //richiesta non corrisponde a quelle soddisfate dal servizio (casistica non si verifica mai per i
                // controlli svolti nella sintassi dei commandi nel Client)
                return this.serverMessageManagement.writeResponse(ServerResponse.OP_INVALID_REQUEST, "");
            }
        }
    }

    //****************************************CICLO LAVORO DEL WORKER************************************************//

    /**
     * Funzione che si occupa di:
     * 1. chiudere il SocketChannel del Client di cui il Worker-thread si sta occupando
     * 2. far terminare il Worker-thread corrente
     */
    private void endWorker(){
        try {

            //@TODO VERIFICARE SE CLIENT STAVA EDITANDO QUALCHE SEZIONE, CHIUDERE EDITING E RILASCIARE MUTUA ESCLUSIONE

            //verifico se Client e' connesso e se lo e', lo disconetto
            this.dataStructures.removeFromOnlineUsers(this.client);

            //chiudo il SocketChannel del Client di cui il Worker si sta occupando
            this.client.close();

            System.out.println(String.format("[%s] >> Socket |%s| chiuso con successo",
                    Thread.currentThread().getName(), this.client.getRemoteAddress().toString()));

            //termino Worker-thread corrente
            Thread.currentThread().interrupt();

        } catch (IOException e) {
            e.printStackTrace();
            try {
                System.err.println(String.format("[%s] Impossibile chiudere il SocketChannel |%s|",
                        Thread.currentThread().getName(), this.client.getRemoteAddress().toString()));
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
            System.exit(-1);
        }
    }

    /**
     * Ciclo di lavoro del worker (implementando l'interfaccia Runnable, il Worker e' un task che viene passato
     * come argomento ad un Thread del ThreadPool, il quale invochera' questo metodo alla sua messa in esecuzione)
     */
    public void run(){

        //resetto variabili eventualmente inizializzate precedentemente
        setDefaultVariablesValues();

        //leggo richiesta del Client
        FunctionOutcome readRequest = this.serverMessageManagement.readRequest();

        if(readRequest == FunctionOutcome.FAILURE){
            try {
                System.err.println(String.format("[%s] >> Lettura richiesta del socket |%s| fallita",
                        Thread.currentThread().getName(), this.client.getRemoteAddress().toString()));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }

            //problemi I/O con SocketChannel del Client => chiudo SocketChannel e termino Worker-thread
            endWorker();
        }

        //lettura richiesta Client andata a buon fine
        //recupero richiesta e suoi eventuali argomenti
        this.currentCommand = this.serverMessageManagement.getCurrentCommand();
        this.currentArg1 = this.serverMessageManagement.getCurrentArg1();
        this.currentArg2 = this.serverMessageManagement.getCurrentArg2();

        try {
            System.out.println(String.format("[%s] >> Lettura richiesta |%s| del socket |%s| avvenuta con successo",
                    Thread.currentThread().getName(), this.currentCommand, this.client.getRemoteAddress().toString()));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        //a seconda della richiesta/comando letto verifico legittimita' argomenti
        //1. se argomenti non sono legittimi, invio msg di errore al Client
        //2. se argomenti sono legittimi, proveddo a soddisfare richiesta e inviare esito al Client
        FunctionOutcome sendResponse = satisfyRequest();

        if(sendResponse == FunctionOutcome.FAILURE){
            try {
                System.err.println(String.format("[%s] >> Invio risposta al socket |%s| fallita",
                        Thread.currentThread().getName(), this.client.getRemoteAddress().toString()));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }

            //problemi I/O con SocketChannel del Client => chiudo SocketChannel e termino Worker-thread
            endWorker();
        }
        else{
            //invio risposta Client andato a buon fine
            //inserisco il SocketChannel del Client nell'insieme di channels da reinserire nel Selector per
            //attendere lettura nuove richieste
            this.dataStructures.addSelectorKeysToReinsert(this.client);
        }
    }
}
