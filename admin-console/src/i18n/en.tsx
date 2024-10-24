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
    realms: 'Realms',

    resources: {
        realms: {
            name: 'Realm |||| Realms',
            fields: {},
            helperText: {
                slug: 'A slug is the addressable identifier for the realm, used for URLs, redirects, etc. ',
                name: 'The user-facing identifier for the realm',
                public: 'When set, the realm will be listed on public pages',
            },
        },
    },
    page: {
        dashboard: {
            welcome: 'Admin console',
            description:
                'Manage the server and the global settings, along with realm registrations',
            realms: {
                title: 'Realms',
                description: 'View and manage realms',
                manage: 'Manage realms',
            },
            appProps: {
                title: 'Application properties',
                description: 'Current properties from config',
            },
        },
        realms: {
            header: 'Realms',
            description: 'View, manage and delete realms',
            create: {
                title: 'Add realm',
                description: 'Create the registration for a new realm',
            },
            edit: {
                title: 'Edit realm',
                description: 'Modify registration properties for realm',
            },
        },
        login: {
            header: 'Login',
            description: 'Login is required to access the console',
        },
    },
    alert: {},
    action: {},
    field: {
        name: 'Name',
        slug: 'Slug',
        id: 'id',
        email: 'Email',
        url: 'URL',
        logo: 'Logo',
        lang: 'Language',
        footer: 'Footer',
    },
    error: {
        already_registered: 'Already registered',
        authentication_service:
            'Authentication problem. Please try again later',
        bad_credentials: 'Invalid user or password',
        duplicated_data: 'Duplicated data',
        internal_error: 'Internal error. Please try again',
        invalid_data: 'Invalid field data',
        missing_data: 'Missing data',
        invalid_field: 'Invalid field data',
        unsupported_operation: 'Unsupported operation',
        wrong_password: 'Wrong password',
        unauthenticated_user: 'User must be authenticated',
        invalid_slug:
            'Invalid slug: only alphanumeric characters (plus -) are allowed',
    },
};

export default enMessages;
