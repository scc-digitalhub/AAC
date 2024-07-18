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

export const GroupEdit = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    return (
        <Edit
            actions={<GroupToolBarActions />}
            mutationMode="pessimistic"
            queryOptions={options}
        >
            <GroupTabComponent />
        </Edit>
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
            <br />
            <Typography variant="h5" sx={{ ml: 2, mt: 1 }}>
                <StarBorderIcon color="primary" /> {record.name}
            </Typography>
            <Typography variant="h6" sx={{ ml: 2 }}>
                <IdField source="id" />
            </Typography>
            <br />
            <TabbedShowLayout sx={{ mr: 1 }} syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Overview">
                    <TextField source="id" />
                    <TextField source="name" />
                    <TextField source="group" />
                    <TextField source="members" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Settings">
                    <EditSetting />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Roles">
                    <TextField source="id" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Members">
                    <TextField source="id" />
                    <TextField source="members" />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

const EditSetting = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    const onSuccess = (data: any) => {
        notify(`Group updated successfully`);
        refresh();
    };
    return (
        <EditBase
            mutationMode="pessimistic"
            mutationOptions={{ ...options, onSuccess }}
            queryOptions={options}
        >
            <Form>
                <Card>
                    <CardContent>
                        <Box>
                            <Box display="flex">
                                <Box flex="1" mt={-1}>
                                    <Box display="flex" width={430}>
                                        <TextInput source="name" fullWidth />
                                    </Box>
                                    <Box display="flex" width={430}>
                                        <TextInput source="group" fullWidth />
                                    </Box>
                                    <Divider />
                                </Box>
                            </Box>
                        </Box>
                    </CardContent>
                    <GroupSettingToolbar />
                </Card>
            </Form>
        </EditBase>
    );
};

const GroupSettingToolbar = (props: any) => (
    <Toolbar {...props}>
        <SaveButton />
    </Toolbar>
);

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
