import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    TopToolbar,
    Button,
    useNotify,
    useTranslate,
    BooleanInput,
    Create,
    SimpleForm,
    SaveButton,
    Toolbar,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import {
    Box,
    Dialog,
    DialogContent,
    DialogTitle,
    Typography,
} from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';
import { AceEditorInput } from '@dslab/ra-ace-editor';

import ImportExportIcon from '@mui/icons-material/ImportExport';
import React from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { GroupCreateForm } from './GroupCreate';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { PageTitle } from '../components/pageTitle';
import { ActionsButtons } from '../components/ActionsButtons';
import { IdField } from '../components/IdField';

export const GroupList = () => {
    const params = useParams();
    const translate = useTranslate();
    const options = { meta: { realmId: params.realmId } };
    useListContext<any>();
    return (
        <>
            <PageTitle
                text={translate('page.group.list.title')}
                secondaryText={translate('page.group.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<GroupListActions />}
                queryOptions={options}
                filters={GroupFilters}
                sort={{ field: 'name', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false} rowClick="show">
                    <TextField source="name" />
                    <IdField source="id" />
                    <TextField source="group" />
                    <ActionsButtons />
                </Datagrid>
            </List>
        </>
    );
};

const GroupFilters = [<SearchInput source="q" alwaysOn />];
const transformCrate = (data: any) => {
    return {
        ...data,
        group: data.key,
    };
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
            </CreateInDialogButton>
            <Button
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
