import { RJSFSchema, UiSchema } from '@rjsf/utils';

export const realmLocalizationSchema: RJSFSchema = {
    type: 'object',
    properties: {
        languages: {
            type: 'array',
            uniqueItems: true,
            items: {
                type: 'string',
                enum: ['en', 'it', 'de', 'es', 'lv'],
            },
            default: [],
            title: 'field.languages.name',
            description: 'field.languages.helperText',
        },
    },
};

export const realmTemplatesSchema: RJSFSchema = {
    type: 'object',
    properties: {
        customStyle: {
            type: ['string', 'null'],
            default: null,
            title: 'field.customStyle.name',
            description: 'field.customStyle.helperText',
        },
    },
};

export const realmOAuthSchema: RJSFSchema = {
    type: 'object',
    properties: {
        enableClientRegistration: {
            type: 'boolean',
            title: 'field.oauth2.enableClientRegistration.name',
            description: 'field.oauth2.enableClientRegistration.helperText',
        },
        openClientRegistration: {
            type: 'boolean',
            title: 'field.oauth2.openClientRegistration.name',
            description: 'field.oauth2.openClientRegistration.helperText',
        },
    },
};
export const realmTosSchema: RJSFSchema = {
    type: 'object',
    properties: {
        enableTOS: {
            type: 'boolean',
            title: 'field.tos.enableTOS.name',
            description: 'field.tos.enableTOS.helperText',
        },
        approveTOS: {
            type: 'boolean',
            title: 'field.tos.approveTOS.name',
            description: 'field.tos.approveTOS.helperText',
        },
    },
};
