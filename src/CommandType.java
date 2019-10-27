public enum CommandType {
    HELP,
    REGISTER,
    LOGIN,
    LOGOUT,
    CREATE,
    SHARE,
    SHOW_DOCUMENT,
    SHOW_SECTION,
    LIST,
    EDIT,
    END_EDIT,
    SEND,
    RECEIVE,
    EXIT,
    I_AM_CLIENT_SOCKET, //flag per notificare al Server che canale che gli sta mandano msg e' da utilizare per leggere richieste / mandare risposte
    I_AM_INVITE_SOCKET, //flag per notificare al Server che canale che gli sta mandano msg e' da utilizzare come canale di invio inviti
    SECTION_IS_COMING, //flag per notificare al Server l'invio di una sezione aggiornata
}
