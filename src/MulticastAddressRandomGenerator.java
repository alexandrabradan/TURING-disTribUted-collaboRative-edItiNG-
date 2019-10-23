import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;

public class MulticastAddressRandomGenerator {
    /**
     * Base di partenze del primo ottetto
     */
    private final int baseFirtsOctet = 224;
    /**
     * Massimo incremento della base di partenze del primo ottetto
     */
    private final int boundFirstOctet = 239 - 224;
    /**
     * Base di partenze del secondo/terzo/quarto ottetto
     */
    private final int baseOtherOctets = 0;
    /**
     * Massimo incremento della base di partenze del secondo/terzo/quarto ottetto
     */
    private final int boundOtherOctets = 255;
    /**
     * Classe che contiene le strutture dati del Server (mi serve solo per accedere al multicast_set e verificare
     * se indirizzi che genero random non sono gia' in uso per qualche chat di qualche documento)
     */
    private ServerDataStructures serverDataStructures;
    /**
     * Random number generator
     */
    private Random randomGenerator;
    /**
     * indirizzi di multicast gia' generati in precedenza
     */
    private LinkedHashSet<String> generatedAddresses;

    public MulticastAddressRandomGenerator(ServerDataStructures serverDataStructures){
        this.serverDataStructures = serverDataStructures;
        this.randomGenerator = new Random();
        this.generatedAddresses = new LinkedHashSet<>();
    }

    /**
     * Funzione che genera un indirizzo di multicast random. Il range di un indirizzo di multicast e':
     * [224.0.0.0 , 239.255.255.255], di cui si escludono gli indirizzi riservati:
     *
     * Address Range                 Size       Designation
     * -------------                 ----       -----------
     * 224.0.0.0 - 224.0.0.255       (/24)      Local Network Control Block
     *
     * 224.0.1.0 - 224.0.1.255       (/24)      Internetwork Control Block
     *
     * 224.0.2.0 - 224.0.255.255     (65024)    AD-HOC Block I
     *
     * 224.1.0.0 - 224.1.255.255     (/16)      RESERVED
     *
     * 224.2.0.0 - 224.2.255.255     (/16)      SDP/SAP Block
     *
     * 224.3.0.0 - 224.4.255.255     (2 /16s)   AD-HOC Block II
     *
     * 224.5.0.0 - 224.255.255.255   (251 /16s) RESERVED
     *
     * 225.0.0.0 - 231.255.255.255   (7 /8s)    RESERVED
     *
     * 232.0.0.0 - 232.255.255.255   (/8)       Source-Specific Multicast Block
     *
     * 233.0.0.0 - 233.251.255.255   (16515072) GLOP Block
     *
     * 233.252.0.0 - 233.255.255.255 (/14)      AD-HOC Block III
     *
     * 234.0.0.0 - 238.255.255.255   (5 /8s)    RESERVED
     *
     * 239.0.0.0 - 239.255.255.255   (/8)       Administratively Scoped Block
     *
     * @return indirizzo di multicast random
     *         "" se gli indirizzi di multicast sono esauriti (sono stati utilizzati tutti)
     */
    public String getRandomAddress() {

        String ind;

        while(true){

            //controllo dimensione lista indirizzi di multicast gia' generati
            //se ho generato il massimo numero di indirizzi disponibili, ossia:
            // 2 alla 28 = 268,435,456 - 256 (primo gruppo indirizzi riservati) = 268,435,200
            //ritorno null per segnalare errore al Worker, che lo deve notificare al Client
            if(this.generatedAddresses.size() == 268435200)
                return ""; //restituisco stringa vuota

            int firstOctet = this.baseFirtsOctet + Math.abs(randomGenerator.nextInt(this.boundFirstOctet));
            int secondOctet = this.baseOtherOctets + Math.abs(randomGenerator.nextInt(this.boundOtherOctets));
            int thirdOcted = this.baseOtherOctets + Math.abs(randomGenerator.nextInt(this.boundOtherOctets));
            int fourthOctet = this.baseOtherOctets + Math.abs(randomGenerator.nextInt(this.boundOtherOctets));

            //verifico che indirizzo non appartenga al primo set di indirizzi riservati (indirizzi utilizzati per
            // implementare algoritmi di routing)
            FunctionOutcome member = checkMembershipFirstGroup(firstOctet, secondOctet, thirdOcted, fourthOctet);
            if(member == FunctionOutcome.SUCCESS){
                continue; //indirizzo generato fa parte primo set indirizzi riservati
            }

            ind = firstOctet + "." + secondOctet + "." + thirdOcted + "." + fourthOctet;

            //verifico se indirizzo e' gia' stato assegnato ad un altro documento
            boolean alreadyAssigned = this.serverDataStructures.checkPresenceInMulticastAddress(ind);

            if(alreadyAssigned) { //indirizzo gia' assegnato ad un altro documento
                this.generatedAddresses.add(ind); //agiungo indirizzo a lista ind. generati ma scartati
                continue; //provo a generare un nuovo indirizzo
            }
            else{ //indirizzo non ancora assegnato

                //ritorno indirizzo creato
                return ind;
            }
        }
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al primo gruppo di indirizzi di multicast riservati,
     * ossia: 224.0.0.0 - 224.0.0.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipFirstGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //224.0.0.0
        int baseFirstOctetGroup = 224;
        int baseSecondOctetGroup = 0;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //224.0.0.255
        int boundFirstOctetGroup = 224;
        int boundSecondOctetGroup = 0;
        int boundThirdOctetGroup = 0;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet == baseSecondOctetGroup && thirdOcted == baseThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }
}
