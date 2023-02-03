import { TranslationMessages } from 'ra-core';
import italianMessages from 'ra-language-italian';

const messages: TranslationMessages = {
    ...italianMessages,
    error: {
        already_registered: 'Già registrato.',
        authentication_service:
            'Problemi di autenticazione. Prova di nuovo dopo.',
        bad_credentials: 'Password o email invalida',
        duplicated_data: 'Dati duplicati',
        internal_error: 'Errore interno. Prego provare di nuovo.',
        invalid_data: 'Dati non validi.',
        invalid_email: 'Email non valida',
        //invalid_password: 'Invalid user or password',
        invalid_password: {
            contains_whitespace: 'Password invalida: contiene spazi',
            empty: 'Password invalida: password vuota',
            max_length: 'Password invalida: troppo lunga',
            min_length: 'Password invalida: troppo corta',
            password_reuse: 'Password invalida: riuso della password',
            require_alpha: 'Password invalida: richiesto almeno un carattere',
            require_number: 'Password invalida: richiesto almeno un numero',
            require_uppercase_alpha:
                'Password invalida: richiesto almeno un carattere maiuscolo',
            require_special:
                'Password invalida: richiesto almeno un carattere speciale',
            not_match: 'Password invalida: le password sono diverse',
        },
        invalid_user: 'Utente non valido',
        locked: 'Utente bloccato',
        mismatch_passwords: 'Le password non corrispondono',
        missing_data: 'Dati mancanti',
        not_confirmed: 'Registrazine non confermata.',
        not_registered: 'Utente o password non valida',
        // registration: 'Registration problem. Please try again later.',
        invalid_field: 'Dati non validi.',
        unsupported_operation: 'Operazione non supportata.',
        wrong_password: 'Password sbagliata',
        unauthenticated_user: 'Utente deve essere autenticato.',
        registration: {
            invalid_password: 'Password invalida',
        },
    },
};

messages.ra.page.dashboard = 'Home';

export default messages;
