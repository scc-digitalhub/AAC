import {
    List,
    SearchInput,
    Datagrid,
    TopToolbar,
    useTranslate,
    NumberField,
    ExportButton,
    ListView,
    DeleteWithConfirmButton,
    useRecordContext,
} from 'react-admin';
import { Box, Stack } from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';

import React, { isValidElement, ReactElement } from 'react';
import {
    RootSelectorButton,
    RootSelectorListProps,
    useRootSelector,
} from '@dslab/ra-root-selector';
import { RealmCreateForm } from './RealmCreate';
import {
    CreateInDialogButton,
    EditInDialogButton,
} from '@dslab/ra-dialog-crud';
import { PageTitle } from '../components/PageTitle';
import { ActionsButtons } from '../components/ActionsButtons';
import { IdField } from '../components/IdField';
import { RealmIcon } from './RealmIcon';
import { NameField } from '../components/NameField';
import { Page } from '../components/Page';
import {
    DeleteWithConfirmDialog,
    DeleteWithDialogButton,
} from '@dslab/ra-delete-dialog-button';
import { RowButtonGroup } from '../components/RowButtonGroup';

export const RealmList = () => {
    console.log('list');
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.realms.list.title')}
                secondaryText={translate('page.realms.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<RealmListActions />}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
            >
                <RealmListView />
            </List>
        </Page>
    );
};
export const RealmSelectorList = (props: RootSelectorListProps) => {
    console.log('selector');
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.realms.list.title')}
                secondaryText={translate('page.realms.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<RealmListActions />}
                filters={RealmFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
            >
                <RealmListView actions={<RealmSelectorActions />} />
            </List>
        </Page>
    );
};
export const RealmListView = (props: { actions?: ReactElement | boolean }) => {
    const { actions: actionProps = true } = props;
    const { selectRoot } = useRootSelector();
    const actions = !actionProps ? (
        false
    ) : isValidElement(actionProps) ? (
        actionProps
    ) : (
        <RootSelectorButton variant="contained" label="action.select" />
    );

    return (
        <Datagrid
            bulkActionButtons={false}
            rowClick={(id, resource, record) => {
                selectRoot(record);
                return false;
            }}
        >
            <NameField
                text="name"
                secondaryText="realm"
                source="name"
                icon={<RealmIcon color={'secondary'} />}
            />
            <IdField source="slug" label="slug" />
            {actions !== false && actions}
        </Datagrid>
    );
};

const RealmSelectorActions = () => {
    const record = useRecordContext();
    console.log('rs', record);
    return (
        <RowButtonGroup label="⋮">
            <Stack direction={'row'} columnGap={1}>
                <RootSelectorButton variant="contained" label="action.select" />
                {record?.editable && (
                    <EditInDialogButton variant="contained">
                        <RealmCreateForm />
                    </EditInDialogButton>
                )}
                {record?.editable && (
                    <DeleteWithDialogButton variant="contained" />
                )}
            </Stack>
        </RowButtonGroup>
    );
};

const RealmFilters = [<SearchInput source="q" alwaysOn />];

const RealmListActions = () => {
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
                <RealmCreateForm />
            </CreateInDialogButton>
        </TopToolbar>
    );
};
