import {
    List,
    SearchInput,
    Datagrid,
    TextField,
    TopToolbar,
    ExportButton,
    useTranslate,
    BulkDeleteButton,
    Labeled,
    useRecordContext,
    FieldProps,
} from 'react-admin';
import { YamlExporter } from '../components/YamlExporter';
import { IdField } from '../components/IdField';
import { PageTitle } from '../components/pageTitle';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { AppCreateForm } from './AppCreate';
import { ActionsButtons } from '../components/ActionsButtons';
import { Avatar, Box, Paper, Stack, Typography } from '@mui/material';
import { Page } from '../components/page';
import { AppIcon } from './AppIcon';
import { grey } from '@mui/material/colors';

const NameField = (props: FieldProps) => {
    return (
        <Stack direction={'row'} columnGap={2} py={1}>
            <Avatar sx={{ mt: 1, backgroundColor: grey[200] }}>
                <AppIcon color={'secondary'} />
            </Avatar>
            <Stack>
                <TextField source="name" color={'primary'} variant="h6" />
                <TextField
                    source="configuration.applicationType"
                    label="type"
                    variant="body2"
                />
                <TextField source="description" defaultValue={' '} />
            </Stack>
        </Stack>
    );
};

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
                actions={<ListActions />}
                filters={ListFilters}
                sort={{ field: 'name', order: 'DESC' }}
                component={Box}
                empty={false}
            >
                <Datagrid bulkActionButtons={false} rowClick="show">
                    <NameField source="name" />
                    <IdField source="clientId" label="id" />
                    <ActionsButtons />
                </Datagrid>
            </List>
        </Page>
    );
};

const ListFilters = [<SearchInput source="q" alwaysOn />];
const transform = (data: any) => {
    return {
        ...data,
        configuration: { applicationType: data.type },
        type: 'oauth2',
    };
};
const ListActions = () => {
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
