import {
    List,
    SearchInput,
    Datagrid,
    TopToolbar,
    ExportButton,
    useTranslate,
} from 'react-admin';
import { YamlExporter } from '../components/YamlExporter';
import { IdField } from '../components/IdField';
import { PageTitle } from '../components/pageTitle';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { AppCreateForm } from './AppCreate';
import { ActionsButtons } from '../components/ActionsButtons';
import { Box } from '@mui/material';
import { Page } from '../components/page';
import { AppIcon } from './AppIcon';
import { NameField } from '../components/NameField';

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
                    <NameField
                        text="name"
                        secondaryText="configuration.applicationType"
                        tertiaryText="description"
                        source="name"
                        icon={<AppIcon color={'secondary'} />}
                    />
                    <IdField source="clientId" label="id" />
                    <ActionsButtons />
                </Datagrid>
            </List>
        </Page>
    );
};

const ListFilters = [<SearchInput source="q" alwaysOn key={'q'} />];

const ListActions = () => {
    const transform = (data: any) => {
        return {
            ...data,
            configuration: { applicationType: data.type },
            type: 'oauth2',
        };
    };

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
