import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MyExecutor extends ThreadPoolExecutor {

    private static final String THREAD_NAME_PATTERN = "%s%d";
    private static final String namePrefix = "Worker_";

    //devo creare un newFixedThreadPool personalizzato, di conseguenza i paramentri che devo
    //passare alla superclasse ThreadPoolExecutor sono:
    //1. corePoolSize = numWorkersInThreadPool (# threads attivati al momento attivazione ThreadPool)
    //2. maximumPoolSize = numWorkersInThreadPool (# max threads attivi in un dato momento nel ThreadPool)
    //3. keepAliveTime = OL (se ci sono + di corePoolSize threads inattivi da piu' di keepAlive time vengono fatti terminare)
    // (caso non si verifica mai perche' nel nostro caso corePoolSize = maximumPoolSize)
    //4. unit = TimeUnit.MILLISECONDS (unita' di misura temporale del keepAliveTime)
    //5. workingQueue =  LinkedBlockingQueue<Runnable> (coda di lavoro)
    public MyExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, final TimeUnit unit,
                                                                    LinkedBlockingQueue<Runnable> workingQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workingQueue,
                //ThreadFactory = oggetto che permette creazione threads on-demand e che consente di conseguenze di
                //specificare una personalizzazione (nel nostro caso nome da attribuire agli Workers del ThreadPool)
                //dei threads che si vogliono creare
                new ThreadFactory() {

                    //contatore atomico (acceduto in mutua esclusione) per dare numerazione
                    //agli Workers creati
                    private final AtomicInteger counter = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        final String threadName = String.format(THREAD_NAME_PATTERN, namePrefix, counter.incrementAndGet());
                        return new Thread(r, threadName);
                    }
                });
    }
}

