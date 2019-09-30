public enum ServerResponse {
    OP_USER_NOT_ONLINE,
    OP_USER_NOT_REGISTERED,
    OP_DOCUMENT_NOT_EXIST,
    OP_SECTION_NOT_EXIST,
    OP_REGISTER_OK,
    OP_REGISTER_USERNAME_ALREADY_TAKEN,
    OP_REGISTER_USER_ALREADY_ONLINE, //bisogna prima fare logout per registrare nuovo utente
    OP_LOGIN_OK,
    OP_LOGIN_USER_ALREADY_ONLINE,
    OP_LOGOUT_OK,
    OP_CREATE_OK,
    OP_CREATE_DOCUMENT_ALREADY_EXIST,
    OP_SHARE_OK,
    OP_USER_NOT_CREATOR, //utente puo' invitare alla collaborazione del documento <=> e' creatore del documento
    OP_SHARE_USER_IS_DEST, //utente condivide documento con se stesso (non lecito)
    OP_DEST_ALREADY_CONTRIBUTOR, //destinatario e' gia' collaboratore del documento
    OP_SHARE_DEST_NOT_REGISTERED,  //@TODO gestione casistica destinatario online/offline
    OP_SHOW_DOCUMENT_OK,
    OP_SHOW_SECTION_OK,
    OP_LIST_OK,
    OP_EDIT_OK,
    OP_EDIT_MULTICAST_ADDRESS, //@TODO Server deve per prima cosa inviare indirizzo di multicast
    OP_END_EDIT_OK,
    OP_SEND_OK,
    OP_RECEIVE_OK
}
