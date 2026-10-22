import { RJSFSchema, UiSchema } from '@rjsf/utils';

const fixIdsRecursive = (obj: any) => {
    if (obj && typeof obj === "object") {
        if ('id' in obj && typeof obj['id'] === 'string' && obj['id'].startsWith("urn:jsonschema:")) {
            obj['$id'] = obj['id'];
            delete obj['id'];
        }
        for (const key in obj) {
            fixIdsRecursive(obj[key]);
        }
    }
    return obj;
};

export const getIdpSchema = (schema?: any) => {
    if (!schema) {
        return {};
    }

    //fix id definition
    //TODO update in backend
    return fixIdsRecursive(schema);
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
    notes: {
        'ui:widget': 'textarea',
    },
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
        12, 12, 12, 12, 6, 6, 12, 6, 6, 4, 4, 4, 12, 12, 12, 12, 12, 6, 6, 12, 12
    ],
    clientJwk: {
        'ui:widget': 'textarea',
    },
    customAuthNParameters: {
        items: {
            'ui:layout': [6, 6],
            'ui:order': ['name', 'value'],
            name: {
                'ui:title': 'field.customAuthNParameters.parameterName.name',
                'ui:description': 'field.customAuthNParameters.parameterName.helperText',
            },
            value: {
                'ui:title': 'field.customAuthNParameters.parameterValue.name',
                'ui:description': 'field.customAuthNParameters.parameterValue.helperText',
            },
        },
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
        12, 6, 6, 12, 12, 12, 12, 6, 6, 6, 6, 12, 6, 6, 8, 4, 8, 12, 4, 4, 4, 6,
        6,
    ],
    'ui:order': [
        'entityId',
        'signingKey',
        'signingCertificate',
        'signingCredentials',
        'activeSigningCredentialId',
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
    authnContextClasses: {
        items: {
            'ui:label': false,
        },
    },
    signingKey: {
        'ui:widget': 'textarea',
        'ui:title': 'field.signingKey.name',
        'ui:description': 'field.signingKey.helperText',
    },
    signingCertificate: {
        'ui:widget': 'textarea',
        'ui:title': 'field.signingCertificate.name',
        'ui:description': 'field.signingCertificate.helperText',
    },
    signingCredentials: {
        items: {
            'ui:layout': [12, 6, 6],
            'ui:order': ['credentialId', 'signingKey', 'signingCertificate'],
            credentialId: {
                'ui:title': 'field.credentialId.name',
                'ui:description': 'field.credentialId.helperText',
            },
            signingKey: {
                "ui:widget": "textarea",
                'ui:title': 'field.signingKey.name',
                'ui:description': 'field.signingKey.helperText',
            },
            signingCertificate: {
                "ui:widget": "textarea",
                'ui:title': 'field.signingCertificate.name',
                'ui:description': 'field.signingCertificate.helperText',
            },
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
    'ui:layout': [6, 6, 12, 12, 12, 12, 12, 12, 4, 4, 4, 6, 6, 12, 12, 12, 6, 6, 12, 12, 6, 6],
    'ui:order': [
        'signingKey',
        'signingCertificate',
        'signingCredentials',
        'activeAuthRequestSigningCredentialId',
        'activeMetadataSigningCredentialId',
        'metadataUrl',
        'metadataXML',
        'entityId',
        'organizationName',
        'organizationDisplayName',
        'organizationUrl',
        'contactPersonEmailAddress',
        'contactPersonIPACode',
        'spidAttributes',
        'idps',
        'idpMetadataUrl',
        'useAssertionConsumerServiceUrl',
        'attributeConsumingServiceIndex',
        'authnContext',
        'purpose',
        'subAttributeName',
        'usernameAttributeName',
    ],
    metadataXML: {
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
    purpose: {
        'ui:title': 'field.purpose.name',
        'ui:description': 'field.purpose.helperText',
    },
    signingKey: {
        "ui:widget": "textarea",
        'ui:title': 'field.signingKey.name',
        'ui:description': 'field.signingKey.helperText',
    },
    signingCertificate: {
        "ui:widget": "textarea",
        'ui:title': 'field.signingCertificate.name',
        'ui:description': 'field.signingCertificate.helperText',
    },
    signingCredentials: {
        items: {
            'ui:layout': [12, 6, 6],
            'ui:order': ['credentialId', 'signingKey', 'signingCertificate'],
            credentialId: {
                'ui:title': 'field.credentialId.name',
                'ui:description': 'field.credentialId.helperText',
            },
            signingKey: {
                "ui:widget": "textarea",
                'ui:title': 'field.signingKey.name',
                'ui:description': 'field.signingKey.helperText',
            },
            signingCertificate: {
                "ui:widget": "textarea",
                'ui:title': 'field.signingCertificate.name',
                'ui:description': 'field.signingCertificate.helperText',
            },
        },
    },
};

// Form del provider eidas. Oggi i campi sono gli stessi di SPID, perche' la config map eidas
// eredita quella SPID, ma lo uiSchema e' autonomo: aggiungere o togliere impostazioni qui non
// tocca il form SPID, e viceversa.
//
// Nota per le modifiche: 'ui:order' e 'ui:layout' sono paralleli per indice (il secondo da' la
// larghezza in griglia del campo che il primo elenca nella stessa posizione), e 'ui:order' deve
// nominare tutte le proprieta' dello schema. Un campo aggiunto va messo in entrambi; un campo da
// nascondere resta in entrambi e prende "ui:widget": "hidden".
//
// 'idps' non compare: la config map eidas lo esclude dallo schema, perche' verrebbe comunque
// ignorato (il nodo si dichiara con idpMetadataUrl).
//
// 'eidasAttributes' sostituisce 'spidAttributes': catalogo proprio del modulo eidas (EIDASAttribute),
// stessi nomi SPID ma solo gli attributi che un nodo rilascia; i quattro del minimum data set
// (spidCode, name, familyName, dateOfBirth) sono il minimo richiesto, il backend lo verifica.
//
// 'authnContext' non ha una sua chiave in i18n/fields.json ed e' rimappato su field.spidLevel:
// il valore sul filo e' davvero un SpidL1/2/3, perche' l'authority eidas gira sullo stack SPID.
export const uiSchemaEidasIdp: UiSchema = {
    'ui:layout': [6, 6, 12, 12, 12, 12, 12, 12, 4, 4, 4, 6, 6, 12, 12, 12, 12, 6, 6, 12, 6, 6],
    'ui:order': [
        'signingKey',
        'signingCertificate',
        'signingCredentials',
        'activeAuthRequestSigningCredentialId',
        'activeMetadataSigningCredentialId',
        'metadataUrl',
        'metadataXML',
        'entityId',
        'organizationName',
        'organizationDisplayName',
        'organizationUrl',
        'contactPersonEmailAddress',
        'contactPersonIPACode',
        'eidasAttributes',
        'idpMetadataUrl',
        'useAssertionConsumerServiceUrl',
        'attributeConsumingServiceIndex',
        'authnContext',
        'subAttributeName',
        'usernameAttributeName',
        // ogni proprieta' ereditata da SPID in futuro (es. assertionConsumerServiceUrlFormat) si
        // renderizza in coda invece di rompere il form: RJSF rifiuta un ui:order incompleto
        '*',
    ],
    metadataXML: {
        'ui:widget': 'textarea',
    },
    eidasAttributes: {
        'ui:title': 'field.eidasAttributes.name',
        'ui:description': 'field.eidasAttributes.helperText',
        items: {
            'ui:label': false,
        },
    },
    authnContext: {
        'ui:title': 'field.spidLevel.name',
        'ui:description': 'field.spidLevel.helperText',
    },
    subAttributeName: {
        'ui:title': 'field.eidasSubAttributeName.name',
        'ui:description': 'field.eidasSubAttributeName.helperText',
    },
    usernameAttributeName: {
        'ui:title': 'field.eidasUsernameAttributeName.name',
        'ui:description': 'field.eidasUsernameAttributeName.helperText',
    },
    signingKey: {
        'ui:widget': 'textarea',
        'ui:title': 'field.signingKey.name',
        'ui:description': 'field.signingKey.helperText',
    },
    signingCertificate: {
        'ui:widget': 'textarea',
        'ui:title': 'field.signingCertificate.name',
        'ui:description': 'field.signingCertificate.helperText',
    },
    signingCredentials: {
        items: {
            'ui:layout': [12, 6, 6],
            'ui:order': ['credentialId', 'signingKey', 'signingCertificate'],
            credentialId: {
                'ui:title': 'field.credentialId.name',
                'ui:description': 'field.credentialId.helperText',
            },
            signingKey: {
                'ui:widget': 'textarea',
                'ui:title': 'field.signingKey.name',
                'ui:description': 'field.signingKey.helperText',
            },
            signingCertificate: {
                'ui:widget': 'textarea',
                'ui:title': 'field.signingCertificate.name',
                'ui:description': 'field.signingCertificate.helperText',
            },
        },
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
    'urn:jsonschema:it:smartcommunitylab:aac:eidas:provider:EIDASIdentityProviderConfigMap':
        uiSchemaEidasIdp,
};
