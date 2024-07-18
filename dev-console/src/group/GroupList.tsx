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
    ShowButton,
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
import React from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { DropDownButton } from '../components/DropdownButton';
import { RowButtonGroup } from '../components/RowButtonGroup';
import { GroupCreateForm } from './GroupCreate';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';

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
                    <RowButtonGroup label="⋮">
                        <DropDownButton>
                            <ShowButton />
                            <EditButton />
                            <ExportRecordButton />
                            <DeleteWithDialogButton />
                        </DropDownButton>
                    </RowButtonGroup>
                </Datagrid>
            </List>
        </>
    );
};

const EditGroupButton = () => {
    const record = useRecordContext();

    if (!record) return null;
    return (
        <>
            <EditButton></EditButton>
        </>
    );
};

const GroupFilters = [<SearchInput source="q" alwaysOn />];
const transformCrate = (data: any) => {
    return {
        ...data,
        group: data.key
    }
};
const Empty = () => {
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No groups registered. Create a realm group to manage users and
                memberships.
            </Typography>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={transformCrate}
            >
                <GroupCreateForm />
            </CreateInDialogButton>
        </Box>
    );
};

const GroupListActions = () => {
    const { root: realmId } = useRootSelector();

    const notify = useNotify();
    const translate = useTranslate();
    const options = {
        meta: { import: false, resetId: false },
    };
    const [open, setOpen] = React.useState(false);
    const importTo = `/groups/r/${realmId}/import`;
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
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={transformCrate}
            >
                <GroupCreateForm />
            </CreateInDialogButton>            <Button
                variant="contained"
                label="Import"
                onClick={handleClick}
                to={importTo}
            >
                {<ImportExportIcon />}
            </Button>
            <Dialog open={open} onClose={handleClose} fullWidth maxWidth="md">
                <DialogTitle>Import Provider</DialogTitle>
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
        <SaveButton />
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
