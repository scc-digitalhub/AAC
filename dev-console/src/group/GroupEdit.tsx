import {
    Box,
    Card,
    CardContent,
    Dialog,
    DialogContent,
    DialogTitle,
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
    SimpleShowLayout,
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
import VisibilityIcon from '@mui/icons-material/Visibility';
import React from 'react';
import AceEditor from 'react-ace';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-github';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { RJSFSchema, UiSchema } from '@rjsf/utils';

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
                    <EditBase queryOptions={options} id={params.id}>
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
                    </EditBase>
                </DialogContent>
            </Dialog>
            <DeleteButtonDialog
                mutationOptions={options}
                confirmTitle="Client App"
                redirect={`/apps/r/${params.realmId}`}
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
    const redirect = useRedirect();
    const realmId = params.realmId;
    const to = `/apps/r/${realmId}/${record.id}`;
    const handleClick = () => {
        redirect(to);
    };
    if (!record) return null;
    return (
        <>
            <Button onClick={handleClick} label="Show"></Button>
        </>
    );
};

const IdField = (props: any) => {
    let s = props.source;
    const record = useRecordContext();
    if (!record) return null;
    return (
        <span>
            {record[s]}
            <IconButton
                onClick={() => {
                    navigator.clipboard.writeText(record[s]);
                }}
            >
                <ContentCopyIcon />
            </IconButton>
        </span>
    );
};
