import { Box } from '@mui/material';
import {
    List,
    Datagrid,
    TopToolbar,
    SearchInput,
    useTranslate,
    ExportButton,
} from 'react-admin';
import { CreateInDialogButton } from '@dslab/ra-dialog-crud';
import { UserCreateForm } from './UserCreate';
import { Page } from '../components/Page';
import { PageTitle } from '../components/PageTitle';
import { YamlExporter } from '../components/YamlExporter';
import { IdField } from '../components/IdField';
import { isValidElement, ReactElement } from 'react';
import { ActionsButtons } from '../components/ActionsButtons';
import { NameField } from '../components/NameField';
import { TagsField } from '../components/TagsField';

export const UserList = () => {
    const translate = useTranslate();
    return (
        <Page>
            <PageTitle
                text={translate('page.users.list.title')}
                secondaryText={translate('page.users.list.subtitle')}
            />
            <List
                empty={false}
                exporter={YamlExporter}
                actions={<UserListActions />}
                filters={UserFilters}
                sort={{ field: 'username', order: 'ASC' }}
                component={Box}
            >
                <UserListView />
            </List>
        </Page>
    );
};

export const UserListView = (props: { actions?: ReactElement | boolean }) => {
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
                text="username"
                secondaryText="id"
                tertiaryText="email"
                source="username"
                label="field.username.name"
            />
            <IdField source="subjectId" label="field.id.name" />
            <TagsField />
            {actions !== false && actions}
        </Datagrid>
    );
};

const UserFilters = [<SearchInput source="q" alwaysOn />];

const UserListActions = () => {
    return (
        <TopToolbar>
            <CreateInDialogButton
                fullWidth
                maxWidth={'md'}
                variant="contained"
                transform={transform}
            >
                <UserCreateForm />
            </CreateInDialogButton>
            <ExportButton variant="contained" />
        </TopToolbar>
    );
};

const transform = (data: any) => {
    return {
        ...data,
    };
};
