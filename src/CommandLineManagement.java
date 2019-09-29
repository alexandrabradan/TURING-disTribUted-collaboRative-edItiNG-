import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandLineManagement {
    private ConfigurationsManagement configurationsManagement; //per reperire var. di configurazione

    private CommandType currentCommand; //commando corrente letto da  tastiera
    private String currentArg1;  //eventuale argomento 1 del comando
    private String currentArg2;  //eventuale argomento 2 del comando


    /**
     * Costruttore della classe CommandLineManagement, che si occupa di inizializzare le variabili della classe
     */
    public CommandLineManagement(){
        this.configurationsManagement = new ConfigurationsManagement();

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
     * Funzione che inizializza il primo argoento del  comando corrente
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
        this.currentArg1 = arg2;
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
    }

    /**
     * Funzione che si occupa di leggere un commando da tastiera e di verificarne la correttezza sintattica
     * @return SUCCESS se il commando e' sintatticamente corretto
     *         FAILURE altrimenti
     */
    public FunctionOutcome readAndParseCommand(){

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
                return readAndParseCommand();
            }
            else{  //e' stato digitato qualcosa, verifico se e' lecito

                //divido commando letto in prossimita' di uno o piu' spazi vuoti
                String[] commandWords = command.split("\\s+");

                int commandWordsLength = commandWords.length;  //numero di parole lette

                //verifico se comando inizia con la parola "turing"
                if(!commandWords[0].equals("turing")){  //comando NON inizia con la parola Turing
                    System.err.println("[Turing] >> Comando non valido.");
                    System.out.println("[Turing] >> Se hai bisogno di aiuto digita:");
                    System.out.println("[Turing] >> turing --help");
                    System.out.println("[Turing] >> Altrimenti digita nuovamente il comando:");
                    return readAndParseCommand();
                }
                else{  // comando inizia con la parola "turing"

                    //verifico se esiste seconda parola (richiesta)
                    if(commandWordsLength  < 2){  //seconda parola NON esiste => ho inserito solo parola "turing"
                        System.err.println("[Turing] >> Comando incompleto.");
                        System.out.println("[Turing] >> Se hai bisogno di aiuto digita:");
                        System.out.println("[Turing] >> turing --help");
                        System.out.println("[Turing] >>  Altrimenti digita nuovamente il comando:");
                        return readAndParseCommand();
                    }
                    else{  //esiste seconda parola (richiesta)

                        //verifico formato richiesta
                        switch (commandWords[1]){
                            
                            case("--help"):{
                                //verifico se dopo il --help ci sono ancora parole
                                String correctCommandToPrint = "--help";
                                FunctionOutcome check =  checkEmptyARGRequest(commandWords,correctCommandToPrint, CommandType.HELP);
                                if(check == FunctionOutcome.SUCCESS) //comando sintatticamente corretto
                                    printHelp();  //stampo msg di aiuto
                                return check;  // ritorno successo/insuccesso
                                }
                            case("register"):{
                                //verifico se c'e' l'username e la password
                                String correctCommandToPrint = "register <username> <password>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint, CommandType.LOGIN);
                            }
                            case("login"):{
                                //verifico se c'e' l'username e la password
                                String correctCommandToPrint = "login";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint, CommandType.LOGIN);
                            }
                            case("logout"):{
                                //verifico se c'e' l'username e la password
                                String correctCommandToPrint = "logout";
                                return checkEmptyARGRequest(commandWords, correctCommandToPrint, CommandType.LOGOUT);
                            }
                            case("create"):{
                                //verifico se c'e' nome documento e numero sezioni
                                String correctCommandToPrint = "register <doc> <numsezioni>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint, CommandType.CREATE);
                            }
                            case("share"):{
                                //verifico se c'e' il documento e l'username per la condivisione
                                String correctCommandToPrint = "share <username> <password>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint, CommandType.SHARE);
                            }
                            case("show"):{
                                //verifico se esiste solo terza parola (=> mostra intero documento)
                                //oppure esiste anche quarta parola (=> mostra una sezione del documento)
                                
                                if(commandWordsLength == 3){ //esiste solo documento
                                    String correctCommandToPrint = "show <doc>";
                                    return checkOneARGRequest(commandWords, correctCommandToPrint, CommandType.SHOW_DOCUMENT);
                                }
                                else if(commandWordsLength >= 4) {  //esiste anche sezione del documento
                                    String correctCommandToPrint = "show <doc> <sec>";
                                    return checkTwoARGSRequest(commandWords, correctCommandToPrint, CommandType.SHOW_SECTION);
                                }
                            }
                            case("list"):{
                                //verifico se dopo il list ci sono ancora parole
                                String correctCommandToPrint = "list";
                                return checkEmptyARGRequest(commandWords, correctCommandToPrint, CommandType.LIST);
                            }
                            case("edit"):{
                                //verifico se c'e' nome documento e numero sezione da mofidicare
                                String correctCommandToPrint = "edit <doc> <sec>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint, CommandType.EDIT);
                            }
                            case("end-edit"):{
                                //verifico se c'e' nome documento e numero sezione modificata
                                String correctCommandToPrint = "end-edit <doc> <sec>";
                                return checkTwoARGSRequest(commandWords, correctCommandToPrint, CommandType.END_EDIT);
                            }
                            case("send"):{
                                //verifico se c'e messaggio da inviare
                                String correctCommandToPrint = "send <msg>";
                                return checkOneARGRequest(commandWords, correctCommandToPrint, CommandType.SEND);
                            }
                            case "receive":{
                                //verifico se dopo il receive ci sono ancora parole
                                String correctCommandToPrint = "receive";
                                return checkEmptyARGRequest(commandWords, correctCommandToPrint, CommandType.RECEIVE);
                            }
                            default:
                                System.err.println("[Turing] >> Comando scoretto. Forse intendevi:");
                                System.out.println("[Turing] >> Se hai bisogno di aiuto digita:");
                                System.out.println("[Turing] >> turing --help");
                                System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
                                return readAndParseCommand();
                        }
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[ERR] >> Errore nella lettura del comando da tastiera");
            System.exit(-1);
        }
        return FunctionOutcome.FAILURE;
    }

    /**
     * Funzione che verifica se l'username/password/documento passato come argomento rientra nel range stabilito dal file
     * di configurazione o meno
     * @param argument username/password/documento da verificare
     * @return SUCCESS se username/password/documento e' lecito
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkMaxNumCharactersArg(String argument){
        if(argument.length() > this.configurationsManagement.maxNumCharactersArg)
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
        if(numSections > this.configurationsManagement.maxNumSectionsPerDocument)
            return FunctionOutcome.FAILURE;  //numero sezioni supera valore consentito
        else
            return FunctionOutcome.SUCCESS;  //numero sezioni non supera valore consentito
    }

    /**
     * Funzione che verifica se l' username/documento  e la password/username non supera i caratteri consentiti
     * dal file di configurazione
     * @param arg1 username / nome del documento da verificare
     * @param arg2 password / username da verificare
     * @param commandType tipo di comando che richiede la verifica
     * @return SUCCESS se username/documento  e la password/username sono lecitti
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkStringARGAndStringArg(String arg1, String arg2, CommandType commandType){
        //verifico se numero dei caratteri del primo argomento e del non superino il numero consentito o meno
        FunctionOutcome check1 = checkMaxNumCharactersArg(arg1);
        FunctionOutcome check2 = checkMaxNumCharactersArg(arg2);
        if(check1 == FunctionOutcome.FAILURE || check2 == FunctionOutcome.FAILURE){
            if(check1 == FunctionOutcome.FAILURE){
                System.err.println("[Turing] >> %s supera i caratteri consentiti. Il valore deve esse <= %d" +
                        arg1 + configurationsManagement.maxNumCharactersArg);
            }

            if(check2 == FunctionOutcome.FAILURE){
                System.err.println("[Turing] >> %s supera i caratteri consentiti. Il valore deve esse <= %d" +
                       arg2 +  configurationsManagement.maxNumCharactersArg);
            }
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return readAndParseCommand();
        }
        else{
            setCurrentCommand(commandType);
            setCurrentArg1(arg1);
            setCurrentArg2(arg2);
            return FunctionOutcome.SUCCESS;
        }
    }

    /**
     * Funzione che verifica se il nome di un documento non supera i caratteri consentiti dal file di configurazione
     * e che il numero di sezione del documento sia nel range stabilito sempre dal file di configurazione
     * @param arg1 nome del documento da verificare
     * @param arg2 numero sezione del documento da verificare
     * @param commandType tipo di comando che richiede la verifica
     * @return SUCCESS se il nome del documento ed il suo numerod di sezione sono lecitti
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkStringARGAndIntArg(String arg1, String arg2, CommandType commandType){
        //verifico se numero dei caratteri del primo argomento e del non superino il numero consentito o meno
        FunctionOutcome check1 = checkMaxNumCharactersArg(arg1);
        FunctionOutcome check2 = checkMaxNumSectionsPerDocument(Integer.parseInt(arg2));
        if(check1 == FunctionOutcome.FAILURE || check2 == FunctionOutcome.FAILURE){
            if(check1 == FunctionOutcome.FAILURE){
                System.err.println("[Turing] >> %s supera i caratteri consentiti. Il valore deve esse <= %d" +
                        arg1 + configurationsManagement.maxNumCharactersArg);
            }

            if(check2 == FunctionOutcome.FAILURE){
                System.err.println("[Turing] >> %d supera i caratteri consentiti. Il valore deve esse <= %d" +
                        Integer.parseInt(arg2) +  configurationsManagement.maxNumCharactersArg);
            }
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return readAndParseCommand();
        }
        else{
            setCurrentCommand(commandType);
            setCurrentArg1(arg1);
            setCurrentArg2(arg2);
            return FunctionOutcome.SUCCESS;
        }
    }

    /**
     * Funzione che verifica che i commandi senza argomenti:
     * 1. turing --help
     * 2. turing logout
     * 3. turing list
     * 4. turing receive
     * non abbiano parole/argomenti a seguirli
     * @param commandWords parole lette da linea di commando
     * @param correctCommandToPrint messaggio personalizzato da stampare sullo schermo
     * @return SUCCESS se comando e' sintaticamente corretto
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkEmptyARGRequest(String[] commandWords, String correctCommandToPrint, CommandType commandType){
        if(commandWords.length != 2){ //dopo richiesta ci sono parole/argomenti
            System.err.println("[Turing] >> Comando scoretto. Forse intendevi:");
            System.out.println("[Turing] >> turing " + correctCommandToPrint);
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return readAndParseCommand();
        }
        else{
            setCurrentCommand(commandType);
            return FunctionOutcome.SUCCESS;
        }
    }

    /**
     * Funzione che verifica che i commandi senza argomenti:
     * 1. show <doc>
     * 2. send <msg>
     * non abbiano parole/argomenti a seguirli
     * @param commandWords parole lette da linea di commando
     * @param correctCommandToPrint messaggio personalizzato da stampare sullo schermo
     * @return SUCCESS se comando e' sintaticamente corretto
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkOneARGRequest(String[] commandWords, String correctCommandToPrint, CommandType commandType){
        //verifico se esiste documento da mostrare / msg da inviare
        if(commandWords.length != 3) { //esiste msg
            System.err.println("[Turing] >> Comando scoretto. Forse intendevi:");
            System.out.println("[Turing] >> turing " + correctCommandToPrint);
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return readAndParseCommand();
        }
        else{
            //verifico che nome documento / msg non superino il num. caratteri del file di configurazione
            FunctionOutcome check = checkMaxNumCharactersArg(commandWords[2]);
            if(check == FunctionOutcome.FAILURE){
                System.err.println("[Turing] >> %d supera i caratteri consentiti. Il valore deve esse <= %d" +
                        commandWords[2] +  configurationsManagement.maxNumCharactersArg);
                System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
                return readAndParseCommand();
            }
            else{
                setCurrentCommand(commandType);
                setCurrentArg1( commandWords[2]);
                return FunctionOutcome.SUCCESS;
            }
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
     * @return SUCCESS se comando e' sintaticamente corretto
     *         FAILURE altrimenti
     */
    private FunctionOutcome checkTwoARGSRequest(String[] commandWords, String correctCommandToPrint, CommandType commandType){
        //verifico se esiste documento da mostrare / msg da inviare
        if(commandWords.length != 3) { //esiste msg
            System.err.println("[Turing] >> Comando scoretto. Forse intendevi:");
            System.out.println("[Turing] >> turing " + correctCommandToPrint);
            System.out.println("[Turing] >> Digita nuovamente il comando, per favore:");
            return readAndParseCommand();
        }
        else{
            //verifico che i 2 argomenti non superino il numero di caratteri / sezioni del file di configurazione
            switch (commandType){
                case REGISTER:
                case LOGIN:
                case SHARE: {
                    return checkStringARGAndStringArg(commandWords[2], commandWords[3], commandType);
                }
                case CREATE:
                case SHOW_SECTION:
                case EDIT: 
                case END_EDIT:{
                    return checkStringARGAndIntArg(commandWords[2], commandWords[3], commandType);
                }
                default:{
                    return FunctionOutcome.FAILURE;
                }
            }
        }
    }
}
