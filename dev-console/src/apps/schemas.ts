import { RJSFSchema, UiSchema } from '@rjsf/utils';

export const schemaOAuthClient: RJSFSchema = {
    type: 'object',
    properties: {
        authenticationMethods: {
            type: 'array',
            uniqueItems: true,
            items: {
                type: 'string',
                enum: [
                    'client_secret_post',
                    'private_key_jwt',
                    'client_secret_basic',
                    'client_secret_jwt',
                    'none',
                ],
            },
        },
        authorizedGrantTypes: {
            type: 'array',
            uniqueItems: true,
            items: {
                type: 'string',
                enum: [
                    'authorization_code',
                    'implicit',
                    'refresh_token',
                    'password',
                    'client_credentials',
                ],
            },
        },
        redirectUris: {
            type: 'array',

            items: {
                type: 'string',
            },
        },
        applicationType: {
            type: 'string',
            enum: ['web', 'native', 'machine', 'spa', 'introspection'],
        },
        firstParty: {
            type: 'boolean',
        },
        idTokenClaims: {
            type: 'boolean',
        },
        refreshTokenRotation: {
            type: 'boolean',
        },
        subjectType: {
            type: 'string',
            enum: ['public', 'pairwise'],
        },
        tokenType: {
            type: 'string',
            enum: ['jwt', 'opaque'],
        },
        accessTokenValidity: {
            type: ['number', 'null'],
            default: null,
        },
        refreshTokenValidity: {
            type: ['number', 'null'],
            default: null,
        },
    },
};

export const uiSchemaOAuthClient: UiSchema = {
    'ui:order': [
        'applicationType',
        'authenticationMethods',
        'authorizedGrantTypes',
        'redirectUris',
        'firstParty',
        'idTokenClaims',
        'refreshTokenRotation',
        'subjectType',
        'tokenType',
        'accessTokenValidity',
        'refreshTokenValidity',
    ],
    'ui:layout': [12, 12, 12, 12, 4, 4, 4, 6, 6, 6, 6],
    redirectUris: {
        items: {
            'ui:label': false,
        },
    },
};

export const schemaWebHooks: RJSFSchema = {
    type: 'object',
    properties: {
        beforeUserApproval: {
            type: ['string', 'null'],
            format: 'url',
        },
        afterUserApproval: {
            type: ['string', 'null'],
            format: 'url',
        },
        beforeTokenGrant: {
            type: ['string', 'null'],
            format: 'url',
        },
        afterTokenGrant: {
            type: ['string', 'null'],
            format: 'url',
        },
    },
};

export const claimMappingDefaultValue =
    'LyoqCiAqIERFRklORSBZT1VSIE9XTiBDTEFJTSBNQVBQSU5HIEhFUkUKKiovCmZ1bmN0aW9uIGNsYWltTWFwcGluZyhjbGFpbXMpIHsKICAgcmV0dXJuIGNsYWltczsKfQo=';
