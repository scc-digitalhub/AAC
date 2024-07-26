import {
    Box,
    Card,
    CardContent,
    Divider,
    IconButton,
    Typography,
} from '@mui/material';
import {
    Button,
    Edit,
    EditBase,
    Form,
    SaveButton,
    ShowButton,
    TabbedForm,
    TabbedShowLayout,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useEditContext,
    useNotify,
    useRecordContext,
    useRedirect,
    useRefresh,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import React from 'react';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { RJSFSchema, UiSchema } from '@rjsf/utils';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { IdField } from '../components/IdField';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { Page } from '../components/page';
import { PageTitle } from '../components/pageTitle';

export const GroupEdit = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    return (
        <Page>
            <Edit
                actions={<GroupToolBarActions />}
                mutationMode="pessimistic"
                queryOptions={options}
                component={Box}
            >
                <GroupTabComponent />
            </Edit>
        </Page>
    );
};

const schemaOAuthClient: RJSFSchema = {
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

const uiSchemaOAuthClient: UiSchema = {};

const GroupTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} />
            <TabbedForm sx={{ mr: 1 }} syncWithLocation={false}>
                <TabbedForm.Tab label="Overview">
                    <TextField source="id" />
                    <TextField source="name" />
                    <TextField source="group" />
                    <TextField source="members" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Settings">
                    <TextInput source="name" fullWidth />
                    <TextInput source="group" fullWidth />
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Roles">
                    <TextField source="id" />
                </TabbedForm.Tab>
                <TabbedForm.Tab label="Members">
                    <TextField source="id" />
                    <TextField source="members" />
                </TabbedForm.Tab>
            </TabbedForm>
        </>
    );
};

const GroupToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;
    return (
        <TopToolbar>
            <ShowButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton />
        </TopToolbar>
    );
};
