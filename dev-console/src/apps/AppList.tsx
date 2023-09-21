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
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { Typography } from '@mui/material';

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
