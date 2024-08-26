import {
    List,
    SearchInput,
    Datagrid,
    TopToolbar,
    useTranslate,
    ExportButton,
    ArrayField,
    ChipField,
    SingleFieldList,
    FunctionField,
} from 'react-admin';
import { Box } from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';

import { isValidElement, ReactElement } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { ServiceCreateForm } from './ServiceCreate';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { PageTitle } from '../components/PageTitle';
import { ActionsButtons } from '../components/ActionsButtons';
import { IdField } from '../components/IdField';
import { ServiceIcon } from './ServiceIcon';
import { NameField } from '../components/NameField';
import { Page } from '../components/Page';

export const ServiceList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.services.list.title')}
                secondaryText={translate('page.services.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<ServiceListActions />}
                filters={ServiceFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
            >
                <ServiceListView />
            </List>
        </Page>
    );
};

export const ServiceListView = (props: {
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
                secondaryText="namespace"
                tertiaryText="description"
                source="name"
                label="field.name.name"
                icon={<ServiceIcon color={'secondary'} />}
            />
            <IdField source="serviceId" label="field.id.name" />
            <ArrayField source="scopes" label="field.scopes.name">
                <SingleFieldList linkType={false}>
                    <FunctionField
                        render={s => <ChipField source="scope" size="small" />}
                    />
                </SingleFieldList>
            </ArrayField>
            {actions !== false && actions}
        </Datagrid>
    );
};

const ServiceFilters = [<SearchInput source="q" alwaysOn />];

const ServiceListActions = () => {
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
                <ServiceCreateForm />
            </CreateInDialogButton>
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};
