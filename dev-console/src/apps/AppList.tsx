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
    EditButton,
    ExportButton,
    useRedirect,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { Box, IconButton, Typography } from '@mui/material';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import { YamlExporter } from '../components/YamlExporter';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';

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
                exporter={YamlExporter}
                empty={<Empty />}
                actions={<AppListActions />}
                queryOptions={options}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="name" />
                    <IdField source="id" />
                    <ShowAppButton />
                    <EditAppButton />
                    <DeleteButtonDialog
                        mutationOptions={options}
                        confirmTitle="Client App Deletion"
                        redirect={`/apps/r/${params.realmId}`}
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
    const options = { realmId: params.realmId };
    const to = `/apps/r/${params.realmId}/create`;
    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="New App"
                sx={{ marginLeft: 2 }}
                to={to}
            />
            <ExportButton variant="contained" meta={options} />
        </TopToolbar>
    );
};

const Empty = () => {
    const params = useParams();
    const to = `/apps/r/${params.realmId}/create`;
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No app available, create one
            </Typography>
            <CreateButton variant="contained" label="New App" to={to} />
        </Box>
    );
};

const ShowAppButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const redirect = useRedirect();
    const realmId = params.realmId;
    const to = '/apps/r/' + realmId + '/' + record.id;
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

const EditAppButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to = `/apps/r/${realmId}/${record.id}/edit`;
    if (!record) return null;
    return (
        <>
            <EditButton to={to}></EditButton>
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
            <Button onClick={handleExport} label="Export"></Button>
        </>
    );
};

const IdField = (props: any) => {
    const record = useRecordContext();
    if (!record) return null;
    return (
        <span>
            {record.id}
            <IconButton
                onClick={() => {
                    navigator.clipboard.writeText(`${record.id}`);
                }}
            >
                <ContentCopyIcon />
            </IconButton>
        </span>
    );
};
