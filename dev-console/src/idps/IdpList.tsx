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

import React from 'react';
import {
    Button,
    Create,
    Datagrid,
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
    BulkDeleteButton
} from 'react-admin';
import { AceEditorInput } from '@dslab/ra-ace-editor';
import { YamlExporter } from '../components/YamlExporter';
import { useRootSelector } from '@dslab/ra-root-selector';
import { IdpCreateForm } from './IdpCreate';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { ActionsButtons } from '../components/ActionsButtons';
import { IdField } from '../components/IdField';
import { PageTitle } from '../components/pageTitle';
import { Page } from '../components/page';


const PostBulkActionButtons = () => (
    <>
        <BulkDeleteButton />
    </>
);
export const IdpList = () => {
    const translate = useTranslate();
    useListContext<any>();
    return (
        <Page>
          <PageTitle
                text={translate('page.idp.list.title')}
                secondaryText={translate('page.idp.list.subtitle')}
            />
            
            <List
                empty={false}
                exporter={YamlExporter}
                actions={<IdpListActions />}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'DESC' }}
                component={Box}

            >
                <Datagrid bulkActionButtons={<PostBulkActionButtons />} rowClick="show">
                    <TextField source="name" />
                    <IdField source="provider" />
                    <TextField source="authority" />
                    <ActionsButtons />
                </Datagrid>
            </List>
        </Page>
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

    const createTransform = (data: any) => {
        return {
            ...data,
            type: 'identity',
            realm: realmId,
            configuration: { applicationType: data.type },
        };
    };
    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={createTransform}
            >
                <IdpCreateForm />
            </CreateInDialogButton>
            <ExportButton variant="contained" />
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





const ImportToolbar = () => (
    <Toolbar>
        <SaveButton label="Import" />
    </Toolbar>
);
