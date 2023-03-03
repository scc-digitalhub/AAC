import { TranslationMessages } from 'ra-core';
import englishMessages from 'ra-language-english';
import utils from '../utils';

const raMessages = utils.deepCopy(englishMessages);
raMessages.ra.page.dashboard = 'Home';

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
            accounts: {
                title: 'Accounts',
                description: 'Review and manage your accounts',
                manage: 'Manage accounts',
            },
            connections: {
                title: 'Connected applications',
                description: 'View and manage 3rd party connected applications',
                manage: 'Manage connections',
            },
            credentials: {
                title: 'Credentials',
                description: 'View and update your credentials',
                manage: 'Manage credentials',
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
        profiles: {
            header: 'Personal info',
            description:
                'Review and inspect your personal information, as available to applications and services',
        },
        connections: {
            header: 'Connected applications',
            description:
                'Manage and control application permissions. You gave access to some of your personal information to applications and sites. Review and remove access to applications you no longer use or trust',
            permissions_num: 'Has access to %{num} permissions',
        },
        credentials: {
            header: 'Credentials',
            description:
                'Review, update, reset and delete the security credentials associated to your account',
        },
        password: {
            title: 'Manage password',
            subtitle: 'Set, change and rotate your personal password for login',
            edit: {
                title: 'Update password',
                description: 'Change your old password and set a new one',
            },
            delete: {
                content:
                    "Are you sure you want to delete your password? You won't be able to login via password",
            },
        },
        webauthn: {
            title: 'Manage security keys',
            subtitle:
                'Register and use security keys such as your phone or a portable key',
            create: {
                title: 'Register a security key',
                description:
                    'Associate a local or roaming security key to your personal account',
            },
            edit: {
                title: 'Update a security key',
                description:
                    'Update the security key registration by changing the properties',
            },
            delete: {
                content:
                    'Are you sure you want to delete the key? This operation can not be undone',
            },
        },
        security: {
            header: 'Security',
            description: 'View your security settings and review the audit',
        },
        audit: {
            title: 'Audit',
            description: 'Recent security activity',
        },
        login: {
            header: 'Login',
            description: 'Login is required to access the console',
        },
    },
    alert: {
        authorization_expired: 'The authorization is expired',
        missing_account: 'You need a registered account to proceed',
        webauthn_unsupported:
            'This browser does not support security keys (webauthn)',
        invalid_attestation: 'Invalid attestation',
    },
    action: {
        register: 'Register',
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
