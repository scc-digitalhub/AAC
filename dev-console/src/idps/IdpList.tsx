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
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { Box, Typography } from '@mui/material';
import { CustomDeleteButtonDialog } from '../components/CustomDeleteButtonDialog';

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
                    <ShowIdpButton />
                    <EditIdpButton />
                    <CustomDeleteButtonDialog
                        realmId={params.realmId}
                        title="Client App Deletion"
                        resourceName="Client Application"
                        registeredResource="apps"
                        redirectUrl={`/apps/r/${params.realmId}`}
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
    const to = `/idps/r/${params.realmId}/create`;
    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="Add Provider"
                sx={{ marginLeft: 2 }}
                to={to}
            />
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

const ShowIdpButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to = `/idps/r/${realmId}/${record.id}`;
    if (!record) return null;
    return (
        <>
            <ShowButton to={to}></ShowButton>
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
        `/idps/${realmId}/${record.id}/export`;
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
