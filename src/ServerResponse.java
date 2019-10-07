public enum ServerResponse{
    OP_OK,  //operazione ha avuto successo
    OP_USER_NOT_ONLINE,  //utente deve essere loggato per effettuare qualsiasi operazione(eccetto registrazione)
    OP_USER_NOT_REGISTERED,  //utente non registrato al servizio (verifica effettuata in fase di login)
    OP_DOCUMENT_NOT_EXIST,  //documento da condividere/scaricare non esiste
    OP_SECTION_NOT_EXIST,  //sezione da editare/scarica non esiste
    OP_USERNAME_ALREADY_TAKEN,  //in fase di registrazione bisogna fornire un nome univoco (non usato da un altro utente)
    OP_USER_MUST_LOGOUT, //bisogna prima fare logout per registrare nuovo utente
    OP_USER_ALREADY_ONLINE,  //utente tenta di fare login ma e' gia' connesso
    OP_USERNAME_INCORRECT,  //username fornito in fase di login scoretto
    OP_PASSWORD_INCORRECT,  //password fornita in fase di login scoretta
    OP_DOCUMENT_ALREADY_EXIST, //documento da creare e' gia' esistente
    OP_USER_NOT_CREATOR, //utente puo' invitare alla collaborazione del documento <=> e' creatore del documento
    OP_USER_IS_DEST, //utente condivide documento con se stesso (non lecito)
    OP_DEST_ALREADY_CONTRIBUTOR, //destinatario e' gia' collaboratore del documento
    OP_DEST_NOT_REGISTERED,  //@TODO gestione casistica destinatario online/offline
}
