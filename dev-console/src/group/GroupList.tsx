import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    Button,
    useNotify,
    useTranslate,
    BooleanInput,
    Create,
    SimpleForm,
    SaveButton,
    Toolbar,
    useRecordContext,
    EditButton,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import {
    Box,
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    Typography,
} from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';
import { AceEditorInput } from '@dslab/ra-ace-editor';
import ImportExportIcon from '@mui/icons-material/ImportExport';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import React from 'react';

export const GroupList = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    useListContext<any>();
    return (
        <>
            <br />
            <Typography variant="h5" sx={{ mt: 1 }}>
                Realm groups
            </Typography>
            <Typography variant="h6">
                Register and manage realm groups.
            </Typography>
            <List
                exporter={YamlExporter}
                empty={<Empty />}
                actions={<GroupListActions />}
                queryOptions={options}
                filters={GroupFilters}
                sort={{ field: 'name', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="name" />
                    <IdField source="id" />
                    <TextField source="group" />
                    <EditGroupButton />
                    <DeleteButtonDialog
                        mutationOptions={options}
                        confirmTitle="Group Deletion"
                        redirect={`/groups/r/${params.realmId}`}
                    />
                    {/* Edit. */}
                    {/* Export */}
                </Datagrid>
            </List>
        </>
    );
};

const EditGroupButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to = `/groups/r/${realmId}/${record.id}/edit`;
    if (!record) return null;
    return (
        <>
            <EditButton to={to}></EditButton>
        </>
    );
};

const GroupFilters = [<SearchInput source="q" alwaysOn />];

const Empty = () => {
    const params = useParams();
    const to = `/groups/r/${params.realmId}/create`;
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No groups registered. Create a realm group to manage users and
                memberships.
            </Typography>
            <CreateButton variant="contained" label="New Group" to={to} />
        </Box>
    );
};

const GroupListActions = () => {
    const params = useParams();
    const notify = useNotify();
    const translate = useTranslate();
    const options = {
        meta: { realmId: params.realmId, import: false, resetId: false },
    };
    const [open, setOpen] = React.useState(false);
    const to = `/groups/r/${params.realmId}/create`;
    const importTo = `/groups/r/${params.realmId}/import`;
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
        notify(`Group imported successfully`);
    };

    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="Add Provider"
                sx={{ marginLeft: 2 }}
                to={to}
            />
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