import {
    List,
    SearchInput,
    Datagrid,
    TopToolbar,
    useTranslate,
    useRecordContext,
    useResourceContext,
    DeleteButton,
} from 'react-admin';
import { Box } from '@mui/material';
import { YamlExporter } from '../components/YamlExporter';

import { isValidElement, ReactElement } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { DeveloperCreateForm, DeveloperEditForm } from './DeveloperCreate';
import {
    CreateInDialogButton,
    EditInDialogButton,
} from '@dslab/ra-dialog-crud';
import { PageTitle } from '../components/PageTitle';
import { IdField } from '../components/IdField';
import { NameField } from '../components/NameField';
import { Page } from '../components/Page';
import { TagsField } from '../components/TagsField';
import { DropDownButton } from '../components/DropdownButton';
import { RowButtonGroup } from '../components/RowButtonGroup';
import { AuthoritiesIcon } from '../components/AuthoritiesDialog';

export const DeveloperList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.developers.list.title')}
                secondaryText={translate('page.developers.list.subtitle')}
            />
            <List
                exporter={YamlExporter}
                actions={<DeveloperListActions />}
                filters={DeveloperFilters}
                sort={{ field: 'name', order: 'ASC' }}
                component={Box}
                empty={false}
            >
                <DeveloperListView />
            </List>
        </Page>
    );
};

export const DeveloperListView = (props: {
    actions?: ReactElement | boolean;
}) => {
    const { actions: actionProps = true } = props;

    const actions = !actionProps ? (
        false
    ) : isValidElement(actionProps) ? (
        actionProps
    ) : (
        <DeveloperActionsButtons />
    );

    return (
        <Datagrid bulkActionButtons={false} rowClick={false}>
            <NameField
                text="username"
                secondaryText="realm"
                tertiaryText="email"
                // icon={<DeveloperIcon color={'secondary'} />}
                source="name"
                label="field.name.name"
                sortable={false}
            />
            <IdField
                source="subjectId"
                label="field.id.name"
                sortable={false}
            />
            <TagsField />
            {actions !== false && actions}
        </Datagrid>
    );
};

export const DeveloperActionsButtons = () => {
    const record = useRecordContext();

    if (!record) {
        return null;
    }

    return (
        <RowButtonGroup label="⋮">
            <DropDownButton>
                <EditInDialogButton
                    label="action.authorities"
                    icon={<AuthoritiesIcon />}
                >
                    <DeveloperEditForm />
                </EditInDialogButton>
                <DeleteButton redirect={false} />
            </DropDownButton>
        </RowButtonGroup>
    );
};

export const DeveloperFilters = [<SearchInput source="q" alwaysOn />];

export const DeveloperListActions = () => {
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
                label="ra.action.add"
            >
                <DeveloperCreateForm />
            </CreateInDialogButton>
        </TopToolbar>
    );
};
