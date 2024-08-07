import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import {
    List,
    Datagrid,
    TextField,
    TopToolbar,
    SearchInput,
} from 'react-admin';
import { ServiceCreateForm } from './ServiceCreate';
import { Box } from '@mui/material';
import { ActionsButtons } from '../components/ActionsButtons';
import { Page } from '../components/Page';
import { YamlExporter } from '../components/YamlExporter';
import { IdField } from '../components/IdField';

export const ServiceList = () => {
    return (
        <Page>
            <List
                actions={<ServiceListActions />}
                component={Box}
                exporter={YamlExporter}
                filters={ServiceFilters}
                sort={{ field: 'name', order: 'DESC' }}
                empty={false}
            >
                <Datagrid bulkActionButtons={false} rowClick="show">
                    <TextField source="name" />
                    <IdField source="id" />
                    <ActionsButtons />
                </Datagrid>
            </List>
        </Page>
    );
};
const ServiceFilters = [<SearchInput source="q" alwaysOn />];
const createTransform = (data: any) => {
    return {
        ...data,
    };
};
const ServiceListActions = () => {
    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={createTransform}
            >
                <ServiceCreateForm />
            </CreateInDialogButton>
        </TopToolbar>
    );
};
