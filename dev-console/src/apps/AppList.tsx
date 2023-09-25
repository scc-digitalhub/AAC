import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    ShowButton,
    useRecordContext,
    Button,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { Typography } from '@mui/material';
import { CustomDeleteButtonDialog } from '../components/CustomDeleteButtonDialog';

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
                    <CustomDeleteButtonDialog
                        realmId={params.realmId}
                        title="Client App Deletion"
                        resourceName="Client Application"
                    />
                    <ExportAppButton />
                </Datagrid>
            </List>
        </>
    );
};

const RealmFilters = [<SearchInput source="q" alwaysOn />];

const AppListActions = () => {
    const params = useParams();
    const to = `/apps/r/${params.realmId}/create`;
    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="New App"
                sx={{ marginLeft: 2 }}
                to={to}
            />
        </TopToolbar>
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

const ExportAppButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to = `/apps/r/${realmId}/${record.id}`;
    const handleExport = (data: any) => {
        window.open(`console/dev/apps/${realmId}/${record.id}/export`);
    };
    if (!record) return null;
    return (
        <>
            <Button onClick={handleExport} label="Export"></Button>
        </>
    );
};
