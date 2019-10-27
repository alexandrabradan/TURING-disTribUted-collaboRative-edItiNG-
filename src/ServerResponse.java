public enum ServerResponse{
    OP_OK,  //operazione ha avuto successo
    OP_INVALID_REQUEST, //richiesta non rientra nei tasks del servizio
    OP_USER_NOT_ONLINE,  //utente deve essere loggato per effettuare qualsiasi operazione(eccetto registrazione)
    OP_USER_NOT_REGISTERED,  //utente non registrato al servizio (verifica effettuata in fase di login)
    OP_DOCUMENT_NOT_EXIST,  //documento da condividere/scaricare non esiste
    OP_DOCUMENT_PERMISSION_DENIED, //utente non ha permessi per visualizzare un documento (non e' creatore ne' collaboratore)
    OP_SECTION_NOT_EXIST,  //sezione da editare/scarica non esiste
    OP_USERNAME_INAVLID_CHARACTERS, //username deve contente solo caratteri alfanumerici per creare cartelle univoche
    OP_DOCUMENT_INAVLID_CHARACTERS, //nome documento deve contente solo caratteri alfanumerici per creare cartelle univoche
    OP_USERNAME_ALREADY_TAKEN,  //in fase di registrazione bisogna fornire un nome univoco (non usato da un altro utente)
    OP_USER_MUST_LOGOUT, //bisogna prima fare logout per registrare nuovo utente
    OP_USER_ALREADY_ONLINE,  //utente tenta di fare login ma e' gia' connesso
    OP_PASSWORD_INCORRECT,  //password fornita in fase di login scoretta
    OP_DOCUMENT_ALREADY_EXIST, //documento da creare e' gia' esistente
    OP_DOCUMENT_MULTICAST_ADDRESS_RUN_OUT, //esaurimento degli indirizzi di multicast
    OP_USER_NOT_CREATOR, //utente puo' invitare alla collaborazione del documento <=> e' creatore del documento
    OP_USER_IS_DEST, //utente condivide documento con se stesso (non lecito)
    OP_DEST_ALREADY_CONTRIBUTOR, //destinatario e' gia' collaboratore del documento
    OP_DEST_NOT_REGISTERED,  //destinatario deve essere un utente registrato al servizio
    OP_USER_NOT_ALLOWED_TO_EDIT, //utente deve essere collaboratore/creatore di un documento per poterlo editare
    OP_SECTION_ALREADY_IN_EDITING_MODE, //se la sezione richiesta da editare e' gia' editata da qualcuno
    OP_SECTION_NOT_IN_EDITING_MODE, //se la sezione di cui si richiede fine editing non era settata ad editabile
    OP_SECTION_EDITED_BY_SOMEONE_ELSE, //sezione e' editata da un Client diverso da quello che richiede END-EDIT
    OP_USER_IS_ALREADY_EDITING_SOMETHING, //utente sta gia' editando una sezione di un documento
    OP_SECTION_IMPOSSIBLE_TO_UPDATE, //problemi da parte del Server con l'aggiornamento di una sezione
    OP_ONLINE_INVITE_ADVERTISEMENT, //flag per segnalare soppraggiungere di un invito ad un utente connesso
    OP_SECTION_IS_COMING, //flag per segnalare il download di una sezione
    OP_DOCUMENT_MULTICAST_IND_IS_COMING, //flag per segnalare invio indirizzo di multicast del documento
    OP_WHO_IS_EDITING, //flag per segnalare l'invio da parte del Server di chi sta editando una sezione/documento
    OP_SERVER_READY_FOR_UPDATE, //flag per segnalare al Client che puo' inviare sezione aggiornata al Server
    OP_SEND_FAILURE, //invio messaggio sulla chat fallito
    OP_SEND_IMPOSSIBLE_TO_READ_MESSAGE, // Server incapacitato di leggere msg da inviare sulla chat
    OP_WELCOME_MESSAGE_SEND, //flag di risposta  di buon esito da parte del Server del welcome-message
}
