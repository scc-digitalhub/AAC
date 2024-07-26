import { TranslationMessages } from 'ra-core';
//use english ra.lang because italian is outdated
import italianMessages from 'ra-language-english';
import utils from '../utils';

const raMessages = utils.deepCopy(italianMessages);

//TODO create a dedicated translation
// raMessages.ra.action = {
//     add_filter: 'Aggiungi filtro',
//     add: 'Aggiungi',
//     back: 'Indietro',
//     bulk_actions:
//         '1 elemento selezionato |||| %{smart_count} elementi selezionati',
//     cancel: 'Cancella',
//     clear_array_input: 'Pulisci la lista',
//     clear_input_value: 'Pulisci il campo',
//     clone: 'Clona',
//     confirm: 'Conferma',
//     create: 'Crea',
//     create_item: 'Crea %{item}',
//     delete: 'Cancella',
//     edit: 'Modifica',
//     export: 'Esporta',
//     list: 'Lista',
//     refresh: 'Aggiorna',
//     remove_filter: 'Rimuovi il filtro',
//     remove_all_filters: 'Rimuovi tutti i filtri',
//     remove: 'Rimuovi',
//     save: 'Salva',
//     search: 'Cerca',
//     select_all: 'Seleziona tutto',
//     select_row: 'Seleziona la riga',
//     show: 'Mostra',
//     sort: 'Ordina',
//     undo: 'Annulla',
//     unselect: 'Deseleziona',
//     expand: 'Espandi',
//     close: 'Chiudi',
//     open_menu: 'Apri menu',
//     close_menu: 'Chiudi menu',
//     update: 'Modifica',
//     move_up: 'Sposta in alto',
//     move_down: 'Sposta in basso',
//     open: 'Apri',
//     toggle_theme: 'Cambia tema',
//     select_columns: 'Colonne',
// };

raMessages.ra.page = {
    create: 'Crea %{name}',
    dashboard: 'Dashboard',
    edit: '%{name} %{recordRepresentation}',
    error: 'Qualcosa è andato storto',
    list: '%{name}',
    loading: 'Caricamento',
    not_found: 'Non trovato',
    show: '%{name} %{recordRepresentation}',
    empty: 'Nessun %{name}.',
    invite: 'Vuoi aggiungerne?',
};

raMessages.ra.boolean = {
    true: 'Si',
    false: 'No',
    null: ' ',
};

raMessages.ra.page.dashboard = 'Home';

const messages: TranslationMessages = {
    ...raMessages,
    admin: 'Admin',
    developer: 'Developer',
    user: 'Utente',
    security: 'Sicurezza',
    accounts: 'Account',
    profiles: 'Informazioni',
    credentials: 'Credenziali',
    connections: 'App connesse',
    languages: {
        english: 'Inglese',
        italian: 'Italiano',
        german: 'Tedesco',
        spanish: 'Spagnolo',
        latvian: 'Lettone',
    },
    menu: {
        configuration: 'Configurazione',
    },
    resources: {
        accounts: {
            name: 'Account |||| Account',
        },
        credentials: {
            name: 'Credenziale |||| Credenziali',
            fields: {
                curPassword: 'Password attuale',
            },
        },
        profiles: {
            name: 'Informazioni personali',
        },
        connections: {
            name: 'App connessa |||| App connesse',
        },
        password: {
            details: 'Una password sicura mantiene al sicuro il tuo account',
            policy: {
                description: 'La password deve soddisfare i seguenti criteri:',
                strength: 'Resistenza della password',
                passwordMinLength: 'Lunghezza minima: %{value}',
                passwordMaxLength: 'Lunghezza massima: %{value}',
                passwordRequireAlpha: 'Contiene lettere',
                passwordRequireUppercaseAlpha: 'Contiene una lettera maiuscola',
                passwordRequireNumber: 'Contiene un numero',
                passwordRequireSpecial: 'Contiene un carattere speciale',
                passwordSupportWhitespace: 'Può contenere spazi',
            },
        },
        webauthn: {
            details:
                'Una chiave di sicurezza garantisce accesso sicuro al tuo account',
        },
    },
    page: {
        dashboard: {
            welcome: 'Benvenuto, %{name}',
            description:
                'Gestisci le tue informazioni personali, gli account e le opzioni di sicurezza',
            apps: {
                title: 'Applicazioni client',
                description: 'Rivedi e gestisci gli applicazioni',
                manage: 'Gestisci gli applicazioni',
            },
            accounts: {
                title: 'Account',
                description: 'Rivedi e gestisci gli account',
                manage: 'Gestisci gli account',
            },
            services: {
                title: 'App servizi',
                description: 'Vedi e gestisci le servizi',
                manage: 'Gestisci le servizi',
            },
            authentications: {
                title: 'Autenticazioni',
                description: 'Visualizza e modifica le tue autenticazioni',
                manage: 'Gestisci le autenticazioni',
            },
        },
        app:{
            overview: {
                title: 'Informazioni',

            },
            credentials:{
                title: 'Credenziali',
                header: {
                    title:'Credenziali OAuth2',
                    subtitle:'Gestisci le credenziali client per AAC',
                }
            },
            settings: {
                title:'Impostazioni',
            },
            configuration: {
                title:'OAuth2',
                header: {
                    title:'Configurazione OAuth2.0',
                    subtitle:'Configurazione base per client OAuth2/OpenId '
                }
            }
        },
        attributeset: {
            list: {
                title: 'Set di attributi',
                subtitle: 'Registra e gestisci i set di attributi personalizzati. Ogni attributo personalizzato sarà disponibile come profilo personalizzato per essere utilizzato sia via profili API che via token, con uno scope associato profile.setidentifier.me',
            },
        },
        accounts: {
            header: 'Account',
            description:
                'Visualizza e gestisci i tuoi account e collega gli account social',
            registered_user: 'Utente registrato',
            unregistered_user: 'Utente non registrato',
            register_user: {
                title: "Registra l'utente",
                text: 'Registra un account locale per persistere il tuo utente e poter effettuare il login con credenziali quali password, chiavi di sicurezza etc',
                action: 'Registrati',
            },
            delete_user: {
                title: "Cancella l'utente",
                text: 'Rimuovi il tuo utente e tutte le informazioni associate',
                action: 'Cancellati',
                confirm: 'Cancellare il tuo utente %{id}?',
                content:
                    'Sei sicuro di voler rimuove il tuo account? Questa operazione non può essere annullata!',
            },
            delete_account: {
                content:
                    'Sei sicuro di voler rimuove il tuo account? Questa operazione non può essere annullata!',
            },
            edit: {
                title: "Modifica l'account",
                description: 'Aggiorna le tue informazioni di registrazione',
            },
        },
        audit: {
            title: 'Registro operazioni',
            description: 'Attività recenti del tuo account',
        },
        login: {
            header: 'Login',
            description: 'Per accedere alla console è richiesto il login',
        },
        idp: {
            list: {
                title: 'Identity Providers',
                subtitle: 'Gestisci gli identity providers',
            },
            import: {
                title: 'Import Provider',
                description:
                    'Fornisci un file YAML valido con la definizione completa del provider o con un elenco di provider validi nidificati sotto i provider.',
            },
            overview: {
                title: 'Informazioni',
            },
            settings: {
                title: 'Impostazioni',
            },
            configuration: {
                title: 'Configurazione',
            },
            hooks: {
                title: 'Hook',
            },
            app:{
                title: 'Applicazioni',
            }
        },
    },
    alert: {
        authorization_expired: "L'autorizzazione è scaduta",
        missing_account: 'Devi avere un account registrato per procedere',
        webauthn_unsupported:
            'Questo browser non supporta le chiavi di sicurezza (webauthn)',
        invalid_attestation: 'Attestato invalido',
        missing_credentials:
            'Nessuna credenziale disponibile, registrane una per effettuare il login',
    },
    action: {
        register: 'Registra',
        actions: 'Azioni',
    },
    field: {
        username: 'Nome utente',
        name: 'Nome',
        surname: 'Cognome',
        email: 'Email',
        email_verified: 'Email verificata',
        id: 'id',
        given_name: 'Nome',
        family_name: 'Cognome',
        preferred_username: 'Nome utente',
        locale: 'Locale',
        zoneinfo: 'Timezone',
    },
    error: {
        already_registered: 'Già registrato',
        authentication_service:
            'Problema di autenticazione. Riprovare più tardi',
        bad_credentials: 'Utente o password invalidi',
        duplicated_data: 'Dati duplicati',
        internal_error: 'Errore interno. Riprovare più tardi',
        invalid_data: 'Dati invalidi',
        invalid_email: 'Email invalida',
        //invalid_password: 'Invalid user or password',
        invalid_password: {
            contains_whitespace: 'Password invalida: contiene spazi',
            empty: 'Password invalida: vuota',
            max_length: 'Password invalida: troppo lunga',
            min_length: 'Password invalida: troppo corta',
            password_reuse: 'Password invalida: riutilizzo password',
            require_alpha: 'Password invalida: è richiesta una lettera',
            require_number: 'Password invalida: è richiesto un numero',
            require_uppercase_alpha:
                'Password invalida: è richiesta una lettera maiuscola',
            require_special:
                'Password invalida: è richiesto un carattere speciale',
            not_match: 'Password invalida: le password non corrispondono',
            policy: 'Password invalida: la password non soddisfa la policy',
        },
        invalid_user: 'Utente invalido',
        locked: 'Utente bloccato',
        mismatch_passwords: 'Le password non corrispondono',
        missing_data: 'Dati mancanti',
        not_confirmed: 'Registrazione non confermata',
        not_registered: 'Utente non registrato',
        // registration: 'Registration problem. Please try again later',
        invalid_field: 'Dati invalidi',
        unsupported_operation: 'Operazione non supportata',
        wrong_password: 'Password errata',
        unauthenticated_user: "L'utente deve essere autenticato",
        registration: {
            invalid_password: 'Password invalida',
        },
    },
};

export default messages;
