public enum ServerResponse{
    OP_OK,  //operazione ha avuto successo
    OP_INVALID_REQUEST, //richiesta non rientra nei tasks del servizio
    OP_USER_NOT_ONLINE,  //utente deve essere loggato per effettuare qualsiasi operazione(eccetto registrazione)
    OP_USER_NOT_REGISTERED,  //utente non registrato al servizio (verifica effettuata in fase di login)
    OP_DOCUMENT_NOT_EXIST,  //documento da condividere/scaricare non esiste
    OP_DOCUMENT_PERMISSION_DENIED, //utente non ha permessi per visualizzare un documento (non e' creatore ne' collaboratore)
    OP_SECTION_NOT_EXIST,  //sezione da editare/scarica non esiste
    OP_USERNAME_INAVLID_CHARACTERS, //username deve contente solo caratteri alfanumerici per creare cartelle univoche
    OP_USERNAME_TOO_SHORT, //userdeme inferiore caratteri minimi consentiti
    OP_USERNAME_TOO_LONG, //username eccede caratteri consentiti
    OP_PASSWORD_TOO_SHORT, //password inferiore caratteri consentiti
    OP_PASSWORD_TOO_LONG, //password eccede caratteri consentiti
    OP_DOCUMENT_TOO_LONG, //documento inferiore caratteri consentiti
    OP_DOCUMENT_TOO_SHORT, //documento eccede caratteri consentiti
    OP_SECTION_EXCEED_LIMIT, //numero sezioni di un documento eccede il numero massimo consentito
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
    OP_ONLINE_INVITE_ADVERTISEMENT, //flag per segnalare soppraggiungere di un invito ad un utente connesso
    OP_SECTION_IS_COMING, //flag per segnalare il download di una sezione
}
