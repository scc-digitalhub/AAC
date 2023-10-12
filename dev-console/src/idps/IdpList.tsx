import ImportExportIcon from '@mui/icons-material/ImportExport';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import {
    Box,
    Dialog,
    DialogContent,
    DialogTitle,
    Typography,
} from '@mui/material';
import 'ace-builds/src-noconflict/mode-yaml';
import 'ace-builds/src-noconflict/theme-github';
import React from 'react';
import {
    Button,
    Create,
    CreateButton,
    Datagrid,
    EditButton,
    Toolbar,
    List,
    SearchInput,
    SimpleForm,
    TextField,
    TopToolbar,
    useListContext,
    useRecordContext,
    SaveButton,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { CustomDeleteButtonDialog } from '../components/CustomDeleteButtonDialog';
import { AceEditorInput } from '@dslab/ra-ace-editor';

export const IdpList = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    useListContext<any>();
    return (
        <>
            <br />
            <Typography variant="h5" sx={{ mt: 1 }}>
                Identity Providers
            </Typography>
            <Typography variant="h6">
                Register and manage identity providers
            </Typography>
            <List
                empty={<Empty />}
                actions={<IdpListActions />}
                queryOptions={options}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="name" />
                    <TextField source="authority" />
                    <TextField source="provider" />
                    <EnableIdpButton />
                    <EditIdpButton />
                    <CustomDeleteButtonDialog
                        rootId={params.realmId}
                        property="provider"
                        title="IDP Deletion"
                        resourceName="Identity Provider"
                        registeredResource="idps"
                        redirectUrl={`/idps/r/${params.realmId}`}
                    />
                    <ExportIdpButton />
                </Datagrid>
            </List>
        </>
    );
};

const RealmFilters = [<SearchInput source="q" alwaysOn />];

const IdpListActions = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const [open, setOpen] = React.useState(false);
    const to = `/idps/r/${params.realmId}/create`;
    const handleClick = () => {
        setOpen(true);
    };
    const handleClose = () => {
        setOpen(false);
    };
    const transform = (data: any) => {
        // let body = createApp(data, params.realmId);
        // return body;
    };

    const onSuccess = (data: any) => {
        notify(`Provider imported successfully`);
        // redirect(`/apps/r/${params.realmId}`);
    };

    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="Add Provider"
                sx={{ marginLeft: 2 }}
                to={to}
            />
            <Button variant="contained" label="Import" onClick={handleClick}>
                {<ImportExportIcon />}
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
                    Import Provider
                </DialogTitle>
                <DialogContent>
                    <Create
                        transform={transform}
                        mutationOptions={{ ...options, onSuccess }}
                    >
                        <SimpleForm toolbar={<ImportToolbar />}>
                            <Typography>
                                Provide a valid YAML file with the full provider
                                definition, or with a list of valid providers
                                nested under key providers.
                            </Typography>
                            <Box>
                                <AceEditorInput
                                    source="yamlInput"
                                    mode="yaml"
                                    theme="github"
                                ></AceEditorInput>
                            </Box>
                        </SimpleForm>
                    </Create>
                </DialogContent>
            </Dialog>
        </TopToolbar>
    );
};

const Empty = () => {
    const params = useParams();
    const to = `/idps/r/${params.realmId}/create`;
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No provider available, create one
            </Typography>
            <CreateButton variant="contained" label="New App" to={to} />
        </Box>
    );
};

const EnableIdpButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to = `/idps/r/${realmId}/${record.id}`;
    if (!record) return null;
    return (
        <>
            <Button
                to={to}
                label="Enable"
                startIcon={<PlayArrowIcon />}
            ></Button>
        </>
    );
};

const EditIdpButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to = `/idps/r/${realmId}/${record.id}/edit`;
    if (!record) return null;
    return (
        <>
            <EditButton to={to}></EditButton>
        </>
    );
};

const ExportIdpButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to =
        process.env.REACT_APP_DEVELOPER_CONSOLE +
        `/idps/${realmId}/${record.provider}/export`;
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
function notify(arg0: string) {
    throw new Error('Function not implemented.');
}

const ImportToolbar = () => (
    <Toolbar>
        <SaveButton label="Import" />
    </Toolbar>
);
