export const pages = {
    dashboard: {
        description: 'Manage apps, users and services',
        logins_7_days: 'Last 7 days logins',
        registrations_7_days: 'Last 7 days registrations',
        tokens_7_days: 'Last 7 days tokens',
    },
    myrealms: {
        name: 'Realm |||| Realms',
    },
    apps: {
        list: {
            title: 'Client applications',
            subtitle: 'Manage web, mobile, server and IoT applications.',
        },
    },
    groups: {
        list: {
            title: 'Groups',
            subtitle: 'Register and manage realm groups.',
        },
    },
    roles: {
        list: {
            title: 'Roles',
            subtitle: 'Register and manage realm roles.',
        },
    },
    users: {
        list: {
            title: 'Users',
            subtitle: 'View and manage user, assign roles and groups',
        },
    },
    idps: {
        list: {
            title: 'Identity providers',
            subtitle:
                'Register and manage identity providers for authentication and identification.',
        },
    },
    aps: {
        list: {
            title: 'Attribute providers',
            subtitle: 'Register and manage attribute providers.',
        },
    },
    resources: {
        list: {
            title: 'API resources',
            subtitle: 'View and inspect resources and scopes.',
        },
    },
    audit: {
        list: {
            title: 'Audit events',
            subtitle:
                'Review and inspect events for authentication, access and token usage.',
        },
    },
    services: {
        list: {
            title: 'API services',
            subtitle: 'Register and manage custom services for APIs.',
        },
    },
    attributeset: {
        list: {
            title: 'Attribute sets',
            subtitle:
                'Register and manage custom attribute sets for users. Each custom attribute set will be available as custom profile for consumption both via profiles api and via token claims, with an associated scope `profile.setidentifier.me`',
        },
    },
    scopes: {
        list: {
            title: 'Scopes',
            subtitle: 'View and inspect resources and scopes.',
        },
    },
};

export const tabs = {
    overview: 'Overview',
    settings: 'Settings',
    configuration: 'Configuration',
    credentials: 'Credentials',
    audit: 'Audit',
    api_access: 'Api access',
    roles: 'Roles',
    groups: 'Groups',
    users: 'Users',
    members: 'Members',
    subjects: 'Subjects',
    providers: 'Providers',
    attributeSets: 'Attribute sets',
    scopes: 'Scopes',
    claims: 'Claims',
    accounts: 'Accounts',
    apps: 'Client apps',
    attributes: 'Attributes',
    test: 'Test',
    hooks: 'Hooks',
    tos: 'Terms of service',
    permissions: 'Permissions',
    endpoints: 'Endpoints',
    localization: 'Localization',
    templates: 'Templates',
    oauth2: 'OAuth2',
    developers: 'Developers',
};

export default pages;
