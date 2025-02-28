import { RJSFSchema, UiSchema } from '@rjsf/utils';

export const getIdpSchema = (schema?: any) => {
    if (!schema) {
        return {};
    }

    //fix id definition
    //TODO update in backend
    if ('id' in schema) {
        schema['$id'] = schema['id'];
        delete schema['id'];
    }

    return schema;
};

export const getIdpUiSchema = (schema?: any) => {
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

export const schemaIdpSettings: RJSFSchema = {
    type: 'object',
    properties: {
        template: {
            type: 'string',
            title: 'field.template_override.name',
            description: 'field.template_override.helperText',
        },
        logo: {
            type: 'string',
            title: 'field.logo.name',
            description: 'field.logo.helperText',
        },        
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
        position: {
            type: ['number', 'null'],
            default: null,
            title: 'field.position.name',
            description: 'field.position.helperText',
        },
        linkable: {
            type: 'boolean',
            title: 'field.linkable.name',
            description: 'field.linkable.helperText',
        },
        notes: {
            type: 'string',
            title: 'field.notes.name',
            description: 'field.notes.helperText',
        },
    },
};

export const uiSchemaIdpSettings: UiSchema = {
    'ui:layout': [12],
};

export const uiSchemaInternalIdp: UiSchema = {
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
export const uiSchemaAppleIdp: UiSchema = {
    'ui:layout': [6, 6, 12, 12, 4, 4, 4],
    privateKey: {
        'ui:widget': 'textarea',
    },
};
export const uiSchemaOidcIdp: UiSchema = {
    'ui:layout': [
        12, 12, 12, 12, 6, 6, 12, 6, 6, 4, 4, 4, 12, 12, 12, 12, 12, 6, 6,
    ],
    clientJwk: {
        'ui:widget': 'textarea',
    },
};
export const uiSchemaPasswordIdp: UiSchema = {
    'ui:layout': [12, 12, 12, 6, 6, 6, 6, 6, 4, 4, 4, 4, 4, 6, 6],
    'ui:order': [
        'displayAsButton',
        'requireAccountConfirmation',
        'maxSessionDuration',
        'repositoryId',
        'enablePasswordReset',
        'passwordResetValidity',
        'passwordMinLength',
        'passwordMaxLength',
        'passwordRequireAlpha',
        'passwordRequireUppercaseAlpha',
        'passwordRequireNumber',
        'passwordRequireSpecial',
        'passwordSupportWhitespace',
        'passwordKeepNumber',
        'passwordMaxDays',
    ],
    repositoryId: {
        'ui:widget': 'hidden',
    },
};

export const uiSchemaWebAuthnIdp: UiSchema = {
    'ui:layout': [12],
};

export const uiSchemaSamlIdp: UiSchema = {
    'ui:layout': [
        12, 6, 6, 6, 6, 12, 12, 6, 6, 6, 6, 12, 6, 6, 8, 4, 8, 12, 4, 4, 4, 6,
        6,
    ],
    'ui:order': [
        'entityId',
        'signingKey',
        'signingCertificate',
        'cryptKey',
        'cryptCertificate',
        'idpMetadataUrl',
        'idpEntityId',
        'webSsoUrl',
        'webLogoutUrl',

        'ssoServiceBinding',
        'signAuthNRequest',

        'verificationCertificate',
        'forceAuthn',
        'isPassive',

        'nameIDFormat',
        'nameIDAllowCreate',

        'authnContextClasses',
        'authnContextComparison',

        'trustEmailAddress',
        'alwaysTrustEmailAddress',
        'requireEmailAddress',

        'userNameAttributeName',
        'subAttributeName',
    ],
    signingKey: {
        'ui:widget': 'textarea',
    },
    signingCertificate: {
        'ui:widget': 'textarea',
    },
    cryptKey: {
        'ui:widget': 'textarea',
    },
    cryptCertificate: {
        'ui:widget': 'textarea',
    },
    authnContextClasses: {
        items: {
            'ui:label': false,
        },
    },
};

export const uiSchemaOpenidfedIdp: UiSchema = {
    'ui:layout': [
        6, 6, 12, 12, 12, 12, 4, 4, 4, 12, 12, 12, 12, 12, 12, 12, 12, 12, 6, 6,
        6, 6,
    ],
    'ui:order': [
        'clientId',
        'clientName',
        'clientJwks',
        'federationJwks',

        'scope',
        'claims',

        'trustEmailAddress',
        'requireEmailAddress',
        'alwaysTrustEmailAddress',

        'organizationName',
        'contacts',
        'logoUri',
        'policyUri',
        'homepageUri',
      
        'trustAnchor',
        'providers',
        'authorityHints',
        'trustMarks',

        'acrValues',
        'respectTokenExpiration',
        'promptMode',
        'userNameAttributeName',
        'subjectType',
        'userInfoJWEAlg',
        'userInfoJWEEnc',
    ],
    clientJwks: {
        'ui:widget': 'textarea',
    },
    federationJwks: {
        'ui:widget': 'textarea',
    },
};
export const uiSchemaSpidIdp: UiSchema = {
    'ui:layout': [12, 6, 6, 6, 6, 12, 6, 6, 6, 6, 12, 12, 12, 12, 6, 6],
    'ui:order': [
        'entityId',
        'signingKey',
        'signingCertificate',
        'organizationDisplayName',
        'organizationName',
        'organizationUrl',
        'contactPerson_EmailAddress',
        'contactPerson_Type',
        'contactPerson_IPACode',
        'contactPerson_Public',
        'idps',
        'idpMetadataUrl',
        'spidAttributes',
        'authnContext',
        'subAttributeName',
        'usernameAttributeName',
    ],
    signingKey: {
        'ui:widget': 'textarea',
    },
    signingCertificate: {
        'ui:widget': 'textarea',
    },
    spidAttributes: {
        items: {
            'ui:label': false,
        },
    },
    authnContext: {
        'ui:title': 'field.spidLevel.name',
        'ui:description': 'field.spidLevel.helperText',
    },
};
//list

export const idpUiSchemas = {
    'urn:jsonschema:it:smartcommunitylab:aac:internal:provider:InternalIdentityProviderConfigMap':
        uiSchemaInternalIdp,
    'urn:jsonschema:it:smartcommunitylab:aac:password:provider:PasswordIdentityProviderConfigMap':
        uiSchemaPasswordIdp,
    'urn:jsonschema:it:smartcommunitylab:aac:oidc:apple:provider:AppleIdentityProviderConfigMap':
        uiSchemaAppleIdp,
    'urn:jsonschema:it:smartcommunitylab:aac:oidc:provider:OIDCIdentityProviderConfigMap':
        uiSchemaOidcIdp,
    'urn:jsonschema:it:smartcommunitylab:aac:openidfed:provider:OpenIdFedIdentityProviderConfigMap':
        uiSchemaOpenidfedIdp,
    'urn:jsonschema:it:smartcommunitylab:aac:webauthn:provider:WebAuthnIdentityProviderConfigMap':
        uiSchemaWebAuthnIdp,
    'urn:jsonschema:it:smartcommunitylab:aac:saml:provider:SamlIdentityProviderConfigMap':
        uiSchemaSamlIdp,
    'urn:jsonschema:it:smartcommunitylab:aac:spid:provider:SpidIdentityProviderConfigMap':
        uiSchemaSpidIdp,
};
