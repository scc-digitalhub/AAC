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
    DeleteWithConfirmButton,
} from 'react-admin';
import { Box, IconButton, Typography } from '@mui/material';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import { YamlExporter } from '../components/YamlExporter';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { RowButtonGroup } from '../components/RowButtonGroup';
import { IdField } from '../components/IdField';
import { PageTitle } from '../components/pageTitle';
import { DropDownButton } from '../components/DropdownButton';

export const AppList = () => {
    return (
        <>
            {/* <Typography variant="h5" sx={{ mt: 1 }}>
                Client applications
            </Typography>
            <Typography variant="h6">
                Manage web, mobile, server and IoT applications
            </Typography> */}
            <PageTitle
                text="Client applications"
                secondaryText="Manage web, mobile, server and IoT applications"
            />
            <List
                exporter={YamlExporter}
                actions={<AppListActions />}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="name" />
                    <IdField source="id" />
                    <RowButtonGroup label="⋮">
                        <DropDownButton>
                            <ShowButton />
                            <EditButton />
                            <DeleteWithConfirmButton />
                        </DropDownButton>
                    </RowButtonGroup>
                </Datagrid>
            </List>
        </>
    );
};

const RealmFilters = [<SearchInput source="q" alwaysOn />];

const AppListActions = () => {
    return (
        <TopToolbar>
            <CreateButton
                variant="contained"
                label="New App"
                sx={{ marginLeft: 2 }}
            />
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};

// const Empty = () => {
//     const params = useParams();
//     const to = `/apps/r/${params.realmId}/create`;
//     return (
//         <Box textAlign="center" mt={30} ml={70}>
//             <Typography variant="h6" paragraph>
//                 No app available, create one
//             </Typography>
//             <CreateButton variant="contained" label="New App" to={to} />
//         </Box>
//     );
// };

// const ExportAppButton = () => {
//     const record = useRecordContext();
//     const params = useParams();
//     const realmId = params.realmId;
//     const to =
//         process.env.REACT_APP_DEVELOPER_CONSOLE +
//         `/apps/${realmId}/${record.id}/export`;
//     const handleExport = (data: any) => {
//         window.open(to, '_blank');
//     };
//     if (!record) return null;
//     return (
//         <>
//             <Button onClick={handleExport} label="Export"></Button>
//         </>
//     );
// };

const IdField2 = (props: any) => {
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
