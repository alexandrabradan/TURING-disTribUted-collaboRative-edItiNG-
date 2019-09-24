import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FileManagement {
    /**
     * Funzione che restituisce il path assoluto della cartella attuale
     * @return path assoluto della cartella corrente
     */
    public String getCurrentPath(){
        return System.getProperty("user.dir");
    }

    /**
     * Funzione che verifica se la cartella passata come argomento esiste ed e' una cartella (discriminante per evitare
     * che si tratti di un file)
     * @param directoryPath path della cartella di cui bisogna verificare esistenza
     * @return SUCCESS se la cartella esiste ed il suo path punta ad una cartella
     *         FAILURE se la cartella non esiste, oppure esiste il path ma non punta ad una cartella
     */
    public boolean checkEsistenceDirectory(String directoryPath){
        Path path = Paths.get(directoryPath);
        return  Files.exists(path) && Files.isDirectory(path);
    }


    public FunctionOutcome checkEsistenceDirectoryOtherwiseCreateIt(String directoryPath){
        Path path = Paths.get(directoryPath);

        //verifico esistenza cartella
        boolean exist = Files.exists(path);

        if(!exist){
            return  createDirectory(directoryPath); //provo a creare cartella
        }
        else{
            return FunctionOutcome.SUCCESS; //cartella esiste
        }
    }

    /**
     * Funzione che crea una cartella nel path passato come argomento
     * @param directoryPath path dove creare la cartella
     * @return SUCCESS se la cartella e' stata creata con successo
     *         FAILURE se non e' stato possibile creare la cartella oppure la cartella era gia' esistente
     */
    public FunctionOutcome createDirectory(String directoryPath){

        //verifico che la cartella non esista gia'
        boolean exist = checkEsistenceDirectory(directoryPath);

        if(!exist){
            Path path = Paths.get(directoryPath);
            try {
                Files.createDirectory(path);

                return FunctionOutcome.SUCCESS; //cartella creata con successo

            } catch (IOException e) {
                //something else went wrong
                System.err.println("Exception thrown  :" + e);
                e.printStackTrace();
            }
        }
        else{
            System.err.println("[ERR] >> Impossibile creare la cartella <<" + directoryPath + ">>, cartella gia' esistente oppure path illecito");
            return FunctionOutcome.FAILURE; //cartella esiste gia'
        }

        System.err.println("[ERR] >> Impossibile creare la cartella <<" + directoryPath + ">>");
        return FunctionOutcome.FAILURE; //se arrivo qui ci sono stati problemi
    }

    /**
     * Funzione che elimina tutti i files e le sottocartelle della cartella che ha il path passato come argomento, per
     * poter eliminare tale cartella (JAVA NIO permette di eliminare cartella solo se vuota)
     * @param directoryPath path della cartella da svuotare e eliminare
     * @return SUCCESS se e' la cartella e' stata eliminata con successo
     *         FAILURE se non e' stato possibile eliminare la cartella oppure la cartella non esiste
     */
    public FunctionOutcome deleteDirectory(String directoryPath){

        //verifico esistenza cartella
        boolean exist = checkEsistenceDirectory(directoryPath);

        if(exist){
            Path path =  Paths.get(directoryPath);

            try {
            /*Files.walk() returns a Stream of Path that we sort in reverse order. This places the paths denoting the
            contents of directories before directories itself. Thereafter it maps Path to File and deletes each File. */
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

                return FunctionOutcome.SUCCESS; //cartella eliminata con successo

            } catch (IOException e) {
                System.err.println("Exception thrown  :" + e);
                e.printStackTrace();
            }
        }
        else{
            System.err.println("[ERR] >> Impossibile eliminare la cartella <<" + directoryPath + ">>, cartella NON esistente");
            return FunctionOutcome.FAILURE; //cartella non esisteva => gia' eliminata
        }

        System.err.println("[ERR] >> Impossibile eliminare la cartella <<" + directoryPath + ">>");
        return FunctionOutcome.FAILURE; //se arrivo qui ci sono stati problemi
    }

    /**
     * Funzione che cancella i files presenti nella cartella che si trova nel path passato come argomento e restituisce
     * il numero totale di files che si trovavano al suo interno
     * @param directoryPath path della cartella che bisogna svuotare
     * @return numero >=0: numero di files che la cartella conteneva e che sono stati eliminati con successo
     *         -1: se e' subentrato un errore nell'eliminazione di un file oppure la cartella non esiste
     */
    public int deleteDirectoryContent(String directoryPath){

        //verifico esistenza cartella
        boolean exist = checkEsistenceDirectory(directoryPath);

        if(exist){
            Path path = Paths.get(directoryPath);
            try {
                List<String> filesList = Files.walk(path).filter(Files::isRegularFile)
                        .map(Path::toString).collect(Collectors.toList());

                filesList.forEach(this::deleteFile);

                return filesList.size(); //eliminazione files con successo

            } catch (IOException e) {
                System.err.println("Exception thrown  :" + e);
                e.printStackTrace();
            }
        }
        else{
            System.err.println("[ERR] >> Impossibile eliminare contenuto della cartella <<" + directoryPath + ">>, cartella non esistente");
            return -1; //cartella non esiste
        }

        System.err.println("[ERR] >> Impossibile eliminare contentuo della cartella <<" + directoryPath + ">>");
        return -1; //se arrivo qui ci sono stati problemi
    }

    /**
     * Funzione che stampa i files presenti nella cartella che si trova nel path passato come argomento e restituisce
     * il numero totale di files che si trovano al suo interno
     * @param directoryPath path della cartella di cui bisogna conteggiare numero di files
     * @return numero >=0: numero di files che la cartella contiene
     *         -1: se ci sono stati problemi nel conteggio dei files oppure la cartella non esiste
     */
    public int getDirectoryFilesNumber(String directoryPath){

        //verifico esistenza cartella
        boolean exist = checkEsistenceDirectory(directoryPath);

        if(exist){
            Path path = Paths.get(directoryPath);
            try {
                List<String> filesList = Files.walk(path).filter(Files::isRegularFile)
                        .map(Path::toString).collect(Collectors.toList());

                filesList.forEach(System.out::println);
                return filesList.size();//conteggio andato a buon fine


            } catch (IOException e) {
                System.err.println("Exception thrown  :" + e);
                e.printStackTrace();
            }
        }
        else{
            System.err.println("[ERR] >> Impossibile conteggiare files della cartella <<" + directoryPath + ">>, cartella non esistente");
            return -1; //cartella non esiste
        }

        System.err.println("[ERR] >> Impossibile conteggiare files della cartella <<" + directoryPath + ">>");
        return -1; //se arrivo qui ci sono stati problemi
    }

    /**
     * Funzione che verifica l'esistenza del file nel path passato come paramentro
     * @param filePath  path del file di cui bisogna verificare esistenza
     * @return true se file esiste
     *         false se file non esiste
     */
    public boolean checkEsistenceFile(String filePath){
        Path path = Paths.get(filePath);
        return  Files.exists(path);
    }

    /**
     * Funzione che verifica l'esistenza del file passato come paramentro e se non esiste lo crea
     * @param filePath path del file di cui bisogna verificare esistene e crearlo altrimenti
     * @return SUCCESS se il file esiste oppure e' stato creato correttamente
     *         FAILURE se ci sono stati problemi con la creazione del file
     */
    public FunctionOutcome checkEsistenceFileOtherwiseCreateIt(String filePath){
        boolean exist = checkEsistenceFile(filePath);
        return createFile(filePath);
    }

    /**
     * Funzione che crea un file nel path passato come argomento
     * @param filePath path dove creare il file
     */
    public FunctionOutcome createFile(String filePath){

        //verifico esistenza file
        boolean exist = checkEsistenceFile(filePath);

        if(!exist){
            Path path = Paths.get(filePath);
            try{
                Files.createFile(path);

                return FunctionOutcome.SUCCESS; //file creato correttamente

            } catch (IOException e){
                System.err.println("Exception thrown  :" + e);
                e.printStackTrace();
            }
        }
        else{
            System.err.println("[ERR] >> Impossibile craere il file <<" + filePath + ">>, file gia' esistente");
            return FunctionOutcome.FAILURE; //file esiste gia'
        }

        System.err.println("[ERR] >> Impossibile craere il file <<" + filePath + ">>");
        return FunctionOutcome.FAILURE; //se arrivo qui ci sono stati problemi
    }

    /**
     * Funzione che cancella il file nel path passato come argomento
     * @param filePath path del file da cancellare
     */
    public FunctionOutcome deleteFile(String filePath){

        //verifico esistenza del file
        boolean exist = checkEsistenceFile(filePath);

        if(exist){
            Path newFilePath = Paths.get(filePath);
            try{
                Files.delete(newFilePath);

                return FunctionOutcome.SUCCESS; //eliminazione avvenuta con successo

            } catch (IOException e){
                System.err.println("Exception thrown  :" + e);
                e.printStackTrace();
            }
        }
        else{
            System.err.println("[ERR] >> Impossibile eliminare il file <<" + filePath + ">>, file non esistente");
            return FunctionOutcome.FAILURE;
        }

        System.err.println("[ERR] >> Impossibile eliminare il file <<" + filePath + ">>");
        return FunctionOutcome.FAILURE; //se arrivo qui ci sono stati problemi
    }

    /**
     * Funzione che legge il contenuto del  file contenuto nel path passato come argomento
     * @param filePath path del file da leggere
     * @return SUCCESS se la lettura del file e' avvenuta con successo
     *         FAILURE se non e' stato possibile leggere il file oppure il file non esiste
     */
    public FunctionOutcome readFile(String filePath){

        //veridico esistenza file
        boolean exist = checkEsistenceFile(filePath);

        if(exist){
            Path path = Paths.get(filePath);
            try {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8); //.get(0);

                for (String line : lines) {
                    System.err.println(line);
                }

                return FunctionOutcome.SUCCESS; //lettura avvenuta con successo
            } catch (IOException e) {
                System.err.println("Exception thrown  :" + e);
                e.printStackTrace();
            }
        }
        else{
            System.err.println("[ERR] >> Impossibile legegre il file <<" + filePath + ">>, file non esistente");
            return FunctionOutcome.FAILURE;
        }

        System.err.println("[ERR] >> Impossibile leggere il file <<" + filePath + ">>");
        return FunctionOutcome.FAILURE; //se arrivo qui ci sono stati problemi
    }

    /**
     * Funzione che scrive sul file contenuto nel path passato come argomento
     * @param filePath  path del file da scrivere
     * @param contentToWrite contenuto da scrivere sul file
     * @return SUCCESS se la scrittura ha avuto successo
     *         FAILURE se la srittura ha avuto problemi oppure il file non esiste
     */
    public FunctionOutcome writeFile(String filePath, String contentToWrite){

        //verifico esistenza file
        boolean exist = checkEsistenceFile(filePath);

        if(exist){
            Path path = Paths.get(filePath);
            byte[] strToBytes = (contentToWrite + System.lineSeparator()).getBytes(); //aggiungo "\n"

            try{
                Files.write(path, strToBytes);

                return FunctionOutcome.SUCCESS; //scrittura avvenuta con successo

            } catch (IOException e){
                System.err.println("Exception thrown  :" + e);
                e.printStackTrace();
            }
        }
        else{
            System.err.println("[ERR] >> Impossibile scrivere il file <<" + filePath + ">>, file non esistente");
            return FunctionOutcome.FAILURE;
        }

        System.err.println("[ERR] >> Impossibile scrivere il file <<" + filePath + ">>");
        return FunctionOutcome.FAILURE; //se arrivo qui ci sono stati problemi
    }

    /**
     * Funzione che appende contenuto al file contenuto nel path passato come argomento
     * @param filePath  path del file a cui appendere
     * @param contentToAppend contenuto da appendere al file
     * @return SUCCESS se l'append ha avuto successo
     *         FAILURE se l'append ha avuto problemi oppure il file non esiste
     */
    public FunctionOutcome appendFile(String filePath, String contentToAppend){

        //verifico esistenza file
        boolean exist = checkEsistenceFile(filePath);

        if(exist){
            Path path = Paths.get(filePath);
            byte[] strToBytes = contentToAppend .getBytes();

            try{
                Files.write(path, strToBytes, StandardOpenOption.APPEND);

                return FunctionOutcome.SUCCESS; //append avventuo con successo

            } catch (IOException e){
                System.err.println("Exception thrown  :" + e);
                e.printStackTrace();
            }
        }
        else{
            System.err.println("[ERR] >> Impossibile fare append al file <<" + filePath + ">>, file non esistente");
            return FunctionOutcome.FAILURE;
        }

        System.err.println("[ERR] >> Impossibile fare append al file <<" + filePath + ">>");
        return FunctionOutcome.FAILURE; //se arrivo qui ci sono stati problemi
    }

}
