import { RJSFSchema, UiSchema } from '@rjsf/utils';

export const uiSchemaInternalIdp: UiSchema = {
    'ui:layout': [12],
    'ui:order': [
        'confirmationRequired',
        'confirmationValidity',
        'enableDelete',
        'enableRegistration',
        'enableUpdate',
        'maxSessionDuration',
    ],
};
export const uiSchemaOIDCIdp: UiSchema = {
    // 'ui:layout':[4,4,4] da fare a mano
};

export const getUiSchema = (type: string) => {
    if (uiIdpSchema[type]) return uiIdpSchema[type];
    else uiIdpSchema['default'];
};
export const uiIdpSchema = {
    default: {},
    'urn:jsonschema:it:smartcommunitylab:aac:internal:provider:InternalIdentityProviderConfigMap':
        uiSchemaInternalIdp,
    'urn:jsonschema:it:smartcommunitylab:aac:oidc:provider:OIDCIdentityProviderConfigMap':
        uiSchemaOIDCIdp,
};
