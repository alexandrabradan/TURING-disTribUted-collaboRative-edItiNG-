import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketChannelReadManagement {
    /**
     * SocketChannel su cui bisogna scrivere
     */
    private SocketChannel socket;

    /**
     * Costruttore della classe SocketChannelReadManagement
     * @param socket SocketChannel su cui bisogna leggere
     */
    public SocketChannelReadManagement(SocketChannel socket){
        this.socket = socket;
    }

    /**
     * Funzione che si occupa di leggere il contenuto dal SocketChannel
     * @param buff ByteBuffer usato per memorizzare il contenuto letto
     * @param size numero di bytes da leggere dal SocketChannel number of byte to read from the socket
     */
    public FunctionOutcome read(ByteBuffer buff, int size){

        while(size > 0){
            int bytesRead = 0;
            try {
                //provo a leggere dal SocketChannel
                bytesRead = this.socket.read(buff);

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
}
