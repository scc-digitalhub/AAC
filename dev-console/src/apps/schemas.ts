import { RJSFSchema, UiSchema } from '@rjsf/utils';

export const schemaOAuthClient: RJSFSchema = {
    type: 'object',
    properties: {
        authenticationMethods: {
            title: 'field.oauth2.authenticationMethods.name',
            description: 'field.oauth2.authenticationMethods.helperText',
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
            title: 'field.oauth2.authorizedGrantTypes.name',
            description: 'field.oauth2.authorizedGrantTypes.helperText',
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
            title: 'field.oauth2.redirectUris.name',
            description: 'field.oauth2.redirectUris.helperText',
            type: 'array',

            items: {
                type: 'string',
                format: 'uri',
            },
        },
        applicationType: {
            title: 'field.oauth2.applicationType.name',
            description: 'field.oauth2.applicationType.helperText',
            type: 'string',
            enum: ['web', 'native', 'machine', 'spa', 'introspection'],
        },
        firstParty: {
            title: 'field.oauth2.firstParty.name',
            description: 'field.oauth2.firstParty.helperText',
            type: 'boolean',
        },
        idTokenClaims: {
            title: 'field.oauth2.idTokenClaims.name',
            description: 'field.oauth2.idTokenClaims.helperText',
            type: 'boolean',
        },
        refreshTokenRotation: {
            title: 'field.oauth2.refreshTokenRotation.name',
            description: 'field.oauth2.refreshTokenRotation.helperText',
            type: 'boolean',
        },
        subjectType: {
            title: 'field.oauth2.subjectType.name',
            description: 'field.oauth2.subjectType.helperText',
            type: 'string',
            enum: ['public', 'pairwise'],
        },
        tokenType: {
            title: 'field.oauth2.tokenType.name',
            description: 'field.oauth2.tokenType.helperText',
            type: 'string',
            enum: ['jwt', 'opaque'],
        },
        accessTokenValidity: {
            title: 'field.oauth2.accessTokenValidity.name',
            description: 'field.oauth2.accessTokenValidity.helperText',
            type: ['number', 'null'],
            default: null,
        },
        idTokenValidity: {
            title: 'field.oauth2.idTokenValidity.name',
            description: 'field.oauth2.idTokenValidity.helperText',
            type: ['number', 'null'],
            default: null,
        },
        refreshTokenValidity: {
            title: 'field.oauth2.refreshTokenValidity.name',
            description: 'field.oauth2.refreshTokenValidity.helperText',
            type: ['number', 'null'],
            default: null,
        },
        response_types: {
            title: 'field.oauth2.responseTypes.name',
            description: 'field.oauth2.responseTypes.helperText',
            type: 'array',
            uniqueItems: true,
            items: {
                type: 'string',
                enum: ['code', 'token', 'id_token', 'none'],
            },
        },
    },
};

export const uiSchemaOAuthClient: UiSchema = {
    'ui:order': [
        'applicationType',
        'authenticationMethods',
        'authorizedGrantTypes',
        'redirectUris',
        'response_types',
        'firstParty',
        'idTokenClaims',
        'refreshTokenRotation',
        'subjectType',
        'tokenType',
        'accessTokenValidity',
        'idTokenValidity',
        'refreshTokenValidity',
    ],
    'ui:layout': [12, 12, 12, 12, 12, 4, 4, 4, 6, 6, 4, 4, 4],
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
            title: 'field.webhooks.beforeUserApproval.name',
            description: 'field.webhooks.beforeUserApproval.helperText',
        },
        afterUserApproval: {
            type: ['string', 'null'],
            format: 'url',
            title: 'field.webhooks.afterUserApproval.name',
            description: 'field.webhooks.afterUserApproval.helperText',
        },
        beforeTokenGrant: {
            type: ['string', 'null'],
            format: 'url',
            title: 'field.webhooks.beforeTokenGrant.name',
            description: 'field.webhooks.beforeTokenGrant.helperText',
        },
        afterTokenGrant: {
            type: ['string', 'null'],
            format: 'url',
            title: 'field.webhooks.afterTokenGrant.name',
            description: 'field.webhooks.afterTokenGrant.helperText',
        },
    },
};

export const claimMappingDefaultValue =
    'LyoqCiAqIERFRklORSBZT1VSIE9XTiBDTEFJTSBNQVBQSU5HIEhFUkUKKiovCmZ1bmN0aW9uIGNsYWltTWFwcGluZyhjbGFpbXMpIHsKICAgcmV0dXJuIGNsYWltczsKfQo=';
