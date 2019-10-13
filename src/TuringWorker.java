import java.nio.channels.SocketChannel;

public class TuringWorker implements Runnable{
    private ServerConfigurationsManagement configurationsManagement; //classe che contiene variabili di configurazione
    private ServerDataStructures dataStructures; //classe che contiene strutture dati del Server
    private TuringTask turingTask; //classe che contiene metodi per gestire e soddisfare richieste/tasks
    private SocketChannel client;  //SocketChannel del Client di cui bisogna leggere richiesta
    private FileManagement fileManagement; //classe per gestire files e directories
    private CommandType currentCommand; //commando corrente letto da  tastiera
    private String currentArg1;  //eventuale argomento 1 del comando
    private String currentArg2;  //eventuale argomento 2 del comando

    public TuringWorker(ServerConfigurationsManagement configurationsManagement, ServerDataStructures dataStructures,
                                                                                                SocketChannel client){
        this.configurationsManagement = configurationsManagement;
        this.dataStructures = dataStructures;
        this.client = client;
        this.turingTask = new TuringTask(configurationsManagement, dataStructures, client);

        this.fileManagement = new FileManagement();
        this.currentCommand = CommandType.HELP;
        this.currentArg1 = "";
        this.currentArg2 = "";
    }

    //****************************GETTER AND SETTER RICHIESTA ED SUOI ARGOMENTI DA SODDISFARE ************************//

    /**
     * Funzione che ritorna il commando richiesto dal Client
     * @return CommandType commando da soddisfare
     */
    public CommandType getCurrentCommand(){
        return this.currentCommand;
    }

    /**
     * Funzione che inizializza il comando richiesto dal Client
     * @param command commando da soddisfare
     */
    private void setCurrentCommand(CommandType command){
        this.currentCommand = command;
    }

    /**
     * Funzione che ritorna il primo argomento del commando richiesto dal Client
     * @return arg1 primo argomento del commando da soddisfare
     */
    public String getCurrentArg1(){
        return this.currentArg1;
    }

    /**
     * Funzione che inizializza il primo argomento del commando richiesto dal Client
     * @param arg1 primo argomento del comando da soddisfare
     */
    private void setCurrentArg1(String arg1){
        this.currentArg1 = arg1;
    }

    /**
     * Funzione che ritorna il secondo argomento del commando  richiesto dal Client
     * @return arg2 secondo argomento del commando da soddisfare
     */
    public String getCurrentArg2(){
        return this.currentArg2;
    }

    /**
     * Funzione che inizializza il secondo argoento del comando richiesto dal Client
     * @param arg2 secondo argomento del comando da soddisfare
     */
    private void setCurrentArg2(String arg2){
        this.currentArg1 = arg2;
    }

    /**
     * Funzione che si occupa di resettare le variabili di condizione della classe
     */
    private void setDefaultVariablesValues(){
        this.currentCommand = CommandType.HELP;
        this.currentArg1 = "";
        this.currentArg2 = "";
    }

    private void parseRequest(String request){
        //divido commando letto in prossimita' di uno o piu' spazi vuoti
        String[] requestAndArgs = request.split("\\s+");
        this.currentCommand = CommandType.valueOf(requestAndArgs[0]); //ricavo valore enum a partire da stringa
        this.currentArg1 = requestAndArgs[1];
        this.currentArg2 = requestAndArgs[2];
    }

    //*********************************VERIFICA ARGOMENTI DELLA RICHIESTA*********************************************//

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
     * Funzione che verifica se il numero di sezione passato come argomento rientra nel range stabilito dal file
     * di configurazione o meno
     * @param numSections numero di sezione da verificare
     * @return SUCCESS se il numero di sezione e' lecito
     *         FAILURE altrimenti
     */
    private  FunctionOutcome checkMaxNumSectionsPerDocument(int numSections){
        int maxNumSectionsPerDocument = this.configurationsManagement.getMaxNumSectionsPerDocument();
        if(numSections > maxNumSectionsPerDocument)
            return FunctionOutcome.FAILURE;  //numero sezioni supera valore consentito
        else
            return FunctionOutcome.SUCCESS;  //numero sezioni non supera valore consentito
    }

    private ServerResponse satisfyRequest(CommandType command, String arg1, String arg2){
        switch(command){
            case LOGIN:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.loginTask(arg1, arg2);
            }
            case LOGOUT:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.logoutTask();
            }
            case CREATE:{
                //verifico se il nomde del documento supera il numero massimo consentito
                FunctionOutcome check = checkMaxNumCharactersArg(arg1);
                if(check == FunctionOutcome.SUCCESS){
                    //verifico che il numero delle sezioni non superi il valore massimo consentito
                    check = checkMaxNumSectionsPerDocument(Integer.parseInt(arg2));
                    if(check == FunctionOutcome.SUCCESS){

                        //provo a soddisfare la richiesta del Client e ritorno esito
                        return this.turingTask.createTask(arg1, Integer.parseInt(arg2));
                    }
                    else return ServerResponse.OP_SECTION_EXCEED_LIMIT;
                }
                else return ServerResponse.OP_DOCUMENT_TOO_LONG;
            }
            case SHARE:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.shareTask(arg1, arg2);
            }
            case SHOW_DOCUMENT:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.showDocumentTask(arg1);
            }
            case SHOW_SECTION:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.showSectionTask(arg1, Integer.parseInt(arg2));
            }
            case LIST:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.listTask();
            }
            case EDIT:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.editTask(arg1, Integer.parseInt(arg2));}
            case END_EDIT:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.endEditTask(arg1, Integer.parseInt(arg2));
            }
            case SEND:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.sendTask(arg1);
            }
            case RECEIVE:{
                //provo a soddisfare la richiesta del Client e ritorno esito
                return this.turingTask.receiveTask();
            }
            default:{
                //richiesta non corrisponde a quelle soddisfate dal servizio (casistica non si verifica mai per i
                // controlli svolti nella sintassi dei commandi nel Client)
                return ServerResponse.OP_INVALID_REQUEST;
            }
        }
    }

    //****************************************CICLO LAVORO DEL WORKER************************************************//

    public void run(){
        while(true){
            //@TODO leggo richiesta - la estrappolo dalla TaskQueue
            //ricavo richiesta ed eventuali argomenti, facendo il parsing del contenuto letto
            parseRequest("");

            //a seconda della richiesta/comando letto verifico legittimita' argomenti
            //1. se argomenti non sono legittimi, invio msg di errore al Client
            //2. se argomenti sono legittimi, proveddo a soddisfare richiesta ed suo invio esito al Client
            ServerResponse serverResponse = satisfyRequest(this.currentCommand, this.currentArg1, this.currentArg2);
        }
    }
}
