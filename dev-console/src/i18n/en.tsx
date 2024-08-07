import { TranslationMessages } from 'ra-core';
import englishMessages from 'ra-language-english';
import utils from '../utils';

const raMessages = utils.deepCopy(englishMessages);

const enMessages: TranslationMessages = {
    ...raMessages,
    admin: 'Admin',
    developer: 'Developer',
    user: 'User',
    security: 'Security',
    accounts: 'Accounts',
    profiles: 'Personal Info',
    credentials: 'Credentials',
    connections: 'Connected Apps',
    languages: {
        english: 'English',
        italian: 'Italian',
        german: 'German',
        spanish: 'Spanish',
        latvian: 'Latvian',
    },
    menu: {
        configuration: 'Configuration',
    },
    resources: {
        accounts: {
            name: 'Account |||| Accounts',
        },
        credentials: {
            name: 'Credential |||| Credentials',
            fields: {
                curPassword: 'Current password',
            },
        },
        profiles: {
            name: 'Personal info',
        },
        connections: {
            name: 'Connected App |||| Connected Apps',
        },
        password: {
            details: 'A secure password helps protecting your account',
            policy: {
                description:
                    'The password must satisfy the following requisites:',
                strength: 'New password strength',
                passwordMinLength: 'Minimum length: %{value}',
                passwordMaxLength: 'Maximum length: %{value}',
                passwordRequireAlpha: 'Require letters',
                passwordRequireUppercaseAlpha: 'Require an uppercase character',
                passwordRequireNumber: 'Require a number',
                passwordRequireSpecial: 'Require a special character',
                passwordSupportWhitespace: 'Supports white-spaces',
            },
        },
        webauthn: {
            details:
                'A secure key serves as a security factor for strong authentication',
        },
    },
    page: {
        dashboard: {
            welcome: 'Welcome, %{name}',
            description:
                'Manage you personal information, accounts and review your security settings',
            apps: {
                title: 'Client Application',
                description: 'Review and manage your applications',
                manage: 'Manage applications',
            },
            accounts: {
                title: 'Accounts',
                description: 'Review and manage your accounts',
                manage: 'Manage accounts',
            },
            services: {
                title: 'Connected services',
                description: 'View and manage services',
                manage: 'Manage services',
            },
            authentications: {
                title: 'Authentications',
                description: 'View and update your authentications',
                manage: 'Manage authentications',
            },
            number: {
                apps: 'Number of client applications',
                users: 'Number of users',
                idp: 'Number of (active) providers',
                services: 'Number of custom services',
            },
        },
        app: {
            overview: {
                title: 'Overview',
            },
            list: {
                title: 'Client applications',
                subtitle: 'Manage web, mobile, server and IoT applications',
            },
            credentials: {
                title: 'Credentials',
                header: {
                    title: 'OAuth2 Credentials',
                    subtitle: 'Client credentials to authenticate with AAC.',
                },
            },
            settings: {
                title: 'Settings',
            },
            configuration: {
                title: 'OAuth2',
                header: {
                    title: 'OAuth2.0 Configuration',
                    subtitle:
                        'Basic client configuration for OAuth2/OpenId Connect',
                },
            },
        },

        attributeset: {
            list: {
                title: 'Attribute sets',
                subtitle:
                    'Register and manage custom attribute sets for users. Each custom attribute set will be available as custom profile for consumption both via profiles api and via token claims, with an associated scope profile.setidentifier.me',
            },
        },
        accounts: {
            header: 'Accounts',
            description:
                'View, manage and connect your local, social and external accounts',
            registered_user: 'Registered user',
            unregistered_user: 'Unregistered user',
            register_user: {
                title: 'Register user',
                text: 'Register a local account to persist your user and be able to login via credentials such as password, security keys etc',
                action: 'Register user',
            },
            delete_user: {
                title: 'Delete user',
                text: 'Remove your user and all the associated information',
                action: 'Delete user',
                confirm: 'Delete your user %{id}?',
                content:
                    'Are you sure you want to delete your account? This action cannot be undone!',
            },
            delete_account: {
                content:
                    'Are you sure you want to delete your account? This action cannot be undone!',
            },
            edit: {
                title: 'Update account',
                description: 'Change your account information',
            },
        },
        audit: {
            title: 'Audit',
            description: 'Recent account activity',
            list: {
                title: 'Audit events',
                subtitle: 'Review and inspect events',
            },
        },
        template: {
            title: 'Template',
            description: 'Templates',
            list: {
                title: 'Templates',
                subtitle: 'Customize appearance and messages',
            },
        },
        login: {
            header: 'Login',
            description: 'Login is required to access the console',
        },
        idp: {
            list: {
                title: 'Identity providers',
                subtitle: 'Manage identity providers',
            },
            import: {
                title: 'Import Provider',
                description:
                    'Provide a valid YAML file with the full provider definition, or with a list of valid providers nested under key providers.',
            },
            overview: {
                title: 'Overview',
            },
            settings: {
                title: 'Settings',
                basic: 'Basic',
                display: 'Display',
                advanced: 'Advanced',
            },
            configuration: {
                title: 'Configuration',
            },
            hooks: {
                title: 'Hooks',
                attribute: 'Attribute mapping',
                attributeDesc:
                    'Provide a function to transform principal attributes during login. Return a valid map with all the attributes.',
                authFunction: 'Authentication function',
                authFunctionDesc:
                    'Provide a function to evaluate an authorization policy during the login flow. Return a boolean true/false to represent a decision.',
            },
            app: {
                title: 'Applications',
            },
        },
        user: {
            list: {
                title: 'Users',
                subtitle: 'View and manage user and roles',
            },
            overview: {
                title: 'Overview',
            },
            account: {
                title: 'Account',
                subTitle: 'Manage accounts for the current user',
            },
            audit: {
                title: 'Audit',
                subTitle: 'Review audit log for the current user',
            },
            apps: {
                title: 'Apps',
                subTitle: 'Manage connected apps for the current user',
            },
            groups: {
                title: 'Groups',
                subTitle: 'Manage groups for the current user',
            },
            roles: {
                title: 'Roles',
                primaryTitle: 'Roles',
                subTitle: 'Manage roles for the current user',
                permissionTitle: 'Permissions',
                permissionSubTitle:
                    'Manage API permissions for the current user',
            },
            attributes: {
                title: 'Attributes',
                identityPrimaryTitle: 'Attributi di identità',
                identitySubTitle:
                    "Esamina gli attributi per l'utente corrente, come ricevuti dai provider di identità.",
                additionalPrimaryTitle: 'Attributi aggiuntivi',
                additionalsubTitle:
                    "samina e gestisci gli attributi aggiuntivi per l'utente corrente.",
            },
            spaceRoles: {
                title: 'Space Roles',
            },
            tos: {
                title: 'Terms of service',
                primaryTitle: 'Terms of service',
                subTitle: 'Manage terms of service flag for the current user',
            },
        },
        group: {
            list: {
                title: 'Realm groups',
                subtitle: 'Register and manage realm groups.',
            },
            overview: {
                title: 'Overview',
            },
            settings: {
                title: 'Settings',
            },
            members: {
                title: 'Members',
            },
            roles: {
                title: 'Roles',
            },
        },
    },
    alert: {
        authorization_expired: 'The authorization is expired',
        missing_account: 'You need a registered account to proceed',
        webauthn_unsupported:
            'This browser does not support security keys (webauthn)',
        invalid_attestation: 'Invalid attestation',
        missing_credentials: 'No credentials available, register one to login',
    },
    message: {
        content_copied: 'Content copied',
        content_copied_x: '%{x} copied',
    },
    action: {
        register: 'Register',
        actions: 'Actions',
        click_to_copy: 'Click to copy',
        authorities: 'Authorities',
        test: 'Test',
    },
    field: {
        username: 'Username',
        name: 'Name',
        surname: 'Surname',
        email: 'Email',
        email_verified: 'Email verified',
        id: 'id',
        given_name: 'Given Name',
        family_name: 'Family Name',
        preferred_username: 'Username',
        locale: 'Locale',
        zoneinfo: 'Timezone',
    },
    error: {
        already_registered: 'Already registered',
        authentication_service:
            'Authentication problem. Please try again later',
        bad_credentials: 'Invalid user or password',
        duplicated_data: 'Duplicated data',
        internal_error: 'Internal error. Please try again',
        invalid_data: 'Invalid field data',
        invalid_email: 'Invalid email',
        //invalid_password: 'Invalid user or password',
        invalid_password: {
            contains_whitespace: 'Invalid password: contains whitespaces',
            empty: 'Invalid password: empty password',
            max_length: 'Invalid password: too long',
            min_length: 'Invalid password: too short',
            password_reuse: 'Invalid password: password reuse',
            require_alpha: 'Invalid password: alpha char required',
            require_number: 'Invalid password: number required',
            require_uppercase_alpha:
                'Invalid password: uppercase alpha char required',
            require_special: 'Invalid password: special character required',
            not_match: 'Invalid password: the passwords do not match',
            policy: 'Invalid password: the password does not satisfy the policy',
        },
        invalid_user: 'Invalid user',
        locked: 'User locked',
        mismatch_passwords: 'Passwords does not match',
        missing_data: 'Missing data',
        not_confirmed: 'Registration not confirmed',
        not_registered: 'Unregistered',
        // registration: 'Registration problem. Please try again later',
        invalid_field: 'Invalid field data',
        unsupported_operation: 'Unsupported operation',
        wrong_password: 'Wrong password',
        unauthenticated_user: 'User must be authenticated',
        registration: {
            invalid_password: 'Invalid password',
        },
    },
};

export default enMessages;
