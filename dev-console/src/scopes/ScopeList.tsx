import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    CreateButton,
    useRecordContext,
    Button,
    EditButton,
    useRedirect,
    ArrayField,
    ChipField,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { Box, IconButton, Typography } from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';

export const ScopeList = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    useListContext<any>();
    return (
        <>
            <br />
            <Typography variant="h5" sx={{ mt: 1 }}>
                Scopes and resources
            </Typography>
            <Typography variant="h6">
                View and inspect resources and scopes.
            </Typography>
            <List
                exporter={YamlExporter}
                empty={<Empty />}
                queryOptions={options}
                filters={RealmFilters}
                sort={{ field: 'resourceId', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="resourceId" />
                    <TextField source="name" />
                    <ArrayField source="scopes" emptyText="">
                        <Datagrid bulkActionButtons={false} empty={<></>}>
                            <ChipField source="scope" size="small" />
                        </Datagrid>
                    </ArrayField>
                </Datagrid>
            </List>
        </>
    );
};

const RealmFilters = [<SearchInput source="q" alwaysOn />];

const Empty = () => {
    const params = useParams();
    const to = `/templates/r/${params.realmId}/create`;
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No template available, create one
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
    const to = '/templates/r/' + realmId + '/' + record.id;
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
    const to = `/templates/r/${realmId}/${record.id}/edit`;
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
        `/templates/${realmId}/${record.id}/export`;
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
