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
            title: 'Redirect uris',
            items: {
                type: 'string',
                default: 'http://localhost',
            },
        },
        applicationType: {
            title: 'Application Type ',
            type: 'string',
            oneOf: [
                {
                    const: 'web',
                    title: 'Web',
                },
                {
                    const: 'native',
                    title: 'Native',
                },
                {
                    const: 'machine',
                    title: 'Machine',
                },
                {
                    const: 'spa',
                    title: 'SPA',
                },
                {
                    const: 'introspection',
                    title: 'Introspection',
                },
            ],
        },
        firstParty: {
            type: 'boolean',
            title: 'Configuration first party',
        },
        idTokenClaims: {
            type: 'boolean',
            title: 'Configuration id token claims',
        },
        refreshTokenRotation: {
            type: 'boolean',
            title: 'Configuration refresh token rotation',
        },
        subjectType: {
            title: 'Subject type ',
            type: 'string',
            oneOf: [
                {
                    const: 'public',
                    title: 'Public',
                },
                {
                    const: 'pairwise',
                    title: 'Pairwise',
                },
            ],
        },
        tokenType: {
            title: 'Token type ',
            type: 'string',
            oneOf: [
                {
                    const: 'jwt',
                    title: 'JWT',
                },
                {
                    const: 'opaque',
                    title: 'Opaque',
                },
            ],
        },
        accessTokenValidity: {
            title: 'Acccess token validity ',
            type: 'number',
        },
        refreshTokenValidity: {
            title: 'Refresh token validity ',
            type: 'number',
        },
    },
};
export const schemaConfIdp: UiSchema = {
    // embedded nel componente
};

export const uiSchemaOAuthClient: UiSchema = {
    'ui:layout': [4, 4, 4]
};
export const uiSchemaInternalIdp: UiSchema = {
    'ui:layout': [12],
    'ui:order': [
        'confirmationRequired',
        'confirmationValidity',
        'enableDelete',
        'enableRegistration',
        'enableUpdate',
        'maxSessionDuration'

    ],
};
export const uiSchemaOIDCIdp: UiSchema = {
    // 'ui:layout':[4,4,4] da fare a mano
};

export const getUiSchema = (type: string) => {
    if (uiIdpSchema[type])
        return uiIdpSchema[type];
    else return {};
}
export const uiIdpSchema = {
    'urn:jsonschema:it:smartcommunitylab:aac:internal:provider:InternalIdentityProviderConfigMap': uiSchemaInternalIdp,
    'urn:jsonschema:it:smartcommunitylab:aac:oidc:provider:OIDCIdentityProviderConfigMap': uiSchemaOIDCIdp,
};
