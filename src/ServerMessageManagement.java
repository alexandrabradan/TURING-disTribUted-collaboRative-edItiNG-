import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;


public class ServerMessageManagement {
    /**
     * SocketChannel del Client di cui leggere richiesta, soddisfare domanda ed inviare esito operazione
     */
    private SocketChannel clientSocket;
    /**
     * richiesta del Client, sottoforma di ENUM, che bisogna leggere
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
     * Classe per leggere il contenuto della richiesta del Client dal suo SocketChannel
     */
    private SocketChannelReadManagement socketChannelReadManagement;
    /**
     * Classe per scrivere la risposta al Client sul suo SocketChannel
     */
    private SocketChannelWriteManagement socketChannelWriteManagement;

    /**
     * Costruttore della classe ResponseManagement
     * @param clientSocket SocketChannel del Client di cui bisogna leggere richiesta ed inviare risposta di esito
     */
    public ServerMessageManagement(SocketChannel clientSocket){
        this.clientSocket = clientSocket;
        this.socketChannelReadManagement = new SocketChannelReadManagement(this.clientSocket);
        this.socketChannelWriteManagement = new SocketChannelWriteManagement(this.clientSocket);

        setDefaultVariablesValues(); //resetto variabili della classe
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
     * Funzione che restituisci il contenuto del BODY sottoforma di stringa
     * @return contenuto del ByteBuffer BODY come stringa
     */
    public String getBodyMessage(){
        return new String(this.body.array(), StandardCharsets.UTF_8);
    }

    /**
     * Funzione che restituisce il tipo di comando / richiesta letto dal SocketChannel del Client
     * @return this.currentCommand
     */
    public CommandType getCurrentCommand(){return this.currentCommand;}

    /**
     * Funzione che restituisce l'eventuale primo argomento della richiesta del Client
     * @return this.currentArg1
     */
    public String getCurrentArg1(){return this.currentArg1;}

    /**
     * Funzione che restituisce l'eventuale secondo argomento della richiesta del Client
     * @return this.currentArg2
     */
    public String getCurrentArg2(){return this.currentArg2;}

    /**
     * Funzione che si occupa di leggere:
     * 1. l'HEADER della richiesta del Client, contenente:
     * a) CommandType codificato come ENUM (intero)
     * b) dimensione eventuale BODY della richiesta (ossia gli argomenti della richiesta separati da un whitespace)
     * 2. il BODY delle risposta del Server
     * @return SUCCESS se la lettura della dimensione delle richiesta e la richiesta sono andati a buon fine
     *         FAILURE altrimenti
     */
    public FunctionOutcome readRequest(){

        //resetto il commando corrente e gli eventuali argomenti per evitare malintesi con
        //invocazioni precedenti
        setDefaultVariablesValues();

        //ricevo HEADER contenente:
        //tipo di richiesta => e' ENUM => intero => codificato con 4 bytes
        // dim. body risposta => intero => codificato con 4 bytes
        this.header = ByteBuffer.allocate(8);

        this.header.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        //leggo dimensione risposta
        FunctionOutcome check = this.socketChannelReadManagement.read(this.header, 8);

        if(check == FunctionOutcome.SUCCESS){ //lettura

            this.header.flip(); //modalita' lettura (position=0, limit = bytesWritten)

            this.currentCommand = CommandType.values()[this.header.getInt()]; //converto valore numerico nel rispettivo ENUM
            int requestBodyLength = this.header.getInt(); //reperisco dimensione BODY

            //devo resettare il contenuto del body (readRequest utilizzata dal Server per leggere contenuto di una
            // sezione editata, eventualemente nulla => requestBodyLength = 0). Se non resetto il buffer scrivo
            //gli argomenti prima sulla sezione, lato server (ossia il nome del documento e il numero della sezione)
            if(requestBodyLength == 0){
                this.body = ByteBuffer.allocate(1);
                String whitespace = " ";
                byte[] whitespaceBytes = whitespace.getBytes();
                this.body.put(whitespaceBytes);
            }

            //leggo eventuale BODY della richiesta
            if(requestBodyLength > 0){

                //alloco nuovo buffer per leggervi BODY, di dimensione letta sopra
                this.body = ByteBuffer.allocate(requestBodyLength);

                this.body.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

                //leggo BODY della richiesta
                FunctionOutcome readBody = this.socketChannelReadManagement.read(this.body, body.limit());

                if(readBody == FunctionOutcome.FAILURE)
                    return FunctionOutcome.FAILURE; //lettura BODY fallita

                //lettura BODY avvenuta con successo => estrappolo argomenti
                this.body.flip(); //modalita' lettura (position=0, limit = bytesWritten)
                String bodyContent = new String(this.body.array(), StandardCharsets.UTF_8);

                //verifico se ho da recuperare uno o due argomenti
                switch(this.currentCommand){
                    case LOGIN:
                    case CREATE:
                    case SHARE:
                    case SHOW_SECTION:
                    case EDIT:
                    case END_EDIT:
                    case SEND:
                    case RECEIVE:{
                        //divido contentuno letto in prossimita' dello spazio vuoto (demarcatore tra argomenti)
                        String[] args = bodyContent.split("\\s+");

                        this.currentArg1 = args[0];
                        if(args.length > 1)
                            this.currentArg2 = args[1];
                        break;
                    }
                    case I_AM_INVITE_SOCKET:
                    case SHOW_DOCUMENT:{
                        //contentuo BODY e' esso stesso l'unico argomento
                        this.currentArg1 = bodyContent;
                        break;
                    }
                }
            return FunctionOutcome.SUCCESS; //lettura BODY avvenuta con successo
            }
            else return FunctionOutcome.SUCCESS;  //lettura HEADER avvenuta con sucesso
        }
        else return FunctionOutcome.FAILURE; //lettura HEADER fallita
    }

    /**
     * Funzione che si occupa di inviare la risposta di esito della richiesta:
     * 1. l'HEADER della risposta contenente:
     * a) ServerResponse codificato come ENUM (intero) della risposta
     * b) la dim. dell'eventuale BODY di risposya
     * 2. il BODY della risposta contenente:
     * @param serverResponse tipo di risposta d'esito del Server
     * @param body eventuale
     * @return SUCCESS se l'invio della dimensione delle risposta e la risposta sono andati a buon fine
     *         FAILURE altrimenti
     */
    public FunctionOutcome writeResponse(ServerResponse serverResponse, String body){

        //invio HEADER contenente:
        //tipo di risposta => e' ENUM => intero => codificato con 4 bytes
        // dim. body risposta => intero => codificato con 4 bytes
        this.header = ByteBuffer.allocate(8);

        this.header.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

        byte[] bodyBytes = body.getBytes(); //converto BODY in bytes per scoprire sua lunghezza
        int responseBodyLength = bodyBytes.length; //ricavo lunghezza del BODY

        this.header.putInt(serverResponse.ordinal()); //ordinale() => reperisco valore numerico enum
        this.header.putInt(responseBodyLength); //inserisco dim. BODY

        this.header.flip(); //modalita' lettura (position=0, limit = bytesWritten)

        //invio HEADER al Server
        FunctionOutcome check = this.socketChannelWriteManagement.write(this.header, 8);

        if(check == FunctionOutcome.SUCCESS){  //invio HEADER avvenuto con successo

            if(responseBodyLength > 0){
                this.body = ByteBuffer.allocate(responseBodyLength);

                this.body.clear();  //modalita' scrittura + sovrascrittura buffer (position=0, limit=capacity)

                this.body.put(bodyBytes);  //inserisco messaggio di risposta nel buffer

                this.body.flip(); //modalita' lettura (position=0, limit = bytesWritten)

                //invio BODY al Client
                check = this.socketChannelWriteManagement.write(this.body, this.body.limit()); //invio BODY

                if(check == FunctionOutcome.FAILURE)
                    return FunctionOutcome.FAILURE; //invio BODY fallito
            }

            try {
                System.out.println(String.format("[%s] >> Invio risposta |%s| al socket |%s| avvenuta con successo",
                        Thread.currentThread().getName(), serverResponse, this.clientSocket.getRemoteAddress().toString()));

            } catch (IOException e) {
                //e.printStackTrace();
                System.err.println("[ERR] >> Impossibile reperire l'indirizzo e la porta del clientSocket");
                System.exit(-1);
            }

            return FunctionOutcome.SUCCESS; //invio HEADER (ed eventuale BODY) avvenuto con successo
        }
        else
            return FunctionOutcome.FAILURE; //invio HEADER fallito
    }
}
