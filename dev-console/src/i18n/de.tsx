import { TranslationMessages } from 'ra-core';
//use english ra.lang because italian is outdated
import germanMessages from 'ra-language-english';
import utils from '../utils';

const raMessages = utils.deepCopy(germanMessages);

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
    menu: {
        configuration: 'Konfiguration',
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
            apps: {
                title: 'Anwendungen Client',
                description: 'überprüfen und verwalten anwendungen',
                manage: 'Konten anwendungen',
            },
            accounts: {
                title: 'Konto',
                description: 'Konten überprüfen und verwalten',
                manage: 'Konten verwalten',
            },
            services: {
                title: 'Verbundene dienstleistungen',
                description: 'Dienste anzeigen und verwalten',
                manage: 'Verbindungen dienstleistungen',
            },
            authentications: {
                title: 'Authentifizierungen',
                description: 'Ihre Authentifizierungen anzeigen und bearbeiten',
                manage: 'Verwalten Sie Ihre Authentifizierungen',
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
        audit: {
            title: 'Betriebsprotokoll',
            description: 'Jüngste Kontoaktivitäten',
        },
        login: {
            header: 'Anmeldung',
            description:
                'Für den Zugriff auf die Konsole ist eine Anmeldung erforderlich',
        },
        idp: {
            import: {
                title: 'Import Provider',
                description:
                    'Stellen Sie eine gültige YAML-Datei mit der vollständigen Anbieterdefinition oder mit einer Liste gültiger Anbieter bereit, die unter Schlüsselanbietern verschachtelt ist.',
            },
        },
    },
    alert: {
        authorization_expired: 'Ihre Autorisierung ist abgelaufen',
        missing_account:
            'Sie müssen ein registriertes Konto haben, um fortzufahren',
        webauthn_unsupported:
            'Dieser Browser unterstützt keine Sicherheitsschlüssel (webauthn)',
        invalid_attestation: 'Ungültiges Zertifikat',
        missing_credentials:
            'Keine Zugangsdaten verfügbar. Registrieren Sie einen, um sich anzumelden',
    },
    action: {
        register: 'Registrieren',
        actions: 'Aktionen',
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
