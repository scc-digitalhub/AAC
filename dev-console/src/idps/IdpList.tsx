import ImportExportIcon from '@mui/icons-material/ImportExport';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
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
    BooleanInput,
    useDelete,
    useUpdate,
    useNotify,
    useRefresh,
    ExportButton,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import { AceEditorInput } from '@dslab/ra-ace-editor';
import { YamlExporter } from '../components/YamlExporter';

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
                exporter={YamlExporter}
                actions={<IdpListActions />}
                queryOptions={options}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="name" />
                    <TextField source="authority" />
                    <TextField source="provider" />
                    {<EnableIdpButton />}
                    <EditIdpButton />
                    <DeleteButtonDialog
                        // rootId={params.realmId}
                        // property="provider"
                        title="IDP Deletion"
                        // resourceName="Identity Provider"
                        // registeredResource="idps"
                        // redirectUrl={`/idps/r/${params.realmId}`}
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
    const notify = useNotify();
    const options = {
        meta: { realmId: params.realmId, import: false, resetId: false },
    };
    const [open, setOpen] = React.useState(false);
    const to = `/idps/r/${params.realmId}/create`;
    const importTo = `/idps/r/${params.realmId}/import`;
    const handleClick = () => {
        setOpen(true);
    };
    const handleClose = () => {
        setOpen(false);
    };
    const transform = (data: any) => {
        options.meta.import = true;
        options.meta.resetId = data.resetId;
        data = data.yamlInput;
        return data;
    };

    const onSuccess = (data: any) => {
        notify(`Provider imported successfully`);
    };

    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="Add Provider"
                sx={{ marginLeft: 2 }}
                to={to}
            />
            <ExportButton meta={options.meta} variant="contained" />
            <Button
                variant="contained"
                label="Import"
                onClick={handleClick}
                to={importTo}
            >
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
                            <Box>
                                <BooleanInput
                                    label="Reset ID(s)"
                                    source="resetId"
                                />
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
    const notify = useNotify();
    const refresh = useRefresh();
    const realmId = params.realmId;
    const [disable] = useDelete(
        'idps',
        {
            id: record.provider + '/status',
            meta: { rootId: realmId },
        },
        {
            onSuccess: () => {
                notify(record.id + ` disabled successfully`);
                refresh();
            },
        }
    );
    const [enable] = useUpdate(
        'idps',
        {
            id: record.provider + '/status',
            data: record,
            meta: { realmId: realmId },
        },
        {
            onSuccess: () => {
                notify(record.id + ` enabled successfully`);
                refresh();
            },
        }
    );

    if (!record) return null;
    return (
        <>
            {record.enabled && (
                <Button
                    onClick={() => {
                        disable();
                    }}
                    label="Disable"
                    startIcon={<StopIcon />}
                ></Button>
            )}
            {!record.enabled && (
                <Button
                    onClick={() => {
                        enable();
                    }}
                    label="Enable"
                    startIcon={<PlayArrowIcon />}
                ></Button>
            )}
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

const ImportToolbar = () => (
    <Toolbar>
        <SaveButton label="Import" />
    </Toolbar>
);
