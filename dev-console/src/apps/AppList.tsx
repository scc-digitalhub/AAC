import {
    List,
    SearchInput,
    Datagrid,
    TopToolbar,
    ExportButton,
    useTranslate,
    useRecordContext,
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
import { TagsField } from '../components/TagsField';
import { getAppIcon } from './utils';
import { ImportButton } from '../components/ImportButton';

export const AppList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.apps.list.title')}
                secondaryText={translate('page.apps.list.subtitle')}
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
            <AppNameField source="name" label="field.name.name" />
            <IdField source="clientId" label="field.id.name" />
            <TagsField />
            {actions !== false && actions}
        </Datagrid>
    );
};

export const AppNameField = (props: { source: string; label?: string }) => {
    const { label } = props;
    const record = useRecordContext();

    const icon = record ? (
        getAppIcon(record.configuration?.applicationType, {
            color: 'primary',
        })
    ) : (
        <AppIcon />
    );
    return (
        <NameField
            text="name"
            secondaryText="configuration.applicationType"
            tertiaryText="description"
            source="name"
            label={label}
            icon={icon}
        />
    );
};

const ListFilters = [<SearchInput source="q" alwaysOn key={'q'} />];

const ListActions = () => {
    const { root: realmId } = useRootSelector();
    const transform = (data: any) => {
        return {
            ...data,
            realm: realmId,
            type: 'oauth2',
        };
    };

    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'sm'}
                variant="contained"
                transform={transform}
            >
                <AppCreateForm />
            </CreateInDialogButton>
            <ImportButton variant="contained" idField="clientId" />
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};
