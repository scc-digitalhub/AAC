import {
    List,
    SearchInput,
    Datagrid,
    TextField,
    TopToolbar,
    ExportButton,
    useTranslate,
    BulkDeleteButton
} from 'react-admin';
import { YamlExporter } from '../components/YamlExporter';
import { IdField } from '../components/IdField';
import { PageTitle } from '../components/pageTitle';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { AppCreateForm } from './AppCreate';
import { ActionsButtons } from '../components/ActionsButtons';
import { Box, Paper } from '@mui/material';
import { Page } from "../components/page";
const PostBulkActionButtons = () => (
    <>
        <BulkDeleteButton />
    </>
);
export const AppList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.app.list.title')}
                secondaryText={translate('page.app.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<AppListActions />}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'DESC' }}
                component={Box}
            >
                <Datagrid bulkActionButtons={<PostBulkActionButtons />} rowClick="show">
                    <TextField source="name" />
                    <IdField source="id" />
                    <ActionsButtons />
                </Datagrid>
            </List>
            </Page>
        // </Paper>
    );
};

const RealmFilters = [<SearchInput source="q" alwaysOn />];
const transform = (data: any) => {
    return {
        ...data,
        configuration: { applicationType: data.type },
        type: 'oauth2',
    };
};
const AppListActions = () => {
    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={transform}
            >
                <AppCreateForm />
            </CreateInDialogButton>
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};
