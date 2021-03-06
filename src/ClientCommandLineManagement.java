import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientCommandLineManagement {
    /**
     * Classe che contiene le variabili di configurazione del Client
     */
    private ClientConfigurationManagement clientConfigurationManagement;
    /**
     * commando corrente letto da  tastiera
     */
    private CommandType currentCommand;
    /**
     * eventuale argomento 1 del comando
     */
    private String currentArg1;
    /**
     * eventuale argomento 2 del comando
     */
    private String currentArg2;


    /**
     * Costruttore della classe CommandLineManagement
     * @param clientConfigurationManagement classe che contiene le variabili di configurazione del Client
     */
    public ClientCommandLineManagement(ClientConfigurationManagement clientConfigurationManagement){
        this.clientConfigurationManagement = clientConfigurationManagement;
        this.currentCommand = CommandType.HELP;
        this.currentArg1 = "";
        this.currentArg2 = "";
    }

    /**
     * Funzione che ritorna il commando corrente letto da tastiera
     * @return CommandType commando corrente
     */
    public CommandType getCurrentCommand(){
        return this.currentCommand;
    }

    /**
     * Funzione che inizializza il comando corrente
     * @param command commando attuale
     */
    private void setCurrentCommand(CommandType command){
        this.currentCommand = command;
    }

    /**
     * Funzione che ritorna il primo argomento del commando corrente letto da tastiera
     * @return arg1 primo argomento del commando corrente
     */
    public String getCurrentArg1(){
        return this.currentArg1;
    }

    /**
     * Funzione che inizializza il primo argomento del  comando corrente
     * @param arg1 primo argomento del comando attuale
     */
    private void setCurrentArg1(String arg1){
        this.currentArg1 = arg1;
    }

    /**
     * Funzione che ritorna il secondo argomento del commando corrente letto da tastiera
     * @return arg2 secondo argomento del commando corrente
     */
    public String getCurrentArg2(){
        return this.currentArg2;
    }

    /**
     * Funzione che inizializza il secondo argoento del  comando corrente
     * @param arg2 secondo argomento del comando attuale
     */
    private void setCurrentArg2(String arg2){
        this.currentArg2 = arg2;
    }

    /**
     * Funzione che si occupa di resettare ad ogni invocazione del metodo "readAndParseCommand" le variabili di
     * condizione della classe
     */
    private void setDefaultVariablesValues(){
        this.currentCommand = CommandType.HELP;
        this.currentArg1 = "";
        this.currentArg2 = "";
    }


    /**
     * Funzione che stampa un messaggio di aiuto qualora il Client lo richieda tramite il comando:
     * >> usage
     * oppure quando il Client digita un comando sintatticamente scoretto
     */
    private void printHelp() {
        System.out.println("usage: turing COMMAND [ARGS...]");
        System.out.println();
        System.out.println("COMMANDS: ");
        System.out.println("	--help                         | Stampa messaggio di aiuto");
        System.out.println("	register <username> <password> | Registra l'user");
        System.out.println("	login <username> <password>    | Connette l'user");
        System.out.println("	logout                         | Disconnette l'user");
        System.out.println();
        System.out.println("	create <doc> <numsezioni>      | Crea un documento");
        System.out.println("	share <doc> <username>         | Condivide un documento");
        System.out.println("	show <doc> <sec>               | Mostra una sezione del documento");
        System.out.println("	show <doc>                     | Mostra l'intero documento");
        System.out.println("	list                           | Mostra la lista dei documenti");
        System.out.println();
        System.out.println("	edit <doc> <sec>               | Modifica una sezione del documento");
        System.out.println("	end-edit					   | Fine modifica della sezione del documento");
        System.out.println();
        System.out.println("	send <msg>                     | Invia un messaggio sulla chat");
        System.out.println("	receive                        | Visualizza i messaggi ricevuti sulla chat");
        System.out.println("	exit                           | Termina l'user");
    }

    /**
     * Funzione che si occupa di leggere un commando da tastiera e di verificarne la correttezza sintattica
     * @param documentToEdit nome del documento che il Client sta eventualmente editando
     * @param sectionToEdit eventuale sezione che il Client sta editando
     * @return SUCCESS se il commando e' sintatticamente corretto
     *         FAILURE altrimenti
     */
    public FunctionOutcome readAndParseCommand(String documentToEdit, int sectionToEdit){

        //assegno valori di defual alle variabili di condizione della classe, per evitare incosistenze con comandi
        //letti da tastiera precedentemente
        setDefaultVariablesValues();

        //leggo commando da tastiera
        //uso BufferedReader + InputStreamReader per utilizzare una componente  del pacchetto "java.io.*"
        //N.B. Nel resto del progetto viene usato esclusivamente pacchetto "java.nio.*"
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));

        try {

            // leggo commando da tastiera
            String command = reader.readLine();

            if(command.length() == 0){  //nessun commando inserito (solo Tab, altrimenti BufferedReader non inescato)
                System.err.println("[Turing] >> Non hai inserito nessun comando.");
                System.out.println("[Turing] >> Per favore, digita nuovamente il comando:");
                return readAndParseCommand(documentToEdit, sectionToEdit);
            }
            else{  //e' stato digitato qualcosa, verifico se e' lecito

                //divido commando letto in prossimita' di uno o piu' spazi vuoti
                String[] commandWords = command.split("\\s+");

                int commandWordsLength = commandWords.length;  //numero di parole lette

                //verifico se comando inizia con la parola "turing"
                if(!commandWords[0].equals("turing")){  //comando NON inizia con la parola turing
                    System.err.println("[Turing] >> Comando non valido.");
                    System.out.println("[Turing] >> Se hai bisogno di aiuto digita:");
                    System.out.println("[Turing] >> turing --help");
                    System.out.println("[Turing] >> Altrimenti digita nuovamente il comando:");
                    return readAndParseCommand(documentToEdit, sectionToEdit);
                }
                else{  // comando inizia con la parola "turing"

                    //verifico se esiste seconda parola (richiesta)
                    if(commandWordsLength  < 2){  //seconda parola NON esiste => ho inserito solo parola "turing"
                        System.err.println("[Turing] >> Comando incompleto.");
                        System.out.println("[Turing] >> Se hai bisogno di aiuto digita:");
                        System.out.println("[Turing] >> turing --help");
                        System.out.println("[Turing] >>  Altrimenti digita nuovamente il comando:");
                        return readAndParseCommand(documentToEdit, sectionToEdit);
                    }
                    else{  //esiste seconda parola (richiesta)

                        //sto editando un documneto => posso fare solo le seguenti operazioni:
                        //1. SEND
                        //2. RECEIVE
                        //3. END-EDIT
                        //4. HELP
                        //5. EXIT
                        if(!documentToEdit.isEmpty()){
                            if(!commandWords[1].equals("send") && !commandWords[1].equals("receive") &&
                                    !commandWords[1].equals("end-edit") && !commandWords[1].equals("help")){
                                System.err.println(String.format("[ERR] Devi prima finire di editare la sezione |%s| del documento |%s| " +
                                                "per poter digitare un nuovo comando", sectionToEdit, documentToEdit));
                                return FunctionOutcome.FAILURE; //segnalo al Client di ripetere digitazione comando
                            }
                        }

                        //verifico formato richiesta
                        switch (commandWords[1]){
                            
                            case("--help"):{
                                //verifico se dopo il --help ci sono ancora parole
                                String correctCommandToPrint = "--help";
                                FunctionOutcome check =  checkEmptyARGRequest(commandWords,correctCommandToPrint,
                                        CommandType.HELP, documentToEdit, sectionToEdit);
                                if(check == FunctionOutcome.SUCCESS) //comando sintatticamente corretto
                                    printHelp();  //stampo msg di aiuto
                                return check;  // ritorno successo/insuccesso
                                }
                            case("register"):{
                                //verifico se c'e' l'username e la password
                                String correctCommandToPrint = "register <username> <password>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint,
                                        CommandType.REGISTER, documentToEdit, sectionToEdit);
                            }
                            case("login"):{
                                //verifico se c'e' l'username e la password
                                String correctCommandToPrint = "login <username> <password>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint,
                                        CommandType.LOGIN, documentToEdit, sectionToEdit);
                            }
                            case("logout"):{
                                String correctCommandToPrint = "logout";
                                return checkEmptyARGRequest(commandWords, correctCommandToPrint,
                                        CommandType.LOGOUT, documentToEdit, sectionToEdit);
                            }
                            case("create"):{
                                //verifico se c'e' nome documento e numero sezioni
                                String correctCommandToPrint = "create <doc> <numsezioni>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint,
                                        CommandType.CREATE, documentToEdit, sectionToEdit);
                            }
                            case("share"):{
                                //verifico se c'e' il documento e l'username per la condivisione
                                String correctCommandToPrint = "share <doc> <username>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint,
                                        CommandType.SHARE, documentToEdit, sectionToEdit);
                            }
                            case("show"):{
                                //verifico se esiste solo terza parola (=> mostra intero documento)
                                //oppure esiste anche quarta parola (=> mostra una sezione del documento)
                                
                                if(commandWordsLength == 3){ //esiste solo documento
                                    String correctCommandToPrint = "show <doc>";
                                    return checkOneARGRequest(commandWords, correctCommandToPrint,
                                            CommandType.SHOW_DOCUMENT, documentToEdit, sectionToEdit);
                                }
                                else if(commandWordsLength >= 4) {  //esiste anche sezione del documento
                                    String correctCommandToPrint = "show <doc> <sec>";
                                    return checkTwoARGSRequest(commandWords, correctCommandToPrint,
                                            CommandType.SHOW_SECTION, documentToEdit, sectionToEdit);
                                }
                                else{
                                    System.err.println("[Turing] >> Comando scoretto. Forse intendevi:");
                                    System.out.println("[Turing] >> turing show <doc> OPPURE turing show <doc> <sec>");
                                    System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
                                    return readAndParseCommand(documentToEdit, sectionToEdit);
                                }
                            }
                            case("list"):{
                                //verifico se dopo il list ci sono ancora parole
                                String correctCommandToPrint = "list";
                                return checkEmptyARGRequest(commandWords, correctCommandToPrint,
                                        CommandType.LIST, documentToEdit, sectionToEdit);
                            }
                            case("edit"):{
                                //verifico se c'e' nome documento e numero sezione da mofidicare
                                String correctCommandToPrint = "edit <doc> <sec>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint,
                                        CommandType.EDIT, documentToEdit, sectionToEdit);
                            }
                            case("end-edit"):{
                                //verifico se c'e' nome documento e numero sezione modificata
                                String correctCommandToPrint = "end-edit <doc> <sec>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint,
                                        CommandType.END_EDIT, documentToEdit, sectionToEdit);
                            }
                            case("send"):{
                                //verifico se c'e messaggio da inviare
                                String correctCommandToPrint = "send <msg>";
                                return checkSendMessage(commandWords, correctCommandToPrint,
                                        CommandType.SEND, documentToEdit, sectionToEdit);
                            }
                            case "receive":{
                                //verifico se dopo il receive ci sono ancora parole
                                String correctCommandToPrint = "receive";
                                return checkEmptyARGRequest(commandWords, correctCommandToPrint,
                                        CommandType.RECEIVE, documentToEdit, sectionToEdit);
                            }
                            case "exit":{
                                //verifico se dopo l' exit ci sono ancora parole
                                String correctCommandToPrint = "exit";
                                return checkEmptyARGRequest(commandWords, correctCommandToPrint,
                                        CommandType.EXIT, documentToEdit, sectionToEdit);
                            }
                            default:
                                System.err.println("[Turing] >> Comando inesistente.");
                                System.out.println("[Turing] >> Se hai bisogno di aiuto digita:");
                                System.out.println("[Turing] >> turing --help");
                                System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
                                return readAndParseCommand(documentToEdit, sectionToEdit);
                        }
                    }
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
            System.err.println("[ERR] >> Errore nella lettura del comando da tastiera");
            System.exit(-1);
        }
        return FunctionOutcome.FAILURE;
    }

    /**
     * Funzione che verifica se la string passata come argomento corrisponde ad un valore numerico oppure meno
     * @param numSections stringa da verificare se e' solamente numerica
     * @return true se la stringa passata come argomento corrisponde ad un valore numerico
     *         false altrimenti
     */
    private boolean checkIfNumSectionIsNumeric(String numSections){
        return numSections != null && numSections.matches("[0-9]+");
    }

    /**
     * Funzione che verifica se il numero di sezioni e' non negativo
     * @param numSections numero di sezioni da verificare
     * @return SUCCESS se il numero di sezioni e' > 0
     *         FAILURE
     */
    private FunctionOutcome checkIFNumSectionIsStrictlyPositive(int numSections){
        if(numSections < 1)
            return FunctionOutcome.FAILURE;
        else
            return FunctionOutcome.SUCCESS;
    }

    /**
     * Funzione che verifica che i commandi senza argomenti:
     * 1. turing --help
     * 2. turing logout
     * 3. turing list
     * 4. turing receive
     * 5. turing exit
     * non abbiano parole/argomenti a seguirli
     * @param commandWords parole lette da linea di commando
     * @param correctCommandToPrint messaggio personalizzato da stampare sullo schermo
     * @param documentToEdit eventuale documento che Client sta editando
     * @param sectionToEdit eventuale sezione che Client sta editando
     * @return SUCCESS se comando e' sintaticamente corretto
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkEmptyARGRequest(String[] commandWords, String correctCommandToPrint, CommandType commandType,
                                                 String documentToEdit, int sectionToEdit){
        if(commandWords.length != 2){ //dopo richiesta ci sono parole/argomenti
            System.err.println("[Turing] >> Comando scoretto. Forse intendevi:");
            System.out.println("[Turing] >> turing " + correctCommandToPrint);
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return readAndParseCommand(documentToEdit, sectionToEdit);
        }
        else{ //comando senza argomenti e' sintatticamente corretto
            setCurrentCommand(commandType);
            return FunctionOutcome.SUCCESS;
        }
    }

    /**
     * Funzione che verifica che i commandi senza argomenti:
     * 1. show <doc>
     * non abbiano parole/argomenti a seguirli
     * @param commandWords parole lette da linea di commando
     * @param correctCommandToPrint messaggio personalizzato da stampare sullo schermo
     * @param commandType tipo di operazione richiesta
     * @param documentToEdit eventuale documento che Client sta editando
     * @param sectionToEdit eventuale sezione che Client sta editando
     * @return SUCCESS se comando e' sintaticamente corretto
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkOneARGRequest(String[] commandWords, String correctCommandToPrint, CommandType commandType,
                                               String documentToEdit, int sectionToEdit){
        //verifico se esiste documento da mostrare
        if(commandWords.length != 3) { //non esiste documento
            System.err.println("[Turing] >> Comando scoretto. Forse intendevi:");
            System.out.println("[Turing] >> turing " + correctCommandToPrint);
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return readAndParseCommand(documentToEdit, sectionToEdit);
        }
        else{ //commando con 1 argomento e' sintatticamente corretto
            setCurrentCommand(commandType);
            setCurrentArg1(commandWords[2]);
            return FunctionOutcome.SUCCESS;
        }
    }

    /**
     * Funzione che verifica che ci sia un messaggio da inviare sulla chat:
     * 1. send <msg>
     * @param commandWords parole lette da linea di commando
     * @param correctCommandToPrint messaggio personalizzato da stampare sullo schermo
     * @param commandType tipo di operazione richiesta dal Client
     * @param documentToEdit evenuale documento che Client sta editando
     * @param sectionToEdit eventuale sezione che Client sta editando
     * @return SUCCESS se comando e' sintaticamente corretto
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkSendMessage(String[] commandWords, String correctCommandToPrint, CommandType commandType,
                                             String documentToEdit, int sectionToEdit){
        //verifico se esiste msg da inviare
        if(commandWords.length == 2) { //non esiste msg
            System.err.println("[Turing] >> Comando scoretto. Forse intendevi:");
            System.out.println("[Turing] >> turing " + correctCommandToPrint);
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return readAndParseCommand(documentToEdit, sectionToEdit);
        }
        else{ //esiste messaggio

            //ricostruisco messaggio letto
            StringBuilder builder = new StringBuilder();
            for(int i = 2; i < commandWords.length; i++){
                builder.append(commandWords[i]);
                //tra una stringa e l'altra c'e' uno spazio (altrimenti non sarebbero state due celle distinete nell'array)
                builder.append(" ");
            }
            String msg = builder.toString();

            setCurrentCommand(commandType);
            setCurrentArg1(msg);
            return FunctionOutcome.SUCCESS;
        }
    }

    /**
     * Funzione che verifica che i commandi senza argomenti:
     * 1. turing register <username> <password>
     * 2. turing login <username> <password>
     * 3. turing create <doc> <numsezioni>
     * 4. turing share <doc> <username>
     * 5. turing show <doc> <sec>
     * 6. turing edit <doc> <sec>
     * 7. turing end-edit <doc> <sec>
     * non abbiano parole/argomenti a seguirli
     * @param commandWords parole lette da linea di commando
     * @param correctCommandToPrint messaggio personalizzato da stampare sullo schermo
     * @param commandType tipo di richiesta, per discrimare controllo degli argomenti da fare
     * @param documentToEdit eventuale documento che Client sta editando
     * @param sectionToEdit eventuale sezione che Client sta editando
     * @return SUCCESS se comando e' sintaticamente corretto
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkTwoARGSRequest(String[] commandWords, String correctCommandToPrint, CommandType commandType,
                                                String documentToEdit, int sectionToEdit){
        //verifico se esiste documento da mostrare / msg da inviare
        if(commandWords.length != 4) { //esiste msg
            System.err.println("[Turing] >> Comando scoretto. Forse intendevi:");
            System.out.println("[Turing] >> turing " + correctCommandToPrint);
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return readAndParseCommand(documentToEdit, sectionToEdit);
        }
        else{
            //verifico che i 2 argomenti non superino il numero di caratteri / sezioni del file di configurazione
            switch (commandType){
                case CREATE:
                case SHOW_SECTION:
                case EDIT:
                case END_EDIT:{
                    if(!checkIfNumSectionIsNumeric(commandWords[3])){ //num. sezione non e' un valore numerico
                        System.err.println("[Turing] >> Comando scoretto. Il secondo argomento deve essere un valore numerico positivo:");
                        System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
                        return readAndParseCommand(documentToEdit, sectionToEdit);
                    }
                    else{
                        //verifico che numero sezione sia strettamente positivo
                        FunctionOutcome check =  checkIFNumSectionIsStrictlyPositive(Integer.parseInt(commandWords[3]));
                        if(check == FunctionOutcome.FAILURE){
                            System.err.println("[Turing] >> Comando scoretto. Il secondo argomento deve essere strettamente positivo:");
                            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
                            return readAndParseCommand(documentToEdit, sectionToEdit);
                        }
                    }
                }
            }

            //verifica username-password-documento viene fatta dal Server a seconda sue configurazioni:
            //1. mininimo numero caratteri
            //2. massimo numero caratteri
            //3. presenza caratteri speciali
            //4. massimo numero sezioni per documento
            setCurrentCommand(commandType);
            setCurrentArg1(commandWords[2]);
            setCurrentArg2(commandWords[3]);
            return FunctionOutcome.SUCCESS; //secondo argomento e' un numero positivo
        }
    }
}
