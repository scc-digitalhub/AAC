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
import { PageTitle } from '../components/PageTitle';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { AppCreateForm } from './AppCreate';
import { ActionsButtons } from '../components/ActionsButtons';
import { Box } from '@mui/material';
import { Page } from '../components/Page';
import { AppIcon } from './AppIcon';
import { NameField } from '../components/NameField';
import { isValidElement, ReactElement } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';

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
                <AppListView />
            </List>
        </Page>
    );
};

export const AppListView = (props: { actions?: ReactElement | boolean }) => {
    const { actions: actionProps = true } = props;

    const actions = !actionProps ? (
        false
    ) : isValidElement(actionProps) ? (
        actionProps
    ) : (
        <ActionsButtons />
    );

    return (
        <Datagrid bulkActionButtons={false} rowClick="show">
            <NameField
                text="name"
                secondaryText="configuration.applicationType"
                tertiaryText="description"
                source="name"
                icon={<AppIcon color={'secondary'} />}
            />
            <IdField source="clientId" label="id" />
            {actions !== false && actions}
        </Datagrid>
    );
};

const ListFilters = [<SearchInput source="q" alwaysOn key={'q'} />];

const ListActions = () => {
    const { root: realmId } = useRootSelector();
    const transform = (data: any) => {
        return {
            ...data,
            realm: realmId,
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
