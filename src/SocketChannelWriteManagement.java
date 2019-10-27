import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketChannelWriteManagement {
    /**
     * SocketChannel su cui bisogna scrivere
     */
    private SocketChannel socket;

    /**
     * Costruttore della classe SocketChannelWriteManagement
     * @param socket SocketChannel su cui bisogna scrivere
     */
    public SocketChannelWriteManagement(SocketChannel socket){
        this.socket = socket;
    }

    /**
     * Funzione che si occupa di scrivere sul SocketChannel
     * @param buff ByteBuffer che contiene il contenuto da scrivere
     * @param size numero di bytes da scrivere
     */
    public FunctionOutcome write(ByteBuffer buff, int size){

        while(size > 0){
            int bytesWrote = 0;
            try {
                //provo a scrivere sul SocketChannel
                bytesWrote = this.socket.write(buff);

                if(bytesWrote<0)
                    return FunctionOutcome.FAILURE; //SocketChannel si e' disconeesso / problemi I/O

                size -= bytesWrote; //decremento bytes da scrivere

            } catch (IOException e) {
                //e.printStackTrace();
                System.err.println(String.format("[ERR] >> Impossibile  scrivere sul SocketChannel: |%s| ", socket));
                return FunctionOutcome.FAILURE; //SocketChannel si e' disconeesso / problemi I/O
            }
        }
        return FunctionOutcome.SUCCESS; //scrittura avvenuta con successo
    }
}
