import {
    List,
    SearchInput,
    Datagrid,
    TopToolbar,
    useTranslate,
    ExportButton,
} from 'react-admin';
import { Box } from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';

import { isValidElement, ReactElement } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { PageTitle } from '../components/PageTitle';
import { ActionsButtons } from '../components/ActionsButtons';
import { IdField } from '../components/IdField';
import { NameField } from '../components/NameField';
import { Page } from '../components/Page';
import { AttributeSetIcon } from './AttributeSetIcon';
import { AttributeSetCreateForm } from './AttributeSetCreate';

export const AttributeSetList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.attributeSets.list.title')}
                secondaryText={translate('page.attributeSets.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<AttributeSetListActions />}
                filters={AttributeSetFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
                filter={{ system: true }}
            >
                <AttributeSetListView />
            </List>
        </Page>
    );
};

export const AttributeSetListView = (props: {
    actions?: ReactElement | boolean;
}) => {
    const { actions: actionProps = true } = props;

    const actions = !actionProps ? (
        false
    ) : isValidElement(actionProps) ? (
        actionProps
    ) : (
        <ActionsButtons />
    );

    return (
        <Datagrid bulkActionButtons={false} rowClick="edit">
            <NameField
                text="name"
                secondaryText="identifier"
                source="name"
                label="field.name.name"
                icon={<AttributeSetIcon color={'secondary'} />}
            />
            <IdField source="id" label="field.id.name" />
            {actions !== false && actions}
        </Datagrid>
    );
};

const AttributeSetFilters = [<SearchInput source="q" alwaysOn />];

const AttributeSetListActions = () => {
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
                <AttributeSetCreateForm />
            </CreateInDialogButton>
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};
