public class TuringServer {
    private static String defaultConfFile = "/src/data/turingServer.conf";
    private static ServerConfigurationsManagement configurationsManagement = new ServerConfigurationsManagement();

    public static void main(String[] args){
        System.out.println("[Turing] >> SERVER TURING (disTribUted collaboRative edItiNG) AVVIATO");
        System.out.println();


        //se il file di configurazioni è inserito al momento dell'esecuzione prendo questo
        if(args.length>0) {
            System.out.println("[Turing] >> Fase di caricamento delle configurazioni del Server");
            String confFile = args[0];

            FunctionOutcome parse = configurationsManagement.parseConf(confFile);

            if(parse == FunctionOutcome.FAILURE){
                System.err.println("[ERR] >> Impossibile caricare le configurazioni del Server");
                System.exit(-1);
            }
        }
        else { //non  e' stato inserito nessun file di configurazione come argomento => parso quello di default
            FunctionOutcome parse = configurationsManagement.parseConf(defaultConfFile);

            if(parse == FunctionOutcome.FAILURE){
                System.err.println("[ERR] >> Impossibile caricare le configurazioni del Server");
                System.exit(-1);
            }

            System.out.println("[Turing] >> Il server è stato eseguito con le configurazioni di default");
            System.out.println("[Turing] >> Se desidi personalizzare le configuarzioni, riesegui il codice inserendo tra gli argomenti il tuo file");
            System.out.println("[Turing] >> Per maggiori dettagli sul formato delle configurazioni, guardare il file <./data/turingServer.conf>");
        }

        //mostro a video le configurazioni con cui è stato eseguito il server Turing
        configurationsManagement.showConf();

        //alloco le risorse estrappolate dal file di configurazione
        FunctionOutcome allocate = configurationsManagement.allocateConf();

        if(allocate == FunctionOutcome.FAILURE){
            System.err.println("[ERR] >> Impossibile allocare le risorse di configurazioni del Server");
            System.exit(-1);
        }

        //***************************************CREAZIONE LISTENER THREAD*********************************************//

        TuringListener listener = new TuringListener(configurationsManagement);
        Thread thread = new Thread(listener);
        thread.start();

        //**********************************ATTENDO TERMINAZIONE LISTENER THREAD**************************************//

        try {
            thread.join();
        } catch (InterruptedException e) {
            System.out.println("[Turing] >> Listener Thread terminato");
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("[Turing] >> SERVER TURING (disTribUted collaboRative edItiNG) TERMINATO");
    }
}
