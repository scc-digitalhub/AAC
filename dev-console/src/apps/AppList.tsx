import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    ShowButton,
    useRedirect,
    useRecordContext,
    useNotify,
    useRefresh,
    useDelete,
    EditButton,
    Confirm,
    DeleteButton,
    DeleteWithConfirmButton,
    Button,
    useDataProvider,
} from 'react-admin';
import { useLocation, useParams } from 'react-router-dom';
import { Typography } from '@mui/material';
import React from 'react';
import { CustomDeleteButton } from '../components/CustomDeleteButton';
import FileUploadIcon from '@mui/icons-material/FileUpload';

export const AppList = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    useListContext<any>();
    return (
        <>
            <br />
            <Typography variant="h5" sx={{ mt: 1 }}>
                Client applications
            </Typography>
            <Typography variant="h6">
                Manage web, mobile, server and IoT applications
            </Typography>
            <List
                actions={<AppListActions />}
                queryOptions={options}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="name" />
                    <TextField source="id" />
                    <ShowAppButton />
                    <CustomDeleteButton realmId={params.realmId} />
                    {/* <DeleteWithConfirmButton></DeleteWithConfirmButton> */}
                    <ExportAppButton />
                </Datagrid>
            </List>
        </>
    );
};

const RealmFilters = [<SearchInput source="q" alwaysOn />];

const AppListActions = () => (
    <TopToolbar>
        <CreateButton
            variant="contained"
            label="New App"
            sx={{ marginLeft: 2 }}
        />
    </TopToolbar>
);

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
            <Button
                onClick={handleExport}
                startIcon={<FileUploadIcon />}
                label="Export"
            ></Button>
        </>
    );
};
