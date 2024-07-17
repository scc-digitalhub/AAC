import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    TopToolbar,
    CreateButton,
    useRecordContext,
    Button,
    EditButton,
    ExportButton,
    useRedirect,
    ShowButton,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import { Box, IconButton, Typography } from '@mui/material';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import { YamlExporter } from '../components/YamlExporter';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { useRootSelector } from '@dslab/ra-root-selector';

export const TemplateList = () => {
    const { root: realmId } = useRootSelector();
    const options = { meta: { realmId:realmId } };
    useListContext<any>();
    return (
        <>
            <br />
            <Typography variant="h5" sx={{ mt: 1 }}>
                Templates
            </Typography>
            <Typography variant="h6">
                Customize appearance and messages
            </Typography>
            <List
                exporter={YamlExporter}
                empty={<Empty />}
                actions={<TemplateListActions />}
                queryOptions={options}
                filters={TemplateFilters}
                sort={{ field: 'template', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="template" />
                    <TextField source="language" />
                    <TextField source="authority" />
                    <IdField source="id" />
                    <ShowButton />
                    <EditAppButton />
                    <DeleteButtonDialog
                        mutationOptions={options}
                        confirmTitle="Template Deletion"
                        redirect={`/templates/r/${realmId}`}
                    />
                    <ExportTemplateButton />
                </Datagrid>
            </List>
        </>
    );
};

const TemplateFilters = [<SearchInput source="q" alwaysOn />];

const TemplateListActions = () => {
    const { root: realmId } = useRootSelector();
    const options = { realmId: realmId };
    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="New Template"
                sx={{ marginLeft: 2 }}
            />
            <ExportButton variant="contained" meta={options} />
        </TopToolbar>
    );
};

const Empty = () => {
    return (
        <Box textAlign="center" mt={30} ml={70}>
            <Typography variant="h6" paragraph>
                No template available, create one
            </Typography>
            <CreateButton variant="contained" label="New App" />
        </Box>
    );
};

// const ShowAppButton = () => {
//     const record = useRecordContext();
//     const params = useParams();
//     const redirect = useRedirect();
//     const realmId = params.realmId;
//     const to = '/templates/r/' + realmId + '/' + record.id;
//     const handleClick = () => {
//         redirect(to);
//     };
//     if (!record) return null;
//     return (
//         <>
//             <Button onClick={handleClick} label="Show"></Button>
//         </>
//     );
// };

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

const ExportTemplateButton = () => {
    const record = useRecordContext();
    const params = useParams();
    const realmId = params.realmId;
    const to =
        process.env.REACT_APP_DEVELOPER_CONSOLE +
        `/templates/${realmId}/models/${record.id}/export`;
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
