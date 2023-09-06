import { TranslationMessages } from 'ra-core';
//use english ra.lang because italian is outdated
import germanMessages from 'ra-language-english';
import utils from '../utils';

const raMessages = utils.deepCopy(germanMessages);

raMessages.ra.action = {
    add_filter: 'Filter hinzufügen',
    add: 'Hinzufügen',
    back: 'Zurück',
    bulk_actions:
        '1 ausgewähltes Element |||| %{smart_count} ausgewählte Elemente',
    cancel: 'Abbrechen',
    clear_array_input: 'Die Liste löschen',
    clear_input_value: 'Das Array löschen',
    clone: 'Klonen',
    confirm: 'Bestätigen',
    create: 'Erstellen',
    create_item: 'Erstellen %{item}',
    delete: 'Entfernen',
    edit: 'Bearbeiten',
    export: 'Exportieren',
    list: 'Liste',
    refresh: 'Aktualisieren',
    remove_filter: 'Filter entfernen',
    remove_all_filters: 'Alle Filter entfernen',
    remove: 'Entfernen',
    save: 'Speichern',
    search: 'Suchen',
    select_all: 'Alles auswählen',
    select_row: 'Zeile auswählen',
    show: 'Anzeigen',
    sort: 'Sortieren',
    undo: 'Rückgängig',
    unselect: 'Abwählen',
    expand: 'Erweitern',
    close: 'Schließen',
    open_menu: 'Menü öffnen',
    close_menu: 'Menü schließen',
    update: 'Bearbeiten',
    move_up: 'Nach oben bewegen',
    move_down: 'Nach unten bewegen',
    open: 'Öffnen',
    toggle_theme: 'Thema wechseln',
    select_columns: 'Spalten auswählen',
};

raMessages.ra.page = {
    create: 'Erstellen Sie %{name}',
    dashboard: 'Dashboard',
    edit: '%{name} %{recordRepresentation}',
    error: 'Etwas ist schief gelaufen',
    list: '%{name}',
    loading: 'Laden',
    not_found: 'Nicht gefunden',
    show: '%{name} %{recordRepresentation}',
    empty: 'Kein %{name}.',
    invite: 'Möchten Sie hinzufügen?',
};

raMessages.ra.boolean = {
    true: 'Ja',
    false: 'Nein',
    null: ' ',
};

raMessages.ra.page.dashboard = 'Home';

const messages: TranslationMessages = {
    ...raMessages,
    admin: 'Admin',
    developer: 'Developer',
    user: 'Benutzer',
    security: 'Sicherheit',
    accounts: 'Kontos',
    profiles: 'Informationen',
    credentials: 'Anmeldeinformationen',
    connections: 'Verbundene Anwendungen',
    languages: {
        english: 'Englisch',
        italian: 'Italienisch',
        german: 'Deutsch',
        spanish: 'Spanisch',
        latvian: 'Lettisch',
    },
    resources: {
        accounts: {
            name: 'Konto |||| Konto',
        },
        credentials: {
            name: 'Anmeldeinformationen |||| Anmeldeinformationen',
            fields: {
                curPassword: 'Aktuelles Passwort',
            },
        },
        profiles: {
            name: 'Persönliche Informationen',
        },
        connections: {
            name: 'Angeschlossene App |||| Angeschlossene Apps',
        },
        password: {
            details: 'Ein sicheres Passwort schützt Ihr Konto',
            policy: {
                description:
                    'Das Passwort muss die folgenden Kriterien erfüllen:',
                strength: 'Passwortstärke',
                passwordMinLength: 'Mindestlänge: %{value}',
                passwordMaxLength: 'Maximale Länge: %{value}',
                passwordRequireAlpha: 'Buchstaben sind erforderlich',
                passwordRequireUppercaseAlpha:
                    'Großbuchstaben sind erforderlich',
                passwordRequireNumber: 'Eine Zahl ist erforderlich',
                passwordRequireSpecial: 'Ein Sonderzeichen ist erforderlich',
                passwordSupportWhitespace: 'Darf Leerzeichen enthalten',
            },
        },
        webauthn: {
            details:
                'Ein Sicherheitsschlüssel gewährleistet einen sicheren Zugang zu Ihrem Konto',
        },
    },
    page: {
        dashboard: {
            welcome: 'Willkommen, %{name}',
            description:
                'Verwalten Sie Ihre persönlichen Informationen, Konten und Sicherheitsoptionen',
            accounts: {
                title: 'Konto',
                description: 'Konten überprüfen und verwalten',
                manage: 'Konten verwalten',
            },
            connections: {
                title: 'Verbundene Apps',
                description:
                    'Anzeigen und Verwalten von Drittanbieteranwendungen, die mit Ihrem Konto verbunden sind',
                manage: 'Verbindungen verwalten',
            },
            credentials: {
                title: 'Anmeldeinformationen',
                description:
                    'Ihre Anmeldeinformationen anzeigen und bearbeiten',
                manage: 'Verwalten Sie Ihre Anmeldeinformationen',
            },
        },
        accounts: {
            header: 'Konto',
            description:
                'Anzeigen und Verwalten Ihrer Konten und Verknüpfen von sozialen Konten',
            registered_user: 'Registrierter Benutzer',
            unregistered_user: 'Unregistrierter Benutzer',
            register_user: {
                title: 'Benutzer registrieren',
                text: 'Registrieren Sie ein lokales Konto, um Ihren Benutzer aufrechtzuerhalten und sich mit Anmeldedaten wie Passwörtern, Sicherheitsschlüsseln usw. anzumelden',
                action: 'Registrieren',
            },
            delete_user: {
                title: 'Benutzer löschen',
                text: 'Entfernen Sie Ihren Benutzer und alle zugehörigen Informationen',
                action: 'Löschen',
                confirm: 'Löschen Sie Ihren Benutzer %{id}?',
                content:
                    'Sind Sie sicher, dass Sie Ihr Konto entfernen möchten? Dieser Vorgang kann nicht rückgängig gemacht werden!',
            },
            delete_account: {
                content:
                    'Sind Sie sicher, dass Sie Ihr Konto entfernen möchten? Dieser Vorgang kann nicht rückgängig gemacht werden!',
            },
            edit: {
                title: 'Konto bearbeiten',
                description:
                    'Aktualisieren Sie Ihre Registrierungsinformationen',
            },
        },
        profiles: {
            header: 'Persönliche Informationen',
            description:
                'Überprüfen und kontrollieren Sie Ihre persönlichen Informationen, die für verwandte Anwendungen und Dienste verfügbar sind',
        },
        connections: {
            header: 'Verbundene Anwendungen',
            description:
                'Verwalten und kontrollieren Sie Anwendungen und Berechtigungen. Sie haben Anwendungen und Websites von Drittanbietern Zugriff auf einige Ihrer persönlichen Daten gewährt. Überprüfen und entfernen Sie Anwendungen, die Sie nicht mehr verwenden oder die Sie nicht für vertrauenswürdig halten',
            permissions_num: 'Sie haben Zugriff auf %{num} Berechtigungen',
        },
        credentials: {
            header: 'Anmeldeinformationen',
            description:
                'Überprüfen, bearbeiten, zurücksetzen und entfernen Sie die mit Ihrem Konto verbundenen Sicherheitsanmeldeinformationen',
        },
        password: {
            title: 'Passwörter verwalten',
            subtitle:
                'Ihr persönliches Anmeldepasswort festlegen, ändern und aktualisieren',
            edit: {
                title: 'Passwort ändern',
                description:
                    'Ändern Sie Ihr altes Passwort und setzen Sie ein neues',
            },
            delete: {
                content:
                    'Sind Sie sicher, dass Sie Ihr Passwort löschen wollen? Ohne dieses können Sie sich nicht mehr mit Ihrem Passwort anmelden',
            },
        },
        webauthn: {
            title: 'Sicherheitsschlüssel verwalten',
            subtitle:
                'Registrieren Sie sich und verwenden Sie für die Anmeldung Sicherheitsschlüssel, wie Ihr Telefon oder einen tragbaren Schlüssel',
            create: {
                title: 'Einen Sicherheitsschlüssel registrieren',
                description:
                    'Verknüpfen Sie einen lokalen oder übertragbaren Schlüssel mit Ihrem persönlichen Konto',
            },
            edit: {
                title: 'Sicherheitsschlüssel bearbeiten',
                description:
                    'Aktualisieren Sie die Schlüsselregistrierung durch Ändern der Eigenschaften',
            },
            delete: {
                content:
                    'Sind Sie sicher, dass Sie den Schlüssel entfernen wollen? Sie können ihn dann nicht mehr für die Anmeldung verwenden',
            },
        },
        security: {
            header: 'Sicherheit',
            description:
                'Zugriff auf Sicherheitseinstellungen und Abfrage des Betriebsprotokolls',
        },
        audit: {
            title: 'Betriebsprotokoll',
            description: 'Jüngste Kontoaktivitäten',
        },
        login: {
            header: 'Anmeldung',
            description:
                'Für den Zugriff auf die Konsole ist eine Anmeldung erforderlich',
        },
    },
    alert: {
        authorization_expired: 'Ihre Autorisierung ist abgelaufen',
        missing_account:
            'Sie müssen ein registriertes Konto haben, um fortzufahren',
        webauthn_unsupported:
            'Dieser Browser unterstützt keine Sicherheitsschlüssel (webauthn)',
        invalid_attestation: 'Ungültiges Zertifikat',
    },
    action: {
        register: 'Registrieren',
    },
    field: {
        username: 'Nutzername',
        name: 'Vorname',
        surname: 'Nachname',
        email: 'E-Mail',
        email_verified: 'Verifizierte E-Mail',
        id: 'id',
        given_name: 'Vorname',
        family_name: 'Nachname',
        preferred_username: 'Benutzername',
        locale: 'Lokal',
        zoneinfo: 'Zeitzone',
    },
    error: {
        already_registered: 'Bereits registriert',
        authentication_service:
            'Authentifizierungsproblem. Bitte versuchen Sie es später erneut',
        bad_credentials: 'Ungültiger Benutzer oder ungültiges Passwort',
        duplicated_data: 'Doppelte Daten',
        internal_error: 'Interner Fehler. Bitte versuchen Sie es später erneut',
        invalid_data: 'Ungültige Daten',
        invalid_email: 'Ungültige E-Mail',
        //invalid_password: 'Ungültiger Benutzer oder ungültiges Passwort',
        invalid_password: {
            contains_whitespace: 'Ungültiges Passwort: enthält Leerzeichen',
            empty: 'Ungültiges Passwort: leer',
            max_length: 'Ungültiges Passwort: zu lang',
            min_length: 'Ungültiges Kennwort: zu kurz',
            password_reuse: 'Ungültiges Passwort: Passwort wiederverwenden',
            require_alpha: 'Ungültiges Kennwort: ein Buchstabe erforderlich',
            require_number: 'Ungültiges Kennwort: eine Zahl ist erforderlich',
            require_uppercase_alpha:
                'Ungültiges Kennwort: ein Großbuchstabe ist erforderlich',
            require_special:
                'Ungültiges Kennwort: ein Sonderzeichen ist erforderlich',
            not_match: 'Ungültiges Kennwort: Kennwörter stimmen nicht überein',
            policy: 'Ungültiges Kennwort: Kennwort stimmt nicht überein',
        },
        invalid_user: 'Ungültiger Benutzer',
        locked: 'Gesperrter Benutzer',
        mismatch_passwords: 'Die Passwörter stimmen nicht überein',
        missing_data: 'Fehlende Daten',
        not_confirmed: 'Registrierung nicht bestätigt',
        not_registered: 'Benutzer nicht registriert',
        // registration: 'Registrierungsproblem. Bitte versuchen Sie es später noch einmal',
        invalid_field: 'Ungültige Daten',
        unsupported_operation: 'Nicht unterstützte Operation',
        wrong_password: 'Falsches Passwort',
        unauthenticated_user: 'Der Benutzer muss authentifiziert sein',
        registration: {
            invalid_password: 'Ungültiges Passwort',
        },
    },
};

export default messages;
