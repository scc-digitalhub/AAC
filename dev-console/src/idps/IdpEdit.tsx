import {
    Box,
    Card,
    CardContent,
    Dialog,
    DialogContent,
    DialogTitle,
    Divider,
    Typography,
} from '@mui/material';
import {
    Button,
    Edit,
    EditBase,
    Form,
    Loading,
    SaveButton,
    ShowBase,
    ShowButton,
    SimpleForm,
    SimpleShowLayout,
    TabbedShowLayout,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useEditContext,
    useGetOne,
    useNotify,
    useRecordContext,
    useRefresh,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import VisibilityIcon from '@mui/icons-material/Visibility';
import React from 'react';
import AceEditor from 'react-ace';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-github';
import { CustomDeleteButtonDialog } from '../components/CustomDeleteButtonDialog';
import { JsonSchemaFormInput } from '../components/JsonSchemaFormInput';
import { RJSFSchema, UiSchema } from '@rjsf/utils';

export const IdpEdit = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    return (
        <Edit
            actions={<EditToolBarActions />}
            mutationMode="pessimistic"
            queryOptions={options}
        >
            <IdpTabComponent />
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

const IdpTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    // return <IdpTabs id={record.provider}></IdpTabs>;

    return (
        <>
            <br />
            <Typography variant="h5" sx={{ ml: 2, mt: 1 }}>
                <StarBorderIcon color="primary" /> {record.name}
            </Typography>
            <Typography variant="h6" sx={{ ml: 2 }}>
                {record.provider}
            </Typography>
            <br />
            {/* <TabbedShowLayout sx={{ mr: 1 }} syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Internal Configuration">
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="authority" />
                    <TextField source="provider" />
                    <TextField source="enabled" />
                    <TextField source="registered" />
                    <InternalConfiguration />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Settings">
                    <Typography variant="h5" sx={{ mr: 2 }}>
                        OAuth2.0 Configuration
                    </Typography>
                    <Typography variant="h6" sx={{ mr: 2 }}>
                        Basic client configuration for OAuth2/OpenId Connect
                    </Typography>
                    <EditOAuthJsonSchemaForm />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout> */}
        </>
    );
};

const IdpTabs = (recordParams: any) => {
    const params = useParams();
    const {
        data: record,
        isLoading,
        error,
    } = useGetOne('idps', {
        id: recordParams.id,
        meta: { realmId: params.realmId },
    });
    if (isLoading) {
        return <Loading />;
    }
    if (error) {
        return <p>ERROR</p>;
    }

    return (
        <>
            <br />
            <Typography variant="h5" sx={{ ml: 2, mt: 1 }}>
                <StarBorderIcon color="primary" /> {record.name}
            </Typography>
            <Typography variant="h6" sx={{ ml: 2 }}>
                {record.provider}
            </Typography>
            <br />
            {/* <TabbedShowLayout sx={{ mr: 1 }} syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Internal Configuration">
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="authority" />
                    <TextField source="provider" />
                    <TextField source="enabled" />
                    <TextField source="registered" />
                    <InternalConfiguration />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Settings">
                    <Typography variant="h5" sx={{ mr: 2 }}>
                        OAuth2.0 Configuration
                    </Typography>
                    <Typography variant="h6" sx={{ mr: 2 }}>
                        Basic client configuration for OAuth2/OpenId Connect
                    </Typography>
                    <EditOAuthJsonSchemaForm />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout> */}
        </>
    );
};
const InternalConfiguration = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    const onSuccess = (data: any) => {
        notify(`Provider updated successfully`);
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
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const [open, setOpen] = React.useState(false);
    const record = useRecordContext();
    if (!record) return null;
    let body = JSON.stringify(record, null, '\t');
    const handleClick = () => {
        setOpen(true);
    };
    const handleClose = () => {
        setOpen(false);
    };
    return (
        <TopToolbar>
            <ShowAppButton />
            <Button label="Inspect" onClick={handleClick}>
                {<VisibilityIcon />}
            </Button>
            <Dialog
                open={open}
                onClose={handleClose}
                fullWidth
                maxWidth="md"
                sx={{
                    '.MuiDialog-paper': {
                        position: 'absolute',
                        top: 50,
                    },
                }}
            >
                <DialogTitle bgcolor={'#0066cc'} color={'white'}>
                    Inpsect Json
                </DialogTitle>
                <DialogContent>
                    <ShowBase queryOptions={options} id={params.id}>
                        {/* <RichTextField source={body} /> */}
                        <SimpleShowLayout>
                            <Typography>Raw JSON</Typography>
                            <Box>
                                <AceEditor
                                    setOptions={{
                                        useWorker: false,
                                    }}
                                    mode="json"
                                    value={body}
                                    width="100%"
                                    maxLines={20}
                                    wrapEnabled
                                    theme="github"
                                    showPrintMargin={false}
                                ></AceEditor>
                            </Box>
                        </SimpleShowLayout>
                    </ShowBase>
                </DialogContent>
            </Dialog>
            <CustomDeleteButtonDialog
                // realmId={params.realmId}
                title="Client App Deletion"
                // resourceName="Client Application"
                // registeredResource="apps"
                // redirectUrl={`/apps/r/${params.realmId}`}
            />
            <ExportAppButton />
        </TopToolbar>
    );
};

const ExportAppButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to =
        process.env.REACT_APP_DEVELOPER_CONSOLE +
        `/apps/${realmId}/${record.id}/export`;
    const handleExport = (data: any) => {
        window.open(to, '_blank');
    };
    if (!record) return null;
    return (
        <>
            <Button onClick={handleExport} label="Export"></Button>
        </>
    );
};

const ShowAppButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to = `/apps/r/${realmId}/${record.id}`;
    if (!record) return null;
    return (
        <>
            <ShowButton to={to}></ShowButton>
        </>
    );
};

const EditOAuthJsonSchemaForm = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    const onSuccess = (data: any) => {
        notify(`App updated successfully`);
        refresh();
    };

    return (
        <EditBase
            mutationMode="pessimistic"
            mutationOptions={{ ...options, onSuccess }}
            queryOptions={options}
        >
            <SimpleForm toolbar={<MyToolbar />}>
                <JsonSchemaFormInput
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
