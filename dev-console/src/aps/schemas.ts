import { RJSFSchema, UiSchema } from '@rjsf/utils';

export const getApSchema = (schema?: any) => {
    if (!schema) {
        return {};
    }

    //fix id definition
    //TODO update in backend
    if ('id' in schema) {
        schema['$id'] = schema['id'];
        delete schema['id'];
    }

    //fix definition
    if (
        schema['$id'] ===
        'urn:jsonschema:it:smartcommunitylab:aac:attributes:provider:MapperAttributeProviderConfigMap'
    ) {
        schema['properties']['type']['enum'] = ['default', 'exact'];
    }

    return schema;
};

export const getApUiSchema = (schema?: any) => {
    if (!schema || (!('$id' in schema) && !('id' in schema))) {
        return {};
    }

    const type = schema['$id'] || schema['id'];
    let uiSchema = {};
    if (type in idpUiSchemas) {
        uiSchema = { ...idpUiSchemas[type] };
    }

    //inject titles if missing
    if (schema) {
        for (const p in schema.properties) {
            if (!(p in uiSchema)) {
                uiSchema[p] = {};
            }

            if (!('ui:title' in uiSchema[p])) {
                uiSchema[p]['ui:title'] =
                    schema.properties[p].title || 'field.' + p + '.name';
            }
            if (!('ui:description' in uiSchema[p])) {
                uiSchema[p]['ui:description'] =
                    schema.properties[p].description ||
                    'field.' + p + '.helperText';
            }
        }
    }

    return uiSchema;
};

export const schemaApSettings: RJSFSchema = {
    type: 'object',
    properties: {
        events: {
            type: 'string',
            enum: ['none', 'minimal', 'details', 'full'],
            default: 'minimal',
            title: 'field.events.name',
            description: 'field.events.helperText',
        },
        persistence: {
            type: 'string',
            enum: ['none', 'repository', 'session'],
            default: 'repository',
            title: 'field.persistence.name',
            description: 'field.persistence.helperText',
        },
        notes: {
            type: 'string',
            title: 'field.notes.name',
            description: 'field.notes.helperText',
        },
    },
};

export const uiSchemaApSettings: UiSchema = {
    'ui:layout': [12],
};

export const uiSchemaInternalAp: UiSchema = {
    'ui:layout': [4, 4, 4],
    'ui:order': [
        'enableRegistration',
        'enableDelete',
        'enableUpdate',
        'maxSessionDuration',
        'confirmationRequired',
        'confirmationValidity',
    ],
};
export const uiSchemaScriptAp: UiSchema = {
    code: {
        'ui:widget': 'hidden',
    },
};
//list

export const idpUiSchemas = {
    'urn:jsonschema:it:smartcommunitylab:aac:attributes:provider:ScriptAttributeProviderConfigMap':
        uiSchemaScriptAp,
    // 'urn:jsonschema:it:smartcommunitylab:aac:internal:provider:InternalIdentityProviderConfigMap':
    //     uiSchemaInternalIdp,
    // 'urn:jsonschema:it:smartcommunitylab:aac:password:provider:PasswordIdentityProviderConfigMap':
    //     uiSchemaPasswordIdp,
    // 'urn:jsonschema:it:smartcommunitylab:aac:oidc:apple:provider:AppleIdentityProviderConfigMap':
    //     uiSchemaAppleIdp,
    // 'urn:jsonschema:it:smartcommunitylab:aac:oidc:provider:OIDCIdentityProviderConfigMap':
    //     uiSchemaOidcIdp,
    // 'urn:jsonschema:it:smartcommunitylab:aac:openidfed:provider:OpenIdFedIdentityProviderConfigMap':
    //     uiSchemaOpenidfedIdp,
    // 'urn:jsonschema:it:smartcommunitylab:aac:webauthn:provider:WebAuthnIdentityProviderConfigMap':
    //     uiSchemaWebAuthnIdp,
    // 'urn:jsonschema:it:smartcommunitylab:aac:saml:provider:SamlIdentityProviderConfigMap':
    //     uiSchemaSamlIdp,
    // 'urn:jsonschema:it:smartcommunitylab:aac:spid:provider:SpidIdentityProviderConfigMap':
    //     uiSchemaSpidIdp,
};
