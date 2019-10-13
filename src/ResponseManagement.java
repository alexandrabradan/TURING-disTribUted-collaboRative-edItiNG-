import java.nio.ByteBuffer;

public class ResponseManagement {
    /**
     * risposta del Server, sottoforma di ENUM
     */
    private ServerResponse response;
    /**
     * dimensionde del body della risposta
     */
    private int dimension;
    /**
     * ByteBuffer che contiene l'eventuale corpo della risposta
     */
    private ByteBuffer body;

    /**
     * Costruttore della classe Message (PRIVATO PERCHE' RICHIAMATO DAL METODO "createResponse")
     * @param response tipo di risposta del Server
     * @param buff ByteBuffer che contiene eventuale contenuto della risposta
     */
    private ResponseManagement(ServerResponse response, ByteBuffer buff){
        this.response = response;

        if(buff == null){ //commando senza argomenti
            dimension = 0; //dimensione body nulla
            body = null; //body nullo
        }
        else{
            //buff setttato in MODALITA' LETTURA => limite quantita' dati che si possono leggere (che buffer contiene)
            dimension = buff.limit(); //dimensione body pari al contenuto della risposta
            body = buff;  //body = contenuto risposta
        }
    }
}
