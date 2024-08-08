import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import {
    List,
    Datagrid,
    TopToolbar,
    useTranslate,
    SearchInput,
    NumberField,
    ExportButton,
} from 'react-admin';
import { RoleCreateForm } from './RoleCreate';
import { Box } from '@mui/material';
import { IdField } from '../components/IdField';
import { ActionsButtons } from '../components/ActionsButtons';
import { PageTitle } from '../components/PageTitle';
import { YamlExporter } from '../components/YamlExporter';
import { ReactElement, isValidElement } from 'react';
import { NameField } from '../components/NameField';
import { Page } from '../components/Page';
import { RoleIcon } from './RoleIcon';
import { useRootSelector } from '@dslab/ra-root-selector';

export const RoleList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.roles.list.title')}
                secondaryText={translate('page.roles.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<RoleListActions />}
                filters={RoleFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
            >
                <RoleListView />
            </List>
        </Page>
    );
};

export const RoleListView = (props: { actions?: ReactElement | boolean }) => {
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
                secondaryText="role"
                source="name"
                icon={<RoleIcon color={'secondary'} />}
            />
            <IdField source="id" label="id" />
            <NumberField source="size" label={'subjects'} sortable={false} />
            {actions !== false && actions}
        </Datagrid>
    );
};

const RoleFilters = [<SearchInput source="q" alwaysOn />];

const RoleListActions = () => {
    const { root: realmId } = useRootSelector();
    const transform = (data: any) => {
        return {
            ...data,
            realm: realmId,
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
                <RoleCreateForm />
            </CreateInDialogButton>
            <ExportButton
                variant="contained"
                meta={{ flatten: [] }}
                sort={{ field: 'name', order: 'ASC' }}
            />
        </TopToolbar>
    );
};
