import {
    List,
    SearchInput,
    Datagrid,
    TopToolbar,
    Button,
    useNotify,
    useTranslate,
    BooleanInput,
    Create,
    SimpleForm,
    SaveButton,
    Toolbar,
    NumberField,
    ExportButton,
} from 'react-admin';
import {
    Box,
    Dialog,
    DialogContent,
    DialogTitle,
    Typography,
} from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';
import { AceEditorInput } from '@dslab/ra-ace-editor';

import ImportExportIcon from '@mui/icons-material/ImportExport';
import React, { isValidElement, ReactElement } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { GroupCreateForm } from './GroupCreate';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { PageTitle } from '../components/PageTitle';
import { ActionsButtons } from '../components/ActionsButtons';
import { IdField } from '../components/IdField';
import { GroupIcon } from './GroupIcon';
import { NameField } from '../components/NameField';
import { Page } from '../components/Page';

export const GroupList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.group.list.title')}
                secondaryText={translate('page.group.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<GroupListActions />}
                filters={GroupFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
            >
                <GroupListView />
            </List>
        </Page>
    );
};

export const GroupListView = (props: { actions?: ReactElement | boolean }) => {
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
                secondaryText="group"
                source="name"
                icon={<GroupIcon color={'secondary'} />}
            />
            <IdField source="groupId" label="id" />
            <NumberField source="size" label={'members'} sortable={false} />
            {actions !== false && actions}
        </Datagrid>
    );
};

const GroupFilters = [<SearchInput source="q" alwaysOn />];

const GroupListActions = () => {
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
                maxWidth={'md'}
                variant="contained"
                transform={transform}
            >
                <GroupCreateForm />
            </CreateInDialogButton>
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};
