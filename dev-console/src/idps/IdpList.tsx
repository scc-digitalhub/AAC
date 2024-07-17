import ImportExportIcon from '@mui/icons-material/ImportExport';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import {
    Box,
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    Typography,
} from '@mui/material';

import React from 'react';
import {
    Button,
    Create,
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
    useTranslate,
    ShowButton,
    CreateButton,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { AceEditorInput } from '@dslab/ra-ace-editor';
import { YamlExporter } from '../components/YamlExporter';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { useRootSelector } from '@dslab/ra-root-selector';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { IdpCreateForm } from './IdpCreate';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';

export const IdpList = () => {
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
                filters={RealmFilters}
                sort={{ field: 'name', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="name" />
                    <TextField source="authority" />
                    <IdField source="provider" />
                    <EnableIdpButton />
                    <EditButton />
                    <DeleteWithDialogButton/>

                    {/* <DeleteWithDialogButton
                        // rootId={params.realmId}
                        // property="provider"
                        title="IDP Deletion"
                        // resourceName="Identity Provider"
                        // registeredResource="idps"
                        // redirectUrl={`/idps/r/${params.realmId}`}
                    /> */}
                    <ExportIdpButton />
                </Datagrid>
            </List>
        </>
    );
};

const RealmFilters = [<SearchInput source="q" alwaysOn />];

const IdpListActions = () => {
    const notify = useNotify();
    const translate = useTranslate();
    const { root: realmId } = useRootSelector();

    const options = {
        meta: { realmId: realmId, import: false, resetId: false },
    };
    const [open, setOpen] = React.useState(false);
    const importTo = `/idps/r/${realmId}/import`;
    const handleClick = () => {
        setOpen(true);
    };
    const handleClose = () => {
        setOpen(false);
    };
    const importTransform = (data: any) => {
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
            />  
            <CreateInDialogButton
                    fullWidth
                    maxWidth={'md'}
                    variant="contained"
                    transform={createTransform}

                >
                    <IdpCreateForm />
                </CreateInDialogButton>
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
                        transform={importTransform}
                        mutationOptions={{ ...options, onSuccess }}
                    >
                        <SimpleForm toolbar={<ImportToolbar />}>
                            <Typography>
                                {translate('page.idp.import.description')}
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
const createTransform = (data: any) => {
    return {
        ...data,
        type: 'identity',
        configuration: { applicationType: data.type },
    };
};
const Empty = () => {

    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No provider available, create one
            </Typography>
            <CreateInDialogButton
                    fullWidth
                    maxWidth={'md'}
                    variant="contained"
                    transform={createTransform}

                >
                    <IdpCreateForm />
                </CreateInDialogButton>
        </Box>
    );
};

export const EnableIdpButton = () => {
    const record = useRecordContext();
    const { root: realmId } = useRootSelector();
    const notify = useNotify();
    const refresh = useRefresh();
    const [disable] = useDelete(
        'idps',
        {
            id: record.provider + '/status',
            meta: { realmId: realmId },
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
    if (!record) return null;
    return (
        <>
            <EditButton></EditButton>
        </>
    );
};

const ExportIdpButton = () => {
    const record = useRecordContext();
    const { root: realmId } = useRootSelector();
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
