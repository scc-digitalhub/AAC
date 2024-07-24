import { Box, Card, CardContent, Divider, Typography } from '@mui/material';
import {
    Edit,
    EditBase,
    Form,
    SaveButton,
    ShowButton,
    SimpleForm,
    TabbedShowLayout,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useEditContext,
    useNotify,
    useRecordContext,
    useRefresh,
    useTranslate,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import React from 'react';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { RJSFSchema, UiSchema } from '@rjsf/utils';
import { JsonSchemaInput } from '@dslab/ra-jsonschema-input';
import { InspectButton } from '@dslab/ra-inspect-button';
import { PageTitle } from '../components/pageTitle';
import { SectionTitle } from '../components/sectionTitle';

export const AppEdit = () => {
    return (
        <Edit actions={<EditToolBarActions />} mutationMode="pessimistic" component={Box}>
            <AppTabComponent />
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

const uiSchemaOAuthClient: UiSchema = {
    // 'ui:submitButtonOptions': {
    //     submitText: 'Confirm Details',
    //     norender: false,
    //     props: {
    //         disabled: false,
    //         className: 'btn btn-info',
    //     },
    // },
    // firstName: {
    //     'ui:autofocus': true,
    //     'ui:emptyValue': '',
    //     'ui:autocomplete': 'family-name',
    // },
    // lastName: {
    //     'ui:title': 'Surname',
    //     'ui:emptyValue': '',
    //     'ui:autocomplete': 'given-name',
    // },
    // telephone: {
    //     'ui:options': {
    //         inputType: 'tel',
    //     },
    // },
};

const AppTabComponent = () => {
    const translate = useTranslate();
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    const onSuccess = () => {
        notify(`App updated successfully`);
        refresh();
    };
    if (!record) return null;
    return (
        <>
            <PageTitle text={record.name} secondaryText={record?.id} />
            <EditBase mutationMode="pessimistic" mutationOptions={{ onSuccess }}>
            <Form>
            <TabbedShowLayout syncWithLocation={false}>
            <TabbedShowLayout.Tab
                    label={translate('page.app.overview.title')}
                >
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="clientId" />
                    <TextField source="scopes" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.app.settings.title')}
                >
                    <EditSetting />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab
                    label={translate('page.app.configuration.title')}
                >
                    <SectionTitle
                        text={translate('page.app.configuration.header.title')}
                        secondaryText={translate(
                            'page.app.configuration.header.subtitle'
                        )}
                    />
                    <EditOAuthJsonSchemaForm />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
            </Form>
        </EditBase>
        </>
    );
};

const EditSetting = () => {
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    const onSuccess = () => {
        notify(`App updated successfully`);
        refresh();
    };
    return (
        <EditBase mutationMode="pessimistic" mutationOptions={{ onSuccess }}>
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
                                        <TextInput
                                            source="description"
                                            fullWidth
                                        />
                                    </Box>
                                    <Divider />
                                </Box>
                            </Box>
                        </Box>
                    </CardContent>
                    <EditSettingToolbar />
                </Card>
            </Form>
        </EditBase>
    );
};

const EditSettingToolbar = (props: any) => (
    <Toolbar {...props}>
        <SaveButton />
    </Toolbar>
);

const EditToolBarActions = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <ShowButton />
            <InspectButton />
            <DeleteWithDialogButton />
            <ExportRecordButton language="yaml" color="info" />
        </TopToolbar>
    );
};

const EditOAuthJsonSchemaForm = () => {
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    const onSuccess = () => {
        notify(`App updated successfully`);
        refresh();
    };

    return (
        <EditBase mutationMode="pessimistic" mutationOptions={{ onSuccess }}>
            <SimpleForm toolbar={<MyToolbar />}>
                <JsonSchemaInput
                    source="configuration"
                    schema={schemaOAuthClient}
                    uiSchema={uiSchemaOAuthClient}
                />
            </SimpleForm>
        </EditBase>
    );
};

const MyToolbar = () => (
    <Toolbar>
        <SaveButton />
    </Toolbar>
);
