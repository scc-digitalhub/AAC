import {
    List,
    useListContext,
    SearchInput,
    Datagrid,
    TextField,
    CreateButton,
    ArrayField,
    ChipField,
    useRecordContext,
    SingleFieldList,
} from 'react-admin';
import { Box, Typography } from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';

export const ScopeList = () => {
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
                filters={RealmFilters}
                sort={{ field: 'resourceId', order: 'DESC' }}
            >
                <Datagrid bulkActionButtons={false}>
                    <TextField source="resourceId" />
                    <TextField source="name" />
                    <ArrayField source="scopes">
                        <SingleFieldList linkType={false}>
                            <ChipField source="scope" size="small" />
                        </SingleFieldList>
                    </ArrayField>
                </Datagrid>
            </List>
        </>
    );
};
const RealmFilters = [<SearchInput source="q" alwaysOn />];

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
