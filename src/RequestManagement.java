import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class RequestManagement {
    /**
     * SocketChannel del Client con il quale inviare richiesta al Server
     */
    private SocketChannel clientSocket;
    /**
     * richiesta del Client, sottoforma di ENUM
     */
    private CommandType currentCommand;
    /**
     * eventuale primo argomento della richiesta
     */
    private String currentArg1;
    /**
     * eventuale secondo argomento della richiesta
     */
    private String currentArg2;
    /**
     * ByteBuffer che contiene l'intestazione della richiesta
     */
    private ByteBuffer header;
    /**
     * ByteBuffer che contiene l'eventuale corpo della richiesta
     */
    private ByteBuffer body;

    /**
     * Costruttore della classe RequestManagement
     */
    public RequestManagement(SocketChannel clientSocket){
        this.clientSocket = clientSocket;
    }

    /**
     * Funzione che si occupa di resettare ad ogni invocazione del metodo "writeRequest" le variabili di
     * condizione della classe
     */
    private void setDefaultVariablesValues(){
        this.currentCommand = CommandType.HELP;
        this.currentArg1 = "";
        this.currentArg2 = "";
    }

    /**
     * Funzione che si occupa di leggere il contenuto da un SocketChannel
     * @param buff ByteBuffer usato per memorizzare il contenuto letto
     * @param size numero di bytes da leggere dal SocketChannel number of byte to read from the socket
     */
    private FunctionOutcome read(ByteBuffer buff,int size){

        while(size > 0){
            int bytesRead = 0;
            try {
                //provo a leggere dal SocketChannel
                bytesRead = this.clientSocket.read(buff);

                if(bytesRead < 0)
                    return FunctionOutcome.FAILURE; //SocketChannel si e' disconeesso / problemi I/O

                size -= bytesRead; //decremento bytes da leggere

            } catch (IOException e) {
                e.printStackTrace();
                return FunctionOutcome.FAILURE; //SocketChannel si e' disconeesso / problemi I/O
            }
        }
        return FunctionOutcome.SUCCESS; //lettura avvenuta con successo
    }

    /**
     * Funzione che si occupa di scrivere su SocketChannel
     * @param buff ByteBuffer che contiene il contenuto da scrivere
     * @param size numero di bytes da scrivere
     */
    private FunctionOutcome write(ByteBuffer buff, int size){
        while(size > 0){
            int bytesWrote = 0;
            try {
                //provo a scrivere sul SocketChannel
                bytesWrote = this.clientSocket.write(buff);

                if(bytesWrote<0)
                    return FunctionOutcome.FAILURE; //SocketChannel si e' disconeesso / problemi I/O

                size -= bytesWrote; //decremento bytes da scrivere

            } catch (IOException e) {
                e.printStackTrace();
                return FunctionOutcome.FAILURE; //SocketChannel si e' disconeesso / problemi I/O
            }
        }
        return FunctionOutcome.SUCCESS; //scrittura avvenuta con successo
    }

    /**
     * Funzione che si occupa di inviare:
     * 1. l'HEADER della richiesta contenente:
     * a) CommandType codificato come ENUM (intero) della richiesta
     * b) il BODY della richiesta contenente:
     * a) gli eventuali argomenti della richiesta, separati da uno whitespace per distinguerli
     * @param command tipo di richiesta
     * @param arg1 eventuale primo argomento
     * @param arg2 eventulae secondo argomento
     * @return SUCCESS se l'invio della dimensione delle richiesta e la richiesta sono andati a buon fine
     *         FAILURE altrimenti
     */
    public FunctionOutcome writeRequest(CommandType command, String arg1, String arg2){

        //resetto il commando corrente e gli eventuali argomenti per evitare malintesi con
        //invocazioni precedenti
        setDefaultVariablesValues();

        //inizializzo commando corrente ed eventuali argomenti
        this.currentCommand = command;
        this.currentArg1 = arg1;
        this.currentArg2 = arg2;

        //invio HEADER contenente:
        //tipo di richiesta => e' ENUM => intero => codificato con 4 bytes
        // dim. body/argomenti richiesta => intero => codificato con 4 bytes
        this.header = ByteBuffer.allocate(8);

        this.header.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        String request =  ""; //richiesta vuota

        //costruisco body della richiesta (argomenti da inviare)
        //controllo se ho da inviare due argomenti
        if(!arg1.isEmpty() && !arg2.isEmpty()){ //ho 2 argomenti da inviare
            request =  arg1 + " " + arg2;   //inserisco degli spazi per distinguere argomenti della richiesta
        }
        else if(!arg1.isEmpty()){ //ho 1 argomento da inviare
            request = arg1;
        }

        int requestLength = request.length(); //ricavo lunghezza del body

        this.header.putInt(command.ordinal()); //ordinale() => reperisco valore numerico enum
        this.header.putInt(requestLength); //inserisco dim. body

        this.header.flip(); //modalita' lettura (position=0, limit = bytesWritten)

        //invio HEADER al Server
        FunctionOutcome check = write(this.header, 8);

        if(check == FunctionOutcome.SUCCESS){  //invio HEADER avvenuto con successo

            if(requestLength > 0){
                this.body = ByteBuffer.allocate(requestLength);

                this.body.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

                this.body.put(request.getBytes());  //inserisco messaggio di richiesta nel buffer

                this.body.flip(); //modalita' lettura (position=0, limit = bytesWritten)

                //invio BODY al Server
                return write(this.body, this.body.limit()); //invio BODY
            }
            return FunctionOutcome.SUCCESS; //invio HEADER avvenuto con successo

        }
        else
            return FunctionOutcome.FAILURE; //invio HEADER fallito
    }

    /**
     * Funzione che si occupa di leggere:
     * 1. l'HEADER della risposta del Server, contenente: dimensionde della risposta del Server (per allocare un buffer di lettura opportuno)
     * a) ServerResponse codificato come ENUM (intero)
     * b) dimensione eventuale BODY della risposta
     * 2. il BODY delle risposta del Server
     * @return SUCCESS se la lettura della dimensione delle risposta e la risposta sono andati a buon fine
     *         FAILURE altrimenti
     */
    public FunctionOutcome readResponse(){
        //ricevo HEADER contenente:
        //tipo di risposta => e' ENUM => intero => codificato con 4 bytes
        // dim. body risposta => intero => codificato con 4 bytes
       this.header = ByteBuffer.allocate(8);

        this.header.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        ServerResponse responseType; //tipo risposta
        int responseBodyLength = 0;  //lunghezza BODY

        //leggo dimensione risposta
        FunctionOutcome check = read(this.header, 8);

        this.header.flip(); //modalita' lettura (position=0, limit = bytesWritten)

        if(check == FunctionOutcome.SUCCESS){ //lettura
            responseType = ServerResponse.values()[this.header.getInt()]; //converto valore numerico nel rispettivo ENUM
            responseBodyLength = this.header.getInt(); //reperisco dimensione BODY

            //leggo eventuale BODY della risposta
            if(responseBodyLength > 0){

                //alloco nuovo buffer per leggervi BODY, di dimensione letta sopra
                this.body = ByteBuffer.allocate(responseBodyLength);

                this.body.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

                //leggo BODY della risposta
                check = read(this.body, body.limit());

                if(check == FunctionOutcome.FAILURE)
                    return FunctionOutcome.FAILURE; //lettura BODY fallita
            }

            //lettura HEADER avvenuta con sucesso => lettura eventuale del BODY e stampa personalizzata
            return manageResponse(responseType, responseBodyLength);
        }
        else return FunctionOutcome.FAILURE; //lettura HEADER fallita
    }

    /**
     * Funzione che stampa la risposta personalizzata del Server alla richiesta fatta dal Client
     * @param responeType risposta del Server (sottofroma di enum "ServerResponse")
     * @param responseBodyLenght eventuale contenuto della risposta del Server
     * @return SUCCESS se la lettura della dimensione delle risposta e la risposta sono andati a buon fine
     *         FAILURE altrimenti
     */
    private FunctionOutcome manageResponse(ServerResponse responeType, int responseBodyLenght){

        String responseBody = "";

        //reperisco il contenuto del BODY della risposta, se presente
        if(responseBodyLenght > 0){
            this.body.flip();  //modalita' lettura (position=0, limit = bytesWritten)
            responseBody = new String(this.body.array(), StandardCharsets.UTF_8);
        }

        //a seconda del comando richiesto posso avere differenti risposte di successo/fallimento da parte del Server
        // verifico che cosa mi ha risposto il Server  ed in base a cio', stampo un messaggio di esito
        switch (responeType){
            case OP_OK:{  //discrimino quale operazione ha avuto successo
                switch (this.currentCommand){
                    case LOGIN:{
                        System.out.println("[Turing] >> Login avvenuto con successo");
                        break;
                    }
                    case LOGOUT:{
                        System.out.println(String.format("[Turing] >> Logout dell'utente |%s| avvenuto con " +
                                "successo", currentArg1));
                        break;
                    }
                    case CREATE:{
                        System.out.println(String.format("[Turing] >> Creazione del documento |%s| con |%s| sezioni" +
                                " avvenuta con successo", currentArg1, currentArg2));
                        break;
                    }
                    case SHARE:{
                        System.out.println(String.format("[Turing] >> Condivisione del documento |%s| con l'utente " +
                                "|%s| avvenuto con successo", currentArg1, currentArg2));
                        break;
                    }
                    case SHOW_DOCUMENT:{
                        System.out.println(String.format("[Turing] >> Contenuto del documento |%s|:", currentArg1));
                        //@TODO APRIRE TUTTE LE SEZIONI DEL DOCUMENTO RICHIESTO
                        break;
                    }
                    case SHOW_SECTION:{
                        System.out.println(String.format("[Turing] >> Contenuto della sezione |%s| del documento" +
                                " |%s|:", currentArg2, currentArg1));
                        //@TODO APRIRE LA SEZIONE RICHIESTA
                        break;
                    }
                    case LIST:{
                        System.out.println("[Turing] >> Recap tuoi documenti");
                        System.out.println(responseBody);  //@TODO SPLIT PER STAMPARE CONTENUTO
                        break;
                    }
                    case EDIT:{
                        System.out.println(String.format("[Turing] >> Inizio modifica della sezione |%s| del" +
                                " documento |%s|", currentArg2, currentArg1));
                        //@TODO APRIRE EDITOR PER EDITARE LA SEZIONE
                        break;
                    }
                    case END_EDIT:{
                        System.out.println(String.format("[Turing] >> Fine modifica della sezione |%s| del" +
                                " documento |%s|", currentArg2, currentArg1));
                        //@TODO SALVATAGGIO SEZIONE E CHIUSUA EDITOR (se non gia' fatta)
                        break;
                    }
                    case SEND:{
                        System.out.println("[Turing] >> Invio messaggio sulla Chat avvenuto con successo");
                        break;
                    }
                    case RECEIVE:{
                        System.out.println("[Turing] >> Hai un nuovo messaggio sulla Chat");
                        System.out.println(responseBody);
                        //@TODO DECIDERE LIMITE HISTORY
                        break;
                    }
                }
            }
            case  OP_INVALID_REQUEST:{
                //non si verifica mai, ma inserito qui per scrupolo qual'ora check del parsing locale del Client fallise
                System.err.println("[ERR] >> Richiesta non rientra nei servizi offerti");
                break;
            }
            case OP_USER_NOT_ONLINE:{
                System.err.println(String.format("[ERR] >> Utente |%s| NON connesso.", currentArg1));
                break;
            }
            case OP_USER_NOT_REGISTERED:{
                System.err.println(String.format("[ERR] >> Utente |%s| NON registrato.", currentArg1));
                break;
            }
            case OP_DOCUMENT_NOT_EXIST:{
                System.err.println(String.format("[ERR] >> Documento |%s| NON esistente.", currentArg1));
                break;
            }
            case OP_SECTION_NOT_EXIST:{
                System.err.println(String.format("[ERR] >> Sezione |%s| del documento |%s| NON " +
                        "registrato.", currentArg2, currentArg1));
                break;
            }
            case OP_DOCUMENT_TOO_SHORT:{
                System.err.println(String.format("[ERR] >> Nome documento |%s| troppo corto.", currentArg1));
                break;
            }
            case OP_DOCUMENT_TOO_LONG:{
                System.err.println(String.format("[ERR] >> Nome documento |%s| troppo lungo.", currentArg1));
                break;
            }
            case OP_SECTION_EXCEED_LIMIT:{
                System.err.println(String.format("[ERR] >> Numero sezioni |%s| eccede il valore consentito.", currentArg1));
                break;
            }
            case OP_USER_ALREADY_ONLINE:{
                System.err.println(String.format("[ERR] >> Username |%s| GIA' connesso.", currentArg1));
                break;
            }
            case OP_PASSWORD_INCORRECT:{
                System.err.println(String.format("[ERR] >> Password |%s| ERRATO.", currentArg2));
                break;
            }
            case OP_DOCUMENT_ALREADY_EXIST:{
                System.err.println(String.format("[ERR] >> Documento |%s| GIA' esistente.", currentArg1));
                break;
            }
            case OP_USER_NOT_CREATOR:{
                System.err.println(String.format("[ERR] >> Non puoi invitare l'utente |%s| a collaborare al" +
                        " documento |%s|. Per farlo devi essere il suo creatore.", currentArg2, currentArg1));
                break;
            }
            case OP_USER_IS_DEST:{
                System.err.println(String.format("[ERR] >> Non puoi invitare te stesso a collaborare al documento |%s|.",
                                                                                                        currentArg1));
                break;
            }
            case OP_DEST_ALREADY_CONTRIBUTOR:{
                System.err.println(String.format("[ERR] >> Non puoi invitare l'utente |%s| a collaborare al" +
                        " documento |%s|, perche' e' gia' collaboratore / creatore.", currentArg2, currentArg1));
                break;
            }
            case OP_DEST_NOT_REGISTERED:{
                System.err.println(String.format("[ERR] >> Non puoi invitare l'utente |%s| a collaborare al" +
                        " documento |%s|. L'utente NON e' registrato alla nostra piattaforma.", currentArg2, currentArg1));
                break;
            }
            case OP_USER_NOT_ALLOWED_TO_EDIT:{
                System.err.println(String.format("[ERR] >> Non puoi editare la sezione|%s| del documento|%s|."
                        + "Per farlo devi essere suo creatore / collaboratore.", currentArg2, currentArg1));
                break;
            }
            case OP_SECTION_ALREADY_IN_EDITING_MODE:{
                System.err.println(String.format("[ERR] >> Non puoi editare la sezione|%s| del documento|%s|."
                        + "Sezione e' gia' editata da qualcuno.", currentArg2, currentArg1));
                break;
            }
            case OP_SECTION_NOT_IN_EDITING_MODE:{
                System.err.println(String.format("[ERR] >> Non puoi finire di fare l'editing della sezione|%s| del documento|%s|."
                        + "Sezione non settata per l'editing precedentemente.", currentArg2, currentArg1));
                break;
            }
        }

        return FunctionOutcome.SUCCESS; //notifico al ciclo principale che la lettura della risposta e' andata a buon fine
    }
}
