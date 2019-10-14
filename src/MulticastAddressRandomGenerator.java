import java.net.InetAddress;
import java.net.UnknownHostException;
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
    private LinkedList<InetAddress> generatedAddresses;

    public MulticastAddressRandomGenerator(ServerDataStructures serverDataStructures){
        this.serverDataStructures = serverDataStructures;
        this.randomGenerator = new Random();
        this.generatedAddresses = new LinkedList<>();
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
     *         null se gli indirizzi di multicast sono esauriti (sono stati utilizzati tutti)
     */
    public InetAddress getRandomAddress() {

        InetAddress chat = null;
        String addr = "";

        while(true){

            //controllo dimensione lista indirizzi di multicast gia' generati
            //se ho generato il massimo numero di indirizzi disponibili, ossia:
            // 2 alla 28 = 268,435,456 - 225 (primo gruppo indirizzi riservati) = 268,435,201
            //ritorno null per segnalare errore al Worker, che lo deve notificare al Client
            if(this.generatedAddresses.size() == 268435201)
                return null;

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

            addr = firstOctet + "." + secondOctet + "." + thirdOcted + "." + fourthOctet;

            try {
                //creo l'indirizzo corrispondente
                chat = InetAddress.getByName(addr);

                //verifico se indirizzo e' gia' stato assegnato ad un altro documento
                boolean alreadyAssigned = this.serverDataStructures.checkPresenceInMulticastAddress(chat);

                if(alreadyAssigned) { //indirizzo gia' assegnato ad un altro documento
                    this.generatedAddresses.add(chat); //agiungo indirizzo a lista ind. generati ma scartati
                    continue; //provo a generare un nuovo indirizzo
                }
                else{ //indirizzo non ancora assegnato

                    //aggiungo indirizzo all'insieme degli indirizzi assegnati
                    this.serverDataStructures.addToMulticastAddress(chat);

                    //ritorno indirizzo creato
                    return chat;
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
                System.exit(-1);
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

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al secondo gruppo di indirizzi di multicast riservati,
     * ossia: 224.0.1.0 - 224.0.1.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipSecondGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //224.0.1.0
        int baseFirstOctetGroup = 224;
        int baseSecondOctetGroup = 0;
        int baseThirdOctetGroup = 1;
        int baseFourthOctetGroup = 0;

        //224.0.1.255
        int boundFirstOctetGroup = 224;
        int boundSecondOctetGroup = 0;
        int boundThirdOctetGroup = 1;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet == baseSecondOctetGroup && thirdOcted == baseThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al terzo gruppo di indirizzi di multicast riservati,
     * ossia: 224.0.2.0 - 224.0.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipThirdGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //224.0.2.0
        int baseFirstOctetGroup = 224;
        int baseSecondOctetGroup = 0;
        int baseThirdOctetGroup = 2;
        int baseFourthOctetGroup = 0;

        // 224.0.255.255
        int boundFirstOctetGroup = 224;
        int boundSecondOctetGroup = 0;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet == baseSecondOctetGroup && thirdOcted >= baseThirdOctetGroup
                                                                                && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al quarto gruppo di indirizzi di multicast riservati,
     * ossia: 224.1.0.0 - 224.1.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipFourthGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //224.1.0.0
        int baseFirstOctetGroup = 224;
        int baseSecondOctetGroup = 1;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //224.1.255.255
        int boundFirstOctetGroup = 224;
        int boundSecondOctetGroup = 1;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet == baseSecondOctetGroup && thirdOcted >= baseThirdOctetGroup
                && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al quinto gruppo di indirizzi di multicast riservati,
     * ossia: 224.2.0.0 - 224.2.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipFifthGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //224.2.0.0
        int baseFirstOctetGroup = 224;
        int baseSecondOctetGroup = 2;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //224.2.255.255
        int boundFirstOctetGroup = 224;
        int boundSecondOctetGroup = 2;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet == baseSecondOctetGroup && thirdOcted >= baseThirdOctetGroup
        && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al sesto gruppo di indirizzi di multicast riservati,
     * ossia: 224.3.0.0 - 224.4.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipSixthGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //224.3.0.0
        int baseFirstOctetGroup = 224;
        int baseSecondOctetGroup = 3;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //224.4.255.255
        int boundFirstOctetGroup = 224;
        int boundSecondOctetGroup = 4;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet >= baseSecondOctetGroup  && secondOctet <= boundSecondOctetGroup
                && thirdOcted >= baseThirdOctetGroup && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al settimo gruppo di indirizzi di multicast riservati,
     * ossia: 224.5.0.0 - 224.255.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipSeventhGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //224.5.0.0
        int baseFirstOctetGroup = 224;
        int baseSecondOctetGroup = 5;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //224.255.255.255
        int boundFirstOctetGroup = 224;
        int boundSecondOctetGroup = 255;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet >= baseSecondOctetGroup  && secondOctet <= boundSecondOctetGroup
                && thirdOcted >= baseThirdOctetGroup && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene all'ottavo gruppo di indirizzi di multicast riservati,
     * ossia: 225.0.0.0 - 231.255.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipEighthGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //225.0.0.0
        int baseFirstOctetGroup = 225;
        int baseSecondOctetGroup = 0;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //231.255.255.255
        int boundFirstOctetGroup = 231;
        int boundSecondOctetGroup = 255;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet >= baseFirstOctetGroup && firstOctet <= boundFirstOctetGroup && secondOctet >= baseSecondOctetGroup
                && secondOctet <= boundSecondOctetGroup && thirdOcted >= baseThirdOctetGroup && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al nono gruppo di indirizzi di multicast riservati,
     * ossia: 232.0.0.0 - 232.255.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipNinethGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //232.0.0.0
        int baseFirstOctetGroup = 232;
        int baseSecondOctetGroup = 0;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //232.255.255.255
        int boundFirstOctetGroup = 232;
        int boundSecondOctetGroup = 255;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet >= baseSecondOctetGroup  && secondOctet <= boundSecondOctetGroup
                && thirdOcted >= baseThirdOctetGroup && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al decimo gruppo di indirizzi di multicast riservati,
     * ossia: 233.0.0.0 - 233.251.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipTenthGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //233.0.0.0
        int baseFirstOctetGroup = 233;
        int baseSecondOctetGroup = 0;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //233.251.255.255
        int boundFirstOctetGroup = 233;
        int boundSecondOctetGroup = 251;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet >= baseSecondOctetGroup  && secondOctet <= boundSecondOctetGroup
                && thirdOcted >= baseThirdOctetGroup && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene all'undicesimo gruppo di indirizzi di multicast riservati,
     * ossia: 233.252.0.0 - 233.255.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipEleventhGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //233.252.0.0
        int baseFirstOctetGroup = 233;
        int baseSecondOctetGroup = 252;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //233.255.255.255
        int boundFirstOctetGroup = 233;
        int boundSecondOctetGroup = 255;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup && secondOctet >= baseSecondOctetGroup  && secondOctet <= boundSecondOctetGroup
                && thirdOcted >= baseThirdOctetGroup && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al dodicesimo gruppo di indirizzi di multicast riservati,
     * ossia: 234.0.0.0 - 238.255.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipTwelvethGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //234.0.0.0
        int baseFirstOctetGroup = 234;
        int baseSecondOctetGroup = 0;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //238.255.255.255
        int boundFirstOctetGroup = 238;
        int boundSecondOctetGroup = 255;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet >= baseFirstOctetGroup && firstOctet <= baseFirstOctetGroup  && secondOctet >= baseSecondOctetGroup
                && secondOctet <= boundSecondOctetGroup && thirdOcted >= baseThirdOctetGroup && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }

    /**
     * Funzione che verifica se l'indirizzo appena generato appartiene al tredicesimp gruppo di indirizzi di multicast riservati,
     * ossia: 239.0.0.0 - 239.255.255.255
     * @param firstOctet primo otteto dell'indirizzo random
     * @param secondOctet secondo otteto dell'indirizzo random
     * @param thirdOcted terzo otteto dell'indirizzo random
     * @param fourthOctet quarto otteto dell'indirizzo random
     * @return FAILURE se l'indirizzo non appartiene al gruppo deegli esclusi
     *         SUCCESS altrimenti
     */
    private FunctionOutcome checkMembershipThirteenthGroup(int firstOctet, int secondOctet, int thirdOcted, int fourthOctet){
        //239.0.0.0
        int baseFirstOctetGroup = 239;
        int baseSecondOctetGroup = 0;
        int baseThirdOctetGroup = 0;
        int baseFourthOctetGroup = 0;

        //239.255.255.255
        int boundFirstOctetGroup = 239;
        int boundSecondOctetGroup = 255;
        int boundThirdOctetGroup = 255;
        int boundFourthOctetGroup = 255;

        if(firstOctet == baseFirstOctetGroup  && secondOctet >= baseSecondOctetGroup && secondOctet <= boundSecondOctetGroup
                && thirdOcted >= baseThirdOctetGroup && thirdOcted <= boundThirdOctetGroup){
            if(fourthOctet >= baseFourthOctetGroup && fourthOctet <= boundFourthOctetGroup)
                return FunctionOutcome.SUCCESS; //indirizzo generato appartiene al gruppo riservato
        }

        return FunctionOutcome.FAILURE; //indirizzo generato NON appartiene al gruppo riservato
    }
}
